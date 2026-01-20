package sandbox.colors;

import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

public class NamedColors {
	private final Map<String,Color> hash = new HashMap<>(150);
	private static NamedColors INSTANCE = null;

	private NamedColors() {
		hash.put("aliceblue",Color.create(240,248,255));
		hash.put("antiquewhite",Color.create(250,235,215));
		hash.put("aqua",Color.create(0,255,255));
		hash.put("aquamarine",Color.create(127,255,212));
		hash.put("azure",Color.create(240,255,255));
		hash.put("beige",Color.create(245,245,220));
		hash.put("bisque",Color.create(255,228,196));
		hash.put("black",Color.create(0,0,0));
		hash.put("blanchedalmond",Color.create(255,235,205));
		hash.put("blue",Color.create(0,0,255));
		hash.put("blueviolet",Color.create(138,43,226));
		hash.put("brown",Color.create(165,42,42));
		hash.put("burlywood",Color.create(222,184,135));
		hash.put("cadetblue",Color.create(95,158,160));
		hash.put("chartreuse",Color.create(127,255,0));
		hash.put("chocolate",Color.create(210,105,30));
		hash.put("coral",Color.create(255,127,80));
		hash.put("cornflowerblue",Color.create(100,149,237));
		hash.put("cornsilk",Color.create(255,248,220));
		hash.put("crimson",Color.create(220,20,60));
		hash.put("cyan",Color.create(0,255,255));
		hash.put("darkblue",Color.create(0,0,139));
		hash.put("darkcyan",Color.create(0,139,139));
		hash.put("darkgoldenrod",Color.create(184,134,11));
		hash.put("darkgray",Color.create(169,169,169));
		hash.put("darkgreen",Color.create(0,100,0));
		hash.put("darkgrey",Color.create(169,169,169));
		hash.put("darkkhaki",Color.create(189,183,107));
		hash.put("darkmagenta",Color.create(139,0,139));
		hash.put("darkolivegreen",Color.create(85,107,47));
		hash.put("darkorange",Color.create(255,140,0));
		hash.put("darkorchid",Color.create(153,50,204));
		hash.put("darkred",Color.create(139,0,0));
		hash.put("darksalmon",Color.create(233,150,122));
		hash.put("darkseagreen",Color.create(143,188,143));
		hash.put("darkslateblue",Color.create(72,61,139));
		hash.put("darkslategray",Color.create(47,79,79));
		hash.put("darkslategrey",Color.create(47,79,79));
		hash.put("darkturquoise",Color.create(0,206,209));
		hash.put("darkviolet",Color.create(148,0,211));
		hash.put("deeppink",Color.create(255,20,147));
		hash.put("deepskyblue",Color.create(0,191,255));
		hash.put("dimgray",Color.create(105,105,105));
		hash.put("dimgrey",Color.create(105,105,105));
		hash.put("dodgerblue",Color.create(30,144,255));
		hash.put("firebrick",Color.create(178,34,34));
		hash.put("floralwhite",Color.create(255,250,240));
		hash.put("forestgreen",Color.create(34,139,34));
		hash.put("fuchsia",Color.create(255,0,255));
		hash.put("gainsboro",Color.create(220,220,220));
		hash.put("ghostwhite",Color.create(248,248,255));
		hash.put("gold",Color.create(255,215,0));
		hash.put("goldenrod",Color.create(218,165,32));
		hash.put("gray",Color.create(128,128,128));
		hash.put("green",Color.create(0,128,0));
		hash.put("greenyellow",Color.create(173,255,47));
		hash.put("grey",Color.create(128,128,128));
		hash.put("honeydew",Color.create(240,255,240));
		hash.put("hotpink",Color.create(255,105,180));
		hash.put("indianred",Color.create(205,92,92));
		hash.put("indigo",Color.create(75,0,130));
		hash.put("ivory",Color.create(255,255,240));
		hash.put("khaki",Color.create(240,230,140));
		hash.put("lavender",Color.create(230,230,250));
		hash.put("lavenderblush",Color.create(255,240,245));
		hash.put("lawngreen",Color.create(124,252,0));
		hash.put("lemonchiffon",Color.create(255,250,205));
		hash.put("lightblue",Color.create(173,216,230));
		hash.put("lightcoral",Color.create(240,128,128));
		hash.put("lightcyan",Color.create(224,255,255));
		hash.put("lightgoldenrodyellow",Color.create(250,250,210));
		hash.put("lightgray",Color.create(211,211,211));
		hash.put("lightgreen",Color.create(144,238,144));
		hash.put("lightgrey",Color.create(211,211,211));
		hash.put("lightpink",Color.create(255,182,193));
		hash.put("lightsalmon",Color.create(255,160,122));
		hash.put("lightseagreen",Color.create(32,178,170));
		hash.put("lightskyblue",Color.create(135,206,250));
		hash.put("lightslategray",Color.create(119,136,153));
		hash.put("lightslategrey",Color.create(119,136,153));
		hash.put("lightsteelblue",Color.create(176,196,222));
		hash.put("lightyellow",Color.create(255,255,224));
		hash.put("lime",Color.create(0,255,0));
		hash.put("limegreen",Color.create(50,205,50));
		hash.put("linen",Color.create(250,240,230));
		hash.put("magenta",Color.create(255,0,255));
		hash.put("maroon",Color.create(128,0,0));
		hash.put("mediumaquamarine",Color.create(102,205,170));
		hash.put("mediumblue",Color.create(0,0,205));
		hash.put("mediumorchid",Color.create(186,85,211));
		hash.put("mediumpurple",Color.create(147,112,219));
		hash.put("mediumseagreen",Color.create(60,179,113));
		hash.put("mediumslateblue",Color.create(123,104,238));
		hash.put("mediumspringgreen",Color.create(0,250,154));
		hash.put("mediumturquoise",Color.create(72,209,204));
		hash.put("mediumvioletred",Color.create(199,21,133));
		hash.put("midnightblue",Color.create(25,25,112));
		hash.put("mintcream",Color.create(245,255,250));
		hash.put("mistyrose",Color.create(255,228,225));
		hash.put("moccasin",Color.create(255,228,181));
		hash.put("navajowhite",Color.create(255,222,173));
		hash.put("navy",Color.create(0,0,128));
		hash.put("oldlace",Color.create(253,245,230));
		hash.put("olive",Color.create(128,128,0));
		hash.put("olivedrab",Color.create(107,142,35));
		hash.put("orange",Color.create(255,165,0));
		hash.put("orangered",Color.create(255,69,0));
		hash.put("orchid",Color.create(218,112,214));
		hash.put("palegoldenrod",Color.create(238,232,170));
		hash.put("palegreen",Color.create(152,251,152));
		hash.put("paleturquoise",Color.create(175,238,238));
		hash.put("palevioletred",Color.create(219,112,147));
		hash.put("papayawhip",Color.create(255,239,213));
		hash.put("peachpuff",Color.create(255,218,185));
		hash.put("peru",Color.create(205,133,63));
		hash.put("pink",Color.create(255,192,203));
		hash.put("plum",Color.create(221,160,221));
		hash.put("powderblue",Color.create(176,224,230));
		hash.put("purple",Color.create(128,0,128));
		hash.put("red",Color.create(255,0,0));
		hash.put("rosybrown",Color.create(188,143,143));
		hash.put("royalblue",Color.create(65,105,225));
		hash.put("saddlebrown",Color.create(139,69,19));
		hash.put("salmon",Color.create(250,128,114));
		hash.put("sandybrown",Color.create(244,164,96));
		hash.put("seagreen",Color.create(46,139,87));
		hash.put("seashell",Color.create(255,245,238));
		hash.put("sienna",Color.create(160,82,45));
		hash.put("silver",Color.create(192,192,192));
		hash.put("skyblue",Color.create(135,206,235));
		hash.put("slateblue",Color.create(106,90,205));
		hash.put("slategray",Color.create(112,128,144));
		hash.put("slategrey",Color.create(112,128,144));
		hash.put("snow",Color.create(255,250,250));
		hash.put("springgreen",Color.create(0,255,127));
		hash.put("steelblue",Color.create(70,130,180));
		hash.put("tan",Color.create(210,180,140));
		hash.put("teal",Color.create(0,128,128));
		hash.put("thistle",Color.create(216,191,216));
		hash.put("tomato",Color.create(255,99,71));
		hash.put("turquoise",Color.create(64,224,208));
		hash.put("violet",Color.create(238,130,238));
		hash.put("wheat",Color.create(245,222,179));
		hash.put("white",Color.create(255,255,255));
		hash.put("whitesmoke",Color.create(245,245,245));
		hash.put("yellow",Color.create(255,255,0));
		hash.put("yellowgreen",Color.create(154,205,50));
		}

	public static NamedColors getInstance() {
		if(INSTANCE==null) {
			synchronized(NamedColors.class) {
				INSTANCE = new NamedColors();
				}
			}
		return INSTANCE;
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
