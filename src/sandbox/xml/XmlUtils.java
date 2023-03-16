package sandbox.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sandbox.StringUtils;
import sandbox.iterator.AbstractIterator;


public class XmlUtils {

public static final Predicate<Node> isElement = (N) -> N!=null && 	N.getNodeType()==Node.ELEMENT_NODE;
public static final Predicate<Node> isText = (N) -> N!=null && 	N.getNodeType()==Node.TEXT_NODE;
public static final Function<Node,Element> toElement = N->{
	if(isElement.test(N)) return Element.class.cast(N);
	throw new IllegalArgumentException("node is not an element");
	};

public static Optional<String> attribute(Node n,final String key) {
	if(!isElement.test(n)) return Optional.empty();
	final Element e=Element.class.cast(n);
	if(!e.hasAttribute(key)) return Optional.empty();
	return Optional.of(e.getAttribute(key));
	}

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

public static boolean isA(final Node n, String ns, String localName) {
	if(!isElement.test(n)) return false;
	final Element e= Element.class.cast(n);
	if(!ns.equals(e.getNamespaceURI())) return false;
	if(!localName.equals(e.getLocalName())) return false;
	return true;
	}

public static Stream<Node> stream(final Node root) {
return children(root).stream();
}

/* returns parents including self ordered from the_node to its parents*/
public static List<Node> ancestorsOrSelf(final Node the_node) {
	if( the_node == null) return Collections.emptyList();
	final List<Node> L = new ArrayList<>();
	Node curr= the_node;
	while(curr!=null) {
		L.add(curr);
		curr=curr.getParentNode();
		}
	return L;
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
/** returns human XPATH-like path for this node 
 * @param node the node
 * @return the path
 */
public static String getNodePath(final org.w3c.dom.Node node) {
	if(node==null) return null;
	return "(todo)";
	}

/** return true if 'n' is not a  Node.ELEMENT_NODE
 * also check that text node are blank
 * */
public static boolean isNotElement(final Node n) {
	switch(n.getNodeType()) {
		case Node.COMMENT_NODE: return true;
		case Node.CDATA_SECTION_NODE:
		case Node.TEXT_NODE:
			{
			String s=CharacterData.class.cast(n).getData();
			if(StringUtils.isBlank(s)) return true;
			throw new DOMException(DOMException.NO_DATA_ALLOWED_ERR,"found non blank node "+XmlUtils.getNodePath(n));
			}
		case Node.ELEMENT_NODE: return false;
		default: throw new DOMException(DOMException.NO_DATA_ALLOWED_ERR,"cannot handle node "+XmlUtils.getNodePath(n));
		}
	}


}
