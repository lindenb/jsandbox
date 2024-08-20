/*
The MIT License (MIT)

Copyright (c) 2015 Pierre Lindenbaum

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
package sandbox;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public final class Json2Dom {
	private static final String NS = "http://www.ibm.com/xmlns/prod/2009/jsonx";
	public static Logger LOG = Logger.getLogger("json2dom");

	public Json2Dom() {
	}

	private void _jsonparseObject(final Document owner,final Node root, final String label, JsonReader r) throws Exception {
		r.beginObject();
		final Element E = owner.createElementNS(NS, "jsonx:object");
		root.appendChild(E);
		if (label != null)
			E.setAttribute("name", label);
		for (;;) {
			if (r.peek() == JsonToken.END_OBJECT)
				{
				r.endObject();
				break;
				}
			if (r.peek() != JsonToken.NAME)
				throw new IllegalStateException(r.peek().name());
			final String s = r.nextName();
			_jsonParse(owner,E, s, r);
		}
	}

	private void _jsonParseArray(final Document owner,final Node root, final String label, JsonReader r) throws Exception {
		r.beginArray();
		final Element E = owner.createElementNS(NS, "jsonx:array");
		root.appendChild(E);
		if (label != null)
			E.setAttribute("name", label);
		for (;;) {
			if (r.peek() == JsonToken.END_ARRAY)
				{
				r.endArray();
				break;
				}
			_jsonParse(owner,E, null, r);
		}
	}

	private void _jsonParse(final Document owner,final Node root, final String label, JsonReader r) throws Exception {
		if (!r.hasNext()) return;
		final JsonToken token = r.peek();
		
		switch (token) {
		case NAME:
			throw new IllegalStateException();
		case BEGIN_OBJECT: {
			_jsonparseObject(owner,root, label, r);
			break;
		}
		case END_OBJECT: {
			throw new IllegalStateException();
		}
		case BEGIN_ARRAY: {
			_jsonParseArray(owner,root, label, r);
			break;
		}
		case END_ARRAY: {
			throw new IllegalStateException();
		}
		case NULL: {
			r.nextNull();
			final Element E = owner.createElementNS(NS, "jsonx:null");
			if (label != null)
				E.setAttribute("name", label);
			root.appendChild(E);
			break;
		}
		case STRING: {
			final Element E = owner.createElementNS(NS, "jsonx:string");
			if (label != null)
				E.setAttribute("name", label);
			E.appendChild(owner.createTextNode(r.nextString()));
			root.appendChild(E);
			break;
		}
		case NUMBER: {
			final Element E = owner.createElementNS(NS, "jsonx:number");
			root.appendChild(E);
			if (label != null)
				E.setAttribute("name", label);
			String s;
			try {
				s = String.valueOf(r.nextLong());
			} catch (Exception err) {
				s = String.valueOf(r.nextDouble());
			}

			E.appendChild(owner.createTextNode(s));
			break;
		}
		case BOOLEAN: {
			final Element E = owner.createElementNS(NS, "jsonx:boolean");
			if (label != null)
				E.setAttribute("name", label);
			E.appendChild(owner.createTextNode(String.valueOf(r.nextBoolean())));
			root.appendChild(E);
			break;
		}
		case END_DOCUMENT: {
			break;
		}
		default:
			throw new IllegalStateException(token.name());
		}
	}
	
	private void _jsonParse(
			final Document owner,
			final Node root, 
			final String label,
			final JsonElement js
			) throws Exception {
			if(js.isJsonNull())
				{
				final Element E = owner.createElementNS(NS, "jsonx:null");
				if (label != null)
					E.setAttribute("name", label);
				root.appendChild(E);
				}
			else if(js.isJsonArray())
				{
				final Element E = owner.createElementNS(NS, "jsonx:array");
				root.appendChild(E);
				if (label != null) E.setAttribute("name", label);
				for(final JsonElement c:js.getAsJsonArray()) {
					_jsonParse(owner,E,null,c);
					}
				}
			else if(js.isJsonObject())
				{
				final Element E = owner.createElementNS(NS, "jsonx:object");
				root.appendChild(E);
				if (label != null) E.setAttribute("name", label);
				for(final Map.Entry<String,JsonElement> kv:js.getAsJsonObject().entrySet()) {
					_jsonParse(owner,E,kv.getKey(),kv.getValue());
					}
				}
			else if(js.isJsonPrimitive())
				{
				final JsonPrimitive sp = js.getAsJsonPrimitive();
				final Element E;
				if(sp.isNumber())
					{
					E=owner.createElementNS(NS, "jsonx:number");
					}
				else if(sp.isBoolean())
					{
					E=owner.createElementNS(NS, "jsonx:boolean");
					}
				else
					{
					E=owner.createElementNS(NS, "jsonx:string");
					}
				if (label != null) E.setAttribute("name", label);
				root.appendChild(E);
				}
			else
				{
				throw new IllegalStateException("unknown json node type.");
				}
			}

	
	
	public Document parse(final InputStream is) throws IOException
		{
		return parse(new InputStreamReader(is, "UTF-8"));
		}

	
	public Document parse(final Reader r) throws IOException
		{
		final JsonReader jr = new JsonReader(r);
		jr.setLenient(true);
		return parse(jr);
		}
	
	public Document parse(final JsonReader r) throws IOException
		{
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document dom = db.newDocument();
			this._jsonParse(dom, dom,null, r);
			return dom;
			}
		catch(final IOException err) {
			throw err;
			}
		catch(final Exception err) {
			throw new RuntimeException(err);
			}
		}
	public Document parse(final JsonElement root) {
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document dom = db.newDocument();
			this._jsonParse(dom, dom,null,root);
			return dom;
			}
		catch(final Exception err) {
			throw new RuntimeException(err);
			}
		}
	
	
	public static void main(final String[] args) {
		try {
			Json2Dom app = new Json2Dom();
			Reader r = null;
			if (args.length == 0) {
				LOG.info("reading JSON from stdin");
				r = new InputStreamReader(System.in);
			} else if (args.length == 1) {
				r = new FileReader(new File(args[0]));
			} else {
				System.err.println("Illegal Number of args");
				System.exit(-1);
			}

			final Document dom = app.parse(r);
			r.close();
			
			//echo
			final TransformerFactory trf = TransformerFactory.newInstance();
			final Transformer tr = trf.newTransformer();
			tr.setOutputProperty(OutputKeys.INDENT,"yes");
			tr.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
			tr.transform(new DOMSource(dom),
					new StreamResult(System.out)
					);
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
