package sandbox.lisp;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class LispList extends AbstractList<LispNode> implements LispNode {
    private final List<LispNode> delegate;
	public LispList() {
    	this.delegate = new ArrayList<>();
        }
    LispList(final List<LispNode> list) {
    	this.delegate = new ArrayList<>(list);
    	}
    @Override
    public final boolean isList() {
    	return true;
    	}
    @Override
    public LispNode get(int index) {
    	return delegate.get(index);
    	}
    @Override
    public int size() {
    	return delegate.size();
    	}
    
    @Override
    public boolean asBoolean() {
        return !isEmpty();
    	}
    
    @Override
    public boolean equals(final Object o) {
    	if(this==o) return true;
    	if(!(o instanceof LispList)) return false;
    	return this.delegate.equals(LispList.class.cast(o).delegate);
    	}
    
    @Override
    public int hashCode() {
    	return delegate.hashCode();
    	}
        
    @Override
    public List<Object> getValue() {
    	return this.stream().map(P->P.getValue()).collect(Collectors.toList());
    	}
    
    @Override
    public String toString() {
        return LispEngine.listToString("(", this, " ", ")");
    	}
	}
