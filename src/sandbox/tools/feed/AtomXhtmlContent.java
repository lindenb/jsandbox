package sandbox.tools.feed;

import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.html.TidyToDom;

/**
 * 
 * change atom content/@type='html' to content/@type='xhtml'
 *
 */
public class AtomXhtmlContent extends Launcher {
	private static final Logger LOG = Logger.builder(AtomXhtmlContent.class).build();
	private final TidyToDom tidyToDom = new TidyToDom();
	
	private void expand(final Document dom,final Element root)
		{
		boolean hasElement=false;
		boolean hasText=false;
		
		for(Node n=root.getFirstChild();
				n!=null;
				n=n.getNextSibling())
			{
			if(n.getNodeType()==Node.ELEMENT_NODE)
				{
				hasElement=true;
				this.expand(dom,Element.class.cast(n));
				}
			}
		if(!hasElement && 
				root.hasAttribute("type") &&
				root.getAttribute("type").equals("html"))
			{
			
			final DocumentFragment df = this.tidyToDom.importString(root.getTextContent(),dom);
			
			if(df!=null && df.hasChildNodes())
				{
				root.setAttribute("type", "xhtml");
				while(root.hasChildNodes()) root.removeChild(root.getFirstChild());
				final Element div = dom.createElement("div");
				div.setAttribute("xmlns", "http://www.w3.org/1999/xhtml");
				root.appendChild(div);
				div.appendChild(df);
				}
			}
		}
	@Override
	public int doWork(final List<String> args)
		{
		try
			{
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final  DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom=null;
			if(args.isEmpty())
				{
				dom = db.parse(System.in);
				}
			else if(args.size()==1)
				{
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
	catch(final Exception err)
		{
		LOG.error(err);
		return -1;
		}
	}
	public static void main(final String[] args) {
		new AtomXhtmlContent().instanceMainWithExit(args);
	}

}
