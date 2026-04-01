package sandbox.lisp;

import java.util.List;

public interface LispNode extends List<Object> {
	public String getName();
	public default boolean hasName(String s) {
		return getName().equals(s);
		}
	public default LispNode arity(int n) {
		return arity(n,n);
		}
	public default LispNode arity(int n,int m) {
		if(n!=-1 && n < size()) throw new IllegalArgumentException("("+getName()+") expect a minimum of "+n+" args but got "+size());
		if(m!=-1 && m > size()) throw new IllegalArgumentException("("+getName()+") expect a maximum of "+m+" args but got "+size());
		return this;
		}
	}
