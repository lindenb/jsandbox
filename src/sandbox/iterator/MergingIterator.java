package sandbox.iterator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class MergingIterator<T> extends AbstractIterator<T>{
private final List<PeekIterator<T>> delegates;
private final Comparator<T> comparator;	
public MergingIterator(final Comparator<T> comparator,final List<Iterator<T>> delegates) {
	this.comparator = comparator;
	this.delegates= delegates.stream().map(T->new PeekIterator<>(T)).collect(Collectors.toCollection(ArrayList::new));
	}

@Override
protected T advance() {
	int best_idx =-1;
	T best = null;
	int i=0;
	while(i< this.delegates.size()) {
		final PeekIterator<T> iter = this.delegates.get(i);
		if(!iter.hasNext()) {
			CloseableIterator.close(iter);
			this.delegates.remove(i);
			continue;
			}
		final T rec = iter.peek();
		if(best_idx==-1 || this.comparator.compare(rec,best)<0) {
			best = rec;
			best_idx = i;
			}
		i++;
		}
	return best_idx==-1?null:this.delegates.get(best_idx).next();
	}

	@Override
	public void close() {
		//delegates.stream().forEach(PeekIterator::close);
		//this.delegates.clear();
		}
	@Override
	public String toString() {
		return "MergingIterator("+ delegates +")";
		}
}
