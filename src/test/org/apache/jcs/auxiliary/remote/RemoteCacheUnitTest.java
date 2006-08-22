package org.apache.jcs.auxiliary.remote;

import junit.framework.TestCase;

import org.apache.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.jcs.engine.CacheElement;
import org.apache.jcs.engine.behavior.ICacheElement;
import org.apache.jcs.engine.behavior.ICacheElementSerialized;
import org.apache.jcs.utils.serialization.SerializationConversionUtil;

/**
 * Unit Tests for the Remote Cache.
 * <p>
 * @author admin
 */
public class RemoteCacheUnitTest
    extends TestCase
{
    /**
     * Verify that the remote service update method is called. The remote cache serializes the obect
     * first.
     * <p>
     * @throws Exception
     */
    public void testUpdate()
        throws Exception
    {
        // SETUP
        IRemoteCacheAttributes cattr = new RemoteCacheAttributes();
        RemoteCacheServiceMockImpl service = new RemoteCacheServiceMockImpl();
        RemoteCacheListenerMockImpl listener = new RemoteCacheListenerMockImpl();

        RemoteCache remoteCache = new RemoteCache( cattr, service, listener );

        String cacheName = "testUpdate";

        // DO WORK
        ICacheElement element = new CacheElement( cacheName, "key", "value" );
        remoteCache.update( element );

        // VERIFY
        assertTrue( "The element should be in the serialized warapper.",
                    service.lastUpdate instanceof ICacheElementSerialized );
        ICacheElement result = SerializationConversionUtil
            .getDeSerializedCacheElement( (ICacheElementSerialized) service.lastUpdate, remoteCache
                .getElementSerializer() );
        assertEquals( "Wrong element updated.", element.getVal(), result.getVal() );
    }
    
    /**
     * Verify that when we call fix events queued in the zombie are propagated to the new service.
     * <p>
     * @throws Exception
     */
    public void testUpdateZombieThenFix()
        throws Exception
    {
        // SETUP
        IRemoteCacheAttributes cattr = new RemoteCacheAttributes();
        ZombieRemoteCacheService zombie = new ZombieRemoteCacheService( 10 );        
        RemoteCacheServiceMockImpl service = new RemoteCacheServiceMockImpl();
        RemoteCacheListenerMockImpl listener = new RemoteCacheListenerMockImpl();

        // set the zombir
        RemoteCache remoteCache = new RemoteCache( cattr, zombie, listener );

        String cacheName = "testUpdate";

        // DO WORK
        ICacheElement element = new CacheElement( cacheName, "key", "value" );
        remoteCache.update( element );
        // set the new service, this should call propogate
        remoteCache.fixCache( service );

        // VERIFY
        assertTrue( "The element should be in the serialized warapper.",
                    service.lastUpdate instanceof ICacheElementSerialized );
        ICacheElement result = SerializationConversionUtil
            .getDeSerializedCacheElement( (ICacheElementSerialized) service.lastUpdate, remoteCache
                .getElementSerializer() );
        assertEquals( "Wrong element updated.", element.getVal(), result.getVal() );
    }    
}
