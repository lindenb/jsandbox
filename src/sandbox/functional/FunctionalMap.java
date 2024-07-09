package sandbox.functional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import sandbox.util.Sets;

import java.util.Map.Entry;

public class FunctionalMap<K,V> implements Iterable<Map.Entry<K, V>> {
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
	
	public FunctionalMap<K,V> plus(K k1,V v1) {
		FunctionalMap<K,V> o = clone();
		o.delegate.put(k1,v1);
		return o;
		}
	public FunctionalMap<K,V> plus(K k1,V v1,K k2,V v2) {
		FunctionalMap<K,V> o = plus(k1,v1);
		o.delegate.put(k2,v2);
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
		return removeIf((k,v)->toRemove.contains(k));
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
	public Iterator<Entry<K, V>> iterator() {
		return entrySet().iterator();
		}
	
	public Set<K> keySet() {
		return Collections.unmodifiableSet(this.delegate.keySet());
		}
	public Collection<V> values() {
		return Collections.unmodifiableCollection(this.delegate.values());
		}
	public Set<Map.Entry<K,V>> entrySet() {
		return Collections.unmodifiableSet(this.delegate.entrySet());
		}
	public Stream<Map.Entry<K,V>> stream() {
		return entrySet().stream();
		}

	@Override
	protected FunctionalMap<K,V> clone() {
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
}
