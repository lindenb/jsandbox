package sandbox.xml.dom;


import javax.xml.namespace.QName;


public abstract class AbstractNamedNode extends AbstractNode {
	private final QName qName;
	protected AbstractNamedNode(final DocumentImpl owner,final QName qName) {
		super(owner);
		this.qName=qName;
		}
	
	@Override
	public QName getQName() {
		return this.qName;
		}
	
	@Override
	public int hashCode() {
		return this.getQName().hashCode();
		}

	}
