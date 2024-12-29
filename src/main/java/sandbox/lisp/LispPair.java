package sandbox.lisp;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import sandbox.iterator.AbstractIterator;

public interface LispPair extends LispNode, LispList {
	static class EmptyImpl extends AbstractList<LispNode> implements LispPair {
		EmptyImpl() {
			}
		
		@Override
		public final boolean isList() {
			return true; /* empty is always an empty list */
			}
		
		public Iterator<LispNode> iterator() {
			return Collections.emptyListIterator();
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
		public int size() {
			int c=0;
			for(Iterator<LispNode> n=iterator();n.hasNext();n.next()) {
				c++;
				}
			return c;
			}
		
		@Override
		public LispNode get(final int index) {
			int c=0;
			for(Iterator<LispNode> n=iterator();n.hasNext();) {
				LispNode x=n.next();
				if(c==index) return x;
				c++;
				}
			throw new NoSuchElementException();
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
		public Iterator<LispNode> iterator() {
			return new MyIter(this);
			}
		
		@Override
		public String toString() {
			return "("+car().toString()+" . "+cdr().toString()+")";
			}
		}
	/*
	static class LoopPairImpl extends AbstractList<LispNode> implements LispPair {
		long start;
		long end;
		long shift;
		LoopPairImpl(long start,long end, long shift) {
			this.start=start;
			this.end=end;
			this.shift=shift;
			}
		public boolean isList() {
			return true;
			}
		public LispNode car() {
			
			}
		}*/
	
	
	
	public LispNode car();
	public LispNode cdr();
	@Override
	default boolean isPair() {
		return true;
		}
	@Override
	default Object getValue() {
		return stream().collect(Collectors.toCollection(ArrayList::new));
		}
	
	
	public static LispPair cons() {
		return EMPTY;
		}
	public static LispPair cons(LispNode a, LispNode b) {
		return new PairImpl(a,b);
		}
	
	@Override
	default LispList asList() {
		return LispList.of(stream().collect(Collectors.toCollection(ArrayList::new)));
		}
	
	
	public static LispPair of(List<LispNode> lst) {
		if(lst instanceof LispPair) {
			LispPair lp = LispPair.class.cast(lst);
			if(lp.isList()) return lp;
			}
		LispPair last=cons();
		for(int i=lst.size()-1;i>=0;i--) {
			last = cons(lst.get(i),last);
			}
		return last;
		}
	
	
	
	
	
	static class MyIter extends AbstractIterator<LispNode> {
		private LispPair curr;
		MyIter(LispPair curr) {
			this.curr=curr;
			}
		@Override
		protected LispNode advance() {
			final LispPair p=curr;
			if(p.isEmpty()) return null;
			LispNode then =this.curr.cdr();
			if(then==p || then==curr) throw new IllegalStateException();
			if(!then.isPair()) throw new IllegalStateException("not a list");
			this.curr = then.asPair();
			return p.car();
			}
		}
	
	}
