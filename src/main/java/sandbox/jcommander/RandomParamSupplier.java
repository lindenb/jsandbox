package sandbox.jcommander;

import java.util.Arrays;
import java.util.Random;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import com.beust.jcommander.IStringConverter;

import sandbox.StringUtils;

public class RandomParamSupplier implements IStringConverter<Supplier<Random>>{
	
	public static Supplier<Random> createDefault() {
		return new Supplier<Random>() {
			@Override
			public Random get() {
				return new Random() ;
				}
			@Override
			public String toString() {
				return "default";
				}
		};
	}
	
	public static Supplier<Random> createNow() {
		return createRandom(System.currentTimeMillis());
	}
	
	public static Supplier<Random> createRandom(long seed) {
		return new Supplier<Random>() {
			@Override
			public Random get() {
				return new Random(seed);
				}
			@Override
			public String toString() {
				return "random("+seed+")";
				}
		};
	}
	
	@Override
	public Supplier<Random> convert(String s) {
		s=s.toLowerCase();
		if(StringUtils.isBlank(s)) {
			return createDefault();
			}
		if(s.equals("now")) {
			return createNow();
			}
		try {
			return createRandom(Long.parseLong(s));
			}
		catch(final NumberFormatException err) {
			throw new IllegalArgumentException(err);
			}
		}
	}
