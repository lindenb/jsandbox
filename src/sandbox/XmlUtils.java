package sandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XmlUtils {

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
			filter(N->N.getNodeType()==Node.ELEMENT_NODE).
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
}
