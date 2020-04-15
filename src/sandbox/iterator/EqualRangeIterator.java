package sandbox.iterator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class EqualRangeIterator<T> extends AbstractIterator<List<T>>{
private final PeekIterator<T> delegate;
private final Comparator<T> comparator;

public EqualRangeIterator(final Comparator<T> comparator,final Iterator<T> delegate) {
	this.comparator = comparator;
	this.delegate = PeekIterator.wrap(delegate);
	}

@Override
protected List<T> advance() {
	if(!this.delegate.hasNext()) {
		System.err.println("Delegate has no next");
		return null;
		}
	final List<T> list = new ArrayList<>();
	final T first = this.delegate.next();
	list.add(first);

	while(this.delegate.hasNext()) {
		final T rec = this.delegate.peek();
		final int i= comparator.compare(first,rec);
		if(i==0) {
			list.add(this.delegate.next());
			}
		else if( i>0 ) {
			throw new IllegalStateException("data are not sorted got:\n  "+rec+"after\n  "+list.get(0));
			}
		else
			{
			break;
			}
		}
	System.err.println(list);
	return list;
	}

	@Override
	public void close() {
		CloseableIterator.close(this.delegate);
		}

	@Override
	public String toString() {
		return "EqualRangeIterator("+this.delegate+")";
		}
}
