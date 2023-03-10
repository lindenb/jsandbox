package sandbox.util.stream;

import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MyCollectors {
	public static <T>  Collector<T,?,T> oneAndOnlyOne() {
		return Collectors.collectingAndThen(
				Collectors.toCollection(OneOrZeroSet::new),
				COL->COL.get()
				);
		}
	
	public static <T>  Collector<T,?,Optional<T>> oneOrNone() {
		return Collectors.collectingAndThen(
				Collectors.toCollection(OneOrZeroSet::new),
				COL->COL.orElse()
				);
		}
	
	private static class OneOrZeroSet<T> extends AbstractSet<T> {
		T value = null;
		@Override
		public boolean add(T e) {
			if(value!=null) throw new IndexOutOfBoundsException("Expected one and only one element but got at least two:"+e+" and "+value);
			value = e;
			return true;
			}
		@Override
		public int size() {
			return value==null?0:1;
			}
		public T get() {
			if(value==null) throw new IllegalStateException("Expected one and only one element but got none");
			return value;
			}
		public Optional<T> orElse() {
			return Optional.ofNullable(this.value);
			}
		@Override
		public Iterator<T> iterator() {
			return  value==null?Collections.emptyIterator():Collections.singleton(value).iterator();
			}
		}
	

}
