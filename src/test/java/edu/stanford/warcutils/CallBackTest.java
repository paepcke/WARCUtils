package edu.stanford.warcutils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;

import org.junit.Before;
import org.junit.Test;

import edu.stanford.javautils.CallBack;

public class CallBackTest {
	
	boolean callbackInvoked = false;
	CallBack callback = null;

	@Before
	public void setUp() throws Exception {
		callbackInvoked = false;
		callback = new CallBack(this, "myCallback");
	}

	public void sleepAndInvoke() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		try {
			Thread.sleep(1000);
			callback.invoke("I ran");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void myCallback(String strArg) {
		callbackInvoked = true;
	}
	
	@Test
	public void testCallback() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		assertFalse(callbackInvoked);
		sleepAndInvoke();
		assertTrue(callbackInvoked);
	}
}
