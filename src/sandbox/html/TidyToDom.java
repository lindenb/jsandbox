package sandbox.html;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.tidy.Tidy;

import sandbox.Logger;


public class TidyToDom {
	private static final Logger LOG = Logger.builder(TidyToDom.class).build();
	private final Tidy tidy;
	
	public TidyToDom() {
		this.tidy = new Tidy();
		this.tidy.setXmlOut(true);
		this.tidy.setErrout(null);
		this.tidy.setShowErrors(0);
		this.tidy.setShowWarnings(false);
		}
	public DocumentFragment importString(final String s) {
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			return importString(s,db.newDocument());
			}
		catch(Throwable err) {
			LOG.error(err);
			return null;
			}
		}
	public DocumentFragment importString(final String s, final Document owner)
		{
		final DocumentFragment df=owner.createDocumentFragment();
		if(s==null) return df;
		StringReader sr = null;
		try
			{
			sr = new StringReader(s);
			this.tidy.setPrintBodyOnly(true);
			final Document tmpDom = tidy.parseDOM(sr, null);
			final Element root = tmpDom==null?null:tmpDom.getDocumentElement();
			Node html = null;
			for(Node c=root.getFirstChild();
				c!=null;
				c=c.getNextSibling())
				{
				if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
				if(c.getNodeName().equalsIgnoreCase("body")) {
					html = c;
					break;
					}
				}
		
			for(Node c=html==null?null:html.getFirstChild();
					c!=null;
					c=c.getNextSibling())
				{
				final Node c2 = importNode(c,owner);
				if(c2!=null) df.appendChild(c2);
				}
			}
		catch(final Exception err)
			{
			
			}
		finally
			{
			sr.close();
			}
		return df;
		}
	
	
	public Document read(final Reader r) throws IOException {
		return this.tidy.parseDOM(r, null);
		}

	
	private Node importNode(final Node n,final Document owner)
		{
		if(n==null) return null;
		switch(n.getNodeType())
			{
			case Node.COMMENT_NODE:
				{
				final String text = Comment.class.cast(n).getTextContent();
				if(text!=null) return owner.createComment(text);
				return null;
				}
			case Node.TEXT_NODE:
				{
				final String text = Text.class.cast(n).getTextContent();
				if(text!=null) return owner.createTextNode(text);
				return null;
				}
			case Node.CDATA_SECTION_NODE:
				{
				final String text = CDATASection.class.cast(n).getTextContent();
				if(text!=null) return owner.createTextNode(text);
				return null;
				}
			case Node.ELEMENT_NODE:
				{
				final Element e= Element.class.cast(n);
				final NamedNodeMap atts = e.getAttributes();
				final Element r = owner.createElement(e.getNodeName());
				for(int i=0;i< atts.getLength();++i)
					{
					final Attr att=(Attr)atts.item(i);
					r.setAttribute(att.getNodeName(),att.getValue());
					}
				for(Node c=e.getFirstChild();
						c!=null;
						c=c.getNextSibling())
					{
					final Node x = importNode(c,owner); 
					if(x==null ) continue;
					r.appendChild(x);
					}
				return r;
				}
			case Node.DOCUMENT_FRAGMENT_NODE:
				{
				final DocumentFragment r = owner.createDocumentFragment();
				for(Node c=n.getFirstChild();
						c!=null;
						c=c.getNextSibling())
					{
					final Node x = importNode(c,owner); 
					if(x==null ) continue;
					r.appendChild(x);
					}
				return r;
				}
			default: LOG.warning(">>>>"+n.getNodeType()+ " "+n); return null;
			}
		}
	}
