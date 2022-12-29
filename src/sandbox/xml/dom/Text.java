package sandbox.xml.dom;

import java.util.Collections;
import java.util.List;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Text extends Node implements CharSequence {
	final String text;
	Text(Element parent,final String text) {
		super(parent);
		this.text = text;
		}
	
	@Override
	public int hashCode() {
		return getData().hashCode();
		}
	
	@Override
	public boolean equals(final Object obj) {
		if(obj == this) return true;
		if(obj==null ||  !(obj instanceof Text)) return false;
		final Text o = Text.class.cast(obj);
		return o.getData().equals(this.getData());
		}
	
	@Override
	public final boolean isText() { return true;}
	@Override
	public final boolean isElement() { return false;}
	@Override
	public final boolean isAttribute() { return false;}
	
	public String getData() {
		return this.text;
		}
	@Override
	public final List<Node> getChildrenAsList() {
		return Collections.emptyList();
		}
	
	@Override
	public void sax(final DefaultHandler handler) throws SAXException {
		char[] ch = getData().toCharArray();
		handler.characters(ch, 0, ch.length);
	}

	@Override
	public char charAt(int i) {
		return getData().charAt(i);
		}
	@Override
	public int length() {
		return getData().length();
		}
	@Override
	public CharSequence subSequence(int beginIndex, int endIndex) {
		return getData().subSequence(beginIndex,endIndex);
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
	public org.w3c.dom.Node toDOM(org.w3c.dom.Document doc) {
		return doc.createTextNode(getData());
		}
	
	@Override
	public String toString() {
		return getData();
		}
	}
