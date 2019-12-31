package sandbox.html;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.beust.jcommander.Parameter;

import sandbox.IOUtils;
import sandbox.Launcher;
import sandbox.TidyToDom;

public class HtmlTidy extends Launcher {
	private static final sandbox.Logger LOG = sandbox.Logger.builder(HtmlTidy.class).build();
	private final TidyToDom tidyToDom = new TidyToDom();

	@Parameter(names={"-o","--out"},description="output file")
	private Path output;
	@Parameter(names={"--css","--style","-y"},description="remove CSS/Style")
	private boolean removeCss = false;
	@Parameter(names={"--script","-s"},description="remove script")
	private boolean removeScript = false;
	@Parameter(names={"--comment","-c"},description="remove comment")
	private boolean removeComment = false;
	@Parameter(names={"--normalize-space","--space","-w"},description="normalize space")
	private boolean normalize_space = false;

	private void cleanup(final Node root) {
		for(;;) {
		boolean changed=false;
		for(Node c=root.getFirstChild();c!=null && !changed;c=c.getNextSibling()) {
			if(c.getNodeType()==Node.ELEMENT_NODE) {
				final Element e = Element.class.cast(c);
				final String tag=e.getTagName();
				if((this.removeCss && tag.equalsIgnoreCase("style")) ||
					(this.removeScript && tag.equalsIgnoreCase("script"))) {
					root.removeChild(e);
					changed=true;
					break;
					}
				}
			else if(c.getNodeType()==Node.COMMENT_NODE && this.removeComment) {
				root.removeChild(c);
				changed=true;
				break;
				}
			else if(c.getNodeType()==Node.TEXT_NODE && this.normalize_space) {
				final String s1= Text.class.cast(c).getData();
				final String s2= s1.replaceAll("[\\s]+", " ");
				if(!s1.equals(s2)) {
					final Text text2 = c.getOwnerDocument().createTextNode(s2);
					root.replaceChild(text2, c);
					changed=true;
					break;
					}
				}
			}
		if(!changed) break;
		}
		
	for(Node c=root.getFirstChild();c!=null ; c=c.getNextSibling()) {
			if(c.hasChildNodes()) cleanup(c);
		}
	}
	
	@Override
	public int doWork(final List<String> args) {
		try {
			final String input = oneFileOrNull(args);
			final Document dom;
			if(input==null) {
				try(InputStreamReader r=new InputStreamReader(System.in)) {
					dom= tidyToDom.read(r);
					}
				}
			else
				{
				try(Reader r=Files.newBufferedReader(Paths.get(input))) {
					dom= tidyToDom.read(r);
					}
				}
			cleanup(dom);
			
			final TransformerFactory trf = TransformerFactory.newInstance();
			final Transformer tr = trf.newTransformer();
			tr.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
			
			try(Writer w=IOUtils.openPathAsWriter(this.output)) {
				tr.transform(
					new DOMSource(dom),
					new StreamResult(w)
					);
				w.flush();
				}

			return 0;
			}
		catch(final Throwable err) {
			LOG.error(err);
			return -1;
			}
		finally {
			
		}
		
		}
	
	public static void main(final String[] args) {
		new HtmlTidy().instanceMainWithExit(args);
	}

}
