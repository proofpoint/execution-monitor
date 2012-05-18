package com.proofpoint.event.monitor.s3;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.proofpoint.event.monitor.EventStore;

import static com.proofpoint.configuration.ConfigurationModule.bindConfig;

public class S3Module
    implements Module
{
    @Override
    public void configure(Binder binder)
    {
        binder.requireExplicitBindings();
        binder.disableCircularProxies();

        bindConfig(binder).to(S3Config.class);

        binder.bind(EventStore.class).to(S3EventStore.class).in(Scopes.SINGLETON);
        binder.bind(S3StorageSystem.class).in(Scopes.SINGLETON);
    }
}
