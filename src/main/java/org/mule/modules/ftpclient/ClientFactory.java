package org.mule.modules.ftpclient;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

public abstract class ClientFactory<T extends ClientWrapper> implements PoolableObjectFactory<T> {
    private GenericObjectPool<T> pool;

    public void setPool(GenericObjectPool<T> pool) {
        this.pool = pool;
    }

    public GenericObjectPool<T> getPool() {
        return pool;
    }

    @Override
    public void destroyObject(T client) throws Exception {
        client.destroy();
    }

    @Override
    public boolean validateObject(T client) {
        return client.validate();
    }

    @Override
    public void activateObject(@SuppressWarnings("unused") T client) {
        // no op
    }

    @Override
    public void passivateObject(@SuppressWarnings("unused") T client) {
        // no op
    }
}
