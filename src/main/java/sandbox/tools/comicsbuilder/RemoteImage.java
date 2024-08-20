package sandbox.tools.comicsbuilder;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import sandbox.awt.HasDimension;

public class RemoteImage extends Node implements HasDimension{

	@Override
	public double getWidth() {
		return getAttributeAsDouble("width");
		}
	@Override
	public double getHeight() {
		return getAttributeAsDouble("height");
		}
	
	public String getSrc() {
		return getAttribute("src");
		}
	void save(XMLStreamWriter out) throws XMLStreamException {
		out.writeStartElement("image");
		out.writeAttribute("src",getSrc());
		out.writeAttribute("width",String.valueOf(getWidth()));
		out.writeAttribute("height",String.valueOf(getHeight()));
		
		out.writeEndElement();
		}
	
}
