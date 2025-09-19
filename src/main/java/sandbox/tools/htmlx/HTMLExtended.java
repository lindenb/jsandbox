package sandbox.tools.htmlx;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.beust.jcommander.Parameter;

import sandbox.ImageUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.image.ImageMetaData;
import sandbox.io.IOUtils;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;

public class HTMLExtended extends Launcher{
	protected static final Logger LOG=Logger.builder(HTMLExtended.class).build();
    @Parameter(names={"-d","--data"},description="data directory")
    private File data_directory = null; 
	private CloseableHttpClient httpClient = null;
	public HTMLExtended() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * NodeHandler
	 */
	private interface NodeHandler {
		public String getName();
		public default String getDescription() {
			return getName();
			}
		public void apply(Document dom) throws Exception;
		}
	/**
	 * NodeHandler
	 */
	private abstract class AbstractHandler implements NodeHandler {
		boolean isA(Node n, String name) {
			return n.getNodeType()==Node.ELEMENT_NODE && n.getNodeName().equals("bash");
		}
		
		Element transfertChildAndAttributes(Node root,Element dest) {
			if(root.getNodeType()!=Node.ELEMENT_NODE) throw new IllegalArgumentException();
			Element src=Element.class.cast(root);
			if(src.hasAttributes()) {
				NamedNodeMap atts= src.getAttributes();
				for(int i=0;i< atts.getLength();i++) {
					dest.setAttributeNode((Attr)atts.item(i));
					}
				}
			if(src.hasChildNodes()) {
				for(Node c=src.getFirstChild();c!=null;c=c.getNextSibling()) {
					src.removeChild(c);
					dest.appendChild(c);
					}
				}
			return dest;
			}

		void inheritIO(final InputStream src, final OutputStream dest) throws Exception {
		    new Thread(new Runnable() {
		        public void run() {
		        	try {
			           int c;
			           while((c=src.read())!=-1) {
			        	dest.write(c);   
			           	}
		        	} catch(IOException err) {
		        		err.printStackTrace();
		        	}
		        }
		    }).start();
		   }
		
		public Node visitNode(Node root) throws Exception {
			visitChildrenOf(root);
			return root;
			}

		public void visitChildrenOf(Node root) throws Exception {
			for(Node n1=root.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
				n1=visitNode(n1);
				}
			}
		
		@Override
		public void apply(Document dom) throws Exception {
			Element root=dom.getDocumentElement();
			if(root==null) return;
			visitNode(root);
			}
		}
	
	private abstract class ElementHandler extends AbstractHandler {
		@Override
		public void apply(Document dom) throws Exception {
			Element root=dom.getDocumentElement();
			if(root==null) return;
			visitNode(root);
			}
		}
	private class AutoHeading extends ElementHandler {
		@Override
		public String getName() {
			return "hx";
			}
		@Override
		public String getDescription() {
			return "use <hx>title</hx> as auto header. Will be replaced by <h1>, <h2> , etc... according to the depth of the <hx>";
			}
		@Override
		public Node visitNode(org.w3c.dom.Node root) throws Exception {
			if(isA(root,"hx")) {
				int level=1;
				Node n=root;
				while(n.getParentNode()!=null) {
					if(n.getNodeType()==Node.ELEMENT_NODE && n.getNodeName().matches("h[1-6]")) {
						level= 1 + Integer.parseInt(n.getNodeName().substring(1));
						}
					n=n.getParentNode();
					}
				Element h = root.getOwnerDocument().createElement("h"+level);
				root = transfertChildAndAttributes(root,h);
				}
			visitChildrenOf(root);
			return root;
			}
		}
	
