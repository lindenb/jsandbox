package sandbox.xml.dom;

import java.util.Collections;
import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

public abstract class AbstractTerminalNode extends AbstractNode {
	protected AbstractTerminalNode(final DocumentImpl owner) {
		super(owner);
		}
	@Override
	public final boolean isTerminal() {
		return true;
		}
	@Override
	public final Node appendChild(Node newChild) throws DOMException {
		throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,"Cannot add node to leaf node");
		}

	@Override
	public Node insertBefore(Node newChild, Node refChild) throws DOMException {
		throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,"Cannot add node to leaf node");
		}

	@Override
	public final Node removeChild(Node oldChild) throws DOMException {
		throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,"Cannot add node to leaf node");
		}
	
	@Override
	public final Node getFirstChild() {
		return null;
		}
	@Override
	public final Node getLastChild() {
		return null;
		}
	@Override
	public final boolean hasChild() {
		return false;
		}
	}
