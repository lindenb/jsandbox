package sandbox;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

import com.beust.jcommander.Parameter;

public class OsmToSvg extends Launcher {
	private static final Logger LOG = Logger.builder(OsmToSvg.class).build();
	private final Map<String, Node> id2node = new HashMap<>();
	private final List<Way> ways = new ArrayList<>();
	
	@Parameter(names={"-o","--out"},description="output file")
	private File output=null;
	@Parameter(names={"-w","--width"},description="output file")
	private double width=1000;

	
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
	private static class WithTag
		{
		final String id;
		final Map<String,String> tags = new HashMap<>();
		WithTag(final String id) {
			this.id =id;
		}
		
		@Override
		public int hashCode() {
			return id.hashCode();
			}
		
		void writeTitle(final XMLStreamWriter w) throws XMLStreamException {
			final String title;
			if(this.tags.containsKey("name")) {
				title = this.tags.get("name");
				}
			else if(this.tags.containsKey("title")) {
				title = this.tags.get("title");
				}
			else
				{
				title = String.valueOf(this.id);
				}
			w.writeStartElement("title");
			w.writeCharacters(title);
			w.writeEndElement();
			}
		}
	
	private static class Node extends WithTag
		{
		final LatLon coord = new LatLon();
		
		boolean in_way = false;
		Node(final String id) {
			super(id);
			}
		
		@Override
		public boolean equals(Object obj) {
			if(obj==this) return true;
			if(obj==null || !(obj instanceof Node )) return false;
			return id.equals(Node.class.cast(obj).id);
			}
		}
	
	private static class Way extends WithTag
		{
		final List<Node> nodes = new ArrayList<>();
		
		Way(final String id) {
			super(id);
			}
		
		@Override
		public boolean equals(Object obj) {
			if(obj==this) return true;
			if(obj==null || !(obj instanceof Way )) return false;
			return id.equals(Node.class.cast(obj).id);
			}
		public double getPerimeter() {
			double t=0;
			if(nodes.size()>1) {
			for(int i=0;i< nodes.size();i++) {
				final Node n1 = nodes.get(i);
				final Node n2 = nodes.get(i+1==nodes.size()?0:i+1);
				final double dx = n1.coord.longitude-n2.coord.longitude;
				final double dy = n1.coord.latitude-n2.coord.latitude;
				t+= Math.sqrt(Math.pow(dx, 2)+Math.pow(dy, 2));
				}
			}
			if(t==0) LOG.error("??"+id);
			return t;
			}
		@SuppressWarnings("unused")
		public LatLon getLatLonCenter() {
			LatLon ll=new LatLon();
			ll.longitude = nodes.stream().mapToDouble(N->N.coord.longitude).average().orElse(0);
			ll.latitude = nodes.stream().mapToDouble(N->N.coord.latitude).average().orElse(0);
			return ll;
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
		
		while(r.hasNext())
			{
			final XMLEvent evt = r.nextEvent();
			if(evt.isStartElement())
				{
				final StartElement E = evt.asStartElement();
				final String name = E.getName().getLocalPart();
				if(name.equals("tag")) {
					String k = E.getAttributeByName(K_ATT).getValue();
					String v = E.getAttributeByName(V_ATT).getValue();
					node.tags.put(k, v);
					}
				}
			else if(evt.isEndElement())
				{
				final String name = evt.asEndElement().getName().getLocalPart();
				if(name.equals("node")) break;
				}
			}
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
					n.in_way = true;
					way.nodes.add(n);
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
	OutputStream outs = System.out;
	Reader r = null;
	try {
		final XMLInputFactory xif = XMLInputFactory.newFactory();
		
		String input=oneFileOrNull(args);
		r = input==null?
			new InputStreamReader(System.in, "UTF-8"):
			IOUtils.openReader(input);
		final LatLon bounds_min= new LatLon();
		final LatLon bounds_max= new LatLon();
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
				else if(name.equals("bounds")){
					bounds_max.longitude = Double.parseDouble(E.getAttributeByName(new QName("maxlon")).getValue());
					bounds_min.longitude = Double.parseDouble(E.getAttributeByName(new QName("minlon")).getValue());
					bounds_max.latitude = Double.parseDouble(E.getAttributeByName(new QName("maxlat")).getValue());
					bounds_min.latitude = Double.parseDouble(E.getAttributeByName(new QName("minlat")).getValue());
					}
				}
			}
		in.close();
		if(this.ways.isEmpty() ) {
			LOG.error("no point");
			return -1;
		}
		
		
		
