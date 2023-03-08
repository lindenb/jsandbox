package sandbox.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.tree.TreeNode;

public class DefaultTreeNode<T extends DefaultTreeNode<T,DATATYPE>,DATATYPE> extends AbstractList<T> implements javax.swing.tree.TreeNode {
	public enum AXIS {
		SELF,
		PARENT,
		ANCESTOR,
		ANCESTOR_OR_SELF,
		PRECEDING_SIBLINGS,
		FOLLOWING_SIBLINGS,
		CHILD,
		DESCENDANT,
		DESCENDANT_OR_SELF
		};
	private static long ID_GENERATOR=0L;
	private final long node_id = ++ID_GENERATOR;
	protected T nextSibling;
	protected T prevSibling;
	protected T parentNode;
	protected T firstChild;
	protected T lastChild;
	private DATATYPE userData;
	
	public DefaultTreeNode(DATATYPE userData) {
		this.userData  = userData;
	}
	
	@SuppressWarnings("unchecked")
	protected T self() {
		return (T)this;
	}
	
	public DefaultTreeNode() {
		this(null);
	}
	
	public DATATYPE getUserData() {
		return this.userData;
		}

	public void setUserData(DATATYPE userData) {
		this.userData = userData;
		}
	
	public String getNodeName() {
		return "node";
		}
	
	public long getNodeId() {
		return this.node_id;
		}
	
	@Override
	public int hashCode() {
		return Long.hashCode(this.node_id);
		}
	
	public boolean hasChildNodes() {
		return getFirstChild()!=null;
		}
	
	public String getPath() {
		String s= getNodeName();
		if(getParentNode()==null) return "/"+s;
		int idx =0;
		for(DefaultTreeNode<T,DATATYPE> c = getParentNode().getFirstChild();c!=null;c=c.getNextSibling() ) {
			if(c.getNodeName().equals(this.getNodeName())) {
				idx++;
				}
			if(c==this) break;
			}
		s=s+"["+(idx)+"]";
		return getParentNode().getPath()+"/"+s;
		}
	
	private DefaultTreeNode<T,DATATYPE> assertIsNotAncestor(final DefaultTreeNode<T,DATATYPE> n) {
		DefaultTreeNode<T,DATATYPE> p = getParentNode();
		while(p!=null) {
			if(p==n) throw new IllegalArgumentException(""+n+" is an ancestor of "+this);
			p = p.getParentNode();
			}
		return n;
		}
	
	private DefaultTreeNode<T,DATATYPE> assertIsMyChild(final DefaultTreeNode<T,DATATYPE> n) {
		if(n==null) throw new IllegalArgumentException("null");
		if(n.getParentNode()!=this) throw new IllegalArgumentException(""+n+" is not a child of "+this);
		return n;
		}
	
	@Override
	public boolean equals(Object o) {
		return this==o;
		}
	
	@Override
	public boolean isEmpty() {
		return !hasChildNodes();
		}
	
	@Override
	public int size() {
		int n=0;
		for(DefaultTreeNode<T,DATATYPE> c=getFirstChild();c!=null;c=c.getNextSibling()) {
			n++;
			}
		return n;
		}
	@Override
	public T get(int index) {
		int n=0;
		for(T c=getFirstChild();c!=null;c=c.getNextSibling()) {
			if(n==index) return c;
			n++;
			}
		throw new IndexOutOfBoundsException();
		}
	@Override
	public boolean add(T e) {
		appendChild(e);
		return true;
		}
	
	public void unlink() {
		if(getParentNode()!=null) {
			getParentNode().removeChild(self());
		}
	}
	
	public T removeChild(T c) {
		assertIsMyChild(c);
		T prev = c.getPrevSibling();
		T next = c.getNextSibling();
		if(prev!=null) prev.nextSibling=next;
		if(next!=null) next.prevSibling=prev;
		if(this.firstChild==c) this.firstChild=next;
		if(this.lastChild==c) this.lastChild=prev;
		c.nextSibling= null;
		c.prevSibling = null;
		c.parentNode = null;
		return c;
		}
	
