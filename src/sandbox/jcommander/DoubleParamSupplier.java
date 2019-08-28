package sandbox.jcommander;

import java.util.function.DoubleSupplier;

import com.beust.jcommander.IStringConverter;

public class DoubleParamSupplier implements IStringConverter<DoubleSupplier>{
	
	public static DoubleSupplier createDefault(final double d) {
		return new DoubleSupplier() {
			@Override
			public double getAsDouble() {
				return d;
				}
			@Override
			public String toString() {
				return String.valueOf(d);
				}
		};
	}
	
	@Override
	public DoubleSupplier convert(String s) {
		s=s.toLowerCase();
		if(s.equals("random") || s.equals("random()")) {
			return ()->Math.random();
		}
		
		if(s.startsWith("random(") && s.endsWith(")")) {
			s = s.substring(7,s.length()-1);
			try {
				int slash = s.indexOf(",");
				if(slash!=-1) {
					final double d1= Double.parseDouble(s.substring(0,slash));
					final double d2= Double.parseDouble(s.substring(slash+1));
					return ()->d1+Math.random()*(d2-d1);
					}
				else
					{
					final double d1= Double.parseDouble(s);
					return ()-> Math.random()*d1;
					}
				}
			catch(NumberFormatException err) {
				throw new IllegalArgumentException(err);
				}
			
			}
		try {
			return createDefault( Double.parseDouble(s));
			}
		catch(final NumberFormatException err) {
			throw new IllegalArgumentException(err);
			}
		}
	}
