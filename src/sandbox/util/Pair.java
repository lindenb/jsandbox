package sandbox.util;

import java.util.AbstractMap;
import java.util.Map;

public interface Pair<K,V> {
	public boolean hasKey(K k);
	public boolean hasValue(V v);
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
		public boolean hasKey(K k) {
			return equals(getKey(),k);
			}
		@Override
		public boolean hasValue(V v) {
			return equals(getValue(),v);
			}
		@Override
		public int hashCode() {
			return (k==null?0:k.hashCode())*31+(v==null?0:v.hashCode());
			}
		
		private boolean equals(Object a,Object b) {
			if(a==null && b==null) return true;
			if(a==null || b==null) return false;
			return a.equals(b);
			}
		@Override
		public boolean equals(Object obj) {
			if(obj ==this) return true;
			if(obj==null || !(obj instanceof Pair)) return false;
			Pair other=(Pair)obj;
			return equals(other.getKey(),this.getKey()) && equals(other.getValue(),this.getValue());
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
