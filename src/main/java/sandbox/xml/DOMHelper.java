package sandbox.xml;

import javax.xml.XMLConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import sandbox.svg.SVG;
import sandbox.util.function.FunctionalMap;

public interface DOMHelper {

	public Document getDocument();
	
	default String toString(Object o) {
		if(o==null) return "";
		return String.valueOf(o);
	}
	public default String getDefaultNamespace() {
		return XMLConstants.NULL_NS_URI;
	}
	
	default String getNamespaceForPrefix(String pfx) {
		if(pfx.equals("svg")) return SVG.NS;
		throw new IllegalArgumentException(""+pfx);
		}
	default Text text(final Object o) {
		if(o==null) return null;
		final String s= toString(o);
		if(s==null) return null;
		return getDocument().createTextNode(s);
		}
	default Element element(final String qName) {
		return element(qName,null,FunctionalMap.empty());
		}
	default Element element(String qName,Object content,FunctionalMap<String, Object> atts) {
		String namespaceURI;
		final Element e;
		int colon = qName.indexOf(':');
		if(colon==-1) {
			namespaceURI = getDefaultNamespace();
			}
		else
			{
			namespaceURI = getNamespaceForPrefix(qName.substring(0,colon));
			}
		
		if( XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
			e= getDocument().createElement(qName);
			}
		else
			{
			e= getDocument().createElementNS(namespaceURI,qName);
			}
		
		if(content!=null) {
			final Text t=text(content);
			if(t!=null) e.appendChild(t); 
			}
		
		if(atts!=null && !atts.isEmpty()) {
			for(String key:atts.keySet()) {
				Object o = atts.get(key);
				if(o==null) continue;
				String v= toString(o);
				if(v==null) continue;
				colon=key.indexOf(":");
				
				if(colon==-1) {
					namespaceURI =  XMLConstants.NULL_NS_URI;// yes, for attributes, use no Ns
					}
				else
					{
					namespaceURI = getNamespaceForPrefix(qName.substring(0,colon));
					}
				if( XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
					e.setAttribute(key, v);
					}
				else
					{
					e.setAttributeNS(
						namespaceURI,
						key, v
						);
					}
				}
			}
		return e;
		}

}
