package sandbox.lisp;


class LispSymbol extends LispAtom<String> implements CharSequence {
	 LispSymbol(final String value) {
     	super(value);
     	}
	@Override
	public char charAt(int index) {
		return value.charAt(index);
		} 
	@Override
	public int length() {
		return value.length();
	 	}
	@Override
	public CharSequence subSequence(int start, int end) {
		return value.subSequence(start, end);
		}
	
	@Override
	public final boolean isSymbol() {
		return true;
	 	}
	 
	 public static LispSymbol of(final String value) {
         return new LispSymbol(value);
     	}
     
     @Override
     public String toString() {
		return value;
	 	}
	}
