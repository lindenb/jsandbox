package sandbox.xml.dom;


import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TextImpl extends AbstractTextNode implements org.w3c.dom.Text {
	TextImpl(DocumentImpl downer,final String text) {
		super(downer, text);
		}
	
	@Override
	public /* final not CDATA override this */ short getNodeType() {
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
	public String getWholeText() {
		return getData();
		}
	
	@Override
	public Node cloneNode(boolean deep) {
		return getOwnerDocument().createTextNode(this.getData());
		}
	
	@Override
	public void sax(final DefaultHandler handler) throws SAXException {
		char[] ch = getData().toCharArray();
		handler.characters(ch, 0, ch.length);
		}

	@Override
	public String getPath() {
		String s= "text()";
		if(getParentNode()!=null) {
			s=getParentNode()+"/"+s;
			}
		return s;
		}
	

	@Override
	public Text splitText(int offset) throws DOMException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isElementContentWhitespace() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Text replaceWholeText(String content) throws DOMException {
		throw new UnsupportedOperationException();
		}
	
	}
