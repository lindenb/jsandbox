package sandbox.lang;

public class DelegateCharSequence<T extends CharSequence> extends AbstractCharSequence {
	private final T delegate;
	protected DelegateCharSequence(final T delegate) {
		this.delegate = delegate ;
		}
	public T getDelegate() {
		return delegate;
		}
	@Override
	public char charAt(int index) {
		return getDelegate().charAt(index);
		}
	@Override
	public int length() {
		return getDelegate().length();
		}
}
