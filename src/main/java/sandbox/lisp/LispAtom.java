package sandbox.lisp;

import java.util.Objects;


abstract class LispAtom<T> implements LispNode {
	protected final T value;
	LispAtom(T value) {
    	this.value = value;
    	}
	@Override
	public final boolean isAtom() {
		return true;
		}
	@Override
    public T getValue() {
        return value;
    	}
    @Override
    public String toString() {
        return this.value instanceof String ? "\"" + LispEngine.escapeString((String) value) + '"' : String.valueOf(value);
    	}
    
    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    	}
    
    @Override
    public boolean equals(final Object o) {
        if(o==this) return true;
    	if (o != null && this.getClass().equals(o.getClass())) {
            return Objects.equals(LispAtom.class.cast(o).getValue(), this.getValue());
        } else {
            return false;
        	}
    	}
    @Override
    public boolean asBoolean() {
        return value != null && !Boolean.FALSE.equals(value);
    	}
	}
