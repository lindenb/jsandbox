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

public class DefaultTreeNode extends AbstractList<DefaultTreeNode> implements javax.swing.tree.TreeNode {
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
	protected DefaultTreeNode nextSibling;
	protected DefaultTreeNode prevSibling;
	protected DefaultTreeNode parentNode;
	protected DefaultTreeNode firstChild;
	protected DefaultTreeNode lastChild;
	private Object userData;
	
	public DefaultTreeNode(Object userData) {
		this.userData  = userData;
	}
	
	public DefaultTreeNode() {
		this(null);
	}
	
	public Object getUserData() {
		return this.userData;
		}

	public void setUserData(Object userData) {
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
		for(DefaultTreeNode c = getParentNode().getFirstChild();c!=null;c=c.getNextSibling() ) {
			if(c.getNodeName().equals(this.getNodeName())) {
				idx++;
				}
			if(c==this) break;
			}
		s=s+"["+(idx)+"]";
		return getParentNode().getPath()+"/"+s;
		}
	
	private DefaultTreeNode assertIsNotAncestor(final DefaultTreeNode n) {
		DefaultTreeNode p = getParentNode();
		while(p!=null) {
			if(p==n) throw new IllegalArgumentException(""+n+" is an ancestor of "+this);
			p = p.getParentNode();
			}
		return n;
		}
	
	private DefaultTreeNode assertIsMyChild(final DefaultTreeNode n) {
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
		for(DefaultTreeNode c=getFirstChild();c!=null;c=c.getNextSibling()) {
			n++;
			}
		return n;
		}
	@Override
	public DefaultTreeNode get(int index) {
		int n=0;
		for(DefaultTreeNode c=getFirstChild();c!=null;c=c.getNextSibling()) {
			if(n==index) return c;
			n++;
			}
		throw new IndexOutOfBoundsException();
		}
	@Override
	public boolean add(DefaultTreeNode e) {
		appendChild(e);
		return true;
		}
	
	public void unlink() {
		if(getParentNode()!=null) {
			getParentNode().removeChild(this);
		}
	}
	
	public DefaultTreeNode removeChild(DefaultTreeNode c) {
		assertIsMyChild(c);
		DefaultTreeNode prev = c.getPrevSibling();
		DefaultTreeNode next = c.getNextSibling();
		if(prev!=null) prev.nextSibling=next;
		if(next!=null) next.prevSibling=prev;
		if(this.firstChild==c) this.firstChild=next;
		if(this.lastChild==c) this.lastChild=prev;
		c.nextSibling= null;
		c.prevSibling = null;
		c.parentNode = null;
		return c;
		}
	
	public DefaultTreeNode appendChild(DefaultTreeNode c) {
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
		c.parentNode = this;
		return c;
		}
	
	
	
	@Override
	public int indexOf(final Object o) {
		int i=0;
		for(DefaultTreeNode c=getFirstChild();c!=null;c=c.getNextSibling()) {
			if(c==o) return i;
			i++;
			}
		return -1;
		}
	
