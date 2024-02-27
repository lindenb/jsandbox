package sandbox.xml.dom;


import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

public abstract class AbstractTerminalNode extends AbstractNode {
	
	protected AbstractNode parentNode;
	protected AbstractNode nextSibNode;
	protected AbstractNode prevSibNode;

	protected AbstractTerminalNode(final DocumentImpl owner) {
		super(owner);
		}
	
	@Override protected void setParentNode(AbstractNode p) {this.parentNode=p;}
	@Override public AbstractNode getParentNode() {return this.parentNode;}
	@Override protected void setNextSibling(AbstractNode p) {this.nextSibNode=p;}
	@Override public AbstractNode getNextSibling() {return this.nextSibNode;}
	@Override protected void setPrevSibling(AbstractNode p) {this.prevSibNode=p;}
	@Override public AbstractNode getPreviousSibling() {return this.prevSibNode;}

	
	@Override
	public final boolean isTerminal() {
		return true;
		}
	
	@Override
	public final boolean hasAttributes() {
		return false;
		}	

	@Override
	public final NamedNodeMapImpl getAttributes() {
		return null;//spec
		}
	
	@Override
	public final AbstractNode appendChild(Node newChild) throws DOMException {
		throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,"Cannot add node to leaf node");
		}

	@Override
	public AbstractNode insertBefore(Node newChild, Node refChild) throws DOMException {
		throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,"Cannot add node to leaf node");
		}

	@Override
	public final AbstractNode getFirstChild() {
		return null;
		}

	@Override
	public final AbstractNode getLastChild() {
		return null;
		}

	@Override
	public final boolean hasChildNodes() {
		return false;
		}
	
	}
