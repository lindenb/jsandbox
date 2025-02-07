package sandbox.lisp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;



public class LispContext {
	
	LispContext() {
		register("+",N->N.arity(1,-1).stream().map(it->toFloating(eval(it))).reduce(BigDecimal.ZERO,BigDecimal::add));
		
		register("-",N-> N.arity(1,-1).stream().map(it->toFloating(eval(it))).reduce(BigDecimal.ZERO,BigDecimal::add));

		}
	
	protected void register(String name,Function<List<Object>,Object> fun) {
		
		}
	
	public Object eval(Object o) {
		if(isNode(o))  {
			return evalNode(LispNode.class.cast(o));
			}
		if(isPrimitive(o)) return o;
		if(isAtom(o)) {
			String atom = LispParser.Atom.class.cast(o).toString();
			if(atom.equals("PI")) return BigDecimal.valueOf(Math.PI);
			return o;
			}
		throw new IllegalArgumentException();
		}
	
	
	
	
	protected boolean isPrimitive(Object o) {
		return isNumber(o) || isBoolean(o) || isString(o) || isNil(o); 
		}
	protected boolean isInstanceOf(Object o, final Class<?> clazz) {
		return o!=null && clazz.isInstance(o);
		}
	protected boolean isNil(Object o) {
		return o==null;
		}
	protected boolean isFloating(Object o) {
		return isInstanceOf(o,BigDecimal.class) || isInstanceOf(o,Double.class) || isInstanceOf(o,Float.class);
		}
	protected boolean isInteger(Object o) {
		return isInstanceOf(o,BigInteger.class) || isInstanceOf(o,Long.class) || isInstanceOf(o,Integer.class);
		}
	protected boolean isNumber(Object o) {
		return isInstanceOf(o,Number.class);
		}
	protected boolean isString(Object o) {
		return isInstanceOf(o,String.class);
		}
	protected boolean isNode(Object o) {
		return isInstanceOf(o,LispNode.class);
		}
	protected boolean isBoolean(Object o) {
		return isInstanceOf(o,Boolean.class);
		}
	
	protected boolean isAtom(Object o) {
		return isInstanceOf(o,LispParser.Atom.class);
		}
	
	protected BigDecimal toFloating(Object o) {
		if(isInstanceOf(o,BigDecimal.class)) return BigDecimal.class.cast(o);
		if(isInstanceOf(o,Double.class)) return BigDecimal.valueOf(Double.class.cast(o));
		if(isInstanceOf(o,Float.class)) return BigDecimal.valueOf(Float.class.cast(o));
		return new BigDecimal(toInteger(o));
		}
	protected BigInteger toInteger(Object o) {
		if(isInstanceOf(o,BigInteger.class)) return BigInteger.class.cast(o);
		if(isInstanceOf(o,Long.class)) return BigInteger.valueOf(Long.class.cast(o));
		if(isInstanceOf(o,Integer.class)) return BigInteger.valueOf(Integer.class.cast(o));
		if(isInstanceOf(o,Short.class)) return BigInteger.valueOf(Short.class.cast(o));
		throw new IllegalArgumentException("cannot convert to toInteger");
		}
	protected String toString(Object o) {
		if(isInstanceOf(o,CharSequence.class)) return CharSequence.class.cast(o).toString();
		throw new IllegalArgumentException("cannot convert to String");
		}
	
	protected boolean toBoolean(Object o) {
		if(o==null) return false;
		if(isInstanceOf(o, Boolean.class)) return Boolean.class.cast(o).booleanValue();
		if(isInteger(o))return toInteger(o).equals(BigInteger.ZERO)==false;
		throw new IllegalArgumentException("cannot convert to boolean");
		}
	
	protected Object evalNode(LispNode n) {
		final String name= n.getName();
		if(name.equals("if")) {
			n.arity(3);
			return toBoolean(eval(n.get(0))) ? eval(n.get(1)): eval(n.get(2));
			}
		
		
		List<Object> values = new ArrayList<>(n.size());
		for(int i=0;i< n.size();i++) {
			values.add(eval(n.get(i)));
			}
		return evalValues(new LispParser.LispNodeImpl(n.getName(),values));
		}
	
