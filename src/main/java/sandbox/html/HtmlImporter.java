package sandbox.html;

import java.util.function.Function;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import sandbox.Logger;

public class HtmlImporter implements Function<Node, DocumentFragment> {
	private static final Logger LOG = Logger.builder(HtmlImporter.class).build();
	private final Document owner;
	public HtmlImporter(final Document owner) {
		this.owner = owner;
		}
	
	@Override
	public DocumentFragment apply(final Node t)
		{
		final DocumentFragment frag= owner.createDocumentFragment();
		clone(t,frag);
		return frag;
		}
	
	private void clone(final Node n,final Node parent)
		{
		switch(n.getNodeType())
			{
			case Node.DOCUMENT_FRAGMENT_NODE:
				{
				final DocumentFragment df = DocumentFragment.class.cast(n);
				for(Node n1=df.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
					clone(n1,parent);
					}
				break;
				}
			case Node.TEXT_NODE:
				{
				final String text = Text.class.cast(n).getTextContent();
				if(text!=null) parent.appendChild(this.owner.createTextNode(text));
				break;
				}
			case Node.CDATA_SECTION_NODE:
				{
				final String text = CDATASection.class.cast(n).getTextContent();
				if(text!=null) parent.appendChild(this.owner.createTextNode(text));
				break;
				}
			case Node.ELEMENT_NODE:
				{
				final Element e= Element.class.cast(n);
				final NamedNodeMap atts = e.getAttributes();
				final Element r = this.owner.createElementNS(e.getNamespaceURI(),e.getNodeName());
				for(int i=0;i< atts.getLength();++i)
					{
					final Attr att=(Attr)atts.item(i);
					r.setAttributeNS("http://www.w3.org/1999/xhtml",att.getNodeName(),att.getValue());
					}
				parent.appendChild(r);
				
				for(Node c=e.getFirstChild();c!=null;c=c.getNextSibling())
					{
					this.clone(c,r); 
					}
				break;
				}
			default: LOG.warning(">>>>"+n.getNodeType()+ " "+n); break;
			}
		}
	}
