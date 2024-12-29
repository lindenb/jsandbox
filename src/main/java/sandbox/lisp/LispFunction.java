package sandbox.lisp;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;


abstract class LispFunction implements LispNode, Function<List<LispNode>,LispNode> {
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
      
      static LispFunction of(final Function<List<LispNode>,LispNode> fun) {
    	  return new LispFunction() {
			@Override
			public LispNode apply(List<LispNode> args) {
				return fun.apply(args);
			}
		};
      }
}
