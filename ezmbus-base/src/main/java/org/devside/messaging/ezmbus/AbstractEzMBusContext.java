package org.devside.messaging.ezmbus;

import java.util.List;

import java.util.ArrayList;

import javax.annotation.PreDestroy;

public abstract class AbstractEzMBusContext<L> {
	
	List<L> receivers = new ArrayList<L>();

	@PreDestroy
	public void dispose() {
		
		disposeInternal();
	}
	
	public void disposeInternal(){};
	
	public List<L> getReceivers() {
		return receivers;
	}
	
	public void addReceiver(L l) {
		getReceivers().add(l);
	}
	
}
