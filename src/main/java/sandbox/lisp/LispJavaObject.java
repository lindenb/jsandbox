package sandbox.lisp;


 class LispJavaObject  extends LispAtom<Object> {
	static final LispJavaObject TRUE=new LispJavaObject(Boolean.TRUE);
	static final LispJavaObject FALSE=new LispJavaObject(Boolean.FALSE);
	static final LispJavaObject NIL=new LispJavaObject(null);
    static LispJavaObject of(Object value) {
    	return new LispJavaObject(value);
     	}
    LispJavaObject(Object o) {
     	super(o);
     }
 }
