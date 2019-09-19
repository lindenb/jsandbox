package sandbox.jcommander;

import java.util.Arrays;
import java.util.Random;
import java.util.function.DoubleSupplier;

import com.beust.jcommander.IStringConverter;

import sandbox.StringUtils;

public class DoubleParamSupplier implements IStringConverter<DoubleSupplier>{
	public static final String OPT_DESC="";
	
	public static DoubleSupplier createReference(final DoubleSupplier other) {
		return new DoubleSupplier() {
			@Override
			public double getAsDouble() {
				return other.getAsDouble();
				}
			@Override
			public String toString() {
				return "copy-of("+other+")";
				}
		};
	}
	
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
	
	public static DoubleSupplier createRandomBetween(final double d1,double d2) {
		return createRandomBetween(()->Math.random(),d1,d2);
	}
	
	public static DoubleSupplier createRandomBetween(final DoubleSupplier randomSupplier,final double d1,double d2) {
		return new DoubleSupplier() {
			@Override
			public double getAsDouble() {
				return d1+randomSupplier.getAsDouble()*(d2-d1);
				}
			@Override
			public String toString() {
				return "random("+d1+","+d2+")";
				}
		};
	}
	
	
	@Override
	public DoubleSupplier convert(String s) {
		s=s.toLowerCase();
		if(s.equals("random") || s.equals("random()") || s.equals("rnd") || s.equals("rnd()")) {
			return ()->Math.random();
		}
		if(s.equals("gaussian") || s.equals("gaussian()")) {
			return new GaussianSupplier();
		}
		if(s.startsWith("random(") && s.endsWith(")")) {
			s = s.substring(7,s.length()-1);
			try {
				int slash = s.indexOf(",");
				if(slash!=-1) {
					final double d1= Double.parseDouble(s.substring(0,slash));
					final double d2= Double.parseDouble(s.substring(slash+1));
					return createRandomBetween(d1, d2);
					}
				else
					{
					final double d1= Double.parseDouble(s);
					return createRandomBetween(0,d1);
					}
				}
			catch(final NumberFormatException err) {
				throw new IllegalArgumentException(err);
				}
			
			}
		if(s.startsWith("sequence(") && s.endsWith(")")) {
			final double a[]= Arrays.stream(s.substring(9, s.length()-1).split("[, ]+")).filter(S->!StringUtils.isBlank(S)).mapToDouble(Double::valueOf).toArray();
			if(a.length==0) throw new IllegalArgumentException("empty array in "+s);
			return new Sequence(a);
			}
		
		
		try {
			return createDefault( Double.parseDouble(s));
			}
		catch(final NumberFormatException err) {
			throw new IllegalArgumentException(err);
			}
		}
	
	private static class Sequence implements DoubleSupplier
		{
		int idx=0;
		final double array[];
		Sequence(final double array[]) {
			this.array=array;
			}
		@Override
		public double getAsDouble() {
			double v =array[idx];
			idx++;
			if(idx>=array.length) idx=0;
			return v;
			}
		}
	private static class GaussianSupplier implements DoubleSupplier
		{
		final Random rnd=new Random();
		@Override
		public double getAsDouble() {
			return rnd.nextGaussian();
			}
		@Override
		public String toString() {
			return "gaussian";
			}
		}
	}
