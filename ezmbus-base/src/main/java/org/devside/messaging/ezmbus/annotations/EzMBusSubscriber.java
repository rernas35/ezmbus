package org.devside.messaging.ezmbus.annotations;

import org.devside.messaging.ezmbus.DummySerialization;
import org.devside.messaging.ezmbus.IEzMBusDeserializer;

public @interface EzMBusSubscriber {

	String topicName();
	Class<? extends IEzMBusDeserializer> deserializerClass() default DummySerialization.class; 
	
}
