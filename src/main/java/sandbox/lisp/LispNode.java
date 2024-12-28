package sandbox.lisp;

public interface LispNode {
	public Object getValue();
    public boolean asBoolean();
    public default LispList asList() { return LispList.class.cast(this);}
    public default LispFunction asFunction() { return LispFunction.class.cast(this);}
    public default boolean isList() { return false;}
    public default boolean isFunction() { return false;}
    public default LispAtom<?> asAtom() { return LispAtom.class.cast(this);}
    public default boolean isAtom() { return false;}
    public default LispSymbol asSymbol() { return LispSymbol.class.cast(this);}
    public default boolean isSymbol() { return false;}
}
