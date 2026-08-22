package me.caseload.knockbacksync.integration;

import me.caseload.knockbacksync.latency.LatencyProvider;
import me.caseload.knockbacksync.latency.LatencyProviderMode;
import me.caseload.knockbacksync.latency.LatencyService;
import me.caseload.knockbacksync.manager.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Constructor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Grim-free Fabric classloading boundary. Grim's API implementation is loaded
 * only after Fabric Loader confirms that the Grim mod is present.
 */
public final class FabricLatencyIntegrationController {
    private static final String KBS_MOD_ID = "knockbacksync";
    private static final String GRIM_MOD_ID = "grimac";
    private static final String GRIM_FABRIC_MOD_PREFIX = "grimac-fabric-";
    private static final String GRIM_PROVIDER_CLASS =
            "me.caseload.knockbacksync.integration.grim.GrimLatencyProvider";

    private final Supplier<LatencyProviderMode> modeSupplier;
    private final LatencyService latencyService;
    private final Logger logger;
    private final BooleanSupplier grimAvailable;
    private final ProviderFactory providerFactory;

    private LatencyProvider grimProvider;
    private boolean enabled;

    public FabricLatencyIntegrationController(ConfigManager configManager, LatencyService latencyService,
                                              Logger logger) {
        this(configManager::getLatencyProviderMode, latencyService, logger,
                FabricLatencyIntegrationController::isGrimModLoaded,
                FabricLatencyIntegrationController::createGrimProvider);
    }

    FabricLatencyIntegrationController(Supplier<LatencyProviderMode> modeSupplier, LatencyService latencyService,
                                       Logger logger, BooleanSupplier grimAvailable,
                                       ProviderFactory providerFactory) {
        this.modeSupplier = modeSupplier;
        this.latencyService = latencyService;
        this.logger = logger;
        this.grimAvailable = grimAvailable;
        this.providerFactory = providerFactory;
    }

    public synchronized void enable() {
        if (enabled) {
            return;
        }
        enabled = true;
        reconfigure();
    }

    public synchronized void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        stopGrimIntegration();
    }

    public synchronized void reconfigure() {
        if (!enabled) {
            return;
        }

        LatencyProviderMode configuredMode = modeSupplier.get();
        LatencyProviderMode mode = configuredMode == null ? LatencyProviderMode.AUTO : configuredMode;
        if (mode == LatencyProviderMode.PACKET_EVENTS) {
            stopGrimIntegration();
            logger.info("Latency provider: PacketEvents");
            return;
        }

        if (!grimAvailable.getAsBoolean()) {
            stopGrimIntegration();
            if (mode == LatencyProviderMode.GRIM) {
                logger.warning("Grim latency provider requested, but the GrimAC Fabric mod is not loaded; using PacketEvents.");
            }
            return;
        }

        boolean replacementAttempted = false;
        try {
            LatencyProvider replacement = providerFactory.create();
            replacementAttempted = true;
            latencyService.replaceProvider(replacement);
            grimProvider = replacement;
            logger.info("Latency provider: Grim transaction ping (PacketEvents fallback for untracked players)");
        } catch (Throwable throwable) {
            if (replacementAttempted) {
                grimProvider = null;
            }
            logger.warning("Unable to enable Grim latency integration"
                    + (grimProvider == null ? "; using PacketEvents: " : "; keeping the current provider: ")
                    + rootCauseMessage(throwable));
        }
    }

    private void stopGrimIntegration() {
        if (grimProvider == null) {
            return;
        }
        grimProvider = null;
        latencyService.deactivate();
    }

    private static boolean isGrimModLoaded() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> mod.getMetadata().getId())
                .anyMatch(id -> GRIM_MOD_ID.equals(id) || id.startsWith(GRIM_FABRIC_MOD_PREFIX));
    }

    private static LatencyProvider createGrimProvider() throws Exception {
        Class<?> implementation = Class.forName(
                GRIM_PROVIDER_CLASS, true, FabricLatencyIntegrationController.class.getClassLoader()
        );
        Constructor<?> constructor = implementation.getConstructor(Object.class);
        Object instance = constructor.newInstance(KBS_MOD_ID);
        if (!(instance instanceof LatencyProvider)) {
            throw new IllegalStateException(GRIM_PROVIDER_CLASS + " does not implement LatencyProvider");
        }
        return (LatencyProvider) instance;
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }

    @FunctionalInterface
    interface ProviderFactory {
        LatencyProvider create() throws Exception;
    }
}
