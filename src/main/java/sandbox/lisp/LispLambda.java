package sandbox.lisp;

import java.util.List;


class LispLambda extends LispFunction {
    private final List<LispNode> params;
    private final List<LispNode>  body;
    private final LispContext env;
    private final LispEngine engine;
    LispLambda(List<LispNode>  params, List<LispNode>  body, LispContext env, LispEngine engine) {
        this.params = params;
        this.body = body;
        this.env = env;
        this.engine = engine;
    	}
    
    
    @Override
    public LispNode apply(final List<LispNode> args) {
        final LispContext tempEnv = new LispContext(env);
        for (int i = 0; i < params.size(); i++) {
            final LispSymbol param = (LispSymbol) params.get(i);
            final LispNode arg = args.get(i);
            tempEnv.put(param, arg);
        	}
        return engine.evaluate(LispPair.of( body), tempEnv);
    	}
    @Override
    protected List<?> getParameterHelpNames() {
        return params;
    	}
}
