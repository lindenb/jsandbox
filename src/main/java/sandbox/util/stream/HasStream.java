package sandbox.util.stream;

import java.util.stream.Stream;

public interface HasStream<T> {
	public Stream<T> stream();
}
