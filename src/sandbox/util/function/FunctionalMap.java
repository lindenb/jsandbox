package sandbox.util.function;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;


public interface FunctionalMap<K,V> {

	public boolean isEmpty();
	public V get(K k);
	public int size();
	public FunctionalMap<K,V> remove(K key);
	public FunctionalMap<K,V> removeAll(Collection<K> keys);
	public FunctionalMap<K,V> put(K key, V value);
	public FunctionalMap<K,V> putAll(Map<K,V> map);
	public FunctionalMap<K,V> putAll(FunctionalMap<K,V> map);
	public FunctionalSet<K> keySet();
	public Stream<Map.Entry<K,V>> stream();
	public Map<K,V> asMap();

	public static <K,V> FunctionalMap<K,V> empty()  {
		return new MapImpl<>();
		}
	
	public static <K,V> FunctionalMap<K,V> of(final Map<K,V> h)  {
		return new MapImpl<>(h);
	}
	
	static class MapImpl<K,V> implements FunctionalMap<K,V>{
		private final Map<K,V> hash;
		MapImpl() {
			this.hash = Collections.emptyMap();
			}
		MapImpl(final Map<K,V> hash) {
			this.hash = new HashMap<>(hash);
			}
		@Override
		public V get(final K key) {
			return this.hash.get(key);
			}
		@Override
		public boolean isEmpty() {
			return this.hash.isEmpty();
			}
		@Override
		public int size() {
			return this.hash.size();
			}
		
		@Override
		public MapImpl<K,V> remove(K key) {
			return removeAll(Collections.singleton(key));
			}
		@Override
		public MapImpl<K,V> removeAll(Collection<K> keys) {
			MapImpl<K,V> cp = new MapImpl<K,V>(this.hash);
			for(K key:keys) {
				cp.hash.remove(key);
				}
			return cp;
			}
		@Override
		public MapImpl<K,V> put(K key,V value) {
			MapImpl<K,V> cp = new MapImpl<K,V>(this.hash);
			cp.put(key,value);
			return cp;
			}
		@Override
		public MapImpl<K,V> putAll(Map<K,V> m) {
			MapImpl<K,V> cp = new MapImpl<K,V>(this.hash);
			cp.putAll(m);
			return cp;
			}
		@Override
		public MapImpl<K,V> putAll(FunctionalMap<K,V> m) {
			if(this==m) return this;
			final MapImpl<K,V> cp = new MapImpl<K,V>(this.hash);
			for(final K k:m.keySet()) {
				cp.hash.put(k, m.get(k));
				}
			return cp;
			}
		@Override
		public Stream<Map.Entry<K, V>> stream() {
			return this.hash.entrySet().stream();
			}
		@Override
		public FunctionalSet<K> keySet() {
			return FunctionalSet.of(this.hash.keySet());
			}
		@Override
		public Map<K,V> asMap() {
			return Collections.unmodifiableMap(this.hash);
			}
		
		public MapImpl<K,V> clone() {
			return new MapImpl<K,V>(this.hash);
			}
		}
	}
