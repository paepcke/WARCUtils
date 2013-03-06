package javautils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import edu.stanford.javautils.CallBack;

public class TestCallBack {

	boolean callBackWasInvoked = false;
	String arg = "";

	// If you want callbacks in their own class, do this:
	public class Callbacks {
		public void myCallbackMethodNestedClass(String theArg) {
			callBackWasInvoked = true;
			arg = theArg;
			//System.out.println(arg);
		}
	}

	// But can also make a method in the same class
	// a callback:
	public void myCallbackMethodSameClass(String theArg) {
		callBackWasInvoked = true;
		arg = theArg;
		//System.out.println(arg);
	}

	
	@Test
	public void testMethodInSameClass() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		CallBack callBack = new CallBack(this, "myCallbackMethodSameClass");
		callBackWasInvoked = false;
		arg = "";
		callBack.invoke("Same class");
		assertTrue(callBackWasInvoked);
		assertEquals("Same class",arg);
	}

	@Test
	public void testMethodInNestedClass() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		CallBack callBack = new CallBack(new Callbacks(), "myCallbackMethodNestedClass");
		callBackWasInvoked = false;
		arg = "";
		callBack.invoke("Nested class");
		assertTrue(callBackWasInvoked);
		assertEquals("Nested class",arg);
	}
	
}
