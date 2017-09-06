package org.devside.messaging.ezmbus.maven;

import javassist.CtMethod;
import javassist.bytecode.AccessFlag;

public class JavassistUtil {

	public static String getModifier(CtMethod m){
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
	
	
	
}
