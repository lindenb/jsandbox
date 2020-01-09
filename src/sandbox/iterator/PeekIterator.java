package sandbox.iterator;

import java.util.Iterator;
import java.util.Objects;

import sandbox.test.RuntimeTest;

public class PeekIterator<T> implements CloseableIterator<T>{
private final Iterator<T> delegate;
private T peeked= null;

private PeekIterator(final Iterator<T> delegate) {
	this.delegate = Objects.requireNonNull(delegate);
	}

@Override
public boolean hasNext() {
	return this.peeked!=null || this.delegate.hasNext();
	}

@Override
public T next() {
	if(this.peeked!=null) {
		final T tmp = this.peeked;
		this.peeked = null;
		return tmp;
		}
	return this.delegate.next();
	}

public T peek() {
	if(this.peeked != null) return this.peeked;
	if(!this.delegate.hasNext()) return null;
	this.peeked = this.delegate.next();
	return this.peeked;
	}

@Override
public void close() {
	this.peeked=null;
	CloseableIterator.close(this.delegate);
	}
public static <X> PeekIterator<X> wrap(final Iterator<X> delegate) {
	if(delegate instanceof PeekIterator) return (PeekIterator<X>)delegate;
	return new PeekIterator<>(delegate);
	}

@Override
public String toString() {
	return "PeekIterator("+this.delegate+")";
	}
@RuntimeTest
private static void runTest() {
	
	}
}
