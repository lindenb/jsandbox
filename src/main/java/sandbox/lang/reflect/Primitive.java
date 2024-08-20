package sandbox.lang.reflect;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class Primitive {
	private final Class<?> clazz;
	private final Class<?> boxed;
	private final Function<String,?> parseString;
	private Primitive(Class<?> clazz,Class<?> boxed,Function<String,?> parseString) {
		this.clazz=clazz;
		this.boxed=boxed;
		this.parseString = parseString;
		}
	
	public Class<?> getType() {
		return this.clazz;
		}
	
	public Class<?> getBoxedType() {
		return this.boxed;
		}

	/** ex. java.lang.Integer.TYPE */
	public String getTypeName() {
		return getBoxedType().getName()+".TYPE";
		}
	
	public boolean isBoolean() { return getType().equals(Boolean.TYPE);}
	public boolean isCharacter() { return getType().equals(Character.TYPE);}
	public boolean isByte() { return getType().equals(Byte.TYPE);}
	public boolean isInteger() { return getType().equals(Integer.TYPE);}
	public boolean isShort() { return getType().equals(Short.TYPE);}
	public boolean isLong() { return getType().equals(Long.TYPE);}
	public boolean isFloat() { return getType().equals(Float.TYPE);}
	public boolean isDouble() { return getType().equals(Double.TYPE);}
	public boolean isDecimal() { return isFloat() || isDouble();}
	public boolean isAnyInt() { return isByte() || isShort() || isInteger() || isLong();}
	public boolean isNumber() { return isDecimal() || isAnyInt();}
	
	
	@Override
	public boolean equals(final Object obj) {
		if(obj==this) return true;
		if(obj==null || !(obj instanceof Primitive)) return false;
		return Primitive.class.cast(obj).getType().equals(this.getType());
		}
	
	@Override
	public int hashCode() {
		return this.getType().hashCode();
		}
	
	@Override
	public String toString() {
		return getTypeName();
		}
	
	public Function<String,?> getParseStringFunction() {
		return this.parseString;
	}

	public Optional<Object> valueOf(final String s) {
		if(s==null) return Optional.empty();
		try {
			return Optional.of(getParseStringFunction().apply(s));
			}
		catch(Throwable err) {
			return Optional.empty();
			}
		}
	
	public boolean canParse(final String s) {
		return  valueOf(s).isPresent();
		}

public static final Primitive P_BOOLEAN=new Primitive(Boolean.TYPE,Boolean.class,Boolean::parseBoolean);
public static final Primitive P_BYTE=new Primitive(Byte.TYPE,Byte.class,Byte::parseByte);
public static final Primitive P_CHAR=new Primitive(Character.TYPE,Character.class,S->{if(S.length()!=1) return new IllegalArgumentException("expected a string with length==1");return S.charAt(0);});
public static final Primitive P_SHORT=new Primitive(Short.TYPE,Short.class,Short::parseShort);
public static final Primitive P_INT=new Primitive(Integer.TYPE,Integer.class,Integer::parseInt);
public static final Primitive P_LONG=new Primitive(Long.TYPE,Long.class,Long::parseLong);
public static final Primitive P_FLOAT=new Primitive(Float.TYPE,Float.class,Float::parseFloat);
public static final Primitive P_DOUBLE=new Primitive(Double.TYPE,Double.class,Double::parseDouble);

public static List<Primitive> getPrimitives() {
	return Arrays.asList(
		P_BOOLEAN,
		P_BYTE,
		P_CHAR,
		P_SHORT,
		P_INT,
		P_LONG,
		P_FLOAT,
		P_DOUBLE
		);
	}
public static Optional<Primitive> findPrimitiveByType(final Class<?> c) {
	return getPrimitives().stream().filter(P->P.getType().equals(c)).findFirst();
	}

public static Optional<Primitive> findPrimitiveByTypeOrBoxed(final Class<?> c) {
	return getPrimitives().stream().filter(P->P.getType().equals(c) || P.getBoxedType().equals(c)).findFirst();
	}


public static boolean isPrimitive(final Class<?> c) {
	return findPrimitiveByType(c).isPresent();
	}
}
