package sandbox.lisp;

import java.util.ArrayList;
import java.util.List;

public interface LispList extends LispNode , List<LispNode>{
@Override
public default boolean isList() {
	return true;
	}
public default LispList arity(int n) {
	if(size()!=n) throw new IllegalStateException("expected arity="+n+" but got "+size());
	return this;
	}

public default LispList subList(int idx) {
	return of(this.subList(idx, size()));
	}

@Override
public default LispPair asPair() {
	return LispPair.of(this);
	}
@Override
public default LispList asList() {
	return this;
	}
@Override
public default boolean asBoolean() {
	return !isEmpty();
	}
public static LispList of(final List<LispNode> n) {
	return new LispListImpl(n);
	}
@SuppressWarnings("serial")
static class LispListImpl extends ArrayList<LispNode> implements LispList {
	LispListImpl(final List<LispNode> n) {
		super(n);
		}
	@Override
	public Object getValue() {
		return new ArrayList<>(this);
		}
	}
}
