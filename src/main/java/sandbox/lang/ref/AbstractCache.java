package sandbox.lang.ref;

public abstract class AbstractCache<T> {
	 public abstract T get(final String key);
	 public abstract void put(final String key,final T value);
	 protected abstract T fetch(final String key);
}
