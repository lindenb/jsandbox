package sandbox;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
	
	private static class FileAndFilter
		{
		final File file;
		Transformer stylesheet=null;
		
		FileAndFilter(final String file)
		{
			this(new File(file));
		}
		
		FileAndFilter(final File file)
		{
			this.file=file;
		}
		Document transform(final Document src) throws Exception{
			if(this.stylesheet == null) {
				if(this.file == null) throw new NullPointerException("no file defined for fileandfilter.");
				final TransformerFactory factory = TransformerFactory.newInstance();
				// Use the factory to create a template containing the xsl file
				final Templates templates = factory.newTemplates(new StreamSource(this.file));
				this.stylesheet = templates.newTransformer();
				}
			final DOMResult domResult = new DOMResult();
			this.stylesheet.transform( new DOMSource(src), domResult);
			return (Document)domResult.getNode();
			}
		}
	
	private static final Logger LOG = LoggerFactory.getLogger("jsandbox");
	private FileAndFilter rss2atom=null;
	private FileAndFilter json2atom=null;
	private final List<FileAndFilter> atomfilters=new ArrayList<>();
	private final List<FileAndFilter> rssfilters=new ArrayList<>();
	
	
	private boolean ignoreErrors=false;
	private Document convertRssToAtom(Document rss) throws Exception
		{
		if(rss2atom==null) 
			throw new RuntimeException("XSLT stylesheet to convert rss to atom was not provided");
		Document atom  = this.rss2atom.transform(rss);
		if(!isAtom(atom))
			{
			throw new RuntimeException("Ouput is not atom");
			}
		return atom;
		}
	
	private boolean isAtom(final Document dom)
		{
		final Element root= dom.getDocumentElement();
		return root!=null && root.getLocalName().equals("feed") && root.getNamespaceURI().equals(ATOM);
		}
	private boolean isRss(final Document dom)
		{
		final Element root= dom.getDocumentElement();
		return root!=null && root.getLocalName().equals("rss");
		}

	
	
	@Override
	protected void fillOptions(Options options) {
		options.addOption(Option.builder("r2a").longOpt("rss2atom").hasArg(true).desc("Optional XSLT stylesheet transforming rss to atom.").build());
		options.addOption(Option.builder("j2a").longOpt("json2atom").hasArg(true).desc("Optional XSLT stylesheet transforming jsonx to atom.").build());
		options.addOption(Option.builder("i").longOpt("ignore").hasArg(true).desc("Ignore Errors").build());
		options.addOption(Option.builder("rf").longOpt("rssfilter").hasArgs().desc("Optional list of XSLT stylesheets filtering RSS (multiple)").build());
		options.addOption(Option.builder("af").longOpt("atomfilter").hasArgs().desc("Optional list of XSLT stylesheets filtering ATOM (multiple)").build());
		super.fillOptions(options);
	}
	
	@Override
	protected int execute(CommandLine cmd) {
		if(cmd.hasOption("r2a")) {
			this.rss2atom= new FileAndFilter( cmd.getOptionValue("r2a"));
		}
		if(cmd.hasOption("j2a")) {
			this.json2atom= new FileAndFilter( cmd.getOptionValue("j2a"));
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
			if(cmd.hasOption("af")) {
				for(String f: IOUtils.expandList(cmd.getOptionValues("af"))) {
					this.atomfilters.add(new FileAndFilter(f));
				}
			}
			if(cmd.hasOption("rf")) {
				for(String f:IOUtils.expandList(cmd.getOptionValues("rf"))) {
					this.rssfilters.add(new FileAndFilter(f));
				}
			}

			final Set<String> paths = new LinkedHashSet<>();
			paths.addAll(IOUtils.expandList(args));
			paths.remove("");
		
			final Set<String> seenids = new HashSet<>();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			Document outdom = db.newDocument();
			Element feedRoot = outdom.createElementNS(ATOM,"feed");
			outdom.appendChild(feedRoot);
			
			Element c = outdom.createElementNS(ATOM,"title");
			c.appendChild(outdom.createTextNode("AtomMerger"));
			feedRoot.appendChild(c);
			
			c = outdom.createElementNS(ATOM,"updated");
			c.appendChild(outdom.createTextNode(
					new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z").format(new Date())
					));
			feedRoot.appendChild(c);
			
			
			for(String path:paths)
				{
				if(path.isEmpty()) continue;
				LOG.info(path);
				Document dom = null;
				
				try {
					//try as dom 
					try 
						{
						dom  = db.parse(path);
						}
					catch(Exception err)
						{
						final Json2Dom json2dom = new Json2Dom();
						InputStream in = IOUtils.openStream(path); 
						try {
						dom = json2dom.parse(in);
						if(this.json2atom==null) {
							throw new RuntimeException("json2atom stylesheet missing");
						}
						dom = this.json2atom.transform(dom);
						}finally {
							IOUtils.close(in);
						}
						}
					if(isRss(dom))
						{
						for(final FileAndFilter f:this.rssfilters)
							{
							dom = f.transform(dom);
							if(!isRss(dom)) {
								throw new RuntimeException(f.file.getPath()+" didn't convert to rss");
							}
							}
						dom = convertRssToAtom(dom);
						}
					else if(!isAtom(dom))
						{
						System.err.println("Not root atom or rss for "+path);
						return -1;
						}
					
					
					for(final FileAndFilter f:this.atomfilters)
						{
						dom = f.transform(dom);
						if(!isAtom(dom)) {
							throw new RuntimeException(f.file.getPath()+" didn't convert to atom");
							}
						}
					final Element root= dom.getDocumentElement();

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
