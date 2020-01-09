package sandbox.test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class RuntimeTester {
private final Set<Class<?>> classes = new HashSet<>();
public RuntimeTester add(final Class<?> clazz) {
	if(clazz==null || clazz.equals(Object.class)) return this;
	if(this.classes.contains(clazz)) return this;
	this.classes.add(clazz);
	add(clazz.getSuperclass());
	final AlsoTest also = clazz.getAnnotation(AlsoTest.class);
	for(Class<?> c2:also.values()) add(c2);
	return this;
	}
public boolean run() {
	boolean ok=true;
	for(Class<?> clazz:this.classes) if(!run(clazz)) ok=false;
	return ok;
	}
private void report(Class<?> c,Method m,Throwable err) {
	
}
private boolean run(Class<?> c) {
	boolean ok=true;
	for(final Method m:c.getMethods()) {
		  if (!Modifier.isStatic(m.getModifiers())) continue;
		  if(m.getAnnotation(RuntimeTest.class)==null) continue;

		  if(m.getParameterCount()==1 && m.getParameterTypes()[0]==RuntimeTester.class) {
			  try {
				  m.invoke(null,this);
				  report(c,m,null);
			  	}
			  catch(final Throwable err) {
				  report(c,m,err);
				  ok=false;
			  }
		  } else if(m.getParameterCount()==0) {
			  try {
				  m.invoke(null);
				  report(c,m,null);
			  	}
			  catch(final Throwable err) {
				  ok=false;
				  report(c,m,err);
			  }
		  }
		  
		}
	return ok;
	}
}
