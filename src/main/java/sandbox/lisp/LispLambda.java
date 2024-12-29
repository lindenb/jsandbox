package sandbox.lisp;

import java.util.List;


class LispLambda extends LispFunction {
    private final LispList params;
    private final LispList  body;
    private final LispEngine engine;
    LispLambda(LispList  params, LispList body, LispEngine engine) {
        this.params = params;
        this.body = body;
        this.engine = engine;
    	}
    
    
    @Override
    public LispNode apply(final LispList args,LispContext env) {
        final LispContext tempEnv = new LispContext(env);
        for (int i = 0; i < params.size(); i++) {
            final LispSymbol param = (LispSymbol) params.get(i);
            final LispNode arg = args.get(i);
            tempEnv.put(param, arg);
        	}
        return engine.evaluate(body, tempEnv);
    	}
    @Override
    protected List<?> getParameterHelpNames() {
        return params;
    	}
}
