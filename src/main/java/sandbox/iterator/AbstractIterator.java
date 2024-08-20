package sandbox.iterator;

import java.util.NoSuchElementException;


public abstract class AbstractIterator<T> implements PeekIterator<T> , CloseableIterator<T>{
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
	public void close() {
		}
	}
