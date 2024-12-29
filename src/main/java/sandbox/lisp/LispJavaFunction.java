package sandbox.lisp;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


class LispJavaFunction extends LispFunction {
    private final Object object;
    private final Method method;
    
    LispJavaFunction(final Object object, final Method method) {
    	this.object = object;
    	this.method = method;
    	}
    
    @Override
    public LispNode apply(List<LispNode>  args) {
        try {
			return LispEngine.expressionOf(method.invoke(this.object, args.stream().map(T->T.getValue()).toArray()));
		} catch (Exception e) {
			throw new RuntimeException(e);
			}
    	}
    @Override protected List<?> getParameterHelpNames() {
    	return Arrays.stream(this.method.getParameterTypes()).
    			map(P->P.getSimpleName()).
    			collect(Collectors.toList());
    }
}
