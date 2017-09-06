package org.devside.messaging.sample.hazelcast;

import org.devside.messaging.ezmbus.annotations.EzMBusSubscriber;
import org.devside.messaging.sample.base.MessageEvent;

import com.hazelcast.core.HazelcastInstance;

public class SubscriberSample {

	HazelcastInstance instance;


	@EzMBusSubscriber(topicName = "generalMessages")
	public void onMessageReceived(MessageEvent e) {
		System.out.println("received message event : " + e);
	}

}
