package sandbox.lisp;

public interface LispNode {
	public Object getValue();
    public boolean asBoolean();
    public default <T> T getValue(Class<T> clazz) { return clazz.cast(getValue());}
    public default LispFunction asFunction() { return LispFunction.class.cast(this);}
    public default boolean isFunction() { return false;}
    public default LispAtom<?> asAtom() { return LispAtom.class.cast(this);}
    public default boolean isAtom() { return false;}
    public default LispSymbol asSymbol() { return LispSymbol.class.cast(this);}
    public default boolean isSymbol() { return false;}
    public default boolean isSymbol(final String name) {
    	return isSymbol() && asSymbol().getValue().equals(name);
    	}
    public default boolean isList() { return false;}
    public default LispList asList() { return LispList.class.cast(this);}
    public default boolean isPair() { return false;}
    public default LispPair asPair() { return LispPair.class.cast(this);}
	}
