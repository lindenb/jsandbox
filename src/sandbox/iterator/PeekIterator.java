package sandbox.iterator;

import java.util.Iterator;
import java.util.Objects;

public interface PeekIterator<T> extends Iterator<T>{

public T peek();

public static <X> PeekIterator<X> wrap(final Iterator<X> delegate) {
	Objects.requireNonNull(delegate);
	if(delegate instanceof PeekIterator) return (PeekIterator<X>)delegate;
	return new AbstractIterator<X>() {
		@Override
		protected X advance() {
			return delegate.hasNext()?delegate.next():null;
			}
		};
	}

}
