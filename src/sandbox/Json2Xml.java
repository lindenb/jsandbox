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
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Logger;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import sandbox.io.IOUtils;

public final class Json2Xml {
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
	
	public static void main(String[] args) {
		try {
			Json2Xml app = new Json2Xml();
			Reader r = null;
			JsonReader jr=null;
			if(args.length==0)
				{
				LOG.info("reading JSON from stdin");
				r = new InputStreamReader(System.in);
				}
			else if(args.length==1)
				{
				r  = new FileReader(new File(args[0]));
				}		
			else
				{
				System.err.println("Illegal Number of args");
				System.exit(-1);
				}
			
			
			jr = new JsonReader(r);
			jr.setLenient(true);
			XMLOutputFactory xof = XMLOutputFactory.newFactory();
			xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
			XMLStreamWriter w = xof.createXMLStreamWriter(System.out,"UTF-8");
			w.setDefaultNamespace(NS);
			w.writeStartDocument("UTF-8", "1.0");
			
			app.parse(w,null,jr);
			w.writeEndDocument();
			w.flush();
			w.close();
			IOUtils.close(jr);
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
