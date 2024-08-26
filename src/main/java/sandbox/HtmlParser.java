package sandbox;

import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.tidy.Tidy;

import sandbox.io.NullWriter;
import sandbox.lang.Result;
import sandbox.xml.DOMIterator;

public class HtmlParser {
private static final Logger LOG = Logger.builder(HtmlParser.class).build();

private final Tidy tidy;
private final DocumentBuilder db;
private boolean namespaceAware=false;
public HtmlParser() {
	try
		{
		this.tidy = new Tidy();
		this.tidy.setXmlOut(true);
		this.tidy.setShowErrors(0);
		this.tidy.setShowWarnings(false);
		this.tidy.setErrout(new PrintWriter(new NullWriter()));
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		this.db = dbf.newDocumentBuilder();
		}
	catch(final Exception err)
		{
		LOG.error(err);
		throw new RuntimeException(err);
		}
	}

public HtmlParser setNamespaceAware(boolean namespaceAware) {
	this.namespaceAware = namespaceAware;
	return this;
	}
public boolean isNamespaceAware() {
	return namespaceAware;
	}

public Result<Document> parseDom(final String htmlStr)  {
	try(final StringReader r=new StringReader(htmlStr)) {
		return parseDom(r);
		}
	}

public Result<Document> parseDom(final Reader r)  {
	try {
		final Document tidyDom = this.tidy.parseDOM(r,null);
		final Document dom = this.db.newDocument();
		final Node newChild = clone(dom,tidyDom.getDocumentElement());		
		if( newChild !=null)
			{					
			dom.appendChild(newChild);
			}
		return Result.success(dom);
		}
	catch(final Exception err) {
		LOG.error(err);
		return Result.fail(err);
		}
	}

private Node clone(final Document owner,final Node n)
	{
	switch(n.getNodeType())
		{
		case Node.TEXT_NODE:
			{
			String text = Text.class.cast(n).getTextContent();
			if(text!=null) return owner.createTextNode(text);
			return null;
			}
		case Node.CDATA_SECTION_NODE:
			{
			String text = CDATASection.class.cast(n).getTextContent();
			if(text!=null) return owner.createTextNode(text);
			return null;
			}
		case Node.ELEMENT_NODE:
			{
			final Element e= Element.class.cast(n);
			final NamedNodeMap atts = e.getAttributes();
			final Element r ;
			if(isNamespaceAware()) {
				r=owner.createElementNS(e.getNamespaceURI(),e.getNodeName());
				}
			else
				{
				r= owner.createElement(e.getNodeName());
				}
			for(int i=0;i< atts.getLength();++i)
				{
				Attr att=(Attr)atts.item(i);
				r.setAttribute(att.getNodeName(),att.getValue());
				}
			
			for(Node c:DOMIterator.nodes(e)) {
				final Node x = clone(owner,c); 
				if(x==null ) continue;
				r.appendChild(x);
				}
			return r;
			}
		default: LOG.warning(">>>>"+n.getNodeType()+ " "+n); return null;
		}
	}

public static void main(final String[] args) {
	try
		{
		final TransformerFactory trf = TransformerFactory.newInstance();
		final Transformer tr = trf.newTransformer();
		tr.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		tr.setOutputProperty(OutputKeys.METHOD, "xml");
		final HtmlParser parser = new HtmlParser();

		
		for(final String filename:args) {
			try(final Reader r = new FileReader(filename)) {
				final Result<Document> dom= parser.parseDom(r);
				if(dom.isError()) continue;
				tr.transform(new DOMSource(dom.get()), new StreamResult(System.out));
				}
			}
			
		}
	catch(final Exception err)
		{
		LOG.error(err);
		System.exit(-1);
		}
	}
	
	
}
