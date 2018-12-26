package sandbox;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class OsmToSvg extends Launcher {
	private static final Logger LOG = Logger.builder(OsmToSvg.class).build();
	private final Map<String, Node> id2node = new HashMap<>();
	private final List<Way> ways = new ArrayList<>();
	
	private static class LatLon
		{
		double latitude=0;
		double longitude=0;
		}
	private static class Point
		{
		double x=0;
		double y=0;
		}
	
	private static class Node
		{
		final LatLon coord = new LatLon();
		final String id;
		Node(final String id) {
			this.id = id;
			}
		@Override
		public int hashCode() {
			return id.hashCode();
			}
		@Override
		public boolean equals(Object obj) {
			if(obj==this) return true;
			if(obj==null || !(obj instanceof Node )) return false;
			return id.equals(Node.class.cast(obj).id);
			}
		}
	
	private static class Way
		{
		final String id;
		final List<Node> nodes = new ArrayList<>();
		final Map<String,String> tags = new HashMap<>();
		Way(final String id) {
			this.id = id;
			}
		@Override
		public int hashCode() {
			return id.hashCode();
			}
		@Override
		public boolean equals(Object obj) {
			if(obj==this) return true;
			if(obj==null || !(obj instanceof Way )) return false;
			return id.equals(Node.class.cast(obj).id);
			}
		}
	private final QName ID_ATT = new QName("id");
	private final QName REF_ATT = new QName("ref");
	private final QName K_ATT = new QName("k");
	private final QName V_ATT = new QName("v");
	private final QName LAT_ATT = new QName("lat");
	private final QName LON_ATT = new QName("lon");
	
	private void parseNode(final XMLEventReader r,final StartElement root) throws XMLStreamException
		{
		Attribute att = root.getAttributeByName(ID_ATT);
		final Node node = new Node(att.getValue());
		
		node.coord.latitude = Double.parseDouble(root.getAttributeByName(LAT_ATT).getValue());
		node.coord.longitude = Double.parseDouble(root.getAttributeByName(LON_ATT).getValue());
		
		this.id2node.put(node.id,node);
		}
	private void parseWay(final XMLEventReader r,final StartElement root) throws XMLStreamException
		{
		Attribute att = root.getAttributeByName(ID_ATT);
		final Way way = new Way(att.getValue());
		this.ways.add(way);
		while(r.hasNext())
			{
			final XMLEvent evt = r.nextEvent();
			if(evt.isStartElement())
				{
				final StartElement E = evt.asStartElement();
				final String name = E.getName().getLocalPart();
				if(name.equals("nd")) {
					att = E.getAttributeByName(REF_ATT);
					if(att==null) throw new XMLStreamException("no @ref", E.getLocation());
					final Node n = id2node.get(att.getValue());
					if(n==null) throw new XMLStreamException("no node with id "+att.getValue(), E.getLocation());
					way.nodes.add(id2node.get(att.getValue()));
					}
				else if(name.equals("tag")) {
					String k = E.getAttributeByName(K_ATT).getValue();
					String v = E.getAttributeByName(V_ATT).getValue();
					way.tags.put(k, v);
					}
				}
			else if(evt.isEndElement())
				{
				final String name = evt.asEndElement().getName().getLocalPart();
				if(name.equals("way")) break;
				}
			}
		}
	
	
@Override
public int doWork(final List<String> args) {
	Reader r = null;
	try {
		final XMLInputFactory xif = XMLInputFactory.newFactory();
		
		String input=oneFileOrNull(args);
		r = input==null?
			new InputStreamReader(System.in, "UTF-8"):
			IOUtils.openReader(input);
		
		XMLEventReader in=	xif.createXMLEventReader(r);
		while(in.hasNext())
			{
			final XMLEvent evt = in.nextEvent();
			if(evt.isStartElement())
				{
				final StartElement E = evt.asStartElement();
				final String name = E.getName().getLocalPart();
				if(name.equals("node")){
					parseNode(in,E);
					}
				else if(name.equals("way")){
					parseWay(in,E);
					}
				}
			}
		in.close();
		if(this.ways.isEmpty()) {
			LOG.error("no point");
			return -1;
		}
		
		
		
		final  double min_lat = this.id2node.values().stream().mapToDouble(N->N.coord.latitude).min().orElse(0);
		final  double max_lat = this.id2node.values().stream().mapToDouble(N->N.coord.latitude).max().orElse(0);
		final  double min_lon = this.id2node.values().stream().mapToDouble(N->N.coord.longitude).min().orElse(0);
		final  double max_lon = this.id2node.values().stream().mapToDouble(N->N.coord.longitude).max().orElse(0);
		final double width=1000;
		final double height=1000;
		final double scalex = width/(max_lon-min_lon);
		final double scaley = height/(max_lat-min_lat);
		
		Function<LatLon,Point> coord2pt = C-> {
			final Point pt = new Point();
			pt.x = (C.longitude - min_lon)* scalex;
			pt.y = height - (C.latitude - min_lat)* scaley;
			return pt;
			};
		
		XMLOutputFactory xof=XMLOutputFactory.newFactory();
		XMLStreamWriter w=xof.createXMLStreamWriter(System.out, "UTF-8");
		w.writeStartDocument("UTF-8", "1.0");
		w.writeStartElement("svg");
		w.writeDefaultNamespace("http://www.w3.org/2000/svg");
		w.writeAttribute("width",String.valueOf(width));
		w.writeAttribute("height",String.valueOf(height));
		for(final Way way: this.ways)
			{
			w.writeStartElement("polyline");
			w.writeAttribute("style", "fill:none;stroke:black;");
			
			w.writeAttribute("points",
					way.nodes.stream().map(P->coord2pt.apply(P.coord)).map(n->String.valueOf(n.x)+","+(n.y)).
					collect(Collectors.joining(" "))
					);
			w.writeStartElement("title");
			w.writeCharacters(way.tags.getOrDefault("name", ""));
			w.writeEndElement();
			w.writeEndElement();
			}
	
		
		w.writeEndElement();
		w.writeEndDocument();
		w.flush();
		return 0;
		}
	catch(Throwable err) {
		LOG.error(err);
		return -1;
		}
	finally
		{
		IOUtils.close(r);
		}
	}	
	
public static void main(final String[] args) {
	new OsmToSvg().instanceMainWithExit(args);
	}
}
