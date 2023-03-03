package sandbox.xml.dom;

import java.util.Collections;
import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public abstract class AbstractBanchNode extends NodeImpl implements org.w3c.dom.Node {
	private NodeImpl firstChild = null;
	private NodeImpl nextSibling = null;
	protected AbstractBanchNode(DocumentImpl owner) {
		super(owner);
		}
	@Override
	public Node appendChild(Node newChild) throws DOMException {
		if(node==this) throw new DOMException(DOMException., getPath())
		if(firstChild==null) {
			this.firstChild = NodeImpl.class.cast(newChild);
			}
		else
			{
			NodeImpl curr = this.firstChild;
			while(curr.nextSibling!=null) {
				curr = curr.nextSibling;
				}
			curr.nextSibling  = NodeImpl.class.cast(newChild)
			}
		}
	}
