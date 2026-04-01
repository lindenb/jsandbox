package sandbox.jcommander;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.beust.jcommander.IStringConverter;


public class DurationConverter
	implements IStringConverter<Duration>
	{
	public static final String OPT_DESC="format: <integer>(years|week|days|hours|minutes|seconds)";
	
	private static class SuffixFun {
		final String suffix;
		final int factor ;
		final Function<Long,Duration> fun;
		SuffixFun(final String suffix,int factor,Function<Long,Duration> fun) {
			this.suffix = suffix;
			this.factor = factor;
			this.fun = fun;
		}
	}
	
	@Override
	public Duration convert(String s) {
		final List<SuffixFun> suffixes = new ArrayList<>();
		suffixes.add(new SuffixFun("years",365,Duration::ofDays));
		suffixes.add(new SuffixFun("year",365,Duration::ofDays));
		suffixes.add(new SuffixFun("y",365,Duration::ofDays));
		suffixes.add(new SuffixFun("weeks",7,Duration::ofDays));
		suffixes.add(new SuffixFun("week",7,Duration::ofDays));
		suffixes.add(new SuffixFun("w",7,Duration::ofDays));
		suffixes.add(new SuffixFun("days",1,Duration::ofDays));
		suffixes.add(new SuffixFun("day",1,Duration::ofDays));
		suffixes.add(new SuffixFun("d",1,Duration::ofDays));
		suffixes.add(new SuffixFun("hours",1,Duration::ofHours));
		suffixes.add(new SuffixFun("hour",1,Duration::ofHours));
		suffixes.add(new SuffixFun("h",1,Duration::ofHours));
		suffixes.add(new SuffixFun("minutes",1,Duration::ofMinutes));
		suffixes.add(new SuffixFun("minute",1,Duration::ofMinutes));
		suffixes.add(new SuffixFun("min",1,Duration::ofMinutes));
		suffixes.add(new SuffixFun("m",1,Duration::ofMinutes));
		suffixes.add(new SuffixFun("secondes",1,Duration::ofSeconds));
		suffixes.add(new SuffixFun("seconde",1,Duration::ofSeconds));
		suffixes.add(new SuffixFun("secs",1,Duration::ofSeconds));
		suffixes.add(new SuffixFun("sec",1,Duration::ofSeconds));
		suffixes.add(new SuffixFun("s",1,Duration::ofSeconds));
		suffixes.add(new SuffixFun("milliseconds",1,Duration::ofMillis));
		suffixes.add(new SuffixFun("millisecond",1,Duration::ofMillis));
		suffixes.add(new SuffixFun("millisec",1,Duration::ofMillis));
		suffixes.add(new SuffixFun("ms",1,Duration::ofMillis));

		
		s=s.toLowerCase().trim();
		for(SuffixFun sf:suffixes) {
			if(!s.endsWith(sf.suffix)) continue;
			s = s.substring(0, s.length()-sf.suffix.length()).trim();
			return sf.fun.apply(sf.factor * Long.parseLong(s));
			}
		
		throw new IllegalArgumentException("illegal suffix in "+s+" should be one of "+suffixes.stream().
			map(S->S.suffix).
			collect(Collectors.joining(" ")));
		}
	}
