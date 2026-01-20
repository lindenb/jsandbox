package sandbox.scalar;

import java.util.Objects;

public class ScalarImpl implements Scalar {
	static final Scalar NIL= new ScalarImpl(null);
	private final Object data;
	public ScalarImpl(final Object data) {
		this.data = data;
		}
	
	@Override
	public Object get() {
		return data;
		}
	
	@Override
	public boolean equals(final Object obj) {
		if(obj==this) return true;
		if(obj == null || !(obj instanceof ScalarImpl)) return false;
		return Objects.equals(this.get(), ScalarImpl.class.cast(obj).get());
		}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(get());
	 	}
	
	@Override
	public String toString() {
		return String.valueOf(get());
		}
	}
