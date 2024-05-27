package sandbox.xml.dom;

import javax.xml.namespace.QName;

import org.w3c.dom.DOMException;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public abstract class AbstractTextNode extends AbstractCharacterNode implements Text{
	protected AbstractTextNode(final DocumentImpl owner,final String text) {
		super(owner,text);
		}
	
	
	@Override
	public final QName getQName() {
		return null;
		}
	
	@Override
	public final String getPrefix() {
		return null;//spec says null
		}
	
	@Override
	public final String getNamespaceURI() {
		return null;//spec says null
		}
	@Override
	public final String getLocalName() {
		return null;//spec says null
		}
	
	@Override
	public final String getWholeText() {
		return getData();
		}
	

	@Override
	public Text splitText(int offset) throws DOMException {
		final String s1 = super.text.substring(0, offset);
		final String s2 = super.text.substring(offset+1);
		this.setData(s1);
		final Text newNode = getOwnerDocument().createTextNode(s2);
		this.insertBefore(newNode, getNextSibling());
		return newNode;
		}
	
	@Override
	public final void sax(final DefaultHandler handler) throws SAXException {
		final char[] ch = getData().toCharArray();
		handler.characters(ch, 0, ch.length);
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
