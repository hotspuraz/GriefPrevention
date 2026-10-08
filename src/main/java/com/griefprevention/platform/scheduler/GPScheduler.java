package com.griefprevention.platform.scheduler;

import com.griefprevention.platform.PlatformDetection;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

/**
 * Scheduling facade that works on Folia (region-threaded) as well as Paper and Spigot.
 *
 * <p>On Folia there is no main thread. Work must be run on the thread that owns the data it touches:</p>
 * <ul>
 *     <li>global - server-wide work that does not touch world state (data stores, console commands,
 *     broadcasts).</li>
 *     <li>entity - work involving a specific entity or player.</li>
 *     <li>chunk/location - work involving blocks or chunks at a position.</li>
 *     <li>async - work that must not block any tick thread.</li>
 * </ul>
 *
 * <p>On every other platform all of the synchronous variants run on the main thread and behave exactly like the
 * equivalent {@link BukkitScheduler} calls.</p>
 */
public final class GPScheduler
{

    private static final boolean FOLIA = PlatformDetection.classExists("io.papermc.paper.threadedregions.RegionizedServer");

    private GPScheduler()
    {
    }

    /**
     * Check if the server is running Folia.
     *
     * @return true if running on Folia
     */
    public static boolean isFolia()
    {
        return FOLIA;
    }

    private static @NotNull Plugin plugin()
    {
        return GriefPrevention.instance;
    }

    /** Run on the next tick of the global scheduler. */
    public static @NotNull TaskHandle runGlobal(@NotNull Runnable task)
    {
        return runGlobalLater(task, 1L);
    }

    /** Run on the global scheduler after a delay in ticks. Delays below 1 are treated as 1. */
    public static @NotNull TaskHandle runGlobalLater(@NotNull Runnable task, long delayTicks)
    {
        delayTicks = Math.max(1L, delayTicks);
        if (FOLIA) return FoliaSchedulers.globalLater(plugin(), task, delayTicks);
        return new BukkitHandle(Bukkit.getScheduler().runTaskLater(plugin(), task, delayTicks).getTaskId());
    }

    /** Run repeatedly on the global scheduler. */
    public static @NotNull TaskHandle runGlobalTimer(@NotNull Runnable task, long delayTicks, long periodTicks)
    {
        delayTicks = Math.max(1L, delayTicks);
        periodTicks = Math.max(1L, periodTicks);
        if (FOLIA) return FoliaSchedulers.globalTimer(plugin(), task, delayTicks, periodTicks);
        return new BukkitHandle(Bukkit.getScheduler().runTaskTimer(plugin(), task, delayTicks, periodTicks).getTaskId());
    }

    /** Run on the thread that owns an entity after a delay in ticks. Delays below 1 are treated as 1. */
    public static @NotNull TaskHandle runForEntity(@NotNull Entity entity, @NotNull Runnable task, long delayTicks)
    {
        delayTicks = Math.max(1L, delayTicks);
        if (FOLIA) return FoliaSchedulers.entityLater(plugin(), entity, task, delayTicks);
        return new BukkitHandle(Bukkit.getScheduler().runTaskLater(plugin(), task, delayTicks).getTaskId());
    }

    /** Run on the thread that owns a chunk after a delay in ticks. Delays below 1 are treated as 1. */
    public static @NotNull TaskHandle runAtChunk(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable task, long delayTicks)
    {
        delayTicks = Math.max(1L, delayTicks);
        if (FOLIA) return FoliaSchedulers.chunkLater(plugin(), world, chunkX, chunkZ, task, delayTicks);
        return new BukkitHandle(Bukkit.getScheduler().runTaskLater(plugin(), task, delayTicks).getTaskId());
    }

    /** Run on the thread that owns a location after a delay in ticks. Delays below 1 are treated as 1. */
    public static @NotNull TaskHandle runAtLocation(@NotNull Location location, @NotNull Runnable task, long delayTicks)
    {
        World world = location.getWorld();
        if (world == null) throw new IllegalArgumentException("Location has no world");
        return runAtChunk(world, location.getBlockX() >> 4, location.getBlockZ() >> 4, task, delayTicks);
    }

    /** Run off of any tick thread as soon as possible. */
    public static @NotNull TaskHandle runAsync(@NotNull Runnable task)
    {
        if (FOLIA) return FoliaSchedulers.asyncNow(plugin(), task);
        return new BukkitHandle(Bukkit.getScheduler().runTaskAsynchronously(plugin(), task).getTaskId());
    }

    /** Run repeatedly off of any tick thread. */
    public static @NotNull TaskHandle runAsyncTimer(@NotNull Runnable task, long delayTicks, long periodTicks)
    {
        delayTicks = Math.max(1L, delayTicks);
        periodTicks = Math.max(1L, periodTicks);
        if (FOLIA) return FoliaSchedulers.asyncTimer(plugin(), task, delayTicks, periodTicks);
        return new BukkitHandle(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin(), task, delayTicks, periodTicks).getTaskId());
    }

    /**
     * Teleport an entity. Uses asynchronous teleportation on Folia, where synchronous teleportation is unsupported.
     * On Folia the teleport completes after this method returns.
     *
     * @param entity the entity to teleport
     * @param destination the destination
     */
    public static void teleport(@NotNull Entity entity, @NotNull Location destination)
    {
        if (FOLIA) entity.teleportAsync(destination);
        else entity.teleport(destination);
    }

    /**
     * Run a task on the global thread, immediately if the current thread already is the global thread (the main
     * thread on non-Folia platforms), otherwise on the next global tick.
     *
     * @param task the task
     */
    public static void ensureGlobal(@NotNull Runnable task)
    {
        if (FOLIA ? FoliaSchedulers.isGlobalThread() : Bukkit.isPrimaryThread())
        {
            task.run();
            return;
        }
        runGlobal(task);
    }

    private record BukkitHandle(int taskId) implements TaskHandle
    {
        @Override
        public void cancel()
        {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        @Override
        public boolean isPending()
        {
            return Bukkit.getScheduler().isQueued(taskId);
        }
    }

}
