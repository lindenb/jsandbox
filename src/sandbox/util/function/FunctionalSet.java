package sandbox.util.function;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;


public interface FunctionalSet<T> extends Iterable<T> {

	public boolean isEmpty();
	public int size();
	public FunctionalSet<T> remove(T t);
	public FunctionalSet<T> add(T t);
	public FunctionalSet<T> addAll(Collection<T> t);
	public Stream<T> stream();
	public Set<T> asSet();

	public static <T> FunctionalSet<T> empty() {
		return  new SetImpl<>();
		}
	
	public static <T> FunctionalSet<T> of(Iterable<T> iterable) {
		return of(iterable.iterator());
		}
	
	public static <T> FunctionalSet<T> of(Iterator<T> iter) {
		final SetImpl<T> set = new SetImpl<>();
		while(iter.hasNext()) {
				set.set.add(iter.next());
			}
		return set;
		}
	
	public static <T> FunctionalSet<T> of(T...array) {
		return of(Arrays.asList(array));
		}
	
	static class SetImpl<T> implements FunctionalSet<T>{
		private final Set<T> set;
		SetImpl() {
			this.set = new HashSet<>();
			}
		SetImpl(final Set<T> L) {
			this.set = new HashSet<>(L);
			}
		
		@Override
		public boolean isEmpty() {
			return set.isEmpty();
			}
		@Override
		public int size() {
			return set.size();
			}
		@Override
		public Iterator<T> iterator() {
			return this.set.iterator();
			}
		
		@Override
		public SetImpl<T> remove(T v) {
			final SetImpl<T> cp = new SetImpl<T>(this.set);
			cp.set.remove(v);
			return cp;
			}
		
		@Override
		public SetImpl<T> add(T t) {
			return addAll(Collections.singletonList(t));
			}
		@Override
		public SetImpl<T> addAll(Collection<T> t) {
			final SetImpl<T> cp = new SetImpl<T>(this.set);
			cp.set.addAll(t);
			return cp;
			}
		@Override
		public Stream<T> stream() {
			return this.set.stream();
			}
		@Override
		public Set<T> asSet() {
			return Collections.unmodifiableSet(this.set);
			}
		
		public SetImpl<T> clone() {
			return new SetImpl<T>(this.set);
			}
		}
	}
