package sandbox.iterator;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface CloseableIterator<T> extends Iterator<T>,AutoCloseable {
public default Stream<T> stream() {
	return StreamSupport.stream(         
		Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED),
		false).onClose(()->close());
	}

@Override
public default void close() {
	}

public static void close(Iterator<?> iter) {
	if(iter instanceof CloseableIterator) CloseableIterator.class.cast(iter).close();
	}
}
