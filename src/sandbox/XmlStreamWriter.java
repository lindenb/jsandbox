package sandbox;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


/**

Wrapper of javax.xml.stream.XMLStreamWriter that don't throw exceptions

*/
public interface XmlStreamWriter {
public static XmlStreamWriter wrap(final XMLStreamWriter delegate) {
	return new XmlStreamWriterImpl(delegate);
	}
public void writeStartElement(final String localName);
public void writeEmptyElement(final String localName);
public void writeAttribute(final String localName,final Object value);

public void writeEndElement();
public void writeStartDocument(final String encoding,final String version);
public void writeEndDocument();
public void writeCharacters(final Object text);
public void close();
public void flush();


static class XmlStreamWriterImpl implements XmlStreamWriter
	{
	final XMLStreamWriter delegate;
	private XmlStreamWriterImpl(final XMLStreamWriter delegate)  {
		this.delegate = delegate;
		}
	public void writeAttribute(final String localName,final Object value) {
		try {
			this.delegate.writeAttribute(localName,String.valueOf(value));
			}
		catch(final XMLStreamException err)
			{
			throw new RuntimeException(err);
			}
		}
	@Override
	public void writeCharacters(final Object text) {
		try {
			this.delegate.writeCharacters(String.valueOf(text));
			}
		catch(final XMLStreamException err)
			{
			throw new RuntimeException(err);
			}
		}
	
	@Override
	public void writeStartDocument(final String encoding,final String version) {
		try {
			this.delegate.writeStartDocument(encoding,version);
			}
		catch(final XMLStreamException err)
			{
			throw new RuntimeException(err);
			}
		}
	@Override
	public void writeEndDocument() {
		try {
			this.delegate.writeEndDocument();
			}
		catch(final XMLStreamException err)
			{
			throw new RuntimeException(err);
			}
		}

	
	@Override
	public void writeStartElement(final String localName) {
		try {
			this.delegate.writeStartElement(localName);
			}
		catch(final XMLStreamException err)
			{
			throw new RuntimeException(err);
			}
		}
	@Override
	public void writeEmptyElement(final String localName) {
		try {
			this.delegate.writeEmptyElement(localName);
			}
		catch(final XMLStreamException err)
			{
			throw new RuntimeException(err);
			}
		}
	
	@Override
	public void writeEndElement() {
		try {
			this.delegate.writeEndElement();
			}
		catch(final XMLStreamException err)
			{
			throw new RuntimeException(err);
			}
		}
	@Override
	public void close() {
		try {
			this.delegate.close();
			}
		catch(final XMLStreamException err)
			{
			throw new RuntimeException(err);
			}
		}
	@Override
	public void flush() {
		try {
			this.delegate.close();
			}
		catch(final XMLStreamException err)
			{
			throw new RuntimeException(err);
			}
		}
	}

}
