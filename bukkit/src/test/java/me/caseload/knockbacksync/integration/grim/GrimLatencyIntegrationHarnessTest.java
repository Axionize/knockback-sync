package me.caseload.knockbacksync.integration.grim;

import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.event.events.GrimJoinEvent;
import ac.grim.grimac.api.event.events.GrimQuitEvent;
import ac.grim.grimac.api.event.events.GrimTransactionReceivedEvent;
import ac.grim.grimac.api.plugin.GrimPlugin;
import me.caseload.knockbacksync.latency.LatencyService;
import me.caseload.knockbacksync.latency.LatencyTarget;
import me.caseload.knockbacksync.player.PingSampleState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * API-faithful integration harness: real published Grim event channels drive
 * the production Grim provider and common KBS latency service end to end.
 */
class GrimLatencyIntegrationHarnessTest {
    @Test
    void grimTransactionEventBecomesKbsSampleAndPreventsSyntheticSend() throws Exception {
        UUID playerId = UUID.randomUUID();
        HarnessTarget target = new HarnessTarget(playerId);
        Map<UUID, HarnessTarget> targets = Collections.singletonMap(playerId, target);
        Map<UUID, GrimUser> users = new HashMap<>();

        GrimTransactionReceivedEvent.Channel transactions = new GrimTransactionReceivedEvent.Channel();
        GrimJoinEvent.Channel joins = new GrimJoinEvent.Channel();
        GrimQuitEvent.Channel quits = new GrimQuitEvent.Channel();
        Map<Class<?>, Object> channels = new HashMap<>();
        channels.put(GrimTransactionReceivedEvent.class, transactions);
        channels.put(GrimJoinEvent.class, joins);
        channels.put(GrimQuitEvent.class, quits);

        EventBus eventBus = proxy(EventBus.class, (method, args) -> {
            if ("get".equals(method)) {
                return channels.get(args[0]);
            }
            return defaultValue(methodReturnType(EventBus.class, method, args));
        });
        GrimPlugin grimPlugin = proxy(GrimPlugin.class,
                (method, args) -> defaultValue(methodReturnType(GrimPlugin.class, method, args)));
        GrimAbstractAPI api = proxy(GrimAbstractAPI.class, (method, args) -> {
            if ("getEventBus".equals(method)) {
                return eventBus;
            }
            if ("getGrimUser".equals(method) && args != null && args.length == 1 && args[0] instanceof UUID) {
                return users.get(args[0]);
            }
            if ("getGrimPlugin".equals(method)) {
                return grimPlugin;
            }
            return defaultValue(methodReturnType(GrimAbstractAPI.class, method, args));
        });

        AtomicInteger transactionPing = new AtomicInteger();
        AtomicInteger receivedTransactions = new AtomicInteger();
        GrimUser grimUser = proxy(GrimUser.class, (method, args) -> {
            switch (method) {
                case "getUniqueId":
                    return playerId;
                case "getTransactionPing":
                    return transactionPing.get();
                case "getLastTransactionReceived":
                    return receivedTransactions.get();
                default:
                    return defaultValue(methodReturnType(GrimUser.class, method, args));
            }
        });
        users.put(playerId, grimUser);

        Collection<HarnessTarget> targetValues = targets.values();
        LatencyService service = new LatencyService(targets::get, () -> targetValues);
        GrimLatencyProvider provider = new GrimLatencyProvider(new Object(), api);
        service.replaceProvider(provider);

        assertTrue(target.external, "existing Grim users are selected during provider startup");
        assertNull(target.samples.getPing(), "zero before Grim's first transaction is not treated as a sample");
        assertEquals(83.0, target.samples.getPingOr(83.0), "platform ping remains the initial fallback");

        transactionPing.set(47);
        receivedTransactions.incrementAndGet();
        transactions.fire(grimUser, -7, true, System.currentTimeMillis());

        assertEquals(47.0, target.samples.getPing());
        AtomicInteger syntheticSends = new AtomicInteger();
        assertFalse(service.runSyntheticPingIfRequired(target, syntheticSends::incrementAndGet));
        assertEquals(0, syntheticSends.get());

        users.remove(playerId);
        quits.fire(grimUser);
        assertFalse(target.external);
        assertNull(target.samples.getPing());
        assertTrue(service.runSyntheticPingIfRequired(target, syntheticSends::incrementAndGet));

        users.put(playerId, grimUser);
        joins.fire(grimUser);
        assertTrue(target.external);
        assertEquals(47.0, target.samples.getPing());

        service.deactivate();
        assertFalse(target.external);
        transactionPing.set(190);
        transactions.fire(grimUser, -8, true, System.currentTimeMillis());
        assertNull(target.samples.getPing(), "disable must unsubscribe the Grim event handler");
    }

    private interface Invocation {
        Object invoke(String method, Object[] args) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                switch (method.getName()) {
                    case "toString":
                        return "HarnessProxy(" + type.getSimpleName() + ")";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    default:
                        return null;
                }
            }
            return invocation.invoke(method.getName(), args);
        });
    }

    private static Class<?> methodReturnType(Class<?> type, String methodName, Object[] args) {
        int argumentCount = args == null ? 0 : args.length;
        for (java.lang.reflect.Method method : type.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == argumentCount) {
                return method.getReturnType();
            }
        }
        return Object.class;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }

    private static final class HarnessTarget implements LatencyTarget {
        private final UUID id;
        private final PingSampleState samples = new PingSampleState();
        private boolean external;

        private HarnessTarget(UUID id) {
            this.id = id;
        }

        @Override
        public UUID getUniqueId() {
            return id;
        }

        @Override
        public boolean setExternalLatencyActive(boolean active) {
            if (external == active) return false;
            external = active;
            samples.reset();
            return true;
        }

        @Override
        public void recordPingSample(double pingMillis) {
            samples.record(pingMillis);
        }
    }
}
