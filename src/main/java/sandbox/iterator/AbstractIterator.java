package sandbox.iterator;

import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import sandbox.util.stream.HasStream;


public abstract class AbstractIterator<T> implements PeekIterator<T>,HasStream<T> /* non, pas closable  please */ {
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
	public T peek() {
		if(!hasNext()) return null;
		return this._next;
		}
	
	@Override
	public Stream<T> stream() {
		return StreamSupport.stream(         
			Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED),
			false);
		}
	}
