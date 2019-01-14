package sandbox;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class AbstractIterator<T> implements Iterator<T>,Closeable {
	private T _next=null;
	protected abstract T advance();
	@Override
	public boolean hasNext() {
		if(_next==null) {
			_next = advance();
			if(_next==null) close();
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
	
	public T peek() {
		if(!hasNext()) return null;
		return this._next;
		}
	
	@Override
	public void close() {
		
		}
	
	public Stream<T> stream() {
		return  StreamSupport.stream(
		          Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED),
		          false);
		}
	}
