package sandbox.tools.xml2ppt;


import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFHyperlink;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.lang.StringUtils;
import sandbox.xml.XmlUtils;

public class XmlToPPT extends Launcher {
	private static final Logger LOG = Logger.builder(XmlToPPT.class).build();
	private static final String TAG_SLIDESHOW="slideshow";
	private static final String TAG_SLIDE="side";
	private static final String TAG_PLACEHOLDER="placeholder";
	private static final String TAG_TEXT="text";
	private static final String TAG_A="a";
	private static final String ATT_LAYOUT="layout";
	private static final String ATT_INDEX="index";
	private static final String ATT_STYLE="style";
	@Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
	private Path outPath = null;

	private void text(XSLFTextParagraph p,String content) {
	
		}
	
	private void paragraph(final XMLSlideShow ppt,int level,String title,List<String> lines) {
		while(!lines.isEmpty() && StringUtils.isBlank(lines.get(lines.size()-1))) lines.remove(lines.size()-1);
		final XSLFSlideMaster slideMaster = ppt.getSlideMasters().get(0);
		
		if(level==1 && !StringUtils.isBlank(title) && lines.isEmpty()) {
			XSLFSlideLayout titleLayout = slideMaster.getLayout(SlideLayout.TITLE);
			XSLFSlide slide1 = ppt.createSlide(titleLayout);
			XSLFTextShape title1 = slide1.getPlaceholder(0);
			XSLFTextParagraph p = title1.addNewTextParagraph();
			text(p,title);
			}
		
		}

	private class Context {
	Context parent=null;
	Node root = null;
 	XMLSlideShow ppt = null;
 	XSLFSlideLayout xslfSlideLayout = null;
 	XSLFSlide slide = null;
 	XSLFTextShape xslfTextShape = null;
 	XSLFTextRun xslfTextRun = null;
 	XSLFHyperlink xslfHyperlink=null;
 	Context() {
 		}
 	Context(Context parent) {
	 		this.parent = parent;
	 		this.root = parent.root;
	 		this.ppt= parent.ppt;
	 		this.xslfSlideLayout= parent.xslfSlideLayout;
	 		this.slide= parent.slide;
	 		this.xslfTextShape= parent.xslfTextShape;
	 		this.xslfTextRun= parent.xslfTextRun;
	 		this.xslfHyperlink= parent.xslfHyperlink;
			}
 	
 	private List<Context> getAncestors() {
 		List<Context> L= new ArrayList<>();
 		Context c=this;
 		while(c!=null) {
 			L.add(c);
 			c=c.parent;
 			}
 		return L;
 		}
 	private Map<String,String> getStyle() {
 		if(!this.root.hasAttributes()) return Collections.emptyMap();
 		final NamedNodeMap m=this.root.getAttributes();
 		final Attr att = (Attr)m.getNamedItem(ATT_STYLE);
 		if(att==null) return Collections.emptyMap();
 		final Map<String, String> map = new HashMap<>();
 		for(final String kv:att.getValue().split(";") ){
 			if(StringUtils.isBlank(kv)) continue;
 			int colon = kv.indexOf(kv);
 			if(colon==-1) continue;
 			final String key = kv.substring(0,colon).trim();
 			if(StringUtils.isBlank(key)) continue;
 			if(map.containsKey(key)) LOG.warning("duplicated key "+key+" in "+att.getValue());
 			String v = kv.substring(colon+1).trim();
 			map.put(key, v);
 			}
 		return map;
 		}
 	
 	boolean isBold() {
 		for(Context c: getAncestors()) {
 			if(c.root.getNodeName().equals("bold")) return true;
 			final Map<String,String> m=c.getStyle();
 			final String v=m.getOrDefault("bold", "");
 			if(v.equals("true")) return true;
 			if(v.equals("false")) return false;
 			}
 		return false;
 		}
 	boolean isItalic() {
	 	for(Context c: getAncestors()) {
			if(c.root.getNodeName().equals("i")) return true;
			final Map<String,String> m=c.getStyle();
			final String v=m.getOrDefault("italic", "");
			if(v.equals("true")) return true;
			if(v.equals("false")) return false;
			}
		return false;
		}

	
	}
	
