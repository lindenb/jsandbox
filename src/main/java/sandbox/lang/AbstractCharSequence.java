package sandbox.lang;

public abstract class AbstractCharSequence implements CharSequence
	{
	public boolean isEmpty() {
		return this.length()==0;
	}
	
	@Override
	public boolean equals(Object obj)
		{
		if(obj==this) return true;
		if(obj==null || !(obj instanceof CharSequence)) return false;
		final CharSequence cp = CharSequence.class.cast(obj);
		final int L= this.length();
		if(L!=cp.length()) return false;
		for(int i=0;i< L;i++) {
			if(this.charAt(i)!=cp.charAt(i)) return false;
			}
		return true;
		}
	
	@Override
	public int hashCode()
		{
		int n=0;
		final int L= this.length();
		for(int i=0;i< L;i++) {
			n+= n*31 + Character.hashCode(this.charAt(i));
			}
		return n;
		}
	
	@Override
	public CharSequence subSequence(int start, int end)
		{
		if(start==0 && end==this.length()) return this;
		final StringBuilder sb = new StringBuilder(end-start);
		while(start<end) {
			sb.append(this.charAt(start));
			start++;
			}
		return sb;
		}
	@Override
	public String toString() {
		// do not call subSequence ( will call toString to itself )
		final int L= this.length();
		final StringBuilder sb = new StringBuilder(L);
		for(int i=0;i<L;++i) {
			sb.append(this.charAt(i));
			}
		return sb.toString();
		}
	}
