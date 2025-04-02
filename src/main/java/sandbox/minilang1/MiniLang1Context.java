package sandbox.minilang1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiniLang1Context {
	public interface Function
		{
		public Object apply(final List<Object> list,Map<String, Object> map);
		}
	private final MiniLang1Context parent;
	private final Map<String,Object> id2variable = new HashMap<>();
	public MiniLang1Context() {
		this.parent = null;
		}
	public MiniLang1Context(MiniLang1Context parent) {
		this.parent = parent;
		}
	public Object put(String key,Object value) {
		this.id2variable.put(key, value);
		return value;
		}
	public Object plus(Object o1,Object o2) {
		return null;
		}
	public Object minus(Object o1,Object o2) {
		return null;
		}
	public Object divide(Object o1,Object o2) {
		return null;
		}
	public Object multiply(Object o1,Object o2) {
		return null;
		}
	public Object negate(Object o) {
		return null;
		}
	public Object call(String funName,List<Object> array,Map<String, Object> hash) {
		return null;
		}
	}
