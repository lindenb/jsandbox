package sandbox.xml;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import sandbox.iterator.AbstractIterator;
import sandbox.util.IterableHasStream;

/** iterator over the children of a DOM node */
public class DOMIterator<T extends Node> extends AbstractIterator<T>{
		private final Node root;
		private Node child=null;
		private boolean first=true;
		private final Function<Node,T> converter;
		/** 
		 * @param root parent node
		 * @param converter convert node to T, or return null to skip
		 */
		public DOMIterator(final Node root,Function<Node,T> converter) {
			this.root=root;
			this.converter=converter;
			}
		@Override
		protected T advance() {
			for(;;) {
				if(first) {
					child=root.getFirstChild();
					first=false;
					}
				else
					{
					child=child.getNextSibling();
					}
				if(child==null) return null;
				final T o = converter.apply(child);
				if(o!=null) return o;
				}
			}
		
		public static IterableHasStream<Node> nodes(Node root) {
			return new IterableHasStream<Node>() {
				@Override
				public Iterator<Node> iterator() {
					return new DOMIterator<Node>(root,T->Node.class.cast(T));
					}
				};
			}
		
		public static IterableHasStream<Element> elements(Node root,Predicate<Element> predicate) {
			return new IterableHasStream<Element>() {
				@Override
				public Iterator<Element> iterator() {
					return new DOMIterator<Element>(root,T->(T.getNodeType()==Node.ELEMENT_NODE && predicate.test(Element.class.cast(T))?Element.class.cast(T):null));
					}
				};
			}
		
		public static IterableHasStream<Element> elements(Node root) {
			return elements(root,E->true);
			}
		}
