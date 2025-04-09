package sandbox.minilang.minilang2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import sandbox.lang.StringWrapper;
import sandbox.minilang.AbstractMiniLangContext;

public class MiniLang2Context extends AbstractMiniLangContext {
	static final int OP_DIVIDE   = 100;
	static final int OP_MULTIPLY = 101;
	static final int OP_PLUS = 102;
	static final int OP_MINUS = 103;
	static final int OP_NEGATE = 104;
	static final int OP_VARIABLE_ASSIGN = 200;
	static final int OP_WRAPPED = 300;
	static final int OP_FUN_CALL = 400;
	
	
	private static class ObjectWrapper<Y> implements java.util.function.Supplier<Y>
		{
		private final Y object;
		ObjectWrapper(final Y object) {
			this.object = object;
			}
		@Override
		public Y get() {
			return object;
			}
		@Override
		public String toString() {
			return String.valueOf(get());
			}
		};
	
	class Variable extends StringWrapper implements java.util.function.Supplier<Object> {
		Variable(final String s) {
			super(s);
			}
		@Override
		public Object get() {
				return MiniLang2Context.this.id2variable.get(this);
				};
		}
		
		
	
	abstract class Function implements java.util.function.Supplier<Object>
		{
		final String funName;
		List<Supplier<Object>> list;
		Map<String, Supplier<Object>> map;
		Function(final String funName) {
			this.funName = funName;
			}
		List<Supplier<Object>> arity(int n) {return list.subList(0, n);}
		Supplier<Object> arity1() {return arity(1).get(0); }
		public abstract Supplier<Object> get();
		}
	private final Map<Variable,Object> id2variable = new HashMap<>();
	private final Map<String,Function> functions = new HashMap<>();
		
	
	
	
	public MiniLang2Context() {
		Function fun = new Function("sqrt") {
			@Override
			public Supplier<Object> get() {
				arity(1);
				Supplier<Object> o=  this.list.get(0);
				BigInteger bi=(BigInteger)o.get();
				return wrap(bi.sqrt());
				}
			};
		this.functions.put(fun.funName, fun);
		
		fun = new Function("parseInt") {
			@Override
			public Supplier<Object> get() {
				Supplier<Object> o=arity1();
				BigInteger bi=(BigInteger)o.get();
				return wrap(bi);
				}
			};
		this.functions.put(fun.funName, fun);
		
		fun = new Function("parseDouble") {
			@Override
			public Supplier<Object> get() {
				Supplier<Object> o=arity1();
				BigInteger bi=(BigInteger)o.get();
				return wrap(bi);
				}
			};
		this.functions.put(fun.funName, fun);
		}
	
	
	
	Supplier<Object> apply(Supplier<Object> opt_left,char op,Supplier<Object> opt_right) {
		return ()->{
			final Object left = opt_left.get();
			final Object right= opt_right.get();
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
						return wrap(s + String.valueOf(right));
						}
					break;
				default: throw new IllegalArgumentException("type:"+op);
				}
			throw new IllegalArgumentException();
			};
		}
	
	Supplier<Object> put(final Variable key,Supplier<Object> value) {
		this.id2variable.put(key, value.get());
		return value;
		}
	


	Supplier<Object> wrap(final Object o) {
		if(o!=null && (o instanceof Variable)) {
			return Variable.class.cast(o);
			}
		return new ObjectWrapper<>(o);
		}
	
	Variable createVariable(final String name) {
		return  new Variable(name);
		}
	
	Supplier<Object> call(String funName,List<Supplier<Object>> array,Map<String, Supplier<Object>> hash) {
		Function fun = this.functions.get(funName);
		if(fun==null) throw new IllegalArgumentException("Cannot fin "+funName);
		fun.list = array;
		fun.map= hash;
		return fun;
		}
	
	Object eval(String funName,List<Object> array,final Map<String, Object> hash) {
		throw new IllegalStateException(""+funName);
		}
	
	
	Supplier<Object> apply(char op,Supplier<Object> opt) {
		return ()->{
			switch(op) {
				case '-':  return apply(opt, '*',wrap(-1));	
				default: throw new IllegalArgumentException("type:"+op);
					
				}
			};
		}
}