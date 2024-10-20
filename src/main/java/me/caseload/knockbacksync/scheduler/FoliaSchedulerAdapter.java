// FoliaSchedulerAdapter.java
package me.caseload.knockbacksync.scheduler;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FoliaSchedulerAdapter implements SchedulerAdapter {
    private final Plugin plugin;
    private GlobalRegionScheduler scheduler = null;

    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        try {
            // Attempt to find and call the `getGlobalRegionScheduler` method
            Method getSchedulerMethod = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler");
            scheduler = (GlobalRegionScheduler) getSchedulerMethod.invoke(Bukkit.getServer());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().severe("Failed to access GlobalRegionScheduler: " + e.getMessage());
        }
    }

    @Override
    public AbstractTaskHandle runTask(Runnable task) {
//        scheduler.execute(plugin, task);
        return new AbstractTaskHandle(scheduler.run(plugin, scheduledTask -> task.run()));
    }

    @Override
    public AbstractTaskHandle runTaskAsynchronously(Runnable task) {
        return new AbstractTaskHandle(scheduler.run(plugin, scheduledTask -> task.run()));
    }

    @Override
    public AbstractTaskHandle runTaskLater(Runnable task, long delayTicks) {
        return new AbstractTaskHandle(scheduler.runDelayed(plugin, scheduledTask -> task.run(), delayTicks));
    }

    @Override
    public AbstractTaskHandle runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        return new AbstractTaskHandle(scheduler.runAtFixedRate(plugin, scheduledTask -> task.run(), delayTicks, periodTicks));
    }

    @Override
    public AbstractTaskHandle runTaskLaterAsynchronously(Runnable task, long delay) {
        return new AbstractTaskHandle(scheduler.runDelayed(plugin, scheduledTask -> task.run(), delay));
    }

    @Override
    public AbstractTaskHandle runTaskTimerAsynchronously(Runnable task, long delay, long period) {
        return new AbstractTaskHandle(scheduler.runAtFixedRate(plugin, scheduledTask -> task.run(), delay, period));
    }
}