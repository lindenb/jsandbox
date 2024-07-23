package sandbox.awt;

import java.util.Map;
import java.util.Optional;
import java.awt.Color;
import java.util.HashMap;

public class Colors {
	private final Map<String,Color> hash = new HashMap<>(150);
	private static Colors INSTANCE = null;

	private Colors() {
		hash.put("aliceblue",new Color(240,248,255));
		hash.put("antiquewhite",new Color(250,235,215));
		hash.put("aqua",new Color(0,255,255));
		hash.put("aquamarine",new Color(127,255,212));
		hash.put("azure",new Color(240,255,255));
		hash.put("beige",new Color(245,245,220));
		hash.put("bisque",new Color(255,228,196));
		hash.put("black",new Color(0,0,0));
		hash.put("blanchedalmond",new Color(255,235,205));
		hash.put("blue",new Color(0,0,255));
		hash.put("blueviolet",new Color(138,43,226));
		hash.put("brown",new Color(165,42,42));
		hash.put("burlywood",new Color(222,184,135));
		hash.put("cadetblue",new Color(95,158,160));
		hash.put("chartreuse",new Color(127,255,0));
		hash.put("chocolate",new Color(210,105,30));
		hash.put("coral",new Color(255,127,80));
		hash.put("cornflowerblue",new Color(100,149,237));
		hash.put("cornsilk",new Color(255,248,220));
		hash.put("crimson",new Color(220,20,60));
		hash.put("cyan",new Color(0,255,255));
		hash.put("darkblue",new Color(0,0,139));
		hash.put("darkcyan",new Color(0,139,139));
		hash.put("darkgoldenrod",new Color(184,134,11));
		hash.put("darkgray",new Color(169,169,169));
		hash.put("darkgreen",new Color(0,100,0));
		hash.put("darkgrey",new Color(169,169,169));
		hash.put("darkkhaki",new Color(189,183,107));
		hash.put("darkmagenta",new Color(139,0,139));
		hash.put("darkolivegreen",new Color(85,107,47));
		hash.put("darkorange",new Color(255,140,0));
		hash.put("darkorchid",new Color(153,50,204));
		hash.put("darkred",new Color(139,0,0));
		hash.put("darksalmon",new Color(233,150,122));
		hash.put("darkseagreen",new Color(143,188,143));
		hash.put("darkslateblue",new Color(72,61,139));
		hash.put("darkslategray",new Color(47,79,79));
		hash.put("darkslategrey",new Color(47,79,79));
		hash.put("darkturquoise",new Color(0,206,209));
		hash.put("darkviolet",new Color(148,0,211));
		hash.put("deeppink",new Color(255,20,147));
		hash.put("deepskyblue",new Color(0,191,255));
		hash.put("dimgray",new Color(105,105,105));
		hash.put("dimgrey",new Color(105,105,105));
		hash.put("dodgerblue",new Color(30,144,255));
		hash.put("firebrick",new Color(178,34,34));
		hash.put("floralwhite",new Color(255,250,240));
		hash.put("forestgreen",new Color(34,139,34));
		hash.put("fuchsia",new Color(255,0,255));
		hash.put("gainsboro",new Color(220,220,220));
		hash.put("ghostwhite",new Color(248,248,255));
		hash.put("gold",new Color(255,215,0));
		hash.put("goldenrod",new Color(218,165,32));
		hash.put("gray",new Color(128,128,128));
		hash.put("green",new Color(0,128,0));
		hash.put("greenyellow",new Color(173,255,47));
		hash.put("grey",new Color(128,128,128));
		hash.put("honeydew",new Color(240,255,240));
		hash.put("hotpink",new Color(255,105,180));
		hash.put("indianred",new Color(205,92,92));
		hash.put("indigo",new Color(75,0,130));
		hash.put("ivory",new Color(255,255,240));
		hash.put("khaki",new Color(240,230,140));
		hash.put("lavender",new Color(230,230,250));
		hash.put("lavenderblush",new Color(255,240,245));
		hash.put("lawngreen",new Color(124,252,0));
		hash.put("lemonchiffon",new Color(255,250,205));
		hash.put("lightblue",new Color(173,216,230));
		hash.put("lightcoral",new Color(240,128,128));
		hash.put("lightcyan",new Color(224,255,255));
		hash.put("lightgoldenrodyellow",new Color(250,250,210));
		hash.put("lightgray",new Color(211,211,211));
		hash.put("lightgreen",new Color(144,238,144));
		hash.put("lightgrey",new Color(211,211,211));
		hash.put("lightpink",new Color(255,182,193));
		hash.put("lightsalmon",new Color(255,160,122));
		hash.put("lightseagreen",new Color(32,178,170));
		hash.put("lightskyblue",new Color(135,206,250));
		hash.put("lightslategray",new Color(119,136,153));
		hash.put("lightslategrey",new Color(119,136,153));
		hash.put("lightsteelblue",new Color(176,196,222));
		hash.put("lightyellow",new Color(255,255,224));
		hash.put("lime",new Color(0,255,0));
		hash.put("limegreen",new Color(50,205,50));
		hash.put("linen",new Color(250,240,230));
		hash.put("magenta",new Color(255,0,255));
		hash.put("maroon",new Color(128,0,0));
		hash.put("mediumaquamarine",new Color(102,205,170));
		hash.put("mediumblue",new Color(0,0,205));
		hash.put("mediumorchid",new Color(186,85,211));
		hash.put("mediumpurple",new Color(147,112,219));
		hash.put("mediumseagreen",new Color(60,179,113));
		hash.put("mediumslateblue",new Color(123,104,238));
		hash.put("mediumspringgreen",new Color(0,250,154));
		hash.put("mediumturquoise",new Color(72,209,204));
		hash.put("mediumvioletred",new Color(199,21,133));
		hash.put("midnightblue",new Color(25,25,112));
		hash.put("mintcream",new Color(245,255,250));
		hash.put("mistyrose",new Color(255,228,225));
		hash.put("moccasin",new Color(255,228,181));
		hash.put("navajowhite",new Color(255,222,173));
		hash.put("navy",new Color(0,0,128));
		hash.put("oldlace",new Color(253,245,230));
		hash.put("olive",new Color(128,128,0));
		hash.put("olivedrab",new Color(107,142,35));
		hash.put("orange",new Color(255,165,0));
		hash.put("orangered",new Color(255,69,0));
		hash.put("orchid",new Color(218,112,214));
		hash.put("palegoldenrod",new Color(238,232,170));
		hash.put("palegreen",new Color(152,251,152));
		hash.put("paleturquoise",new Color(175,238,238));
		hash.put("palevioletred",new Color(219,112,147));
		hash.put("papayawhip",new Color(255,239,213));
		hash.put("peachpuff",new Color(255,218,185));
		hash.put("peru",new Color(205,133,63));
		hash.put("pink",new Color(255,192,203));
		hash.put("plum",new Color(221,160,221));
		hash.put("powderblue",new Color(176,224,230));
		hash.put("purple",new Color(128,0,128));
		hash.put("red",new Color(255,0,0));
		hash.put("rosybrown",new Color(188,143,143));
		hash.put("royalblue",new Color(65,105,225));
		hash.put("saddlebrown",new Color(139,69,19));
		hash.put("salmon",new Color(250,128,114));
		hash.put("sandybrown",new Color(244,164,96));
		hash.put("seagreen",new Color(46,139,87));
		hash.put("seashell",new Color(255,245,238));
		hash.put("sienna",new Color(160,82,45));
		hash.put("silver",new Color(192,192,192));
		hash.put("skyblue",new Color(135,206,235));
		hash.put("slateblue",new Color(106,90,205));
		hash.put("slategray",new Color(112,128,144));
		hash.put("slategrey",new Color(112,128,144));
		hash.put("snow",new Color(255,250,250));
		hash.put("springgreen",new Color(0,255,127));
		hash.put("steelblue",new Color(70,130,180));
		hash.put("tan",new Color(210,180,140));
		hash.put("teal",new Color(0,128,128));
		hash.put("thistle",new Color(216,191,216));
		hash.put("tomato",new Color(255,99,71));
		hash.put("turquoise",new Color(64,224,208));
		hash.put("violet",new Color(238,130,238));
		hash.put("wheat",new Color(245,222,179));
		hash.put("white",new Color(255,255,255));
		hash.put("whitesmoke",new Color(245,245,245));
		hash.put("yellow",new Color(255,255,0));
		hash.put("yellowgreen",new Color(154,205,50));
		}