	public T appendChild(T c) {
		if(c==null) throw new IllegalArgumentException();
		assertIsNotAncestor(c);
		c.unlink();
		if(this.lastChild==null) {
			this.firstChild = c;
			this.lastChild = c;
			}
		else
			{
			this.lastChild.nextSibling = c;
			c.prevSibling = this.lastChild;
			this.lastChild = c;
			}
		c.parentNode = self();
		return c;
		}
	
	
	
	@Override
	public int indexOf(final Object o) {
		int i=0;
		for(DefaultTreeNode<T,DATATYPE> c=getFirstChild();c!=null;c=c.getNextSibling()) {
			if(c==o) return i;
			i++;
			}
		return -1;
		}
	
	@Override
	public T set(int index, T element) {
		int i=0;
		for(T c=getFirstChild();c!=null;c=c.getNextSibling()) {
			if(i==index) {
				replaceChild(c,element);
				return c;
				}
			i++;
			}
		throw new IndexOutOfBoundsException();
		}
	
	@Override
	public boolean remove(Object o) {
		for(T c=getFirstChild();c!=null;c=c.getNextSibling()) {
			if(c==o) {
				removeChild(c);
				return true;
				}
			}
		return false;
		}
	
	@Override
	public T remove(int index) {
		int i=0;
		for(T c=getFirstChild();c!=null;c=c.getNextSibling()) {
			if(i==index) {
				return removeChild(c);
				}
			i++;
			}
		throw new IndexOutOfBoundsException();
		}
	
	@Override
	public final void clear() {
		removeAllChild();
		}
	
	public void removeAllChild() {
		while(getFirstChild()!=null) {
			removeChild(getFirstChild());
		}
	}
	
	@Override
	public boolean removeIf(Predicate<? super T> filter) {
		boolean flag = false;
		T curr= getFirstChild();
		while(curr!=null) {
			T next  =  curr.getNextSibling();
			if(filter.test(curr)) {
				flag = true;
				removeChild(curr);
				}
			curr  = next;
			}
		return flag;
		}
	
	public DefaultTreeNode<T,DATATYPE> insertBefore(T newChild,T refNode) {
		if(newChild==null)  throw new IllegalArgumentException();
		if(refNode==newChild)  throw new IllegalArgumentException();
		if(refNode==null) {
			return appendChild(newChild);
			}
		assertIsMyChild(refNode);
		assertIsNotAncestor(newChild);
		newChild.unlink();
		final T prev = refNode.getPrevSibling();
		
		if(prev!=null) prev.nextSibling=newChild;
		refNode.prevSibling=prev;
		if(this.firstChild==refNode) this.firstChild=newChild;
		newChild.nextSibling= refNode;
		newChild.prevSibling= prev;
		newChild.parentNode = self();
		return newChild;
		}
	
	public T replaceChild(T oldChild,T newChild) {
		if(newChild==null)  throw new IllegalArgumentException();
		if(oldChild==newChild)  return newChild;
		
		assertIsMyChild(oldChild);
		assertIsNotAncestor(newChild);
		
		newChild.unlink();
		T prev = newChild.getPrevSibling();
		T next = newChild.getNextSibling();
		if(prev!=null) prev.nextSibling = newChild;
		if(next!=null) next.prevSibling = newChild;
		newChild.prevSibling= prev;
		newChild.nextSibling = next;
		newChild.parentNode = self();
		if(newChild.prevSibling==null) this.firstChild=newChild;
		if(newChild.nextSibling==null) this.lastChild=newChild;
		return newChild;
		}
	
	public T getPrevSibling() {
		return prevSibling;
	}

	public T getNextSibling() {
		return nextSibling;
	}

	public T getFirstChild() {
		return firstChild;
	}

	public T getLastChild() {
		return lastChild;
	}

	public boolean isRoot() {
		return getParentNode()==null;
		}
	
