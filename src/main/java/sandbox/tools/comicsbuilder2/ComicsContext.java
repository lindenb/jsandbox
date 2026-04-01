package sandbox.tools.comicsbuilder2;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

import sandbox.lang.Named;


public class ComicsContext {
	public interface Function extends Named {
		public String getName();
		public Object apply(List<Object> L,Map<String,Object> att) ;
		}
	private class BasicFunction implements Function {
			private final String name;
			BasicFunction(final String name) {
				this.name=name;
				}
			@Override
			public String getName() { return name;}
			@Override
			public Object apply(List<Object> L,Map<String,Object> att) {
				String funName = this.name;
				Method m;
				try {
					m = ComicsContext.this.getClass().getMethod(funName,List.class, Map.class);
					return m.invoke(att,L);
					} 
				catch (Throwable e) {
					throw new RuntimeException(e);
					}
				}
			}
	void init() {
		
		}
	private Object call(String funName,List<Object> L,Map<String,Object> att) {
		return null;
		}
	}
