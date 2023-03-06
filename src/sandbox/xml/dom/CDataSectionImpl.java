package sandbox.xml.dom;

import org.w3c.dom.Node;

public class CDataSectionImpl extends AbstractTextNode implements org.w3c.dom.CDATASection {
	CDataSectionImpl(DocumentImpl owner,final String text) {
		super(owner, text);
		}

	@Override
	public final String getNodeName() {
		return "#cdata-section"; // in spec
		}
	
	@Override
	public boolean equals(final Object obj) {
		if(obj == this) return true;
		if(obj==null ||  !(obj instanceof CDataSectionImpl)) return false;
		final CDataSectionImpl o = CDataSectionImpl.class.cast(obj);
		return o.getData().equals(this.getData());
		}
	
	@Override
	public final short getNodeType() {
		return CDATA_SECTION_NODE;
		}
	
	@Override
	public Node cloneNode(boolean deep) {
		return getOwnerDocument().createCDATASection(this.getData());
		}
	
	@Override
	public String getPath() {
		String s= "cdata()";
		if(getParentNode()!=null) {
			s=getParentNode().getPath()+"/"+s;
			}
		return s;
		}
	}
