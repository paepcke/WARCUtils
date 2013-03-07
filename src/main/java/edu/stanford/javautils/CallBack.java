package edu.stanford.javautils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Peter Wilkinson
 * From http://stackoverflow.com/questions/443708/callback-functions-in-java
 * 
 * Approximation to callbacks in Java.
 *
 * Use like this with a nested class that holds the callback methods: 
 * 
 * public class CallBackTest {
	    @Test
	    public void testCallBack() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
	        TestClass testClass = new TestClass();
	        CallBack callBack = new CallBack(testClass, "hello");
	        callBack.invoke();
	        callBack.invoke("Fred");
	    }
	
	    public class TestClass {
	        public void hello() {
	            System.out.println("Hello World");
	        }
	
	        public void hello(String name) {
	            System.out.println("Hello " + name);
	        }
	    }
	}
	
 * Or like this, with the callback method being part of the class itself:
 * 
 * public MyClass {
 
 	public MyClass {
		CallBack callBack = new CallBack(this, "myCallbackMethodSameClass");
		callBack.invoke("Same class");
 
 	public void myCallbackMethodSameClass(String theArg) {
		System.out.println("Called.");
	}

 * 
 */
public class CallBack {
    private String methodName;
    private Object scope;

    public CallBack(Object scope, String methodName) {
        this.methodName = methodName;
        this.scope = scope;
    }

    public Object invoke(Object... parameters) throws InvocationTargetException {
    	try {
    		Method method = scope.getClass().getMethod(methodName, getParameterClasses(parameters));
    		return method.invoke(scope, parameters);
    	} catch (IllegalAccessException e) {
    		throw new InvocationTargetException(null, e.getMessage());
    	} catch (NoSuchMethodException e1) {
    		throw new InvocationTargetException(null, e1.getMessage());
    	}
    }

    @SuppressWarnings("rawtypes")
	private Class[] getParameterClasses(Object... parameters) {
        Class[] classes = new Class[parameters.length];
        for (int i=0; i < classes.length; i++) {
            classes[i] = parameters[i].getClass();
        }
        return classes;
    }
}

