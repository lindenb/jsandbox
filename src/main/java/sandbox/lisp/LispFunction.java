package sandbox.lisp;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;


abstract class LispFunction implements LispNode, Function<LispList,LispNode> {
	String location = "";
	protected LispFunction() {
		
		}
	
	public String getLocation() {
		return location;
		}
	
	public abstract LispNode invoke(LispList args) throws Exception;
	 
	 @Override
	 public final boolean isFunction() {
	    	return true;
	    	}
	 
	@Override
	public final LispNode apply(final LispList args) {
		try {
			return invoke(args);
			}
		catch(Exception err) {
			throw new RuntimeException(err);
			}
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
      
      static LispFunction of(final Function<LispList,LispNode> fun) {
    	  return new LispFunction() {
			@Override
			public LispNode invoke(LispList args) throws Exception {
				return fun.apply(args);
			}
		};
      }
}
