package sandbox;

import java.io.PrintStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class XMLSerializer
	{
	private boolean omit_xml_declaration = false;
	
	
	public XMLSerializer setOmitXmDeclaration(boolean omit_xml_declaration)
		{
		this.omit_xml_declaration = omit_xml_declaration;
		return this;
		}
	
	public XMLSerializer serialize(final Document dom,final PrintStream out) {
    	this.serialize(new DOMSource(dom), new StreamResult(out));
    	out.flush();
    	return this;
		}
	
	public XMLSerializer serialize(final Source source,final Result result) {
	    final TransformerFactory tFactory =  TransformerFactory.newInstance();
	    try
	    	{
		    final Transformer transformer =  tFactory.newTransformer();
		    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, this.omit_xml_declaration?"yes":"no");
		    transformer.transform(source, result);
	    	}
	    catch(final Exception err) {
	    	throw new RuntimeException(err);
	    	}
	    return this;
		}

	
	public XMLSerializer serialize(final Document dom)
		{
		return this.serialize(dom, System.out);
		}	
	}
