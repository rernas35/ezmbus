package org.devside.messaging.ezmbus.hazelcast;

import org.devside.messaging.ezmbus.AbstractEzMBusContext;

import com.hazelcast.core.MessageListener;

public class HazelcastEzMBusContext extends AbstractEzMBusContext<MessageListener> {

	private static HazelcastEzMBusContext instance;
	
	@Override
	public void disposeInternal() {
	}
	
	public static HazelcastEzMBusContext getInstance() {
		return instance;
	}
	
}
