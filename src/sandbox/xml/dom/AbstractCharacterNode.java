package sandbox.xml.dom;



import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * base class for text, CDATA, Comment...
 *
 */
public abstract class AbstractCharacterNode extends AbstractTerminalNode implements CharacterData,CharSequence {
	protected final StringBuilder text ;

	protected AbstractCharacterNode(final DocumentImpl owner,final String text) {
		super(owner);
		this.text = new StringBuilder(text);
		}
	


	
	@Override
	public final int hashCode() {
		return getData().hashCode();
		}
	
	@Override
	public boolean equals(final Object obj) {
		if(obj == this) return true;
		if(obj==null ||  !(obj instanceof AbstractCharacterNode)) return false;
		final AbstractCharacterNode o = AbstractCharacterNode.class.cast(obj);
		return o.getData().equals(this.getData());
		}
	
	
	@Override
	public final char charAt(int i) {
		return this.text.charAt(i);
		}
	@Override
	public final int length() {
		return this.text.length();
		}
	@Override
	public final CharSequence subSequence(int beginIndex, int endIndex) {
		return this.text.subSequence(beginIndex,endIndex);
		}
		
	@Override
	public final String getNodeValue() throws DOMException {
		return getData();
		}
	
	@Override
	public final String getTextContent() throws DOMException {
		return getData();
		}
	
	public final String getData() {
		return this.text.toString();
		}

	@Override
	public final void setTextContent(final String textContent) throws DOMException {
		setData(textContent);
		}
	
	@Override
	public void setData(final String data) throws DOMException {
		this.text.delete(0, this.text.length()).append(data);
		}
	
	@Override
	public final int getLength() {
		return length();
		}

	@Override
	public void deleteData(int offset, int count) throws DOMException {
		this.text.delete(offset, offset+count);
		}
	@Override
	public void appendData(String arg) throws DOMException {
		this.text.append(arg);
		}


	@Override
	public void insertData(int offset, String arg) throws DOMException {
		this.text.insert(offset, arg);
		}


	@Override
	public void replaceData(int offset, int count, String arg) throws DOMException {
		this.text.replace(offset, offset+count,arg);
		}

	
	@Override
	public String substringData(int offset, int count) throws DOMException {
		return this.text.substring(offset, offset+count);
		}
	


	
	@Override
	public final String toString() {
		return getData();
		}
	}
