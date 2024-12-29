package sandbox.lisp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("serial")
public class LispContext extends HashMap<LispSymbol,LispNode> {
	public LispContext() {
        super();
    }
    public LispContext(final Map<LispSymbol, LispNode> env) {
        super(env);
    }
    public LispNode alias(LispSymbol from, LispSymbol to) {
        return put(to, get(from));
    }
    
    public LispNode alias(String from, String to) {
        return alias(LispSymbol.of(from),LispSymbol.of(to));
    	}
    
    public void define(String name, Function<List<LispNode>,LispNode> fun) {
    	put(LispSymbol.of(name), LispFunction.of(fun));
    }
}