	public static Colors getInstance() {
		if(INSTANCE==null) {
			synchronized(Colors.class) {
				INSTANCE = new Colors();
				}
			}
		return INSTANCE;
		}

	public  Optional<Color> parse(String s) {
		s=s.trim();
		if(s==null || s.equals("null") || s.equals("none")) return Optional.empty();
		if(s.startsWith("#")) {
			return Optional.of(new Color(Integer.valueOf(s.substring(1),16) ));
			}
		else if(s.toLowerCase().startsWith("rgb(") && s.endsWith(")")) {
			String[] tokens=s.substring(4,s.length()-1).split("[,]");
			if(tokens.length==1) {
				final int gray = Integer.parseInt(tokens[0]);
				return Optional.of(new Color(gray,gray,gray));
				}
			if(tokens.length==3) {
				return Optional.of(new Color(
					Integer.parseInt(tokens[0]),
					Integer.parseInt(tokens[1]),
					Integer.parseInt(tokens[2])
					));
				}
			return Optional.empty();
			}
		return findByName(s);
		} 

	public Optional<Color> findByName(final String s) {
		if(s==null) return Optional.empty();
		return Optional.ofNullable(this.hash.get(s.toLowerCase()));
		}

	public Optional<String> findColorName(final Color c) {
		if(c==null) return Optional.empty();
		return this.hash.entrySet().
				stream().
				filter(KV->KV.getValue().equals(c)).
				map(KV->KV.getKey()).
				findFirst();
		}
	}
