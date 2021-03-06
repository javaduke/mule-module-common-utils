/**
 * This file was automatically generated by the Mule Development Kit
 */
package com.mulesoft.module.utils;

import java.util.concurrent.BlockingQueue;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Payload;

/**
 * Common utils module
 *
 * @author MuleSoft, Inc.
 */
@Module(name="utils", schemaVersion="3.3.0")
public class UtilsModule
{
	protected static final Log logger = LogFactory.getLog(UtilsModule.class);

	@Inject org.mule.api.registry.Registry registry;
	
	private ThrottleQueueManager queueManager;
	
	/**
     * Throttle requests (requires Mule EE!)
     *
     * {@sample.xml ../../../doc/Utils-connector.xml.sample utils:throttle}
     *
     * @param payload the message payload
     * @param key the key 
     * @param noOfRequests the number of requests per interval
     * @param interval the interval
     * @param objectStore the reference to the object store
     * @return The same payload
     */
    @Processor
    public Object throttle(@Payload Object payload, String key, int noOfRequests, long interval) throws Exception {
    	
    	BlockingQueue queue = getQueueManager().getQueue(key, noOfRequests);

    	logger.debug("Queue size is " + queue.size());
    	
    	if (queue.size() < noOfRequests) {
    		logger.debug("Queue is not full, adding the element now");
    		long timestamp = System.currentTimeMillis();
    		queue.add(timestamp);
    		return payload; //The number of requests is less than the limit, so we are ok
    	} else {
    		boolean stopPeeking = false;
    	
    		while (!stopPeeking) {
    			long timestamp = System.currentTimeMillis();
    			Long nextQueued = (Long)queue.peek();
    			if (nextQueued == null) { //the queue is empty
    				stopPeeking = true;
    			} else if ((timestamp - nextQueued) > interval) { //the oldest entry is outside of the interval window - remove it
    				queue.remove();
    			} else { //timestamp within interval
    				stopPeeking = true;
    			}
    		}
    		
    		if (queue.size() < noOfRequests) {
    			long timestamp = System.currentTimeMillis();
    			queue.add(timestamp);
    			return payload; //The number of requests is less than the limit, so we are ok
    		} else {
    			throw new Exception("The throttle queue is full, message is rejected!");
    		}   			
    	}
    }
    
    protected ThrottleQueueManager getQueueManager() {
    	if (queueManager == null) {
    		queueManager = new ThrottleQueueManager(registry);
    	}
    	return queueManager;
    }

	public org.mule.api.registry.Registry getRegistry() {
		return registry;
	}

	public void setRegistry(org.mule.api.registry.Registry registry) {
		this.registry = registry;
	}
}
