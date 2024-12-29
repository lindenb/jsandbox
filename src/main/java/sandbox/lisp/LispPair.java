package sandbox.lisp;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public interface LispPair extends LispNode, List<LispNode> {
	static class EmptyImpl extends AbstractList<LispNode> implements LispPair {
		EmptyImpl() {
			}
		
		@Override
		public final boolean isList() {
			return true; /* empty is always an empty list */
			}
		@Override
		public int size() {
			return 0;
			}
		@Override
		public LispNode get(int index) {
			throw new NoSuchElementException();
			}
		@Override
		public String toString() {
			return "()";
			}

		@Override
		public LispNode car() {
			throw new NoSuchElementException("!first");
		}

		@Override
		public LispNode cdr() {
			throw new NoSuchElementException("!second");
			}
		}
	static final EmptyImpl EMPTY=new EmptyImpl();
	
	static class PairImpl extends AbstractList<LispNode> implements LispPair {
		private final LispNode first;
		private final LispNode second;
		PairImpl(LispNode first, LispNode second) {
			this.first = first;
			this.second = second;
			}
		@Override
		public LispNode car() {
			return first;
			}

		@Override
		public LispNode cdr() {
			return second;
			}
		@Override
		public boolean isList() {
			LispPair p=this;
			for(;;) {
				LispNode n=p.cdr();
				if(!n.isPair()) return false;
				p=n.asPair();
				if(p.isEmpty()) return true;
				}
			}
		@Override
		public final boolean isEmpty() {
			return false;
			}
		@Override
		public int size() {
			int count=0;
			LispPair p=this;
			while(!p.isEmpty()) {
				count++;
				LispNode n=p.cdr();
				if(!n.isPair()) throw new IllegalStateException("not a list");
				p=n.asPair();
				}
			return count;
			}
		@Override
		public LispNode get(final int index) {
			int count=0;
			LispPair p=this;
			while(!p.isEmpty()) {
				if(index==count) return p.car();
				++count;
				LispNode n=p.cdr();
				if(!n.isPair()) throw new IllegalStateException("not a list");
				p=n.asPair();
				}
			throw new NoSuchElementException();
			}
		@Override
		public String toString() {
			return "("+car().toString()+" . "+cdr().toString()+")";
			}
		}

	
	public LispNode car();
	public LispNode cdr();
	public boolean isList();
	@Override
	default boolean isPair() {
		return true;
		}
	@Override
	default Object getValue() {
		return stream().collect(Collectors.toCollection(ArrayList::new));
		}
	
	@Override
	public default boolean asBoolean() {
		return !isEmpty();
		}
	public static LispPair cons() {
		return EMPTY;
		}
	public static LispPair cons(LispNode a, LispNode b) {
		return new PairImpl(a,b);
		}
	
	public default List<LispNode> toList() {
		List<LispNode> L=new ArrayList<>();
		LispPair p=this;
		while(!p.isEmpty()) {
			L.add(p.car());
			LispNode n=p.cdr();
			if(!n.isPair()) throw new IllegalStateException("not a list");
			p=n.asPair();
			}
		return L;
		}
	
	public static LispPair of(List<LispNode> lst) {
		LispPair last=cons();
		for(int i=lst.size()-1;i>=0;i--) {
			last = cons(lst.get(i),last);
			}
		return last;
		}
	}
