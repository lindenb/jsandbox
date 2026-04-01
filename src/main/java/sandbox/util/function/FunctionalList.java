package sandbox.util.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;


public interface FunctionalList<T> extends Iterable<T> {

	public boolean isEmpty();
	public T get(int index);
	public int indexOf(T t);
	public int size();
	public FunctionalList<T> remove(int index);
	public FunctionalList<T> add(T t);
	public FunctionalList<T> addAll(Collection<T> t);
	public Stream<T> stream();
	public List<T> asList();
	public FunctionalList<T> removeIf(Predicate<T> predicate);

	
	public static <T> FunctionalList<T> empty() {
		return  new ListImpl<>();
		}
	
	public static <T> FunctionalList<T> of(Iterable<T> iterable) {
		return of(iterable.iterator());
		}
	
	public static <T> FunctionalList<T> of(Iterator<T> iter) {
		final ListImpl<T> L = new ListImpl<>();
		while(iter.hasNext()) {
				L.array.add(iter.next());
			}
		return L;
		}
	public static <T> FunctionalList<T> of(List<T> L) {
		return  new ListImpl<>(L);
		}
	
	
	static class ListImpl<T> implements FunctionalList<T>{
		private final List<T> array;
		ListImpl() {
			this.array = new ArrayList<>();
			}
		ListImpl(final List<T> L) {
			this.array = new ArrayList<>(L);
			}
		@Override
		public T get(int index) {
			return array.get(index);
			}
		@Override
		public boolean isEmpty() {
			return array.isEmpty();
			}
		@Override
		public int size() {
			return array.size();
			}
		@Override
		public Iterator<T> iterator() {
			return this.array.iterator();
			}
		@Override
		public int indexOf(T t) {
			return this.array.indexOf(t);
			}
		@Override
		public FunctionalList<T> remove(int idx) {
			final ListImpl<T> cp = new ListImpl<T>(this.array);
			cp.array.remove(idx);
			return cp;
			}
		
		@Override
		public FunctionalList<T> add(T t) {
			return addAll(Collections.singletonList(t));
			}
		@Override
		public FunctionalList<T> addAll(Collection<T> t) {
			final ListImpl<T> cp = new ListImpl<T>(this.array);
			cp.array.addAll(t);
			return cp;
			}
		@Override
		public Stream<T> stream() {
			return this.array.stream();
			}
		@Override
		public List<T> asList() {
			return Collections.unmodifiableList(this.array);
			}
		@Override
		public FunctionalList<T> removeIf(Predicate<T> predicate) {
			final ListImpl<T> cp = new ListImpl<T>(this.array);
			cp.array.removeIf(predicate);
			return cp;
			}
		public ListImpl<T> clone() {
			return new ListImpl<T>(this.array);
			}
		}
	}
