package sandbox.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class Counter<T>  {
	private Map<T,Long> count=new HashMap<>();
	public Counter() {
		}
	public long increase(T t,long dn) {
		long n = count(t) + dn;
		count.put(t, n);
		return n;
		}
	public long increase(T t) {
		return increase(t,1L);
		}
	public Set<T> keySet() {
		return this.count.keySet();
		}
	public long count(final T t) {
		return count.getOrDefault(t,0L);
		}
	}
