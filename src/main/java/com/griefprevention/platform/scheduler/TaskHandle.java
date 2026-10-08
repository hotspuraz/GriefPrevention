package com.griefprevention.platform.scheduler;

/**
 * A handle to a scheduled task, abstracting over the Bukkit and Folia scheduler task types.
 */
public interface TaskHandle
{

    /**
     * Cancel the task. Has no effect if the task has already run or been cancelled.
     */
    void cancel();

    /**
     * Check if the task is still waiting to run.
     *
     * @return true if the task has neither run nor been cancelled
     */
    boolean isPending();

    /** A handle for a task that could not be scheduled, for example because its entity was removed. */
    TaskHandle NOOP = new TaskHandle()
    {
        @Override
        public void cancel()
        {
        }

        @Override
        public boolean isPending()
        {
            return false;
        }
    };

}
