package org.devside.messaging.ezmbus.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.devside.ee.dcjb.client.annotation.JumpNextOne;
import org.devside.messaging.ezmbus.annotations.EzMBusPublisher;
import org.devside.messaging.ezmbus.annotations.EzMBusSubscriber;
import org.devside.messaging.ezmbus.hazelcast.HazelcastEzMBusContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devside.test.ReceiverGen;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;


@Mojo(name="applyPubSubCode")
public class EzMBusPubSubAspect extends AbstractMojo{
	
	private final static Logger LOGGER = LoggerFactory.getLogger(EzMBusPubSubAspect.class);
	
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;
	
	static ClassPool pool = ClassPool.getDefault();
	static Class[] defaultExceptions = {IllegalStateException.class/*,SocketException.class,IOException.class*/};
	
	public  void execute() {
		
		try {
			String path = project.getBuild().getDirectory() + File.separator + "classes";
			pool.appendClassPath(path);
			changeClass( path,"",path);
		} catch (NotFoundException ee) {
			getLog().error(ee);
		} catch (IOException e) {
			getLog().error(e);
		} catch (CannotCompileException e) {
			getLog().error(e);
		} catch (ClassNotFoundException e) {
			getLog().error(e);
		}
		
		
		
		
	}

	private void changeClass(String path,String pack, String basePath)
			throws NotFoundException, ClassNotFoundException, CannotCompileException, IOException {
		
		File folder = new File(path);
		File[] files = folder.listFiles();
		for(File f:files){
			if (f.isDirectory()){
				if (!pack.isEmpty())
					pack = pack + "." + f.getName();
				else 
					pack = f.getName();
				changeClass(path + File.separator + f.getName(), pack , basePath);
			}else if (f.getName().endsWith(".class")){
				CtClass cc;
				cc = pool.get(pack + "." + f.getName().substring(0,f.getName().indexOf(".class")));
				
				
				boolean aspectApplied = false;
				for (CtMethod m : cc.getDeclaredMethods()) {
					EzMBusSubscriber subscriberAnnotation = (EzMBusSubscriber) m.getAnnotation(EzMBusSubscriber.class);
					if (subscriberAnnotation != null){
						aspectApplied = true; 
						addSubscriberCode(cc, m, subscriberAnnotation);
					}
					
					EzMBusPublisher publisherAnnotation = (EzMBusPublisher) m.getAnnotation(EzMBusPublisher.class);
					if (publisherAnnotation != null){
						aspectApplied = true;
						addPublisherCode(cc, m, publisherAnnotation);
					}
				}
				if (aspectApplied)
					cc.writeFile(basePath);
			}
		}
		
		
		
	}
	
	private void addPublisherCode(CtClass cc, CtMethod m, EzMBusPublisher subscriberAnnotation) {
	}

	private void addSubscriberCode(CtClass cc, CtMethod m, EzMBusSubscriber subscriberAnnotation)
			throws NotFoundException, CannotCompileException {
		
		CtClass[] parameterTypes = m.getParameterTypes();
		if (parameterTypes.length > 1 ) {
			LOGGER.warn("Multiple({}) parameters are defined.First parameter will be used for Easy Message Bus, rest will be ignored.",parameterTypes.length);
		}
		CtClass messageClass = parameterTypes[0];
		
		String methodName = m.getName();
		String topicName = subscriberAnnotation.topicName();
		
		CtConstructor[] constructors = cc.getConstructors();
		for (CtConstructor constructor : constructors) {
			
			String n = messageClass.getName();
			String defineListener = "com.hazelcast.core.MessageListener<" + n +"> "+ getListenerString(topicName)+" = new com.hazelcast.core.MessageListener<"+ n +">() {" + 
									"			@Override" + 
									"			public void onMessage(Message<"+ n +"> msg) {" + 
									"				ReceiverGen.this.onReceivePlayStatus(msg);" + 
									"				" + 
									"			}" + 
									"		};" + 
									"		"+ getHazelcastInstanceStr()+".addReceiver(mlistener4onReceivePlayStatus);" + 
									"		ITopic<Object> "+ getTopicString(topicName) +" = instance.getTopic(\""+ topicName +"\");" + 
									"		"+ getTopicString(topicName)+".addMessageListener("+getListenerString(topicName)+");";
			constructor.insertAfter(defineListener);
		}
		
	}
	
	private String getTopicString(String topicName) {
		return  topicName + "Topic";
	}
	
	private String getListenerString(String topicName) {
		return "mListener4" + topicName;
	}
	
	private String getHazelcastInstanceStr() {
		// TODO Auto-generated method stub
		return null;
	}

	private static String getModifier(CtMethod m){
		switch (m.getModifiers()) {
		case AccessFlag.PRIVATE:
			return "private";
		case AccessFlag.PUBLIC:
			return "public";
		case AccessFlag.PROTECTED:
			return "protected";		
		default:
			return "";
		}
	}
	
	
	public static String addCatch(String varStr,List<Class> clazzList) throws NotFoundException, CannotCompileException{
		String catchStr = "";
		for(Class clazz:clazzList){
			catchStr += "catch(" + clazz.getName() + " e){ "
								+ "retry=true; "
						+ "}";
		}
		
		return catchStr;
	}
	
	
	public static void main(String[] args) {
		
		
		
	}
	
	
}
