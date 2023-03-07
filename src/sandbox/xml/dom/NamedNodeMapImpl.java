package sandbox.xml.dom;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.xml.namespace.QName;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import sandbox.StringUtils;

public class NamedNodeMapImpl extends AbstractList<Node>  implements NamedNodeMap {
	private final List<Node> array;
	private static final NamedNodeMapImpl EMPTY_NODE_MAP = new NamedNodeMapImpl(Collections.emptyList());
	
	public static NamedNodeMapImpl getEmptyNamedNodeMap() {
		return EMPTY_NODE_MAP;
	}
	
	public NamedNodeMapImpl(final List<Node> array) {
		this.array = array;// = not a copy
	}
	public NamedNodeMapImpl() {
		this(new ArrayList<>());
	}
	
	@Override
	public boolean add(final Node e) {
		if(e.getNamespaceURI()!=null) {
			setNamedItemNS(e);
			}
		else
			{
			setNamedItem(e);
			}
		return true;
		}
	
	private Node setNamedItem(final Node arg,Predicate<Node> filter) throws DOMException {
		int i=0;
		while(i<array.size()) {
			if(filter.test(this.array.get(i))) {
				this.array.set(i, arg);
				return arg;
				}
			i++;
			}
		this.array.add(arg);
		return arg;
		}
	
	@Override
	public Node setNamedItem(Node arg) throws DOMException {
		return setNamedItem(arg,AbstractNode.createNodeMatcher(arg.getNodeName()));
		}
	
	@Override
	public Node setNamedItemNS(Node arg) throws DOMException {
		return setNamedItem(arg,AbstractNode.createNodeMatcher(arg.getNamespaceURI(),arg.getLocalName()));
		}

	@Override
	public Node item(int index) {
		return this.array.get(index);
		}
	
	@Override
	public final Node get(int index) {
		return item(index);
		}

	@Override
	public int getLength() {
		return this.array.size();
		}

	@Override
	public final int size() {
		return getLength();
		}
	
	private Node getNamedItem(Predicate<Node> matcher)  {
		return this.array.stream().
				filter(matcher).
				findFirst().
				orElse(null);
		}
	
	@Override
	public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException {
		return getNamedItem(AbstractNode.createNodeMatcher(namespaceURI, localName));
		}

	@Override
	public Node getNamedItem(String name) {
		return getNamedItem(AbstractNode.createNodeMatcher(name));
		}
	
	@Override
	public boolean remove(Object o) {
		return this.array.remove(o);
		}

	
	private Node removeItem(final Predicate<Node> matcher) throws DOMException {
		int i=0;
		while(i<array.size()) {
			if(matcher.test(this.array.get(i))) {
				return array.remove(i);
				}
			i++;
			}
		throw new DOMException(DOMException.NOT_FOUND_ERR, "No such item");
		}
	
	@Override
	public Node removeNamedItem(final String name) throws DOMException {
		return removeItem(AbstractNode.createNodeMatcher(name));
		}

	
	@Override
	public Node removeNamedItemNS(final String namespaceURI, final String localName) throws DOMException {
		return removeItem(AbstractNode.createNodeMatcher(namespaceURI,localName));
		}
	

	
	public Map<QName,String> asMap() {
		final HashMap<QName,String> hash = new HashMap<>(this.array.size());
		for(int i=0;i< getLength();i++) {
			hash.put(AbstractNode.toQName(item(i)),item(i).getNodeValue());
			}
		return hash;
		}
	}