	protected Object evalValues(LispNode n) {
		final String name= n.getName();
		if(name.equals("+")) {
			if(n.stream().allMatch(it->isFloating(it))) {
				return n.stream().map(it->toFloating(eval(it))).reduce(BigDecimal.ZERO,BigDecimal::add);
				}
			if(n.stream().allMatch(it->isInteger(it))) {
				return n.stream().map(it->toInteger(eval(it))).reduce(BigInteger.ZERO,BigInteger::add);
				}
			throw new IllegalArgumentException("for "+name);
			}
		if(name.equals("-")) {
			if(n.stream().allMatch(it->isFloating(it))) {
				return n.stream().map(it->toFloating(eval(it))).reduce(BigDecimal.ZERO,BigDecimal::subtract);
				}
			if(n.stream().allMatch(it->isInteger(it))) {
				return n.stream().map(it->toInteger(eval(it))).reduce(BigInteger.ZERO,BigInteger::subtract);
				}
			throw new IllegalArgumentException("for "+name);
			}
		if(name.equals("*")) {
			if(n.stream().allMatch(it->isFloating(it))) {
				return n.stream().map(it->toFloating(eval(it))).reduce(BigDecimal.ONE,BigDecimal::multiply);
				}
			if(n.stream().allMatch(it->isInteger(it))) {
				return n.stream().map(it->toInteger(eval(it))).reduce(BigInteger.ONE,BigInteger::multiply);
				}
			throw new IllegalArgumentException("for "+name);
			}
		if(name.equals("/")) {
			if(n.stream().allMatch(it->isFloating(it))) {
				return n.stream().map(it->toFloating(eval(it))).reduce(BigDecimal.ONE,BigDecimal::divide);
				}
			if(n.stream().allMatch(it->isInteger(it))) {
				return n.stream().map(it->toInteger(eval(it))).reduce(BigInteger.ONE,BigInteger::divide);
				}
			throw new IllegalArgumentException("for "+name);
			}
		if(name.equals("%")) {
			throw new IllegalArgumentException("for "+name);
			}
		
		/** aggregate function */
		if(name.equals("min")) {
			if(n.stream().allMatch(it->isString(it))) {
				return n.arity(1,-1).stream().map(it->toString(eval(it))).min(String::compareTo);
				}
			if(n.stream().allMatch(it->isInteger(it))) {
				return n.arity(1,-1).stream().map(it->toInteger(eval(it))).min(BigInteger::compareTo);
				}
			if(n.stream().allMatch(it->isFloating(it))) {
				return n.arity(1,-1).stream().map(it->toFloating(eval(it))).min(BigDecimal::compareTo);
				}
			throw new IllegalArgumentException("for "+name);
			}
		if(name.equals("max")) {
			if(n.stream().allMatch(it->isString(it))) {
				return n.arity(1,-1).stream().map(it->toString(eval(it))).max(String::compareTo);
				}
			if(n.stream().allMatch(it->isInteger(it))) {
				return n.arity(1,-1).stream().map(it->toInteger(eval(it))).max(BigInteger::compareTo);
				}
			if(n.stream().allMatch(it->isFloating(it))) {
				return n.arity(1,-1).stream().map(it->toFloating(eval(it))).max(BigDecimal::compareTo);
				}
			throw new IllegalArgumentException("for "+name);
			}
		
		
		/** triogonometric functions */
		if(name.equals("cos")) {
			return Math.cos(toFloating(n.arity(1).get(0)).doubleValue());
			}
		if(name.equals("sin")) {
			return Math.sin(toFloating(n.arity(1).get(0)).doubleValue());
			}
		if(name.equals("tan")) {
			return Math.tan(toFloating(n.arity(1).get(0)).doubleValue());
			}
		if(name.equals("sqrt")) {
			return Math.sqrt(toFloating(n.arity(1).get(0)).doubleValue());
			}
		if(name.equals("abs")) {
			n.arity(1);
			if(isInteger(n.get(0))) return toInteger(n.get(0)).abs();
			 return toFloating(n.arity(1).get(0)).abs();
			}
		/** math compare */
		if(name.equals("<") || name.equals("<=")|| name.equals(">")|| name.equals(">=")|| name.equals("==") || name.equals("!=")) {
			n.arity(2);
			int diff;
			if(isInteger(n.get(0)) && isInteger(n.get(1)) ) {
				diff = toInteger(n.get(0)).compareTo(toInteger(n.get(1))) ;
				}
			else if(isFloating(n.get(0)) || isFloating(n.get(1)) ) {
				diff = toFloating(n.get(0)).compareTo(toFloating(n.get(1))) ;
				}
			else if(isString(n.get(0)) || isString(n.get(1)) ) {
				diff = toString(n.get(0)).compareTo(toString(n.get(1))) ;
				}
			if(name.equals("<")) return diff <0;
			if(name.equals("<=")) return diff <= 0; 
			if(name.equals(">=")) return diff >= 0; 
			if(name.equals(">")) return diff > 0; 
			if(name.equals("==")) return diff == 0; 
			if(name.equals("!=")) return diff != 0; 
			throw new IllegalArgumentException();
			}
		
		
		throw new IllegalArgumentException();
		}
	
	
	
	}
