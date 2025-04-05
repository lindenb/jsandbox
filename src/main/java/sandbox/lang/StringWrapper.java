package sandbox.lang;

public class StringWrapper implements CharSequence {
	private final String delegate;
	public StringWrapper(final String s) {
		this.delegate = s;
		}
	
	@Override
	public int hashCode() {
		return delegate.hashCode();
		}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;
		if(obj==null || !(this.getClass().equals(obj.getClass()))) return false;
		return this.getString().equals(StringWrapper.class.cast(obj).getString());
		}
	
	public String getString() {
		return this.delegate;
		}
	
	
	@Override
	public String toString() {
		return getString();
		}

	@Override
	public int length() {
		return delegate.length();
	}

	@Override
	public char charAt(int index) {
		return getString().charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return getString().subSequence(start, end);
	}
	}