	private int recursive(final Context ctx)  {
			if(ctx.root.getNodeType()==Node.ELEMENT_NODE) {
				final Element E = Element.class.cast(ctx.root);
				final String localName = E.getLocalName();
				if(localName.equals(TAG_SLIDESHOW)) {
					}
				else if(localName.equals(TAG_SLIDE)) {
					if(ctx.slide!=null) {
						LOG.error("nested slide");
						return -1;
						}
					if(E.hasAttribute(ATT_LAYOUT)) {
						final XSLFSlideMaster slideMaster = ctx.ppt.getSlideMasters().get(0);
						SlideLayout slideLayout;
						try {
							slideLayout=SlideLayout.valueOf(E.getAttribute(ATT_LAYOUT));
							} 
						catch(final Throwable err)
							{
							LOG.error(err);
							slideLayout=null;
							}
						if(slideLayout==null) {
							LOG.error("Bad "+ATT_LAYOUT+". AVailable Layout are:"+Arrays.stream(SlideLayout.values()).map(T->T.name()).collect(Collectors.joining(",")));
							return -1;
							}
						ctx.xslfSlideLayout =slideMaster.getLayout(slideLayout);
						if(ctx.xslfSlideLayout==null) {
							LOG.error("Master is missing layout "+ slideLayout.name());
							return -1;
							}
						ctx.slide = ctx.ppt.createSlide(ctx.xslfSlideLayout);
						}
					else
						{
						ctx.slide = ctx.ppt.createSlide();
						}
					}
				else if(localName.equals(TAG_PLACEHOLDER)) {
					if(ctx.slide==null) throw new IllegalArgumentException("not in "+TAG_SLIDE);
					if(ctx.xslfTextShape!=null) throw new IllegalArgumentException("nested "+TAG_PLACEHOLDER);
					int index = Integer.parseInt(XmlUtils.attribute(ctx.root,ATT_INDEX).orElseThrow(()->new IllegalArgumentException(ATT_INDEX)));
					ctx.xslfTextShape = ctx.slide.getPlaceholder(index);
					
					}
				else if(localName.equals(TAG_A)) {
					ctx.xslfHyperlink = ctx.xslfTextRun.createHyperlink();
					ctx.xslfHyperlink.setLabel(E.getAttribute("title"));
					ctx.xslfHyperlink.setAddress(E.getAttribute("href"));
					}
				else if(localName.equals(TAG_TEXT)) {
				
					}
				for(Node c=ctx.root;c!=null;c=c.getNextSibling()) {
					final Context ctx2 = new Context(ctx);			
					ctx2.root = c;
					if(recursive(ctx2)!=0) return -1;
					}
				}
			else if(ctx.root.getNodeType()==Node.TEXT_NODE) {
				final String content = Text.class.cast(ctx.root).getData();
				if(ctx.xslfHyperlink!=null) {
					ctx.xslfHyperlink.setAddress(content);
					}
				if(ctx.xslfTextRun==null) throw new IllegalArgumentException("not in text");
				ctx.xslfTextRun.setBold(ctx.isBold());
				ctx.xslfTextRun.setItalic(ctx.isItalic());
				ctx.xslfTextRun.setText(content);
				}
		return 0;	
		}
	
	
	@Override
	public int doWork(final List<String> args) {
		try {
			
			String input  = oneFileOrNull(args);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setCoalescing(true);
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			final Document dom;
			if(input==null) {
				dom = db.parse(System.in);
				}
			else
				{
				dom = db.parse(new File(input));
				}
			final Element root = dom.getDocumentElement();
			if(root==null || !root.getLocalName().equals(TAG_SLIDESHOW)) {
				LOG.error("No root element is not "+TAG_SLIDESHOW);
				return -1;
				}
			final Context ctx = new Context();
			ctx.ppt = new XMLSlideShow();
			ctx.root = root;
			
			if(recursive(ctx)!=0) {
				return -1;
				}
			
			try(OutputStream out = this.outPath==null?System.out:Files.newOutputStream(outPath)) {
				ctx.ppt.write(out);
				out.flush();
				}
			return 0;
			} 
		catch(final Throwable err) {
			err.printStackTrace();
			return -1;
			}
		}
	
	public static void main(String[] args)
		{
		new XmlToPPT().instanceMainWithExit(args);
		}

	}
