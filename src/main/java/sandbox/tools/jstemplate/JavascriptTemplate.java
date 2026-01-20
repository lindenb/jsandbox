package sandbox.tools.jstemplate;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.lang.StringUtils;

public class JavascriptTemplate  extends Launcher {
	private static final Logger LOG = Logger.builder(JavascriptTemplate.class).build();
private String namespaceuri = "http://github.com/lindenb/jstemplate";
private class Context {
	Context parent=null;
	int id_generator = 0;
	private String _root_id = null;
	private StringWriter sw ;
	private PrintWriter pw ;
	void append(String o) {
		pw.append(o);
		}
	void println(String o) {
		pw.println(o);
		}
	int nextId() {
		if(parent!=null) return parent.nextId();
		++id_generator;
		return id_generator;
		}
	String getRootId() {
		if(_root_id==null) {
			_root_id = "_n"+nextId();
			}
		return _root_id;
		}
	
	Context(Context parent) {
		this.parent=parent;
		this.sw = new StringWriter();
		this.pw=new PrintWriter(this.sw);
		}
	Context() {
		this(null);
		}
	@Override
	public String toString() {
		this.pw.flush();
		return sw.toString();
		}
	}

private Element asElement(final Node c) {
	return Element.class.cast(c);
	}

private boolean isElement(final Node c) {
	return c.getNodeType()==Node.ELEMENT_NODE;
	}

private boolean isText(final Node c) {
	return c.getNodeType()==Node.TEXT_NODE;
	}
private Text asText(final Node c) {
	return Text.class.cast(c);
	}

private boolean isBlank(final Node c) {
	return isText(c) && StringUtils.isBlank(asText(c).getTextContent());
	}


private boolean hasNamespace(final Node c,final String ns) {
	if(!isElement(c)) return false;
	return ns.equals(asElement(c).getNamespaceURI());
	}

private String quote(final String s) {
	if(s.startsWith("${") && s.endsWith("}")) {
		return s.substring(2, s.length()-1);
		}
	else
		{
		return "\""+ StringUtils.escapeC(s)+"\"";
		}
}

private void parse(final Context ctx,final Document dom) {
	ctx.append("return id;");
	ctx.append("}");
	}

private void handleMain(final Context ctx,Node c) {
	while(c!=null) {
		if(hasNamespace(c,this.namespaceuri)) {
			final String lcl = asElement(c).getLocalName();
			if(lcl.equals("for-each")) handleForEach(ctx, asElement(c));
			else if(lcl.equals("if")) handleIf(ctx, asElement(c));
			else if(lcl.equals("choose")) handleChoose(ctx, asElement(c));
			else if(lcl.equals("element")) handleElement(ctx, asElement(c));
			else if(lcl.equals("value-of")) handleValueOf(ctx, asElement(c));
			else {
				throw new IllegalStateException("undefined tag "+namespaceuri+":"+lcl);
				}
			}
		else if(isElement(c)) {
			handleXmlElement(ctx,asElement(c));
			}
		else if(c.getNodeType()==Node.TEXT_NODE) {
			
			}
		else if(c.getNodeType()==Node.COMMENT_NODE) {
			
			}
		else
			{
			throw new IllegalStateException("undefined node type "+c.getNodeType());
			}
		c=c.getNextSibling();
	}
}

private void handleTemplate(final Context ctx0,final Element root) {
	final Context ctx=new Context(ctx0);
	String name = StringUtils.ifBlank(root.getAttribute("name"),"undefined");
	final List<String> params = new ArrayList<>();
	params.add("doc");
	Node c = root.getFirstChild();
	while(c!=null) {
		if(isElement(c) && asElement(c).getNodeName().equals("param")) {
			params.add(asElement(c).getAttribute("name"));
			}
		else if(isBlank(c)) {
			
			}
		else
			{
			break;
			}
		c=c.getNextSibling();
		}
	ctx.println(name+" : function("+ String.join(",",params)+") {");
	final String frag = ctx.getRootId();
	ctx.println("var "+frag+" = doc.createDocumentFragment();");

	handleMain(ctx,c);
	ctx.println("return "+frag+";");
	ctx.println("}");
	ctx0.pw.append(ctx.toString());
	}
private void handleIf(final Context ctx,final Element root) {
	
	}

private void handleChoose(final Context ctx,final Element root) {
	}

private void handleWhen(final Context ctx,final Element root) {
	}

private void handleOtherwise(final Context ctx,final Element root) {
	}


private void handleText(final Context ctx,final Element root) {
	
	}

private void handleValueOf(final Context ctx,final Element root) {
	
	}

private void handleElement(final Context ctx0,final Element root) {
	final Context ctx = new Context(ctx0);
	}

private void handleXmlElement(final Context ctx0,final Element root) {
	final Context ctx=new Context(ctx0);
	final String eid = ctx.getRootId();
	ctx.println("var "+eid+" = document.createElement("+quote(root.getNodeName())+");");
	if(root.hasAttributes()) {
		final NamedNodeMap nm=root.getAttributes();
		for(int i=0;i< nm.getLength();i++) {
			final Attr attr = Attr.class.cast(nm.item(i));
			ctx.println(eid+".setAttribute(" +quote(attr.getName())+","+quote(attr.getValue())+");");
			}
		}
	handleMain(ctx, root.getFirstChild());
	ctx.println(ctx0.getRootId()+".appendChild("+eid+");");
	ctx0.pw.append(ctx.toString());
	}

private void handleForEach(final Context ctx,final Element root) {
	final String name=root.getAttribute("name");
	final String in =root.getAttribute("select");
	ctx.println("for("+name+" in "+in+") {");
	handleMain(ctx, root.getFirstChild());
	ctx.println("}");
	}

@Override
public int doWork(final List<String> args) {
	try {
		File f1 = new File(super.oneAndOnlyOneFile(args));
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db= dbf.newDocumentBuilder();
		Document dom = db.parse(f1);
		Element root=dom.getDocumentElement();
		if(!hasNamespace(root, this.namespaceuri)) {
			LOG.error("bad root ns uri. Expected "+this.namespaceuri+" got "+root.getNamespaceURI());
			return -1;
			}
		if(!root.getLocalName().equals("templates")) {
			LOG.error("bad root name. Expected <templates> got "+root.getLocalName());
			return -1;
			}
		String name= StringUtils.ifBlank(root.getAttribute("name"),"name");
		Context ctx = new Context();
		ctx.append("var "+name+" = {");
		for(Node c=root.getFirstChild();c!=null;c=c.getNextSibling()) {
			if(isElement(c)) {
				if(hasNamespace(c, this.namespaceuri) && asElement(c).getLocalName().equals("template")) {
					handleTemplate(ctx, asElement(c));
					}
				else
					{
					LOG.error("Undefined element "+c.getNodeName());
					return -1;
					}
				}
			else if(isBlank(c)) {
				continue;
				}
			else if(c.getNodeType()==Node.COMMENT_NODE) {
				continue;
				}
			else
				{
				LOG.error("Undefined node type "+c.getNodeType());
				return -1;
				}
			}
		ctx.append("}");
		System.out.println(ctx.toString());
		return 0;
		}
	catch(final Throwable err ) {
		LOG.error(err);
		return -1;
		}
	}


public static void main(String[] args) {
	new JavascriptTemplate().instanceMainWithExit(args);
	}
}
