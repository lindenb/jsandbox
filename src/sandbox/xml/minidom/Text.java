package sandbox.xml.minidom;

import java.util.function.Consumer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class Text extends Node implements CharSequence {
	protected String content;
	public Text(final Object s) {
		this.content= s==null?"":Node.toString(s);
		}
	@Override
	public final boolean isText() {
		return true;
		}
	@Override
	public final boolean isElement() {
		return false;
		}
	@Override
	public int length() {
		return content.length();
		}
	@Override
	public char charAt(int index) {
		return content.charAt(index);
		}
	@Override
	public CharSequence subSequence(int start, int end) {
		return content.subSequence(start, end);
		}
	@Override
	public int hashCode() {
		return content.hashCode();
		}
	@Override
	public void setTextContent(Object o) {
		this.content= Node.toString(o);
		}
	@Override
	public String getTextContent() {
		return this.content;
		}

	
	@Override
	public final String toString() {
		return getTextContent();
		}
	
	@Override
	public void find(Consumer<Node> consumer) {
		consumer.accept(this);
		}
	@Override
	public void write(XMLStreamWriter w) throws XMLStreamException {
		w.writeCharacters(getTextContent());
		}

	}
