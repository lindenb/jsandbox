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
package sandbox.tools.json2xml;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import sandbox.Launcher;
import sandbox.annotation.IncludeUrl;
import sandbox.io.IOUtils;

@IncludeUrl(url="http://en.wikipedia.org http://fr.wikipedia.org",directory="XXX")
public final class Json2Xml extends Launcher {
	private static final String NS="http://www.ibm.com/xmlns/prod/2009/jsonx";
	public static Logger LOG=Logger.getLogger("json2xml");
	
	private Json2Xml() {}
	
	private void parseObject(final XMLStreamWriter w,final String label,final JsonReader r) throws Exception
		{
		
		w.writeStartElement(NS, "object");
		if(label!=null) w.writeAttribute("name", label);
		for(;;)
			{
			if(r.peek()==JsonToken.END_OBJECT) 
				{
				w.writeEndElement();
				r.endObject();		
				break;
				}
			if(r.peek()!=JsonToken.NAME) throw new IllegalStateException(r.peek().name());
			final String s = r.nextName();
			parse(w,s,r);
			}
		}
	private void parseArray(final XMLStreamWriter w,final String label,final JsonReader r) throws Exception
		{
		
		w.writeStartElement(NS, "array");
		if(label!=null) w.writeAttribute("name", label);
		for(;;)
			{
			if(r.peek()==JsonToken.END_ARRAY) 
				{
				w.writeEndElement();
				r.endArray();		
				break;
				}
			parse(w,null,r);
			}
		}
	
	
	private void parse(final XMLStreamWriter w,final String label,final JsonReader r) throws Exception
		{
		if(!r.hasNext()) return;
		    JsonToken token= r.peek();
		    switch(token)
		    	{
		    	case END_OBJECT://through
		    	case END_ARRAY://through
		    	case NAME: throw new IllegalStateException("unexpected "+ token);
		    	case BEGIN_OBJECT:
		    		{
		    		r.beginObject();
		    		parseObject(w,label,r);	
		    		break;
		    		}
		    	case BEGIN_ARRAY:
		    		{
		    		r.beginArray();
		    		parseArray(w,label,r);
		    		break;
		    		}
		    	case NULL:
		    		{
		    		r.nextNull();
		    		w.writeEmptyElement(NS, "null");
		    		if(label!=null) w.writeAttribute("name", label);
		    		break;
		    		}
		    	case STRING:
		    		{
		    		w.writeStartElement(NS, "string");
		    		if(label!=null) w.writeAttribute("name", label);
		    		w.writeCharacters(r.nextString());
		    		w.writeEndElement();
		    		break;
		    		}
		    	case NUMBER:
		    		{
		    		w.writeStartElement(NS, "number");
		    		if(label!=null) w.writeAttribute("name", label);
		    		String s;
		    		try
		    			{
		    			s= String.valueOf(r.nextLong());
		    			}
		    		catch(Exception err)
		    			{
		    			s= String.valueOf(r.nextDouble());
		    			}

		    		
		    		w.writeCharacters(s);
		    		w.writeEndElement();
		    		break;
		    		}
		    	case BOOLEAN:
		    		{
		    		w.writeStartElement(NS, "boolean");
		    		if(label!=null) w.writeAttribute("name", label);
		    		w.writeCharacters(String.valueOf(r.nextBoolean()));
		    		w.writeEndElement();
		    		break;
		    		}
		    	case END_DOCUMENT:
		    		{
		    		break;
		    		}
		    	default: throw new IllegalStateException(token.name());
		    	}
			
		}
	
	@Override
	public int doWork(List<String> args)
		{
		try {
			Reader r = null;
			JsonReader jr=null;
			final String input = super.oneFileOrNull(args);
			if(input==null)
				{
				r = new InputStreamReader(System.in);
				}
			else
				{
				r  = IOUtils.openBufferedReader(input);
				}		
			
			
			
			jr = new JsonReader(r);
			jr.setLenient(true);
			XMLOutputFactory xof = XMLOutputFactory.newFactory();
			xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
			XMLStreamWriter w = xof.createXMLStreamWriter(System.out,"UTF-8");
			w.setDefaultNamespace(NS);
			w.writeStartDocument("UTF-8", "1.0");
			
			this.parse(w,null,jr);
			w.writeEndDocument();
			w.flush();
			w.close();
			IOUtils.close(jr);
			return 0;
		} catch (Throwable err) {
			err.printStackTrace();
			return -1;
		}

	}
	public static void main(String[] args)
		{
		new Json2Xml().instanceMainWithExit(args);
		}
}
