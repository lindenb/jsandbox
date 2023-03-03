package sandbox.xml.dom;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeListImpl extends AbstractList<Node> implements NodeList {
	private final List<Node> array = new ArrayList<>();

	@Override
	public boolean addAll(Collection<? extends Node> c) {
		return array.addAll(c);
		}
	@Override
	public boolean add(Node element) {
		return array.add(element);
		}
	
	@Override
	public int getLength() {
		return array.size();
		}
	@Override
	public Node item(int index) {
		return array.get(index);
		}
	@Override
	public final int size() {
		return getLength();
		}
	@Override
	public final Node get(int index) {
		return item(index);
		}
	}
