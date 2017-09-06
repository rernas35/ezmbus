package org.devside.messaging.ezmbus.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.devside.messaging.ezmbus.annotations.EzMBusPublisher;
import org.devside.messaging.ezmbus.annotations.EzMBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

@Mojo(name = "applyPubSubCode")
public class EzMBusPubSubAspect extends AbstractMojo {

	private final static Logger LOGGER = LoggerFactory.getLogger(EzMBusPubSubAspect.class);

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;
	String path;
	
	static ClassPool pool = ClassPool.getDefault();
	
	public void execute() {
		importProjectDependency();
		try {
			path = project.getBuild().getDirectory() + File.separator + "classes";
			pool.appendClassPath(path);
			changeClass(path, "", path);
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

	private void importProjectDependency() {
		Set dependencyArtifacts = project.getDependencyArtifacts();
		for (Object object : dependencyArtifacts) {
			String  jarPath = ((DefaultArtifact)object).getFile().getAbsolutePath();
			LOGGER.info("adding {} to the classpath",jarPath);
			try {
				pool.appendClassPath(jarPath);
			} catch (NotFoundException e) {
				LOGGER.error("",e);
			}
		}
	}

	private void changeClass(String path, String pack, String basePath)
			throws NotFoundException, ClassNotFoundException, CannotCompileException, IOException {

		File folder = new File(path);
		File[] files = folder.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				if (!pack.isEmpty())
					pack = pack + "." + f.getName();
				else
					pack = f.getName();
				changeClass(path + File.separator + f.getName(), pack, basePath);
			} else if (f.getName().endsWith(".class")) {
				CtClass clazz;
				clazz = pool.get(pack + "." + f.getName().substring(0, f.getName().indexOf(".class")));

				boolean aspectApplied = false;
				for (CtMethod m : clazz.getDeclaredMethods()) {
					EzMBusSubscriber subscriberAnnotation = (EzMBusSubscriber) m.getAnnotation(EzMBusSubscriber.class);
					if (subscriberAnnotation != null) {
						aspectApplied = true;
						addSubscriberCode(clazz, f, m, subscriberAnnotation);
					}

					EzMBusPublisher publisherAnnotation = (EzMBusPublisher) m.getAnnotation(EzMBusPublisher.class);
					if (publisherAnnotation != null) {
						aspectApplied = true;
						addPublisherCode(clazz, m, publisherAnnotation);
					}
				}
				if (aspectApplied)
					clazz.writeFile(basePath);
			}
		}

	}

	private void addPublisherCode(CtClass cc, CtMethod m, EzMBusPublisher subscriberAnnotation) {
	}

	private void addSubscriberCode(CtClass clazz, File file, CtMethod m, EzMBusSubscriber subscriberAnnotation)
			throws NotFoundException, CannotCompileException {
		
		CtClass[] parameterTypes = m.getParameterTypes();
		if (parameterTypes.length > 1) {
			LOGGER.warn(
					"Multiple({}) parameters are defined.First parameter will be used for Easy Message Bus, rest will be ignored.",
					parameterTypes.length);
		}
		CtClass messageClass = parameterTypes[0];

		String methodName = m.getName();
		String topicName = subscriberAnnotation.topicName();
		
		String generatedClassPrefix = topicName.substring(0,1).toUpperCase() + topicName.substring(1);
		String className = generatedClassPrefix + "MessageListener";
		String fqClassName = clazz.getPackageName() + "." + className;
		CtClass genClazz = pool.makeClass(fqClassName );
		
		String n = messageClass.getName();
		genClazz.addInterface(pool.getCtClass("com.hazelcast.core.MessageListener"));
		genClazz.addField(new CtField(clazz, "j", genClazz));
		genClazz.addConstructor(new CtConstructor(new CtClass[] {clazz}, genClazz));
		genClazz.getConstructors()[0].setBody("j = $1;");
		CtMethod onMessageMethod = new CtMethod(CtClass.voidType, "onMessage",new CtClass[] {pool.get("com.hazelcast.core.Message")}, genClazz);
		onMessageMethod.setBody("j."+methodName+"(("+n+")$1.getMessageObject());");
		genClazz.addMethod(onMessageMethod);
		
//		genClazz.addMethod(CtNewMethod.make("public void onMessage(com.hazelcast.core.Message msg) {"
//											+ "				" + "j."+methodName+"(("+n+")$1.getMessageObject());" 
//											+ "}", genClazz));
		try {
			genClazz.writeFile(path);
		} catch (IOException e) {
			LOGGER.error("Error while saving generated class file " , e);
		}
		
		CtConstructor[] constructors = clazz.getConstructors();
		for (CtConstructor constructor : constructors) {
			String defineListener = "com.hazelcast.core.MessageListener " + getListenerString(methodName) +" = new " + fqClassName + "(this);"
					+ "		com.hazelcast.core.ITopic "+ getTopicString(topicName) + " = " + getHazelcastInstanceStr(clazz)  + ".getTopic(\"" + topicName + "\");" + "		"
					+ 		getTopicString(topicName) + ".addMessageListener(" + getListenerString(methodName) + ");";
			System.out.println("defineListener:" + defineListener);
			constructor.insertAfter(defineListener);
		}

	}

	private String getTopicString(String topicName) {
		return topicName + "Topic";
	}

	private String getListenerString(String methodName) {
		return "mListener4" + methodName;
	}

	private String getHazelcastInstanceStr(CtClass clazz) throws NotFoundException {
		CtClass hazelcastInterface = pool.get("com.hazelcast.core.HazelcastInstance");

		CtField[] declaredFields = clazz.getDeclaredFields();

		for (CtField field : declaredFields) {
			try {
				if (field.getType().subclassOf(hazelcastInterface)) {
					return field.getName();
				}
			} catch (NotFoundException e) {
				LOGGER.error("Error while checking fields...", e);
			}
		}
		return null;
	}

	public static void main(String[] args) {

	}

}
