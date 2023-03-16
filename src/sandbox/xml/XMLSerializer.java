package sandbox.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import sandbox.io.IOUtils;

public class XMLSerializer
	{
	private boolean omit_xml_declaration = false;
	private String encoding = "UTF-8";
	
	public XMLSerializer setOmitXmDeclaration(boolean omit_xml_declaration)
		{
		this.omit_xml_declaration = omit_xml_declaration;
		return this;
		}
	public XMLSerializer setEncoding(String encoding) {
		this.encoding = encoding;
		return this;
		}
	
	public String getEncoding() {
		return encoding;
		}
	public XMLSerializer serialize(final Document dom,final Path out) throws IOException {
    	if(out==null) {
    		this.serialize(dom,System.out);
    		}
    	else
    		{
    		try(OutputStream os= IOUtils.openPathAsOutputStream(out)) {
    			this.serialize(dom,out);
    			os.flush();
    			}
    		}
    	return this;
		}
	
	public XMLSerializer serialize(final Document dom,final OutputStream out) {
    	this.serialize(new DOMSource(dom), new StreamResult(out));
    	return this;
		}
	
	public XMLSerializer serialize(final Source source,final Result result) {
	    final TransformerFactory tFactory =  TransformerFactory.newInstance();
	    try
	    	{
		    final Transformer transformer =  tFactory.newTransformer();
		    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, this.omit_xml_declaration?"yes":"no");
		    transformer.setOutputProperty(OutputKeys.ENCODING, getEncoding());
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