		final double scalex = this.width/(bounds_max.longitude-bounds_min.longitude);
		final double height= scalex * (bounds_max.latitude-bounds_min.latitude);
		final double scaley = height/(bounds_max.latitude-bounds_min.latitude);
		
		final Function<LatLon,Point> coord2pt = C-> {
			final Point pt = new Point();
			pt.x = (C.longitude - bounds_min.longitude)* scalex;
			pt.y = height - (C.latitude - bounds_min.latitude)* scaley;
			return pt;
			};
		final Point view_min = coord2pt.apply(bounds_min);
		final Point view_max = coord2pt.apply(bounds_max);
			
		XMLOutputFactory xof=XMLOutputFactory.newFactory();
		XMLStreamWriter w;
		
		outs=IOUtils.openFileAsOutputStream(this.output);
		w=xof.createXMLStreamWriter(outs, "UTF-8");
		w.writeStartDocument("UTF-8", "1.0");
		w.writeStartElement("svg");
		w.writeDefaultNamespace("http://www.w3.org/2000/svg");
		w.writeAttribute("viewBox",
				String.valueOf(view_min.x)+" "+String.valueOf(view_min.y)+" "+
					String.valueOf(view_max.x-view_min.x)+" "+String.valueOf(view_max.y-view_min.y)
				);
		w.writeAttribute("width",String.valueOf(width));
		w.writeAttribute("height",String.valueOf(height));
		
		this.ways.sort((A,B)->Double.compare(B.getPerimeter(),A.getPerimeter()));
		
		w.writeStartElement("style");
		w.writeCharacters(
				 ".nodealone  {fill:none;stroke-width:0.2;fill-opacity:0.5; stroke:red;}"
				+ "polyline {fill:gray;fill-opacity:0.5; stroke:darkgray;}"
				+ ".water {fill:blue;}"
				+ ".route {fill:brown;}"
				+ ".building {fill.yellow;}"
				+ ".grass {fill:green;}");
		w.writeEndElement();
		
		w.writeStartElement("g");
		for(final Way way: this.ways)
			{
			if(way.nodes.isEmpty()) continue;
			w.writeStartElement("polyline");
			
			if(way.tags.containsKey("water") || way.tags.containsKey("waterway")) {
				w.writeAttribute("class","water");
				}
			else if(way.tags.containsKey("route") || way.tags.containsKey("highway")) {
				w.writeAttribute("class","route");
				}
			else if((way.tags.containsKey("type") &&  way.tags.get("type").equals("building")) || way.tags.values().stream().anyMatch(S->S.equals("building"))) {
				w.writeAttribute("class","building");
				}
			else if(way.tags.values().stream().anyMatch(S->S.equals("park")) ||
					way.tags.values().stream().anyMatch(S->S.equals("garden"))) {
				w.writeAttribute("class","grass");
				}
			
			if(!way.nodes.get(0).equals(way.nodes.get(way.nodes.size()-1)))
				{
				w.writeAttribute("style","fill:none;");
				}
			
			w.writeAttribute("points",
					way.nodes.stream().map(P->coord2pt.apply(P.coord)).map(n->String.valueOf(n.x)+","+(n.y)).
					collect(Collectors.joining(" "))
					);
			way.writeTitle(w);
			w.writeEndElement();
			}
		w.writeStartElement("g");
		for(final Node n:this.id2node.values()) {
			if(n.in_way) continue;
			final Point pt=coord2pt.apply(n.coord);
			w.writeStartElement("circle");
			w.writeAttribute("class","nodealone");
			w.writeAttribute("cx",String.valueOf(pt.x));
			w.writeAttribute("cy",String.valueOf(pt.y));
			w.writeAttribute("r","1");
			n.writeTitle(w);
			w.writeEndElement();
		}
		w.writeEndElement();//g
		
		w.writeEndElement();//g
	
		
		w.writeEndElement();
		w.writeEndDocument();
		w.flush();
		w.close();
		outs.close();
		outs=null;
		return 0;
		}
	catch(final Throwable err) {
		LOG.error(err);
		return -1;
		}
	finally
		{
		IOUtils.close(outs);
		IOUtils.close(r);
		}
	}	
	
public static void main(final String[] args) {
	new OsmToSvg().instanceMainWithExit(args);
	}
}
