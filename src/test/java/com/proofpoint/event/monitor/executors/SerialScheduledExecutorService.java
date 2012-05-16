package com.proofpoint.event.monitor.executors;

import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.proofpoint.units.Duration;

import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SerialScheduledExecutorService
    extends SerialExecutorService
    implements ScheduledExecutorService
{
    class SerialScheduledFuture<T>
        implements ScheduledFuture<T>
    {
        long remainingDelayMillis;
        FutureTask<T> task;

        public SerialScheduledFuture(FutureTask<T> task, Duration delay)
        {
            this.task = task;
            this.remainingDelayMillis = (long)delay.toMillis();
        }

        public long remainingMillis()
        {
            return remainingDelayMillis;
        }

        // wind time off the clock, return the amount of used time in millis
        public long elapseTime(long quantumMillis)
        {
            if (task.isDone() || task.isCancelled()) {
                return 0;
            }

            if (remainingDelayMillis <= quantumMillis) {
                task.run();
                return remainingDelayMillis;
            }

            remainingDelayMillis -= quantumMillis;
            return quantumMillis;
        }

        public boolean isRecurring()
        {
            return false;
        }

        public void restartDelayTimer()
        {
            throw new UnsupportedOperationException("Can't restart a non-recurring task");
        }

        @Override
        public long getDelay(TimeUnit timeUnit)
        {
            return (long) (new Duration(remainingDelayMillis, TimeUnit.MILLISECONDS).convertTo(timeUnit));
        }

        @Override
        public int compareTo(Delayed delayed)
        {
            if (delayed instanceof SerialScheduledFuture) {
                SerialScheduledFuture other = (SerialScheduledFuture) delayed;
                return Longs.compare(this.remainingDelayMillis, other.remainingDelayMillis);
            }
            return Longs.compare(this.remainingMillis(), delayed.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public boolean cancel(boolean b)
        {
            return task.cancel(b);
        }

        @Override
        public boolean isCancelled()
        {
            return task.isCancelled();
        }

        @Override
        public boolean isDone()
        {
            return task.isDone();
        }

        @Override
        public T get()
                throws InterruptedException, ExecutionException
        {
            if (isCancelled()) {
                throw new CancellationException();
            }

            if (!isDone()) {
                throw new IllegalStateException("Called get() before result was available in SerialScheduledFuture");
            }

            return task.get();
        }

        @Override
        public T get(long l, TimeUnit timeUnit)
                throws InterruptedException, ExecutionException, TimeoutException
        {
            return get();
        }
    }

    class RecurringRunnableSerialScheduledFuture<T>
        extends SerialScheduledFuture<T>
    {
        private final Duration recurringDelay;
        private final Runnable runnable;
        private final T value;

        RecurringRunnableSerialScheduledFuture(Runnable runnable, T value, Duration initialDelay, Duration recurringDelay)
        {
            super(new FutureTask<T>(runnable, value), initialDelay);
            this.runnable = runnable;
            this.value = value;
            this.recurringDelay = recurringDelay;
        }

        @Override
        public boolean isRecurring()
        {
            return true;
        }

        @Override
        public void restartDelayTimer()
        {
            task = new FutureTask<T>(runnable, value);
            remainingDelayMillis = (long)recurringDelay.toMillis();
        }
    }

    private final PriorityQueue<SerialScheduledFuture<?>> tasks = new PriorityQueue<SerialScheduledFuture<?>>();

    public void elapseTime(Duration quantumDuration)
    {
        elapseTime((long) quantumDuration.toMillis());
    }

    private void elapseTime(long quantum)
    {
        List<SerialScheduledFuture<?>> toRequeue = Lists.newArrayList();

        while (tasks.peek() != null) {
            SerialScheduledFuture<?> current = tasks.poll();

            if (current.isCancelled()) {
                // Drop cancelled tasks
                continue;
            }

            if (current.isDone()) {
                // This isn't right - there shouldn't be done tasks in the queue
                throw new IllegalStateException("Found a done task in the queue (contrary to expectation)");
            }

            // Try to elapse the time quantum off the current item
            long used = current.elapseTime(quantum);

            // If the item isn't done yet, we'll need to put it back in the queue
            if (!current.isDone()) {
                toRequeue.add(current);
                continue;
            }

            if (used < quantum) {
                // Partially used the quantum. Elapse the used portion off the rest of the queue so that we can reinsert
                // this item in its correct spot (if necessary) before continuing with the rest of the quantum. This is
                // because tasks may execute more than once during a single call to elapse time.
                elapseTime(used);
                rescheduleTaskIfRequired(tasks, current);
                quantum -= used;
            }
            else {
                // Completely used the quantum, once we're done with this pass through the queue, may want need to add it back
                rescheduleTaskIfRequired(toRequeue, current);
            }
        }
        for (SerialScheduledFuture<?> future : toRequeue) {
            tasks.add(future);
        }
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable, long l, TimeUnit timeUnit)
    {
        SerialScheduledFuture<?> future = new SerialScheduledFuture<Void>(new FutureTask<Void>(runnable, null), new Duration(l, timeUnit));
        tasks.add(future);
        return future;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> vCallable, long l, TimeUnit timeUnit)
    {
        SerialScheduledFuture<V> future = new SerialScheduledFuture<V>(new FutureTask<V>(vCallable), new Duration(l, timeUnit));
        tasks.add(future);
        return future;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long l, long l1, TimeUnit timeUnit)
    {
        SerialScheduledFuture<?> future = new RecurringRunnableSerialScheduledFuture<Void>(runnable, null, new Duration(l, timeUnit), new Duration(l1, timeUnit));
        tasks.add(future);
        return future;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long l, long l1, TimeUnit timeUnit)
    {
        SerialScheduledFuture<?> future = new RecurringRunnableSerialScheduledFuture<Void>(runnable, null, new Duration(l, timeUnit), new Duration(l1, timeUnit));
        tasks.add(future);
        return future;
    }

    private static void rescheduleTaskIfRequired(Collection<SerialScheduledFuture<?>> tasks, SerialScheduledFuture<?> task)
    {
        if (task.isRecurring()) {
            task.restartDelayTimer();
            tasks.add(task);
        }
    }
}