	public T getRoot() {
		T p  = self();
		while(!p.isRoot()) {
			p = p.getParentNode();
			}
		return p;
		}
	
	public T getParentNode() {
		return parentNode;
	}
	
	/** apply Consummer to this node and his descendants */
	public void consume(final Consumer<T> consumer) {
		consumer.accept(self());
		for(DefaultTreeNode<T,DATATYPE> c=getFirstChild();c!=null;c=c.getNextSibling()) {
			c.consume(consumer);
			}
		}
	
	public List<T> findAll(final AXIS axis) {
		return findAll(axis,N->true);
		}
	
	public List<T> findAll(final AXIS axis,final Predicate<? super T> filter) {
		switch(axis) {
			case SELF: return filter.test(self())?Collections.singletonList(self()):Collections.emptyList();
			case PARENT: return getParentNode()==null?Collections.emptyList():getParentNode().findAll(AXIS.SELF,filter);
			case ANCESTOR: //cont
			case ANCESTOR_OR_SELF: {
				final List<T> L = new ArrayList<>();
				if(axis==AXIS.ANCESTOR_OR_SELF && filter.test(self())) L.add(self());
				T p = getParentNode();
				while(p!=null) {
					if(filter.test(p)) L.add(p);
					p = p.getParentNode();
					}
				return L;
				}
			case PRECEDING_SIBLINGS: {
				final List<T> L = new ArrayList<>();
				T c = getPrevSibling();
				while(c!=null) {
					if(filter.test(c)) L.add(c);
					c = c.getPrevSibling();
					}
				return L;
				}
			case FOLLOWING_SIBLINGS: {
				final List<T> L = new ArrayList<>();
				T c = getNextSibling();
				while(c!=null) {
					if(filter.test(c)) L.add(c);
					c = c.getNextSibling();
					}
				return L;
				}
			case CHILD:
				{
				final List<T> L = new ArrayList<>();
				T c = getFirstChild();
				while(c!=null) {
					if(filter.test(c)) L.add(c);
					c = c.getNextSibling();
					}
				return L;
				}
			case DESCENDANT:
			case DESCENDANT_OR_SELF:
				{
				final List<T> L = new ArrayList<>();
				if(axis==AXIS.DESCENDANT_OR_SELF && filter.test(self())) L.add(self());
				consume(X->{
					if(filter.test(X)) L.add(X);
					});
				return L;
				}
			default: throw new IllegalStateException();
			}
		}
	
	@Override
	public boolean getAllowsChildren() {
		return true;
		}
	@Override
	public final int getChildCount() {
		return size();
		}
	@Override
	public final DefaultTreeNode<T,DATATYPE> getParent() {
		return getParentNode();
		}
	@Override
	public boolean isLeaf() {
		return !hasChildNodes();
		}
	
	private class IterImpl implements Iterator<T>, Enumeration<T> {
		private T curr;
		IterImpl(T c) {
			this.curr= c;
			}
		@Override
		public boolean hasNext() {
			return curr!=null;
			}
		@Override
		public boolean hasMoreElements() {
			return hasNext();
			}
		@Override
		public T next(){
			if(curr==null) throw new IllegalStateException();
			T n = curr;
			curr=curr.nextSibling;
			return n;
			}
		public T nextElement() {
			return next();
			}
		}
	

	@Override
	public Iterator<T> iterator() {
		return new IterImpl(getFirstChild());
		}
	
	@Override
	public Enumeration<? extends DefaultTreeNode<T,DATATYPE>> children() {
		return new IterImpl(getFirstChild());
		}
	@Override
	public final DefaultTreeNode<T,DATATYPE> getChildAt(int childIndex) {
		return get(childIndex);
		}
	@Override
	public final int getIndex(TreeNode node) {
		return indexOf(node);
		}
	
	@Override
	public Stream<T> stream() {
		return StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(iterator(),Spliterator.ORDERED)
			,false);
		}
	
	@Override
	public String toString() {
		return getPath();
		}
	
	}
