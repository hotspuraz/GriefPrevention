package com.griefprevention.platform.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Folia scheduler bindings. Only loaded on Folia; never reference this class without checking
 * {@link GPScheduler#isFolia()} first.
 */
final class FoliaSchedulers
{

    private FoliaSchedulers()
    {
    }

    static @NotNull TaskHandle globalLater(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks)
    {
        return new FoliaHandle(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduled -> task.run(), delayTicks));
    }

    static @NotNull TaskHandle globalTimer(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks, long periodTicks)
    {
        return new FoliaHandle(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> task.run(), delayTicks, periodTicks));
    }

    static @NotNull TaskHandle entityLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task, long delayTicks)
    {
        // The retired callback is omitted: if the entity is removed first, the task is simply dropped.
        ScheduledTask scheduled = entity.getScheduler().runDelayed(plugin, ignored -> task.run(), null, delayTicks);
        return scheduled == null ? TaskHandle.NOOP : new FoliaHandle(scheduled);
    }

    static @NotNull TaskHandle chunkLater(@NotNull Plugin plugin, @NotNull World world, int chunkX, int chunkZ, @NotNull Runnable task, long delayTicks)
    {
        return new FoliaHandle(Bukkit.getRegionScheduler().runDelayed(plugin, world, chunkX, chunkZ, scheduled -> task.run(), delayTicks));
    }

    static @NotNull TaskHandle asyncNow(@NotNull Plugin plugin, @NotNull Runnable task)
    {
        return new FoliaHandle(Bukkit.getAsyncScheduler().runNow(plugin, scheduled -> task.run()));
    }

    static @NotNull TaskHandle asyncTimer(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks, long periodTicks)
    {
        return new FoliaHandle(Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin, scheduled -> task.run(), delayTicks * 50L, periodTicks * 50L, TimeUnit.MILLISECONDS));
    }

    static boolean isGlobalThread()
    {
        return Bukkit.isGlobalTickThread();
    }

    private record FoliaHandle(@NotNull ScheduledTask task) implements TaskHandle
    {
        @Override
        public void cancel()
        {
            task.cancel();
        }

        @Override
        public boolean isPending()
        {
            return task.getExecutionState() == ScheduledTask.ExecutionState.IDLE;
        }
    }

}
