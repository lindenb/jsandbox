package sandbox;

import java.awt.Color;
import java.util.function.Function;

public interface ColorParser extends Function<String,Color>{
public static ColorParser getInstance() {
	return new ColorParserImpl();
	}
static class ColorParserImpl implements ColorParser
	{
	@Override
	public Color apply(final String s) {
		if(s.equalsIgnoreCase("BLACK")) return Color.BLACK;
		if(s.equalsIgnoreCase("BLUE")) return Color.BLUE;
		if(s.equalsIgnoreCase("CYAN")) return Color.CYAN;
		if(s.equalsIgnoreCase("GRAY")) return Color.GRAY;
		if(s.equalsIgnoreCase("GREEN")) return Color.GREEN;
		if(s.equalsIgnoreCase("MAGENTA")) return Color.MAGENTA;
		if(s.equalsIgnoreCase("ORANGE")) return Color.ORANGE;
		if(s.equalsIgnoreCase("PINK")) return Color.PINK;
		if(s.equalsIgnoreCase("RED")) return Color.RED;
		if(s.equalsIgnoreCase("WHITE")) return Color.WHITE;
		if(s.equalsIgnoreCase("YELLOW")) return Color.YELLOW;

		final String tokens[]=s.split("[,]");
		switch(tokens.length){
			case 3:
				return new Color(
					Integer.parseInt(tokens[0]),
					Integer.parseInt(tokens[1]),
					Integer.parseInt(tokens[2])
					);
			case 4:
				return new Color(
						Integer.parseInt(tokens[0]),
						Integer.parseInt(tokens[1]),
						Integer.parseInt(tokens[2]),
						Integer.parseInt(tokens[3])
						);
			default:
				throw new IllegalArgumentException("bad rgb color components:"+s);
			}
		}
	}

}
