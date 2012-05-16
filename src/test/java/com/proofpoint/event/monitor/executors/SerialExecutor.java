package com.proofpoint.event.monitor.executors;

import java.util.concurrent.Executor;

public class SerialExecutor
    implements Executor
{
    @Override
    public void execute(Runnable runnable)
    {
        runnable.run();
    }
}
