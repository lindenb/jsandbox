package sandbox.iterator;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Supplier;

import sandbox.Logger;

public class EqualRangeIterator<C extends Collection<T>,T> extends AbstractIterator<C> implements CloseableIterator<C> {

private @SuppressWarnings("unused")
static final Logger LOG = Logger.builder(EqualRangeIterator.class).build();

private final PeekIterator<T> delegate;
private final Comparator<T> comparator;
private final Supplier<C> colFactory;
public EqualRangeIterator(
		final Comparator<T> comparator,
		final Iterator<T> delegate,
		final Supplier<C> colFactory) {
	this.comparator = comparator;
	this.delegate = PeekIterator.wrap(delegate);
	this.colFactory = colFactory;
	}

@Override
protected C advance() {
	if(!this.delegate.hasNext()) {
		return null;
		}
	final C list = this.colFactory.get();
	final T first = this.delegate.next();
	T prev = first;
	list.add(first);
	
	while(this.delegate.hasNext()) {
		final T rec = this.delegate.peek();
		final int i= this.comparator.compare(first,rec);
		if(i==0) {
			list.add(this.delegate.next());
			}
		else if( i>0 ) {
			throw new IllegalStateException("data are not sorted got:\n  "+rec+"after\n  "+prev);
			}
		else
			{
			break;
			}
		prev = rec;
		}
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
