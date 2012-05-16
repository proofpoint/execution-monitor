package com.proofpoint.event.monitor.executors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SerialExecutorService
    extends SerialExecutor
    implements ExecutorService
{
    private boolean isShutdown = false;

    @Override
    public void shutdown()
    {
        isShutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow()
    {
        shutdown();
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown()
    {
        return isShutdown;
    }

    @Override
    public boolean isTerminated()
    {
        return isShutdown();
    }

    @Override
    public boolean awaitTermination(long l, TimeUnit timeUnit)
            throws InterruptedException
    {
        return true;
    }

    @Override
    public <T> Future<T> submit(Callable<T> tCallable)
    {
        try {
            return Futures.immediateFuture(tCallable.call());
        }
        catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public <T> Future<T> submit(Runnable runnable, T t)
    {
        try {
            runnable.run();
            return Futures.immediateFuture(t);
        }
        catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public Future<?> submit(Runnable runnable)
    {
        return submit(runnable, null);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> callables)
            throws InterruptedException
    {
        ImmutableList.Builder<Future<T>> resultBuilder = ImmutableList.builder();
        for (Callable<T> callable : callables) {
            try {
                resultBuilder.add(Futures.immediateFuture(callable.call()));
            }
            catch (Exception e) {
                resultBuilder.add(Futures.<T>immediateFailedFuture(e));
            }
        }
        return resultBuilder.build();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> callables, long l, TimeUnit timeUnit)
            throws InterruptedException
    {
        return invokeAll(callables);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> callables)
            throws InterruptedException, ExecutionException
    {
        Preconditions.checkNotNull(callables, "callables is null");
        Preconditions.checkArgument(!callables.isEmpty(), "callables is empty");
        try {
            return callables.iterator().next().call();
        }
        catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> callables, long l, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException
    {
        return invokeAny(callables);
    }
}
