package sandbox.util.function;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sandbox.util.Sets;

import java.util.Objects;

public class FunctionalMap<K,V> implements Iterable<FunctionalMap.Pair<K, V>> {
	public interface Pair<K,V> {
		public K getKey();
		public V getValue();
		}
	private static class PairImpl<K,V> implements Pair<K,V> {
		private final K key;
		private final V value;
		PairImpl(K key,V value) {
			this.key = key;
			this.value = value;
			}
		PairImpl(Map.Entry<K, V> e) {
			this(e.getKey(),e.getValue());
			}
		@Override
		public K getKey() {
			return this.key;
			}
		@Override
		public V getValue() {
			return this.value;
			}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hash(key, value);
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PairImpl<?,?> other = PairImpl.class.cast(obj);
			return Objects.equals(key, other.key) && Objects.equals(value, other.value);
			}
		@Override
		public String toString() {
			return String.valueOf(key)+"="+value;
			}
		}
	private final Map<K,V> delegate = new HashMap<>();
	public FunctionalMap() {
		}
	public FunctionalMap(FunctionalMap<K,V> copy) {
		this(copy.delegate);
		}
	public FunctionalMap(Map<K,V> copy) {
		this.delegate.putAll(copy);
		}
	public FunctionalMap(K k1,V v1) {
		this.delegate.put(k1,v1);
		}
	
	public FunctionalMap(K k1,V v1,K k2,V v2) {
		this(k1,v1);
		this.delegate.put(k2,v2);
		}
	
	public FunctionalMap(K k1,V v1,K k2,V v2,K k3,V v3) {
		this(k1,v1,k2,v2);
		this.delegate.put(k3,v3);
		}
	public FunctionalMap(K k1,V v1,K k2,V v2,K k3,V v3,K k4,V v4) {
		this(k1,v1,k2,v2,k3,v3);
		this.delegate.put(k4,v4);
		}
	public FunctionalMap<K,V> plus(Pair<K,V> p) {
		return plus(p.getKey(),p.getValue());
		}
	public FunctionalMap<K,V> plus(K k1,V v1) {
		final FunctionalMap<K,V> o = clone();
		o.delegate.put(k1,v1);
		return o;
		}
	public FunctionalMap<K,V> plus(K k1,V v1,K k2,V v2) {
		FunctionalMap<K,V> o = plus(k1,v1);
		o.delegate.put(k2,v2);
		return o;
		}
	public FunctionalMap<K,V> plus(K k1,V v1,K k2,V v2,K k3,V v3) {
		FunctionalMap<K,V> o = plus(k1,v1,k2,v2);
		o.delegate.put(k3,v3);
		return o;
		}
	
	public FunctionalMap<K,V> plus(FunctionalMap<K,V> o) {
		return plus(o.delegate);
		}
	
	public FunctionalMap<K,V> plus(Map<K,V> hash) {
		FunctionalMap<K,V> o = clone();
		o.delegate.putAll(hash);
		return o;
		}
	
	public FunctionalMap<K,V> minus(K k1) {
		return minus(Collections.singleton(k1));
		}
	
	public FunctionalMap<K,V> minus(K k1, K k2) {
		return minus(Sets.of(k1, k2));
		}
	
	public FunctionalMap<K,V> minus(final Set<K> toRemove) {
		if(toRemove.isEmpty() || toRemove.stream().noneMatch(K->this.delegate.containsKey(K))) return this;
		final FunctionalMap<K,V> o = clone();
		for(K k:toRemove) {
			o.delegate.remove(k);
			}
		return o;
		}
	
	public FunctionalMap<K,V> removeIf(BiPredicate<K,V> remover) {
		FunctionalMap<K,V> o = new FunctionalMap<>();
		for(K k: this.delegate.keySet()) {
			V v = this.delegate.get(k);
			if(!remover.test(k,v)) {
				o.delegate.put(k, v);
				}
			}
		return o;
		}
	
	public boolean containsKey(K k) {
		return this.delegate.containsKey(k);
		}
	public V get(K k) {
		return this.delegate.get(k);
		}
	
	public V getOrDefault(K k,V d) {
		return this.delegate.getOrDefault(k,d);
		}
	public boolean isEmpty() {
		return this.delegate.isEmpty();
		}
	public int size() {
		return this.delegate.size();
		}
	
	
	@Override
	public Iterator<Pair<K, V>> iterator() {
		return entrySet().iterator();
		}
	
	public Set<K> keySet() {
		return Collections.unmodifiableSet(this.delegate.keySet());
		}
	public Collection<V> values() {
		return Collections.unmodifiableCollection(this.delegate.values());
		}
	public Set<Pair<K,V>> entrySet() {
		return stream().collect(Collectors.toSet());
		}
	public Stream<Pair<K,V>> stream() {
		return this.delegate.entrySet().stream().map(p->new PairImpl<>(p));
		}

	@Override
	public FunctionalMap<K,V> clone() {
		return new FunctionalMap<>(this);
		}
	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;
		if(obj==null || !(obj instanceof FunctionalMap)) return false;
		FunctionalMap<?,?> o = FunctionalMap.class.cast(obj);
		return this.delegate.equals(o.delegate);
		}
	@Override
	public int hashCode() {
		return this.delegate.hashCode();
		}
	@Override
	public String toString() {
		return this.delegate.toString();
		}
	
	public static <X,Y> FunctionalMap<X,Y> empty() { return new FunctionalMap<X,Y>();}
	public static <X,Y> FunctionalMap<X,Y> of(X k1,Y v1) {
		return new FunctionalMap<>(k1,v1);
		}
	public static <X,Y> FunctionalMap<X,Y> of(X k1,Y v1,X k2,Y v2) {
		return of(k1,v1).plus(k2,v2);
		}
	public static <X,Y> FunctionalMap<X,Y> of(X k1,Y v1,X k2,Y v2,X k3,Y v3) {
		return of(k1,v1,k2,v2).plus(k3,v3);
		}
	public static <X,Y> FunctionalMap<X,Y> of(X k1,Y v1,X k2,Y v2,X k3,Y v3,X k4,Y v4) {
		return of(k1,v1,k2,v2,k3,v3).plus(k4,v4);
		}
	public static <X,Y> FunctionalMap<X,Y> of(X k1,Y v1,X k2,Y v2,X k3,Y v3,X k4,Y v4,X k5,Y v5) {
		return of(k1,v1,k2,v2,k3,v3,k4,v4).plus(k5,v5);
		}
	public static <X,Y> FunctionalMap<X,Y> of(X k1,Y v1,X k2,Y v2,X k3,Y v3,X k4,Y v4,X k5,Y v5,X k6,Y v6) {
		return of(k1,v1,k2,v2,k3,v3,k4,v4,k5,v5).plus(k6,v6);
		}
	public static <X,Y> FunctionalMap<X,Y> of(X k1,Y v1,X k2,Y v2,X k3,Y v3,X k4,Y v4,X k5,Y v5,X k6,Y v6,X k7,Y v7) {
		return of(k1,v1,k2,v2,k3,v3,k4,v4,k5,v5,k6,v6).plus(k7,v7);
		}
	public static <X,Y> FunctionalMap<X,Y> of(X k1,Y v1,X k2,Y v2,X k3,Y v3,X k4,Y v4,X k5,Y v5,X k6,Y v6,X k7,Y v7,X k8,Y v8) {
		return of(k1,v1,k2,v2,k3,v3,k4,v4,k5,v5,k6,v6,k7,v7).plus(k8,v8);
		}
	}
