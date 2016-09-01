package sandbox;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class AbstractIterator<T> implements Iterator<T>,Closeable {
	private T _next=null;
	protected abstract T advance();
	@Override
	public boolean hasNext() {
		if(_next==null) {
			_next = advance();
			}
		return _next!=null;
		}
	
	@Override
	public T next() {
		if(!hasNext()) throw new NoSuchElementException();
		final T old= this._next;
		this._next = null;
		return old;
		}
	
	@Override
	public void close() throws IOException {
		
		}
	}
