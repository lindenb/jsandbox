package sandbox.lang;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.ToLongFunction;

public class Counter<T> implements Consumer<T>,ToLongFunction<T> {
private final Map<T, Long> hash = new HashMap<>();
@Override
public final void accept(T t) {
	increment(t);
	}
public long increment(final T t) {
	return increment(t,1L);
	}
public long increment(final T t,long n) {
	return this.hash.merge(t, n, (A,B)->A+B);
	}
public long count(final T key) {
	return this.hash.getOrDefault(key, 0L);
	}

@Override
public final long applyAsLong(T key) {
	return count(key);
	}
}
