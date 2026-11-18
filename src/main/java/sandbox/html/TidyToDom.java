/*
The MIT License (MIT)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/
package sandbox.html;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.tidy.Tidy;

import sandbox.Logger;
import sandbox.io.NullWriter;


public class TidyToDom {
	private static final Logger LOG = Logger.builder(TidyToDom.class).build();
	private final Tidy tidy;
	
	public TidyToDom() {
		this.tidy = new Tidy();
		this.tidy.setXmlOut(true);
		this.tidy.setErrout( new PrintWriter(new NullWriter()));
		this.tidy.setShowErrors(0);
		this.tidy.setShowWarnings(false);
		}
	public DocumentFragment importString(final String s) {
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			return importString(s,db.newDocument());
			}
		catch(final Throwable err) {
			LOG.error(err);
			return null;
			}
		}
	public DocumentFragment importString(final String s, final Document owner)
		{
		final DocumentFragment df=owner.createDocumentFragment();
		if(s==null) return df;
		try(StringReader sr=new StringReader(s)) {
			this.tidy.setPrintBodyOnly(true);
			final Document tmpDom = tidy.parseDOM(sr, null);
			final Element root = tmpDom==null?null:tmpDom.getDocumentElement();
			Node html = null;
			for(Node c=(root==null?null:root.getFirstChild());
				c!=null;
				c=c.getNextSibling())
				{
				if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
				if(c.getNodeName().equalsIgnoreCase("body")) {
					html = c;
					break;
					}
				}
			for(Node c=html==null?null:html.getFirstChild();
					c!=null;
					c=c.getNextSibling())
				{
				final Node c2 = importNode(c,owner);
				if(c2!=null) df.appendChild(c2);
				}
			}
		catch(final Throwable err) {
			LOG.info(err);
			}
		return df;
		}
	
	
	public Document read(final Reader r) throws IOException {
		return this.tidy.parseDOM(r, null);
		}

	public Document read(final InputStream in) throws IOException {
		try(Reader r=new InputStreamReader(in)){
			return read(r);
			}
		}
	
	private Node importNode(final Node n,final Document owner)
		{
		if(n==null) return null;
		switch(n.getNodeType())
			{
			case Node.COMMENT_NODE:
				{
				final String text = Comment.class.cast(n).getData();
				if(text!=null) return owner.createComment(text);
				return null;
				}
			case Node.TEXT_NODE:
				{
				final String text = Text.class.cast(n).getData();
				if(text!=null) return owner.createTextNode(text);
				return null;
				}
			case Node.CDATA_SECTION_NODE:
				{
				final String text = CDATASection.class.cast(n).getData();
				if(text!=null) return owner.createTextNode(text);
				return null;
				}
			case Node.ELEMENT_NODE:
				{
				final Element e= Element.class.cast(n);
				final NamedNodeMap atts = e.getAttributes();
				final Element r = owner.createElement(e.getNodeName());
				for(int i=0;i< atts.getLength();++i)
					{
					final Attr att=(Attr)atts.item(i);
					r.setAttribute(att.getNodeName(),att.getValue());
					}
				for(Node c=e.getFirstChild();
						c!=null;
						c=c.getNextSibling())
					{
					final Node x = importNode(c,owner); 
					if(x==null ) continue;
					r.appendChild(x);
					}
				return r;
				}
			case Node.DOCUMENT_FRAGMENT_NODE:
				{
				final DocumentFragment r = owner.createDocumentFragment();
				for(Node c=n.getFirstChild();
						c!=null;
						c=c.getNextSibling())
					{
					final Node x = importNode(c,owner); 
					if(x==null ) continue;
					r.appendChild(x);
					}
				return r;
				}
			default: LOG.warning(">>>>"+n.getNodeType()+ " "+n); return null;
			}
		}
	}
