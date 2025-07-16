package sandbox.rdf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Literal implements RDFNode {
	@SuppressWarnings("rawtypes")
	private final Comparable  object;
	
	public Literal(LocalDate i) {
		this.object= Objects.requireNonNull(i);
	}
	
	public Literal(LocalDateTime i) {
		this.object= Objects.requireNonNull(i);
	}
	
	public Literal(BigDecimal i) {
		this.object= Objects.requireNonNull(i);
	}
	
	public Literal(double i) {
		this(BigDecimal.valueOf(i));
	}
	
	public Literal(float i) {
		this(BigDecimal.valueOf(i));
	}
	
	public Literal(BigInteger i) {
		this.object= Objects.requireNonNull(i);
	}
	public Literal(long i) {
		this(BigInteger.valueOf(i));
	}
	public Literal(short i) {
		this(BigInteger.valueOf(i));
	}
	public Literal(byte i) {
		this(BigInteger.valueOf(i));
	}
	public Literal(final String s) {
		this.object= Objects.requireNonNull(s);
	}
	public Literal(boolean i) {
		this.object= i;
	}
	
	public boolean isInteger() {
		return this.object instanceof BigInteger;
	}
	
	public boolean isFloating() {
		return this.object instanceof BigInteger;
	}
	
	public boolean isNumber() {
		return this.isInteger() || isFloating();
	}
	
	public boolean isString() {
		return this.object instanceof String;
	}
	
	public Object getObject() {
		return this.object;
		}
	
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(final RDFNode o) {
		if(o.isResource()) return 1;
		Literal l = o.asLiteral();
		int i= getObject().getClass().getName().compareTo(l.getObject().getClass().getName());
		if(i!=0) return i;
		return this.object.compareTo(o.asLiteral().object);
		}
	
	@Override
	public int hashCode() {
		return object.hashCode();
		}
	
	@Override
	public final boolean isResource() {
		return false;
		}
	@Override
	public final boolean isLiteral() {
		return true;
		}
}
