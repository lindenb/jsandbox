package sandbox.util;

import java.util.LinkedHashSet;
import java.util.Set;

public class Sets {
	public static <T>  Set<T> of(T v1) {
		final Set<T> m=new LinkedHashSet<>();
		m.add(v1);
		return m;
		}
	public static <T>  Set<T> of(T v1,T v2) {
		final Set<T> m=of(v1);
		m.add(v2);
		return m;
		}
	public static <T>  Set<T> of(T v1,T v2, T v3) {
		final Set<T> m=of(v1,v2);
		m.add(v3);
		return m;
		}
	public static <T>  Set<T> of(T v1,T v2, T v3, T v4) {
		final Set<T> m=of(v1,v2,v3);
		m.add(v4);
		return m;
		}
	}
