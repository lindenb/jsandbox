package sandbox.xml.dom;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Text;

public class TextImpl extends AbstractTextNode implements Text {
	TextImpl(DocumentImpl downer,final String text) {
		super(downer, text);
		}
	
	@Override
	public final String getNodeName() {
		return "#text"; // in spec
		}
	
	@Override
	public final short getNodeType() {
		return TEXT_NODE;
		}
	
	@Override
	public boolean equals(final Object obj) {
		if(obj == this) return true;
		if(obj==null ||  !(obj instanceof TextImpl)) return false;
		final TextImpl o = TextImpl.class.cast(obj);
		return o.getData().equals(this.getData());
		}
	
	@Override
	public Text cloneNode(boolean deep) {
		return getOwnerDocument().createTextNode(this.getData());
		}
	
	@Override
	public String getPath() {
		String s= "text()";
		if(getParentNode()!=null) {
			s=getParentNode().getPath()+"/"+s;
			}
		return s;
		}
	
	
	}
