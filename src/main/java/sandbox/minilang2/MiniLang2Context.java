package sandbox.minilang2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sandbox.lang.StringWrapper;

public class MiniLang2Context {
	static final int OP_DIVIDE   = 100;
	static final int OP_MULTIPLY = 101;
	static final int OP_PLUS = 102;
	static final int OP_MINUS = 103;
	static final int OP_NEGATE = 104;
	static final int OP_VARIABLE_ASSIGN = 200;
	static final int OP_WRAPPED = 300;
	static final int OP_FUN_CALL = 400;
	
	
	static class Variable extends StringWrapper {
		Variable(final String s) {
			super(s);
			}
		}
	
	abstract class Function
		{
		final String funName;
		List<Object> list;
		Map<String, Object> map;
		Function(final String funName) {
			this.funName = funName;
			}
		void arity(int n) {}
		public abstract Object eval();
		}
	private final Map<Variable,Object> id2variable = new HashMap<>();
	private final Map<String,Function> functions = new HashMap<>();
		
	
	
	
	public MiniLang2Context() {
		Function fun = new Function("sqrt") {
			@Override
			public Object eval() {
				arity(1);
				Object o=  this.list.get(0);
				BigInteger bi=(BigInteger)o;
				return bi.sqrt();
				}
			};
		this.functions.put(fun.funName, fun);
		}
	
	
	
	Object apply(Object left,char op,Object right) {
		switch(op) {
			case '+': case '-' : case '*' : case '/':
				if(isNumber(left) && isNumber(right)) {
					if(isInteger(left) && isInteger(right)) {
						BigInteger a = toBigInteger(left);
						BigInteger b = toBigInteger(right);
						switch(op) {
							case '+': return a.add(b); 
							case '-': return a.subtract(b); 
							case '*': return a.multiply(b); 
							case '/': return a.divide(b); 
							default: throw new IllegalArgumentException();
							}
						}
					else
						{
						BigDecimal a = toBigDecimal(left);
						BigDecimal b = toBigDecimal(right);
						switch(op) {
							case '+': return a.add(b); 
							case '-': return a.subtract(b); 
							case '*': return a.multiply(b); 
							case '/': return a.divide(b); 
							default: throw new IllegalArgumentException();
							}						
						}
					}
				else if(isString(left) && op=='+') {
					String s = toString(left);
					return s + String.valueOf(right);
					}
				break;
			default: throw new IllegalArgumentException("type:"+op);
			}
		throw new IllegalArgumentException();
		}
	
	Object put(final Variable key,Object value) {
		this.id2variable.put(key, value);
		return value;
		}

	Variable createVariable(String name) {
		return new Variable(name);
		}
	Object call(String funName,List<Object> array,Map<String, Object> hash) {
		Function fun = this.functions.get(funName);
		if(fun==null) throw new IllegalArgumentException("Cannot fin "+funName);
		fun.list = array;
		fun.map= hash;
		return fun;
		}
	
	Object eval(String funName,List<Object> array,Map<String, Object> hash) {
		throw new IllegalStateException(""+funName);
		}
	
	static boolean isNil(Object o) {
		return o==null;
		}
	
	static boolean isArray(Object o) {
		return !isNil(o) && (o instanceof List);
		}
	static boolean isMap(Object o) {
		return !isNil(o) && (o instanceof Map);
		}
	static boolean isString(Object o) {
		return !isNil(o) && (o instanceof String);
		}
	static boolean isNumber(Object o) {
		return isInteger(o) || isFloating(o);
		}
	static boolean isInteger(Object o) {
		return !isNil(o) && (o instanceof Integer || o instanceof Long || o instanceof BigInteger) ;
		}
	static String toString(Object o) {
		return String.valueOf(o);
		}
	static BigInteger toBigInteger(Object o) {
		if(isNil(o)) throw new IllegalArgumentException("not an integer "+o);
		if(o instanceof Integer) {
			return BigInteger.valueOf(Integer.class.cast(o));
			}
		if(o instanceof Long) {
			return BigInteger.valueOf(Long.class.cast(o));
			}
		if(o instanceof BigInteger) {
			return BigInteger.class.cast(o);
			}
		throw new IllegalArgumentException("not an integer "+o);
		}
	
	static BigDecimal toBigDecimal(Object o) {
		if(isNil(o)) throw new IllegalArgumentException("not a decimal "+o);
		if(o instanceof Integer) {
			return BigDecimal.valueOf(Integer.class.cast(o));
			}
		if(o instanceof Long) {
			return BigDecimal.valueOf(Long.class.cast(o));
			}
		if(o instanceof BigInteger) {
			return new BigDecimal(BigInteger.class.cast(o));
			}
		if(o instanceof BigDecimal) {
			return BigDecimal.class.cast(o);
			}
		if(o instanceof Double) {
			return BigDecimal.valueOf(Double.class.cast(o));
			}
		throw new IllegalArgumentException("not a decimal "+o);
		}
	
	static boolean isFloating(Object o) {
		return !isNil(o) && (o instanceof Double || o instanceof BigDecimal) ;
		}
	
	
	
	Number parseFloat(final String s) {
		try {
			return Double.parseDouble(s);
			}
		catch(NumberFormatException err) {
			return new BigDecimal(s);
			}
		}
	
	Number parseInt(final String s) {
		try {
			return Integer.parseInt(s);
			}
		catch(NumberFormatException err) {
			try {
				return Long.parseLong(s);
				}
			catch(NumberFormatException err2) {
				return new BigInteger(s);
				}
			}
		}
	
	Object apply(char op,Object o) {
		switch(op) {
			case '-':  return apply(o, '*',-1);	
			default: throw new IllegalArgumentException("type:"+op);
				
			}
		}
	}
