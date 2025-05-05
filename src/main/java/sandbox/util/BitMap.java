package sandbox.util;

import java.util.BitSet;
import java.util.Objects;

public class BitMap {
	private final int _width;
	private final int _height;
	private final BitSet bitSet;
	public BitMap(int width,int height) {
		this._width = width;
		this._height = height;
		this.bitSet=new BitSet(_width*_height);
		}
	
	public BitMap(final BitMap cp) {
		this._width = cp.getWidth();
		this._height = cp.getHeight();
		this.bitSet = BitSet.class.cast(cp.bitSet.clone());
		}
	
	private int getWidth() {
		return _width;
		}
	public int getHeight() {
		return _height;
		}
	private int index(int x,int y) {
		return y*_width+x;
		}
	public boolean get(int x,int y) {
		return this.bitSet.get(index(x,y));
	}
	public void set(int x,int y,boolean b) {
		this.bitSet.set(index(x,y),b);
	}

	@Override
	public int hashCode() {
		return Objects.hash(_height, _width, bitSet);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final BitMap other = (BitMap) obj;
		return _height == other._height && _width == other._width && 
				Objects.equals(bitSet, other.bitSet);
		}
	
	@Override
	protected BitMap clone() {
		return new BitMap(this);
		}

	@Override
	public String toString() {
		return "BitMap("+getWidth()+","+getHeight()+")";
		}
	}
