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
	this.delegate= new PeekIterator<>(delegate);
	}

@Override
protected List<T> advance() {
	if(!this.delegate.hasNext()) {
		this.close();
		return null;
		}
	List<T> list= new ArrayList<>();
	list.add(this.delegate.next());
	while(this.delegate.hasNext()) {
		T rec = this.delegate.peek();
		int i= comparator.compare(rec, list.get(0));
		if(i==0) {
			list.add(this.delegate.next());
			}
		else if( i<0) {
			throw new IllegalStateException("data are not sorted");
			}
		else
			{
			break;
			}
		}
	return list;
	}
@Override
	public void close() {
		CloseableIterator.close(this.delegate);
		}
}
