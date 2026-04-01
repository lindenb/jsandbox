package sandbox.scalar;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Supplier;

public interface Scalar extends Supplier<Object> {
	public static Scalar nil() {
		return wrap(null);
		}
	public static Scalar wrap(final Object o) {
		if(o==null) return ScalarImpl.NIL;
		if(o instanceof Scalar) return Scalar.class.cast(o);
		return new ScalarImpl(o);
	}
	
	public default boolean isNil() { return isNull();}
	public default boolean isNull() { return get()==null;}
	
	public default boolean isNumber() {
		return isFloating() || isInteger();
	}
	
	public default boolean isFloating() {
		return isFloat() || isDouble() || isBigDecimal();
		}
	public default boolean isInteger() {
		return isBoolean() || isByte() || isShort() || isInt() || isLong() || isBigInteger();
		}
	public default boolean isInstanceOf(Class<?> clazz) {
		return clazz.isInstance(get());
	}
	public default boolean isBoolean() {return isInstanceOf(Boolean.class);}
	public default boolean isByte() {return isInstanceOf(Byte.class);}
	public default boolean isShort() {return isInstanceOf(Short.class);}
	public default boolean isInt() {return isInstanceOf(Integer.class);}
	public default boolean isLong() {return isInstanceOf(Long.class);}
	public default boolean isBigInteger() {return isInstanceOf(BigInteger.class);}
	public default boolean isFloat() {return isInstanceOf(Float.class);}
	public default boolean isDouble() {return isInstanceOf(Double.class);}
	public default boolean isBigDecimal() {return isInstanceOf(BigDecimal.class);}
	public default boolean isString() {return isInstanceOf(String.class);}

	public default BigInteger toBigInteger() {
		if(isBoolean()) return BigInteger.valueOf(Boolean.class.cast(get())?1L:0L);
		if(isByte()) return BigInteger.valueOf(Byte.class.cast(get()));
		if(isShort()) return BigInteger.valueOf(Short.class.cast(get()));
		if(isInt()) return BigInteger.valueOf(Integer.class.cast(get()));
		if(isLong()) return BigInteger.valueOf(Long.class.cast(get()));
		if(isBigDecimal()) return BigInteger.class.cast(get());
		return new BigInteger(this.toString());
		}
	
	public default BigDecimal toBigDecimal() {
		if(isInteger()) return new BigDecimal(toBigInteger());
		if(isFloat()) return BigDecimal.valueOf(Float.class.cast(get()));
		if(isDouble()) return BigDecimal.valueOf(Double.class.cast(get()));
		if(isBigDecimal()) return BigDecimal.class.cast(get());
		return new BigDecimal(this.toString());
		}

}
