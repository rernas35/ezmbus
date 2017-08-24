package org.devside.messaging.ezmbus.annotations;

import org.devside.messaging.ezmbus.DummySerialization;
import org.devside.messaging.ezmbus.IEzMBusSerializer;

public @interface EzMBusPublisher  {

	String topicName();
	Class<? extends IEzMBusSerializer> serializerClass() default DummySerialization.class;
	
}
