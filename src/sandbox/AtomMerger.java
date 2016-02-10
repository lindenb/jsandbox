package sandbox;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



public class AtomMerger extends AbstractApplication
	{
	private static final String ATOM="http://www.w3.org/2005/Atom";

	private static final Logger LOG = LoggerFactory.getLogger("jsandbox");
	private File xslStylesheetRssToAtom = null;
	private Transformer rss2atom=null;
	private boolean ignoreErrors=false;
	private Document convertRssToAtom(Document rss) throws Exception
		{
		if(rss2atom==null)
			{
			if(xslStylesheetRssToAtom==null)
				{
				throw new RuntimeException("XSLT stylesheet to convert rss to atom was not provided");
				}
			TransformerFactory factory = TransformerFactory.newInstance();

			// Use the factory to create a template containing the xsl file
			Templates templates = factory.newTemplates(new StreamSource( new FileInputStream(this.xslStylesheetRssToAtom)));
			this.rss2atom = templates.newTransformer();
			}
		DOMResult domResult = new DOMResult();
		this.rss2atom.transform( new DOMSource(rss), domResult);
		Document atom  = (Document)domResult.getNode();
		Element root= atom.getDocumentElement();
		if(!root.getLocalName().equals("feed"))
			{
			throw new RuntimeException("Ouput is not atom");
			}
		return atom;
		}
	
	@Override
	protected void fillOptions(Options options) {
		options.addOption(Option.builder("rss2atom").longOpt("rss2atom").hasArg(true).desc("Optional XSLT stylesheet transforming rss to atom.").build());
		options.addOption(Option.builder("i").longOpt("ignore").hasArg(true).desc("Ignore Errors").build());
		super.fillOptions(options);
	}
	
	@Override
	protected int execute(CommandLine cmd) {
		if(cmd.hasOption("rss2atom")) {
			this.xslStylesheetRssToAtom = new File(cmd.getOptionValue("rss2atom"));
		}
		if(cmd.hasOption("i")) {
			this.ignoreErrors = true;
		}

		final List<String> args=cmd.getArgList();
		if(args.isEmpty())
			{
			System.err.println("Empty input");
			return -1;
			}
		try {
			Set<String> paths = new LinkedHashSet<>();
			for(final String arg:args)
				{
				if(!IOUtils.isURL(arg) && arg.endsWith(".list"))
					{
					paths.addAll( Files.readAllLines(Paths.get(arg),  Charset.defaultCharset()));
					}
				else
					{
					paths.add(arg);
					}
				}
			paths.remove("");
		
			final Set<String> seenids = new HashSet<>();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			Document outdom = db.newDocument();
			Element feedRoot = outdom.createElementNS(ATOM,"feed");
			outdom.appendChild(feedRoot);
			
			
			
			for(String path:paths)
				{
				if(path.isEmpty()) continue;
				LOG.info(path);
				Document dom = null;
				
				try {
					
					dom  = db.parse(path);
					Element root= dom.getDocumentElement();
					if(root.getLocalName().equals("rss"))
						{
						dom = convertRssToAtom(dom);
						root= dom.getDocumentElement();
						}
					else if(!(root.getLocalName().equals("feed") && root.getNamespaceURI().equals(ATOM)))
						{
						System.err.println("Not root atom or rss for "+path);
						return -1;
						}
					
					for(Node c1= root.getFirstChild();c1!=null;c1=c1.getNextSibling())
						{
						if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
						if(!c1.getLocalName().equals("entry")) continue;
						String id=null;
						for(Node c2= c1.getFirstChild();c2!=null;c2=c2.getNextSibling())
							{
							if(c2.getNodeType()!=Node.ELEMENT_NODE) continue;
							if(!c2.getLocalName().equals("id")) continue;
							id  = c2.getTextContent().trim();
							break;
							}
						if(id==null || seenids.contains(id)) continue;
						seenids.add(id);
						
						feedRoot.appendChild(outdom.importNode(c1, true));
						}
				} catch(Exception err)
					{
					if(this.ignoreErrors) {
						LOG.error("Ignore error", err);
						}
					else
						{
						throw err;
						}
					}
				}
			
			final TransformerFactory trf = TransformerFactory.newInstance();
			final Transformer tr = trf.newTransformer();
			tr.setOutputProperty(OutputKeys.INDENT,"yes");
			tr.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
			tr.transform(new DOMSource(outdom),
					new StreamResult(System.out)
					);

			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
		}
		
		}
	
	public static void main(String[] args)
		{
		new AtomMerger().instanceMainWithExit(args);
		}
	
	
	}
