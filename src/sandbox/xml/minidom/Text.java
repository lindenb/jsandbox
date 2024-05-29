package sandbox.xml.minidom;

import java.util.function.Consumer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import sandbox.StringUtils;

public class Text extends Node implements CharSequence {
	protected String content;
	public Text(final Object s) {
		this.content= s==null?"":Node.toString(s);
		}
	public boolean isBlank() {
		return StringUtils.isBlank(getTextContent());
	}
	
	public void appendData(Object s) {
		this.content+=(s==null?"":Node.toString(s));
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
		return getTextContent().length();
		}
	@Override
	public char charAt(int index) {
		return getTextContent().charAt(index);
		}
	@Override
	public CharSequence subSequence(int start, int end) {
		return getTextContent().subSequence(start, end);
		}
	@Override
	public int hashCode() {
		return getTextContent().hashCode();
		}
	@Override
	public void setTextContent(Object o) {
		this.content= o==null?"":Node.toString(o);
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
	public void find(final Consumer<Node> consumer) {
		consumer.accept(this);
		}
	@Override
	public void write(final XMLStreamWriter w) throws XMLStreamException {
		w.writeCharacters(getTextContent());
		}
	@Override
	public Node clone(boolean deep) {
		return new Text(this.getTextContent());
		}
	@Override
	public String getPath() {
		return (hasParentNode()?getParentNode().getPath()+"/":"")+"text()";
		}
	@Override
	public org.w3c.dom.Node toDOM(org.w3c.dom.Document owner) {
		return owner.createTextNode(getTextContent());
		}
	public boolean isEqualNode(final Node other) {
		if(other==null || !other.isText()) return false;
		if(this.isSameNode(other)) return true;
		return getTextContent().equals(other.asText().getTextContent());
		}

	}
