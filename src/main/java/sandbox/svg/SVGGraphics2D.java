package sandbox.svg;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sandbox.awt.AbstractGraphics2D;

public class SVGGraphics2D extends AbstractGraphics2D {
	private final int width;
	private final int height;
	private final Document dom;
	private final Element svgRoot;
	private final Element styleNode;
	private final Element mainNode;
	private final List<Style> styles = new ArrayList<>();
	private static class Style {
		String name;
		Map<String,String> props = new HashMap<>();
	}
	
	private Element element(final String name) {
		return this.dom.createElementNS(SVG.NS, name);
		}
	
	public SVGGraphics2D(int width,int height) {
		this.width = width;
		this.height = height;
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			this.dom = db.newDocument();
			this.svgRoot = element("svg");
			this.svgRoot.setAttribute("width", String.valueOf(width));
			this.svgRoot.setAttribute("height", String.valueOf(height));
			this.dom.appendChild(this.svgRoot);
			this.styleNode = element("style");
			this.svgRoot.appendChild(this.styleNode);
			this.mainNode = element("g");
			this.svgRoot.appendChild(this.mainNode);
		} catch(final Throwable err) {
			throw new RuntimeException(err);
		}
	}
	
	private void appendPoint(final StringBuilder sb,float x,float y) {
		sb.append(String.valueOf(x));
		sb.append(" ");
		sb.append(String.valueOf(y));
		}
	
    private String shapeToSvg(Shape path) {
        StringBuilder d = new StringBuilder();
        PathIterator pi = path.getPathIterator(null);
        float[] seg = new float[6];
        int segType = 0;
        while (!pi.isDone()) {
            segType = pi.currentSegment(seg);
            switch(segType) {
            case PathIterator.SEG_MOVETO:
                d.append('M');
                appendPoint(d, seg[0], seg[1]);
                break;
            case PathIterator.SEG_LINETO:
                d.append('L');
                appendPoint(d, seg[0], seg[1]);
                break;
            case PathIterator.SEG_CLOSE:
                d.append('Z');
                break;
            case PathIterator.SEG_QUADTO:
                d.append('Q');
                appendPoint(d, seg[0], seg[1]);
                appendPoint(d, seg[2], seg[3]);
                break;
            case PathIterator.SEG_CUBICTO:
                d.append('C');
                appendPoint(d, seg[0], seg[1]);
                appendPoint(d, seg[2], seg[3]);
                appendPoint(d, seg[4], seg[5]);
                break;
            default:
                throw new Error("invalid segmentType:" + segType );
            }
            pi.next();
        } // while !isDone
        return d.toString();
    }

	
	private String colorToStr(final Color c) {
		return "rgb("+c.getRed()+","+c.getGreen()+","+c.getBlue()+")";
	}
	
	private Style getStyle(boolean filled) {
		final Map<String,String> map = new HashMap<>();
		float opacity = 1f;
		final Composite composite = getComposite();
		if(composite instanceof AlphaComposite) {
			opacity = AlphaComposite.class.cast(composite).getAlpha();
			}
		final Stroke stroke=getStroke();
		if(stroke instanceof BasicStroke) {
			final BasicStroke b2 = BasicStroke.class.cast(stroke);
			map.put("stroke-width", String.valueOf(b2.getLineWidth()));
			switch(b2.getLineJoin()) {
				case BasicStroke.CAP_BUTT: map.put("stroke-linecap", "butt"); break; 
				case BasicStroke.CAP_SQUARE: map.put("stroke-linecap", "square"); break; 
				case BasicStroke.CAP_ROUND: map.put("stroke-linecap", "round"); break; 
				default: break;
				}
			}
		if(filled) {
			map.put("stroke", "none");
			map.put("fill",colorToStr(getColor()));
			if(opacity<1f) map.put("fill-opacity", String.valueOf(opacity));
		}
		else
		{
			map.put("stroke",colorToStr(getColor()));
			map.put("fill", "none");
			if(opacity<1f) map.put("stroke-opacity", String.valueOf(opacity));
		}
		
		Style st = styles.stream().filter(S->S.props.equals(map)).findFirst().orElse(null);
		
		
		if(st==null) {
			st = new Style();
			st.name="s"+styles.size();
			st.props = map;
			styles.add(st);
			
			final String kv = map.entrySet().stream().map(KV->KV.getKey()+":"+KV.getValue()).collect(Collectors.joining(";"));
			this.styleNode.appendChild(this.dom.createTextNode(kv));
			}
		return st;
		}
	@Override
	protected void paintTransformedShape(Shape shape, boolean filled) {
		final Element e = element("path");
		e.setAttribute("d", shapeToSvg(shape));
		final Style st = getStyle(filled);
		e.setAttribute("class",st.name);
		mainNode.appendChild(e);
		}
}
