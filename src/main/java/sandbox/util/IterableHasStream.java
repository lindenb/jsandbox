package sandbox.util;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import sandbox.util.stream.HasStream;

public interface IterableHasStream<T> extends HasStream<T>, Iterable<T> {
	public default Stream<T> stream() {
		return StreamSupport.stream(spliterator(),false);
	}
}
