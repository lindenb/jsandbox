package sandbox;

import java.io.StringReader;
import java.util.List;
import java.util.function.Function;

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

public class HtmlInXml extends Launcher {
	private static final Logger LOG = Logger.builder(HtmlInXml.class).build();

	public static class TidyToDom
		implements Function<Node, Node>
		{
		private final Document owner;
		TidyToDom(final Document owner) {
			this.owner = owner;
			}
		
		@Override
		public Node apply(Node t)
			{
			return clone(t);
			}
		private Node clone(final Node n)
			{
			switch(n.getNodeType())
				{
				case Node.TEXT_NODE:
					{
					String text = Text.class.cast(n).getTextContent();
					if(text!=null) return this.owner.createTextNode(text);
					return null;
					}
				case Node.CDATA_SECTION_NODE:
					{
					String text = CDATASection.class.cast(n).getTextContent();
					if(text!=null) return this.owner.createTextNode(text);
					return null;
					}
				case Node.ELEMENT_NODE:
					{
					final Element e= Element.class.cast(n);
					final NamedNodeMap atts = e.getAttributes();
					final Element r = this.owner.createElementNS(e.getNamespaceURI(),e.getNodeName());
					for(int i=0;i< atts.getLength();++i)
						{
						Attr att=(Attr)atts.item(i);
						r.setAttributeNS("http://www.w3.org/1999/xhtml",att.getNodeName(),att.getValue());
						}
					
					for(Node c=e.getFirstChild();c!=null;c=c.getNextSibling())
						{
						final Node x = this.clone(c); 
						if(x==null ) continue;
						r.appendChild(x);
						}
					return r;
					}
				default: LOG.warning(">>>>"+n.getNodeType()+ " "+n); return null;
				}
			}
		
		}
	
	
	
	private void expand(final Document dom,final Element root)
		{
		boolean hasElement=false;
		boolean hasText=false;
		
		for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling())
			{
			if(n.getNodeType()==Node.ELEMENT_NODE)
				{
				hasElement=true;
				this.expand(dom,Element.class.cast(n));
				}
			else if(n.getNodeType()==Node.TEXT_NODE || n.getNodeType()==Node.CDATA_SECTION_NODE)
				{
				final String s=org.w3c.dom.CharacterData.class.cast(n).getTextContent();
				if(s.contains("<") && s.contains(">")) hasText=true;
				}
			}
		if(!hasElement && hasText)
			{
			final Tidy tidy = new Tidy();
			tidy.setXmlOut(true);
			tidy.setShowErrors(0);
			tidy.setShowWarnings(false);
			StringReader sr = new StringReader(root.getTextContent());
			final Document newdoc = tidy.parseDOM(sr, null);
			sr.close();
			if(newdoc!=null && newdoc.getDocumentElement()!=null)
				{
				
				final Node newChild = new TidyToDom(dom).apply(newdoc.getDocumentElement());
				
				
				if( newChild !=null)
					{					
					while(root.hasChildNodes()) root.removeChild(root.getFirstChild());
					root.appendChild(newChild);
					}
				}
			}
		}
	@Override
	public int doWork(final List<String> args)
		{
		try
			{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom=null;
			if(args.isEmpty())
				{
				LOG.info("read stdin");
				dom = db.parse(System.in);
				}
			else if(args.size()==1)
				{
				LOG.info("read "+args.get(0));
				dom = db.parse(args.get(0));
				}
			else
				{
				LOG.warning("Illegal number of args");
				return -1;
				}
			
			this.expand(dom,dom.getDocumentElement());
			
			final TransformerFactory trf = TransformerFactory.newInstance();
			final Transformer tr = trf.newTransformer();
			tr.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
			tr.transform(new DOMSource(dom),
					new StreamResult(System.out)
					);
			
			return 0;
			}
	catch(Exception err)
		{
		LOG.error(err);
		return -1;
		}
	}
	public static void main(final String[] args) {
		new HtmlInXml().instanceMainWithExit(args);
	}

}
