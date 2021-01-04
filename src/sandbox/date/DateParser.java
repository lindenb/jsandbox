package sandbox.date;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import sandbox.StringUtils;

public class DateParser implements Function<String, Optional<Date>> {
	private final List< Function<String,Date>> delegates = new ArrayList<>();
	public DateParser() {
		this.delegates.add(new SimpleDateFormatParser("yyyy-MM-dd'T'HH:mm:ssXXX"));
		this.delegates.add(new SimpleDateFormatParser("EEE, dd MMM yyyy HH:mm:ss"));
		this.delegates.add(new SimpleDateFormatParser("yyyyMMdd"));
		}
	@Override
	public Optional<Date> apply(final String t) {
		return delegates.stream().map(P->P.apply(t)).filter(P->P!=null).findFirst();
		}
	

	private static class SimpleDateFormatParser
		implements Function<String,Date>
		{
		private final SimpleDateFormat fmt;
		SimpleDateFormatParser(final String format) {
			this.fmt = new SimpleDateFormat(format);
			this.fmt.setLenient(true);
			}
		@Override
		public Date apply(final String t) {
			if(StringUtils.isBlank(t)) return null;
			try {
				return this.fmt.parse(t.replaceAll(" PST",""));
				}
			catch(final ParseException err) {
				return null;
				}
			}
		}
}
