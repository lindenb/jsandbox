package sandbox.xml.dom;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeListImpl<T extends Node> extends AbstractList<T> implements NodeList {
	private final List<T> array;
	
	@SuppressWarnings("rawtypes")
	private static final NodeListImpl EMPTY_NODE_LIST = new NodeListImpl(Collections.emptyList());
	
	@SuppressWarnings("unchecked")
	public static <X extends Node> NodeListImpl<X> emptyNodeList() {
		return EMPTY_NODE_LIST;
	}
	
	public NodeListImpl() {
		this(new ArrayList<>());
		}
	
	public NodeListImpl( final List<T> array) {
		this.array = array;//must be = , not clone
		}
	
	@Override
	public boolean addAll(Collection<? extends T> c) {
		return array.addAll(c);
		}
	@Override
	public boolean add(T element) {
		return array.add(element);
		}
	
	@Override
	public int getLength() {
		return array.size();
		}
	@Override
	public final T item(int index) {
		return get(index);
		}
	@Override
	public final int size() {
		return getLength();
		}
	@Override
	public final T get(int index) {
		return array.get(index);
		}
	
	public <Z extends Node> NodeListImpl<Z> map(Function<Node,Z> mapper) {
		return new NodeListImpl<Z>(
			(this.array.stream().map(mapper)).
			collect(Collectors.toList())
			);
		}
	
	public NodeListImpl<ElementImpl> asElements() {
		return map(X->ElementImpl.class.cast(X));
		}
	}
