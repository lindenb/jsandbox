package sandbox.xml.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class Node {
	private final Element parent;
	protected Node(final Element parent) {
		this.parent = parent;
		}
	
	public abstract org.w3c.dom.Node toDOM(final org.w3c.dom.Document doc);
	
	public boolean hasParent() {
		return getParentNode()!=null;
	}
	public Element getParentNode() {
		return this.parent;
		}
	public boolean isRoot() {
		return this.parent==null;
		}
	
	public Node getRoot() {
		Node r=this;
		while(r.hasParent())  {
			r = r.getParentNode();
			}
		return r;
		}
	
	public List<Node> getPrecedingSiblingAsList() {
		if(!hasParent()) return Collections.emptyList();
		final List<Node> L = new ArrayList<>();
		for(Node n: getParentNode().getChildrenAsList()) {
			if(n==this) break;
			L.add(n);
			}
		return L;
		}
	public List<Node> getFollowingSiblingAsList() {
		if(!hasParent()) return Collections.emptyList();
		final List<Node> L = new ArrayList<>();
		int state=0;
		for(Node n: getParentNode().getChildrenAsList()) {
			if(n==this) {
				state=1;
				}
			else if(state==1) {
				L.add(n);
				}
			}
		return L;
		}
	
	public abstract List<Node> getChildrenAsList();
	
	public boolean hasChild() {
		return !getChildrenAsList().isEmpty();
		}
	
	public Stream<Node> getChildren() {
		return getChildren(N->true);
		}
	public Stream<Node> getChildren(final Predicate<Node> filter) {
		return getChildrenAsList().stream().filter(filter);
		}
	public List<Node> getChildrenAsList(final Predicate<Node> filter) {
		return getChildren(filter).collect(Collectors.toList());
		}
	

	public Stream<Element> getElements(final Predicate<Element> filter) {
		return getChildren(N->N.isElement()).
				map(C->C.asElement()).
				filter(filter)
				;
		}
	public Stream<Element> getElements() {
		return getElements(E->true);
		}
	
	public List<Element> getElementsAsList(final Predicate<Element> filter) {
		return getElements(filter).collect(Collectors.toList());
		}
	public List<Element> getElementsAsList() {
		return getElementsAsList(E->true);
		}
	
	public abstract boolean isText();
	public abstract boolean isElement();
	public abstract boolean isAttribute();
	public Element asElement() {
		if(!isElement()) throw new IllegalStateException("not an element");
		return Element.class.cast(this);
		}
	public Text asText() {
		if(!isText()) throw new IllegalStateException("not a text");
		return Text.class.cast(this);
		}
	
	
	
	
	public abstract String getPath();
	
	public abstract void sax(final DefaultHandler handler) throws SAXException;
	
	public void consume(final Consumer<Node> consumer) {
		consumer.accept(this);
		for(Node n:getChildrenAsList()) {
			n.consume(consumer);
			}
		}
	/*
	public void print() {
		final TransformerFactory trf = TransformerFactory.newInstance();
		final Transformer tr = trf.newTransformer();
		final org.w3c.dom.Document doc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().newDocument();
		tr.transform(new DOMSource(toDOM(doc)), new );
		}*/
 	
	}
