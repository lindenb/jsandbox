package sandbox.iterator;

import java.util.Iterator;

public class PeekIterator<T> implements CloseableIterator<T>{
private final Iterator<T> delegate;
private T peeked= null;

PeekIterator(final Iterator<T> delegate) {
	this.delegate = delegate;
	}
@Override
public boolean hasNext() {
	return peeked!=null || this.delegate.hasNext();
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
	if(this.peeked!=null) return this.peeked;
	if(!this.delegate.hasNext()) return null;
	this.peeked=this.delegate.next();
	return this.peeked;
	}
@Override
public void close() {
	this.peeked=null;
	CloseableIterator.close(this.delegate);
	}
}
