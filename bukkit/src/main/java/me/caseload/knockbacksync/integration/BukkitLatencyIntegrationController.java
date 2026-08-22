package me.caseload.knockbacksync.integration;

import me.caseload.knockbacksync.latency.LatencyProvider;
import me.caseload.knockbacksync.latency.LatencyProviderMode;
import me.caseload.knockbacksync.latency.LatencyService;
import me.caseload.knockbacksync.manager.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;

/**
 * Grim-free classloading boundary. The implementation containing Grim API
 * symbols is loaded by name only after Bukkit confirms GrimAC is enabled.
 */
public final class BukkitLatencyIntegrationController implements Listener {
    private static final String GRIM_PLUGIN_NAME = "GrimAC";
    private static final String GRIM_PROVIDER_CLASS =
            "me.caseload.knockbacksync.integration.grim.GrimLatencyProvider";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final LatencyService latencyService;

    private LatencyProvider grimProvider;
    private boolean enabled;

    public BukkitLatencyIntegrationController(JavaPlugin plugin, ConfigManager configManager,
                                               LatencyService latencyService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.latencyService = latencyService;
    }

    public void enable() {
        if (enabled) {
            return;
        }
        enabled = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        reconfigure();
    }

    public void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        PluginDisableEvent.getHandlerList().unregister(this);
        PluginEnableEvent.getHandlerList().unregister(this);
        stopGrimIntegration();
    }

    public void reconfigure() {
        if (!enabled) {
            return;
        }

        LatencyProviderMode mode = LatencyProviderMode.fromConfig(
                configManager.getConfigWrapper().getString("latency.provider", LatencyProviderMode.AUTO.name())
        );
        if (mode == LatencyProviderMode.PACKET_EVENTS) {
            stopGrimIntegration();
            plugin.getLogger().info("Latency provider: PacketEvents");
            return;
        }

        Plugin grim = plugin.getServer().getPluginManager().getPlugin(GRIM_PLUGIN_NAME);
        if (grim == null || !grim.isEnabled()) {
            stopGrimIntegration();
            if (mode == LatencyProviderMode.GRIM) {
                plugin.getLogger().warning("Grim latency provider requested, but GrimAC is not enabled; using PacketEvents.");
            }
            return;
        }

        boolean replacementAttempted = false;
        try {
            Class<?> implementation = Class.forName(
                    GRIM_PROVIDER_CLASS, true, BukkitLatencyIntegrationController.class.getClassLoader()
            );
            Constructor<?> constructor = implementation.getConstructor(Object.class);
            Object instance = constructor.newInstance(plugin);
            if (!(instance instanceof LatencyProvider)) {
                throw new IllegalStateException(GRIM_PROVIDER_CLASS + " does not implement LatencyProvider");
            }

            LatencyProvider replacement = (LatencyProvider) instance;
            replacementAttempted = true;
            latencyService.replaceProvider(replacement);
            grimProvider = replacement;
            plugin.getLogger().info("Latency provider: Grim transaction ping (PacketEvents fallback for untracked players)");
        } catch (Throwable throwable) {
            if (replacementAttempted) {
                grimProvider = null;
            }
            plugin.getLogger().warning("Unable to enable Grim latency integration"
                    + (grimProvider == null ? "; using PacketEvents: " : "; keeping the current provider: ")
                    + rootCauseMessage(throwable));
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (GRIM_PLUGIN_NAME.equals(event.getPlugin().getName())) {
            reconfigure();
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (GRIM_PLUGIN_NAME.equals(event.getPlugin().getName())) {
            stopGrimIntegration();
        }
    }

    private void stopGrimIntegration() {
        if (grimProvider == null) {
            return;
        }
        grimProvider = null;
        latencyService.deactivate();
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
