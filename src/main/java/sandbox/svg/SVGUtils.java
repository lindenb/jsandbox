package sandbox.svg;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Scanner;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import sandbox.StringUtils;
import sandbox.awt.Dimension2D;
import sandbox.io.RuntimeIOException;
import sandbox.svg.path.SVGPathParser;

public class SVGUtils  extends SVG {



	static public AffineTransform svgToaffineTransform(String transform)
	{
	if(StringUtils.isBlank(transform)) return null;
	String s=transform.trim();
	
	if(s.startsWith("matrix("))
		{
		int i=s.indexOf(")");
		if(i==-1) throw new IllegalArgumentException(s);
		if(!StringUtils.isBlank(s.substring(i+1))) throw new IllegalArgumentException(s);
		String tokens[]=s.substring(7, i).split("[,]");
		if(tokens.length!=6) throw new IllegalArgumentException(s);
		return new AffineTransform(new double[]{
			Double.parseDouble(tokens[0]),
			Double.parseDouble(tokens[1]),
			Double.parseDouble(tokens[2]),
			Double.parseDouble(tokens[3]),
			Double.parseDouble(tokens[4]),
			Double.parseDouble(tokens[5])
			});
		}
	AffineTransform tr= new AffineTransform();
	while(s.length()!=0)
		{
	
		
		if(s.startsWith("scale("))
			{
			int i=s.indexOf(")");
			if(i==-1) throw new IllegalArgumentException(s);
			
			String s2= s.substring(6,i).trim();
			s= s.substring(i+1).trim();
			i= s2.indexOf(',');
			if(i==-1)
				{
				double scale= Double.parseDouble(s2.trim());
				
				AffineTransform tr2= AffineTransform.getScaleInstance(
						scale,scale
					);
				tr2.concatenate(tr);
				tr=tr2;
				}
			else
				{
				double scalex= Double.parseDouble(s2.substring(0,i).trim());
				double scaley= Double.parseDouble(s2.substring(i+1).trim());
				
				AffineTransform tr2= AffineTransform.getScaleInstance(
						scalex,scaley
					);
				tr2.concatenate(tr);
				tr=tr2;
				}
			}
		else if(s.startsWith("translate("))
			{
			int i=s.indexOf(")");
			if(i==-1) throw new IllegalArgumentException(s);
			String s2= s.substring(10,i).trim();
			s= s.substring(i+1).trim();
			i= s2.indexOf(',');
			if(i==-1)
				{
				double translate= Double.parseDouble(s2.trim());
				
				AffineTransform tr2= AffineTransform.getTranslateInstance(
						translate,0
					);
				tr2.concatenate(tr);
				tr=tr2;
				}
			else
				{
				double translatex= Double.parseDouble(s2.substring(0,i).trim());
				double translatey= Double.parseDouble(s2.substring(i+1).trim());
				
				AffineTransform tr2= AffineTransform.getTranslateInstance(
						translatex,translatey
					);
				tr2.concatenate(tr);
				tr=tr2;
				}
			}
		else if(s.startsWith("rotate("))
			{
			int i=s.indexOf(")");
			if(i==-1) throw new IllegalArgumentException(s);
			String s2= s.substring(7,i).trim();
			s= s.substring(i+1).trim();
			i= s2.indexOf(',');
			if(i==-1)
				{
				double angle= Double.parseDouble(s2.trim());
				
				AffineTransform tr2= AffineTransform.getRotateInstance((angle/180.0)*Math.PI);
				tr2.concatenate(tr);
				tr=tr2;
				}
			else
				{
				double angle= Double.parseDouble(s2.substring(0,i).trim());
				s2=s2.substring(i+1);
				i= s2.indexOf(',');
				if(i==-1) throw new IllegalArgumentException("bad rotation "+s);
				
				double cx= Double.parseDouble(s2.substring(0,i).trim());
				double cy= Double.parseDouble(s2.substring(i+1).trim());
				
				AffineTransform tr2= AffineTransform.getRotateInstance(
						angle,cx,cy
					);
				tr2.concatenate(tr);
				tr=tr2;
				}
			}
		else if(s.startsWith("skewX("))
			{
			int i=s.indexOf(")");
			if(i==-1) throw new IllegalArgumentException(s);
			String s2= s.substring(6,i).trim();
			s= s.substring(i+1).trim();
			
			double shx= Double.parseDouble(s2.trim());
			
			AffineTransform tr2= AffineTransform.getShearInstance(shx, 1f);
			tr2.concatenate(tr);
			tr=tr2;
			}
		else if(s.startsWith("skewY("))
			{
			int i=s.indexOf(")");
			if(i==-1) throw new IllegalArgumentException(s);
			String s2= s.substring(6,i).trim();
			s= s.substring(i+1).trim();
			
			double shy= Double.parseDouble(s2.trim());
			
			AffineTransform tr2= AffineTransform.getShearInstance(1f,shy);
			tr2.concatenate(tr);
			tr=tr2;
			}
		
		}
	return tr;
	}

	
/**
 * transform a shape into a SVG path as String
 * @param shape the shape
 * @return the SVG points for &lt;path&gt;
 */
static public String shapeToPath(final Shape shape)
	{
	final StringBuilder out= new StringBuilder();
	try {
		shapeToPath(out,shape);
		}
	catch(IOException err) {
		throw new RuntimeIOException(err);
		}
	return out.toString();
	}


/**
 * transform a shape into a SVG path
 * @param shape
 * @return
 */
static public void shapeToPath(Appendable path,Shape shape) throws IOException
{
	
	double tab[] = new double[6];
	PathIterator pathiterator = shape.getPathIterator(null);

	while(!pathiterator.isDone())
	{
		int currSegmentType= pathiterator.currentSegment(tab);
		switch(currSegmentType) {
		case PathIterator.SEG_MOVETO: {
			path.append( "M " + (tab[0]) + " " + (tab[1]) + " ");
			break;
		}
		case PathIterator.SEG_LINETO: {
			path.append( "L " + (tab[0]) + " " + (tab[1]) + " ");
			break;
		}
		case PathIterator.SEG_CLOSE: {
			path.append( "Z ");
			break;
		}
		case PathIterator.SEG_QUADTO: {
			path.append( "Q " + (tab[0]) + " " + (tab[1]));
			path.append( " "  + (tab[2]) + " " + (tab[3]));
			path.append( " ");
			break;
		}
		case PathIterator.SEG_CUBICTO: {
			path.append( "C " + (tab[0]) + " " + (tab[1]));
			path.append( " "  + (tab[2]) + " " + (tab[3]));
			path.append( " "  + (tab[4]) + " " + (tab[5]));
			path.append( " ");
			break;
		}
		default:
		{
			throw new IllegalStateException("Cannot handled "+currSegmentType);
		}
		}
		pathiterator.next();
	}
}


public static GeneralPath polygonToShape(String lineString )
	{
	final GeneralPath p = polylineToShape(lineString);
	p.closePath();
	return p;
	}

public static Shape pathToShape(final String s) {
	return SVGPathParser.parse(s);
}

public static OptionalDouble castUnit(String s) {
	double factor=1;
	s=s.trim();
	if(s.endsWith("px") || s.endsWith("pt") || s.endsWith("cm"))
		{
		s=s.substring(0,s.length()-2).trim();
		}
	if(s.endsWith("in"))
		{
		s=s.substring(0,s.length()-2).trim();
		factor=75.0;
		}
	try {
		double d= factor * Double.parseDouble(s);
		return OptionalDouble.of(d);
		}
	catch(NumberFormatException err) {
		return OptionalDouble.empty();
		}
	}


public static GeneralPath polylineToShape(String lineString ) {
	final GeneralPath p = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
	try(Scanner scanner= new Scanner(new StringReader(lineString))) {
		scanner.useDelimiter("[ \n,\t]+");
		
		boolean found=false;
		Double prev=null;
		while(scanner.hasNext())
			{
			String s=scanner.next();
			if(s.length()==0) continue;
			double v= Double.parseDouble(s);
			if(prev==null)
				{
				prev=v;
				}
			else
				{
				if(!found)
					{
					p.moveTo(prev, v);
					found=true;
					}
				else
					{
					p.lineTo(prev, v);
					}
				prev=null;
				}
			}
		if(prev!=null) throw new IllegalArgumentException("bad polyline "+lineString);
		}
	return p;
	}


/** return the dimension of a SVG document */
public static Optional<Dimension2D> getSize(final Element svgRoot)
	{
	try
		{
		Attr width= svgRoot.getAttributeNode("width");		
		if(width==null) return Optional.empty();
		OptionalDouble w= castUnit(width.getValue());
		if(!w.isPresent()) return Optional.empty();
		
		Attr height= svgRoot.getAttributeNode("height");
		if(height==null)return Optional.empty();
		OptionalDouble h= castUnit(height.getValue());
		if(!h.isPresent()) return Optional.empty();

		return Optional.of( new  Dimension2D(w.getAsDouble(),h.getAsDouble()));
		}
	catch(NumberFormatException err)
		{
		return Optional.empty();
		}
	}

public static OptionalInt toBasicStrokeJoin(String s) {
	if(s.equals("bevel")) return OptionalInt.of(BasicStroke.JOIN_BEVEL);
	if(s.equals("round")) return OptionalInt.of(BasicStroke.JOIN_ROUND);
	if(s.equals("miter")) return OptionalInt.of(BasicStroke.JOIN_MITER);
	return OptionalInt.empty();
	}
public static OptionalInt toBasicStrokeCap(String s) {
	if(s.equals("butt")) return OptionalInt.of(BasicStroke.CAP_BUTT);
	if(s.equals("round")) return OptionalInt.of(BasicStroke.CAP_ROUND);
	if(s.equals("square")) return OptionalInt.of(BasicStroke.CAP_SQUARE);
	return OptionalInt.empty();
	}

}
