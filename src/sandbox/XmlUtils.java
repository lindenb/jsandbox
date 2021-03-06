package sandbox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sandbox.iterator.AbstractIterator;

public class XmlUtils {

static final Predicate<Node> isElement = (N) -> N!=null && 	N.getNodeType()==Node.ELEMENT_NODE;
static final Predicate<Node> isText = (N) -> N!=null && 	N.getNodeType()==Node.TEXT_NODE;
static final Function<Node,Element> toElement = N->{
	if(isElement.test(N)) return Element.class.cast(N);
	throw new IllegalArgumentException("node is not an element");
	};



static final Collector<Node,?, List<Element>> collectElements = Collector.of(
		()->new ArrayList<>(),
		(L,N)->{if(isElement.test(N)) L.add(Element.class.cast(N));},
		(left,right)-> { left.addAll(right); return left; }
		);



public static List<Node> allNodesAsList(final Node root) {
	final List<Node> L = new ArrayList<>();
	_recurse(L,root);
	return L;
	}

public static void _recurse(final List<Node> L,final Node root) {
	if( root == null) return;
	L.add(root);
	for(Node n = root.getFirstChild();n!=null;n=n.getNextSibling()) {
		_recurse(L,n);
		}
	}



public static Stream<Node> stream(final Node root) {
return children(root).stream();
}


public static List<Node> children(final Node root) {
	final ArrayList<Node> L = new ArrayList<>();
	if( root == null) return L;
	for(Node n = root.getFirstChild();n!=null;n=n.getNextSibling()) {
		L.add(n);
	}
	return L;
	}

public static List<Element> elements(final Node root) {
	return children(root).stream().
			filter(isElement).
			map(T->Element.class.cast(T)).
			collect(Collectors.toList())
			;
	}


public static List<Element> elements(final Node root,final Predicate<Element> predicate) {
return children(root).stream().
		filter(N->N.getNodeType()==Node.ELEMENT_NODE).
		map(T->Element.class.cast(T)).
		filter(predicate).
		collect(Collectors.toList())
		;
	}
public static Stream<Node> stream(final NodeList nl) {
	final Iterator<Node> iter = new AbstractIterator<Node>() {
		int i=0;
		@Override
		protected Node advance() {
			if(nl==null || i>=nl.getLength()) return null;
			final Node ret = nl.item(i);
			i++;
			return ret;
			}
		};
	
	return StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED),
			false
			);
	}		


}
