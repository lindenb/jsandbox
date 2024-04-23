package sandbox.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface Tuple<K,V> extends Iterable<Pair<K,V>> {
	
	static class TupleImpl<K,V>  implements Tuple<K,V> {
		private final List<Pair<K,V>> array;
		TupleImpl() {
			this.array = Collections.emptyList();
			}
		TupleImpl(TupleImpl<K,V> cp) {
			this.array = new ArrayList<>(cp.array);
			}
		@Override
		public Stream<Pair<K,V>> stream() {
			return array.stream();
			}

		@Override
		public Iterator<Pair<K,V>> iterator() {
			return array.iterator();
			}
		@Override
		public int size() {
			return this.array.size();
			}
		@Override
		public Pair<K,V> get(int i) {
			return this.array.get(i);
			}
		@Override
		public int indexOf(K k) {
			for(int i=0;i< this.size();i++) {
				if(get(i).hasKey(k)) return i;
				}
			return -1;
			}

		@Override
		public Tuple<K, V> plus(K k,V v) {
			return plus(Pair.of(k, v));
			}
		@Override
		protected TupleImpl<K,V> clone() {
			return new TupleImpl<>(this);
			}
		@Override
		public Tuple<K, V> plus(Pair<K,V> kv) {
			final TupleImpl<K,V> cp;
			final int i=this.indexOf(kv.getKey());
			if(i<0) {
				cp= clone();
				cp.array.add(kv);
				return cp;
				}
			else
				{
				if(get(i).hasValue(kv.getValue())) return this;
				cp= clone();
				cp.array.set(i,kv);
				return cp;
				}
			}
	
		@Override
		public Tuple<K, V> plus(Tuple<K,V> other) {
			if(other==this || other.isEmpty()) return this;
			Tuple<K,V> cp= clone();
			for(Pair<K,V> kv:other) {
				cp = cp.plus(kv);
				}
			return cp;
			}
		
		
		
		@Override
		public List<Pair<K, V>> asList() {
			return Collections.unmodifiableList(this.array);
			}
		@Override
		public Tuple<K, V> minus(K k) {
			return minus(Collections.singleton(k));
		}
		@Override
		public Tuple<K, V> minus(final Set<K> keys) {
			if(keys.isEmpty()) return this;
			if(keys.stream().noneMatch(K->containsKey(K))) return this;
			final TupleImpl<K, V> cp = clone();
			cp.array.removeIf(KV->keys.contains(KV.getKey()));
			return cp;
			}
	}

	public Pair<K,V> get(int i);
	public int size();
	public default boolean isEmpty() {
		return this.size()==0;
		}
	public default Map<K,V> asMap() {
		if(isEmpty()) return Collections.emptyMap();
		final Map<K,V> h = new LinkedHashMap<>(this.size());
		for(Pair<K,V> kv:this) {
			h.put(kv.getKey(), kv.getValue());
			}
		return h;
		}

	public List<Pair<K,V>> asList();

	
	public int indexOf(K k);
	public default boolean containsKey(K k) {
		return indexOf(k)!=-1;
	}
	
	public static <K,V> Tuple<K,V> empty() {
		return new TupleImpl<>();
		}
	
	public Stream<Pair<K,V>> stream();

	public default Optional<V> get(K k) {
		return stream().filter(it->it.hasKey(k)).findFirst().map(it->it.getValue());
	}
	public Tuple<K,V> plus(K k,V v);
	public Tuple<K,V> plus(Pair<K,V> kv);
	public Tuple<K,V> plus(Tuple<K,V> cp);
	public Tuple<K,V> minus(K k);
	public Tuple<K,V> minus(Set<K> keys);
}
