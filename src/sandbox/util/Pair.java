package sandbox.util;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;

public interface Pair<K,V> {
	public default boolean hasKey(K k) {
		return Objects.equals(getKey(),k);
		}
	public default boolean hasValue(V v) {
		return Objects.equals(getValue(),v);
		}
	public K getKey();
	public V getValue();
	public default Map.Entry<K, V> toEntry() {
		return new AbstractMap.SimpleEntry<>(getKey(), getValue());
		}
	/** must be immutable */
	public static <K,V> Pair<K,V> of(K k,V v) {
		return new PairImpl<K,V>(k,v);
		}
	static class PairImpl<K,V> implements Pair<K,V> {
		private final K k;
		private final V v;
		PairImpl(K k,V v) {
			this.k=k;
			this.v=v;
			}

		@Override
		public int hashCode() {
			return Objects.hash(getKey(),getValue());
			}
		
		
		@Override
		public boolean equals(Object obj) {
			if(obj ==this) return true;
			if(obj==null || !(obj instanceof Pair)) return false;
			@SuppressWarnings("rawtypes")
			Pair other=(Pair)obj;
			return Objects.equals(other.getKey(),this.getKey()) && 
					Objects.equals(other.getValue(),this.getValue());
			}
		
		@Override
		public K getKey() {
			return k;
			}
		@Override
		public V getValue() {
			return v;
			}
		@Override
		public String toString() {
			return "["+getKey()+":"+getValue()+"]";
			}
		}
}
