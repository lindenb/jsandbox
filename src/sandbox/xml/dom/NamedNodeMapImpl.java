package sandbox.xml.dom;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

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
	public Node getNamedItem(String name) {
		int i=0;
		while(i<array.size()) {
			final Node n = this.array.get(i);
			if(name.equals(n.getNodeName())) {
				return n;
				}
			i++;
			}
		return null;
		}

	@Override
	public Node setNamedItem(Node arg) throws DOMException {
		int i=0;
		while(i<array.size()) {
			if(arg.getNodeName().equals(this.array.get(i).getNodeName())) {
				this.array.set(i, arg);
				return arg;
				}
			i++;
			}
		this.array.add(arg);
		return arg;
		}

	@Override
	public Node removeNamedItem(final String name) throws DOMException {
		int i=0;
		while(i<array.size()) {
			final Node n = this.array.get(i);
			if(name.equals(n.getNodeName())) {
				return array.remove(i);
				}
			i++;
			}
		throw new DOMException(DOMException.NOT_FOUND_ERR, "No such item");
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
	
	@Override
	public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException {
		int i=0;
		while(i<array.size()) {
			final Node n = this.array.get(i);
			if(namespaceURI.equals(n.getNamespaceURI()) && localName.equals(n.getLocalName())) {
				return n;
				}
			i++;
			}
		return null;
		}

	@Override
	public Node setNamedItemNS(Node arg) throws DOMException {
		int i=0;
		while(i<array.size()) {
			final Node n = this.array.get(i);
			if(arg.getLocalName().equals(n.getLocalName()) && 
				arg.getNamespaceURI().equals(n.getNamespaceURI())) {
				this.array.set(i, arg);
				return arg;
				}
			i++;
			}
		this.array.add(arg);
		return arg;		
		}
	
	@Override
	public boolean remove(Object o) {
		return this.array.remove(o);
		}

	@Override
	public Node removeNamedItemNS(final String namespaceURI, final String localName) throws DOMException {
		int i=0;
		while(i<array.size()) {
			final Node n = this.array.get(i);
			if(namespaceURI.equals(n.getNamespaceURI()) && localName.equals(n.getLocalName())) {
				return array.remove(i);
				}
			i++;
			}
		throw new DOMException(DOMException.NOT_FOUND_ERR, "No such item");
		}
	
	}
