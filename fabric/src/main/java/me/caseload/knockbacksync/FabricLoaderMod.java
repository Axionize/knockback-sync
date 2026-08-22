package me.caseload.knockbacksync;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.server.MinecraftServer;

public class FabricLoaderMod implements PreLaunchEntrypoint, ModInitializer {

    private final FabricBase core = new FabricBase();

    public static MinecraftServer getServer() {
        return (MinecraftServer) FabricLoader.getInstance().getGameInstance();
    }

    @Override
    public void onPreLaunch() {
        ensureServer();
        core.load();
    }

    @Override
    public void onInitialize() {
        ensureServer();
        core.enable();
        // All mod initializers have completed before this fires, so Grim's API
        // provider is available regardless of Fabric entrypoint ordering.
        ServerLifecycleEvents.SERVER_STARTING.register((server) -> core.enableLatencyIntegration());
        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
            core.disable();
            core.getScheduler().shutdown();
            // bstats removal
//            core.statsManager.getMetrics().shutdown();
        });
    }

    private void ensureServer() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            throw new IllegalStateException("This mod can only be run on servers");
        }
    }
}
