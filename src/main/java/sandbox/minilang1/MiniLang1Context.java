package sandbox.minilang1;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sandbox.lang.StringWrapper;

public class MiniLang1Context {
	static final int OP_DIVIDE   = 100;
	static final int OP_MULTIPLY = 101;
	static final int OP_PLUS = 102;
	static final int OP_MINUS = 103;
	static final int OP_NEGATE = 104;
	static final int OP_VARIABLE_ASSIGN = 200;
	static final int OP_WRAPPED = 300;
	static final int OP_FUN_CALL = 400;
	
	
	static abstract class ASTNode {
		int type;
		int depth=0;
		ASTValue asASTValue() {
			return ASTValue.class.cast(this);
			}
		}
	private static class ASTBiNode extends ASTNode {
		ASTNode left;
		ASTNode right;
		}
	private static class ASTUnaryNode extends ASTNode {
		ASTNode node;
		}
	private static class ASTValue extends ASTNode {
		WrappedValue value;
		}
	private static class ASTFunctionCall extends ASTNode {
		String funName;
		List<ASTNode> list;
		Map<String, ASTNode> map;
		}
	static class Variable extends StringWrapper {
		Variable(final String s) {
			super(s);
			}
		}
	
	abstract class Function
		{
		final String funName;
		List<ASTNode> list;
		Map<String, ASTNode> map;
		Function(final String funName) {
			this.funName = funName;
			}
		void arity(int n) {}
		public abstract Object eval(State state);
		}
	private static class State {
		private  State parent;
		private final Map<Variable,Object> id2variable = new HashMap<>();
		private final Map<String,Function> functions = new HashMap<>();
		}
	
	private static class WrappedValue {
		Object o;
		WrappedValue(Object o) {
			this.o=o;
		}
		boolean isNil() {
			return this.o==null;
			}
		}
	
	State state =new State();
	
	
	
	public MiniLang1Context() {
		Function fun = new Function("sqrt") {
			@Override
			public Object eval(State state) {
				arity(1);
				Object o= MiniLang1Context.this.eval(state,this.list.get(0));
				BigInteger bi=(BigInteger)o;
				return bi.sqrt();
				}
			};
		state.functions.put(fun.funName, fun);
		}
	
	private void initNode(ASTNode n,int type) {
		
		}
	
	ASTNode wrapValue(Object o) {
		ASTValue n=new ASTValue();
		initNode(n,OP_WRAPPED);
		n.value = new WrappedValue(o);
		return n;
		}
	
	ASTNode create(ASTNode left,int type,ASTNode right) {
		ASTBiNode n=new ASTBiNode();
		initNode(n,type);
		n.left = left;
		n.right = right;
		return n;
		}
	ASTNode create(int type,ASTNode node) {
		ASTUnaryNode n=new ASTUnaryNode();
		initNode(n,type);
		n.node = node;
		return n;
		}
	Object put(Variable key,Object value) {
		state.id2variable.put(key, value);
		return value;
		}

	ASTNode createVariable(String name) {
		return wrapValue(new Variable(name));
		}
	ASTNode call(String funName,List<ASTNode> array,Map<String, ASTNode> hash) {
		ASTFunctionCall fun = new ASTFunctionCall();
		initNode(fun,OP_FUN_CALL);
		fun.funName = funName;
		fun.list = array;
		fun.map= hash;
		return fun;
		}
	
	Object eval(State state,String funName,List<ASTNode> array,Map<String, ASTNode> hash) {
		throw new IllegalStateException(""+funName);
		}
	
	Object eval(State state,ASTNode node) {
		switch(node.type) {
			case OP_WRAPPED: {
				Object o= ASTValue.class.cast(node).value;
				if(o!=null && o instanceof Variable ) {
					o= state.id2variable.get(Variable.class.cast(o));
					}
				return o;
				}	
			case OP_FUN_CALL:
				ASTFunctionCall fc = ASTFunctionCall.class.cast(node);
				Function fun = state.functions.get(fc.funName);
				fun.list = fc.list;
				fun.map = fc.map;
				return fun.eval(state);
			default: throw new IllegalStateException(""+node.type);
			}
		}
	}
