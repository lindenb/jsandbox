package sandbox.lisp;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;


abstract class LispFunction implements LispNode, BiFunction<LispList,LispContext,LispNode> {
	String location = "";
	protected LispFunction() {
		
		}
	
	public String getLocation() {
		return location;
		}
		 
	 @Override
	 public final boolean isFunction() {
	    	return true;
	    	}
	 
	  
	  protected List<?> getParameterHelpNames() {
          return Collections.emptyList();
      	  }
	  
	  
      @Override
      public Object getValue() {
        return this;
      	}
      @Override
      public boolean asBoolean() {
        return true;
      	}
      @Override
      public String toString() {
          return LispEngine.listToString("LispFunction(", getParameterHelpNames(), ",", ")");
      }
      
      static LispFunction of(final BiFunction<LispList,LispContext,LispNode> fun) {
    	  return new LispFunction() {
			@Override
			public LispNode apply(LispList args,LispContext ctx) {
				return fun.apply(args,ctx);
			}
		};
      }
      
}