	private class RemoteImage extends ElementHandler {
		@Override
		public String getName() {
			return "remote-image";
			}
		@Override
		public String getDescription() {
			return "remote-image";
			}
		@Override
		public org.w3c.dom.Node visitNode(org.w3c.dom.Node node) throws Exception {
			if(!isA(node,"remote-image")) return node;
			Element root = Element.class.cast(node);
			if(!root.hasAttribute("src")) return root;
			String src =root.getAttribute("src");
			if(!IOUtils.isURL(src)) return null;
			String md5= StringUtils.md5(src);
			Element img= root.getOwnerDocument().createElement("img");
			img.removeAttribute("img");
 			transfertChildAndAttributes(root, img);
			File toFile =new File(data_directory,md5+".png");
			if(toFile.exists()) {
				try(CloseableHttpResponse resp=getHttpClient().execute(new HttpGet(src))) {
					if(resp.getStatusLine().getStatusCode()==HttpStatus.SC_OK) {
						IOUtils.copyTo(resp.getEntity().getContent(), toFile);
						}
					
					}
				}
			if(toFile.exists()) {
				Optional<ImageMetaData> meta= sandbox.image.ImageUtils.getMetaData(toFile);
				if(meta.isPresent()) {
					if(!img.hasAttribute("width")) img.setAttribute("width", String.valueOf(meta.get().getWidth()));
					if(!img.hasAttribute("height")) img.setAttribute("height", String.valueOf(meta.get().getHeight()));
					}
				img.setAttribute("img", toFile.toString());
				}
			
			
			return img;
			}
		}
	private class BashHandler extends ElementHandler {
		@Override
		public String getName() {
			return "bash";
			}
		@Override
		public org.w3c.dom.Node visitNode(org.w3c.dom.Node node) throws Exception {
				if(isA(node,"bash")) {
				Element root = Element.class.cast(node);
				String script=root.getTextContent();
				File f=File.createTempFile("tmp", ".bash");
				Files.writeString(f.toPath(), script);
				Element div = root.getOwnerDocument().createElement("div");
				ByteArrayOutputStream stdout =new ByteArrayOutputStream();
				ByteArrayOutputStream stderr =new ByteArrayOutputStream();
				Process p = new ProcessBuilder("/bin/bash", f.toString()).start();
				inheritIO(p.getInputStream(), stdout);
				inheritIO(p.getErrorStream(), stderr);
				int exit_value=p.waitFor();
				stdout.close();
				stderr.close();
				f.delete();
				
				Element pre= root.getOwnerDocument().createElement("pre");
				pre.setAttribute("class", "script");
				pre.setAttribute("lang", "bash");
				pre.appendChild(root.getOwnerDocument().createTextNode(script));
				div.appendChild(pre);
				
				pre= root.getOwnerDocument().createElement("pre");
				pre.setAttribute("class", "stdout");
				pre.setAttribute("lang", "bash");
				pre.appendChild(root.getOwnerDocument().createTextNode(new String(stdout.toByteArray())));
				div.appendChild(pre);
				
				pre= root.getOwnerDocument().createElement("pre");
				pre.setAttribute("class", "stderr");
				pre.setAttribute("lang", "bash");
				pre.appendChild(root.getOwnerDocument().createTextNode(new String(stderr.toByteArray())));
				div.appendChild(pre);
				return div;
				}
			visitChildrenOf(node);
			return node;
			}
		}
	
	private class GraphVizHandler extends ElementHandler {
		@Override
		public String getName() {
			return "graphviz";
			}
		@Override
		public String getDescription() {
			return "invoke graphviz dot";
			}
		@Override
		public Node visitNode(Node root) throws Exception {
			if(!isA(root,"graphviz")) {
				String script=root.getTextContent();
				File f=File.createTempFile("tmp", ".dot");
				Files.writeString(f.toPath(), script);
				Process p = new ProcessBuilder("dot", f.toString()).start();
				
				f.delete();
				}
			return root;
			}
		}
	
	private class ImageBase64 extends ElementHandler {
		@Override
		public String getName() {
			return "img64";
			}
		@Override
		public String getDescription() {
			return "include image as data:base64";
			}
		@Override
		public Node visitNode(Node node) throws Exception {
			if(isA(node,"img")) {
				Element root = Element.class.cast(node);
				if(root.hasAttribute("src")) {
					String src = root.getAttribute("src");
					BufferedImage img=null;
					if(src.endsWith(".svg") || src.endsWith(".svg.gz")) {
						//nothing
						}
					else if(IOUtils.isURL(src)) {
						img= ImageIO.read(URI.create(src).toURL());
						}
					else if(!src.startsWith("data:")) {
						img = ImageIO.read(new File(src));
						}
					if(img!=null) {
						
						}
					}
				}
			visitChildrenOf(node);
			return node;
			}
		}
	
	private final List<NodeHandler> handlers= new ArrayList<>();
	
	
	
	
	private CloseableHttpClient getHttpClient() {
		if(this.httpClient==null) {
			final HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setUserAgent(IOUtils.getUserAgent());
			this.httpClient = builder.build();
			}
		return httpClient;
		}
	
	@Override
	public int doWork(List<String> args) {
		
		try {
			
			
			String input = super.oneFileOrNull(args);
			DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setCoalescing(true);
			dbf.setXIncludeAware(true);
			DocumentBuilder db=dbf.newDocumentBuilder();
			final Document dom;
			if(input==null) {
				dom = db.parse(System.in);
				}
			else
				{
				dom = db.parse(input);
				}
			
			this.handlers.add(new BashHandler());
			this.handlers.add(new AutoHeading());
			this.handlers.add(new GraphVizHandler());
			
			for(NodeHandler h:this.handlers) {
				h.apply(dom);
				}
			final Result result= new StreamResult(System.out);
			final TransformerFactory trf=TransformerFactory.newInstance();
			final Transformer tr=trf.newTransformer();
			tr.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
			tr.transform(new DOMSource(dom), result);
			return -1;
			}
		catch(Throwable err) {
			LOG.error(err);;
			return -1;
			}
		}
	

	  public static ProgramDescriptor getProgramDescriptor() {
			return new ProgramDescriptor() {
				@Override
				public String getName() {
					return "htmlx";
					}
				};
			}
	
	public static void main(String[] args) {
		new HTMLExtended().instanceMainWithExit(args);
		}

}