	@Override
	public DefaultTreeNode set(int index, DefaultTreeNode element) {
		int i=0;
		for(DefaultTreeNode c=getFirstChild();c!=null;c=c.getNextSibling()) {
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
		for(DefaultTreeNode c=getFirstChild();c!=null;c=c.getNextSibling()) {
			if(c==o) {
				removeChild(c);
				return true;
				}
			}
		return false;
		}
	
	@Override
	public DefaultTreeNode remove(int index) {
		int i=0;
		for(DefaultTreeNode c=getFirstChild();c!=null;c=c.getNextSibling()) {
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
	public boolean removeIf(Predicate<? super DefaultTreeNode> filter) {
		boolean flag = false;
		DefaultTreeNode curr= getFirstChild();
		while(curr!=null) {
			DefaultTreeNode next  =  curr.getNextSibling();
			if(filter.test(curr)) {
				flag = true;
				removeChild(curr);
				}
			curr  = next;
			}
		return flag;
		}
	
	public DefaultTreeNode insertBefore(DefaultTreeNode newChild,DefaultTreeNode refNode) {
		if(newChild==null)  throw new IllegalArgumentException();
		if(refNode==newChild)  throw new IllegalArgumentException();
		if(refNode==null) {
			return appendChild(newChild);
			}
		assertIsMyChild(refNode);
		assertIsNotAncestor(newChild);
		newChild.unlink();
		final DefaultTreeNode prev = refNode.getPrevSibling();
		
		if(prev!=null) prev.nextSibling=newChild;
		refNode.prevSibling=prev;
		if(this.firstChild==refNode) this.firstChild=newChild;
		newChild.nextSibling= refNode;
		newChild.prevSibling= prev;
		newChild.parentNode = this;
		return newChild;
		}
	
	public DefaultTreeNode replaceChild(DefaultTreeNode oldChild,DefaultTreeNode newChild) {
		if(newChild==null)  throw new IllegalArgumentException();
		if(oldChild==newChild)  return newChild;
		
		assertIsMyChild(oldChild);
		assertIsNotAncestor(newChild);
		
		newChild.unlink();
		DefaultTreeNode prev = newChild.getPrevSibling();
		DefaultTreeNode next = newChild.getNextSibling();
		if(prev!=null) prev.nextSibling = newChild;
		if(next!=null) next.prevSibling = newChild;
		newChild.prevSibling= prev;
		newChild.nextSibling = next;
		newChild.parentNode = this;
		if(newChild.prevSibling==null) this.firstChild=newChild;
		if(newChild.nextSibling==null) this.lastChild=newChild;
		return newChild;
		}
	
	public DefaultTreeNode getPrevSibling() {
		return prevSibling;
	}

	public DefaultTreeNode getNextSibling() {
		return nextSibling;
	}

	public DefaultTreeNode getFirstChild() {
		return firstChild;
	}

	public DefaultTreeNode getLastChild() {
		return lastChild;
	}

	public boolean isRoot() {
		return getParentNode()==null;
		}
	
	public DefaultTreeNode getRoot() {
		DefaultTreeNode p  = this;
		while(!p.isRoot()) {
			p = p.getParentNode();
			}
		return p;
		}
	
	public DefaultTreeNode getParentNode() {
		return parentNode;
	}
	
	/** apply Consummer to this node and his descendants */
	public void consume(final Consumer<DefaultTreeNode> consumer) {
		consumer.accept(this);
		for(DefaultTreeNode c=getFirstChild();c!=null;c=c.getNextSibling()) {
			c.consume(consumer);
			}
		}
	
	public List<DefaultTreeNode> findAll(final AXIS axis) {
		return findAll(axis,N->true);
		}
	
	public List<DefaultTreeNode> findAll(final AXIS axis,final Predicate<DefaultTreeNode> filter) {
		switch(axis) {
			case SELF: return filter.test(this)?Collections.singletonList(this):Collections.emptyList();
			case PARENT: return getParentNode()==null?Collections.emptyList():getParentNode().findAll(AXIS.SELF,filter);
			case ANCESTOR: //cont
			case ANCESTOR_OR_SELF: {
				final List<DefaultTreeNode> L = new ArrayList<>();
				if(axis==AXIS.ANCESTOR_OR_SELF && filter.test(this)) L.add(this);
				DefaultTreeNode p = getParentNode();
				while(p!=null) {
					if(filter.test(p)) L.add(p);
					p = p.getParentNode();
					}
				return L;
				}
			case PRECEDING_SIBLINGS: {
				final List<DefaultTreeNode> L = new ArrayList<>();
				DefaultTreeNode c = getPrevSibling();
				while(c!=null) {
					if(filter.test(c)) L.add(c);
					c = c.getPrevSibling();
					}
				return L;
				}
			case FOLLOWING_SIBLINGS: {
				final List<DefaultTreeNode> L = new ArrayList<>();
				DefaultTreeNode c = getNextSibling();
				while(c!=null) {
					if(filter.test(c)) L.add(c);
					c = c.getNextSibling();
					}
				return L;
				}
			case CHILD:
				{
				final List<DefaultTreeNode> L = new ArrayList<>();
				DefaultTreeNode c = getFirstChild();
				while(c!=null) {
					if(filter.test(c)) L.add(c);
					c = c.getNextSibling();
					}
				return L;
				}
			case DESCENDANT:
			case DESCENDANT_OR_SELF:
				{
				final List<DefaultTreeNode> L = new ArrayList<>();
				if(axis==AXIS.DESCENDANT_OR_SELF && filter.test(this)) L.add(this);
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
	public final DefaultTreeNode getParent() {
		return getParentNode();
		}
	@Override
	public boolean isLeaf() {
		return !hasChildNodes();
		}
	
	private static class IterImpl implements Iterator<DefaultTreeNode>, Enumeration<DefaultTreeNode> {
		private DefaultTreeNode curr;
		IterImpl(DefaultTreeNode c) {
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
		public DefaultTreeNode next(){
			if(curr==null) throw new IllegalStateException();
			DefaultTreeNode n = curr;
			curr=curr.nextSibling;
			return n;
			}
		public DefaultTreeNode nextElement() {
			return next();
			}
		}
	

	@Override
	public Iterator<DefaultTreeNode> iterator() {
		return new IterImpl(getFirstChild());
		}
	
	@Override
	public Enumeration<? extends DefaultTreeNode> children() {
		return new IterImpl(getFirstChild());
		}
	@Override
	public final DefaultTreeNode getChildAt(int childIndex) {
		return get(childIndex);
		}
	@Override
	public final int getIndex(TreeNode node) {
		return indexOf(node);
		}
	
	@Override
	public Stream<DefaultTreeNode> stream() {
		return StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(iterator(),Spliterator.ORDERED)
			,false);
		}
	
	@Override
	public String toString() {
		return getPath();
		}
	
	}
