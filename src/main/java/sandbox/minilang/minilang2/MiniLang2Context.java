package sandbox.minilang.minilang2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import sandbox.lang.StringWrapper;
import sandbox.scalar.Scalar;

public class MiniLang2Context {
	static final int OP_DIVIDE   = 100;
	static final int OP_MULTIPLY = 101;
	static final int OP_PLUS = 102;
	static final int OP_MINUS = 103;
	static final int OP_NEGATE = 104;
	static final int OP_VARIABLE_ASSIGN = 200;
	static final int OP_WRAPPED = 300;
	static final int OP_FUN_CALL = 400;
	
	
	class Variable extends StringWrapper implements Scalar{
		Variable(final String s) {
			super(s);
			}
		@Override
		public Object get() {
			return MiniLang2Context.this.id2variable.get(this);
			};
		}
		
		
	
	abstract class Function
		{
		List<Scalar> list;
		Map<String, Scalar> map;
		List<Scalar> arity(int n) {return list.subList(0, n);}
		Scalar arity1() {return arity(1).get(0); }
		public abstract Scalar get();
		}
	private final Map<Variable,Scalar> id2variable = new HashMap<>();
	private final Map<String,Function> functions = new HashMap<>();
		
	
	
	
	public MiniLang2Context() {
		registerFunction("sqrt",  new Function() {
			@Override
			public Scalar get() {
				arity(1);
				Scalar o=  this.list.get(0);
				BigInteger bi= o.toBigInteger();
				return wrap(bi.sqrt());
				}
			});
		
		registerFunction("parseInt", new Function() {
			@Override
			public Scalar get() {
				Scalar o=arity1();
				return wrap(o.toBigInteger());
				}
			});
		
		registerFunction("parseDouble",  new Function() {
			@Override
			public Scalar get() {
				Scalar o=arity1();
				return wrap(o.toBigDecimal());
				}
			});
		}
	
	public void registerFunction(final String fn, Function fun) {
		this.functions.put(fn, fun);
	}
	
	Scalar apply(Scalar left,char op,Scalar right) {
		return ()->{
			switch(op) {
				case '+': case '-' : case '*' : case '/':
					if(left.isNumber() && right.isNumber()) {
						if(left.isInteger() && right.isInteger()) {
							BigInteger a = left.toBigInteger();
							BigInteger b =right. toBigInteger();
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
							BigDecimal a = left.toBigDecimal();
							BigDecimal b = right.toBigDecimal();
							switch(op) {
								case '+': return a.add(b); 
								case '-': return a.subtract(b); 
								case '*': return a.multiply(b); 
								case '/': return a.divide(b); 
								default: throw new IllegalArgumentException();
								}						
							}
						}
					else if(left.isString() && op=='+') {
						String s = left.toString();
						return wrap(s + String.valueOf(right));
						}
					break;
				default: throw new IllegalArgumentException("type:"+op);
				}
			throw new IllegalArgumentException();
			};
		}
	
	Scalar put(final Variable key,Scalar value) {
		this.id2variable.put(key, value);
		return value;
		}
	


	Scalar wrap(final Object o) {
		return Scalar.wrap(o);
		}
	
	Variable createVariable(final String name) {
		return  new Variable(name);
		}
	
	Scalar call(String funName,List<Scalar> array,Map<String, Scalar> hash) {
		Function fun = this.functions.get(funName);
		if(fun==null) throw new IllegalArgumentException("Cannot fin "+funName);
		fun.list = array;
		fun.map= hash;
		return fun.get();
		}
	
	Object eval(String funName,List<Scalar> array,final Map<String, Scalar> hash) {
		throw new IllegalStateException(""+funName);
		}
	
	
	Scalar apply(char op,Scalar opt) {
		return ()->{
			switch(op) {
				case '-':  return apply(opt, '*',wrap(-1));	
				default: throw new IllegalArgumentException("type:"+op);
					
				}
			};
		}
}
