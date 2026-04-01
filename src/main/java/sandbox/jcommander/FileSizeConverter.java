package sandbox.jcommander;

import com.beust.jcommander.IStringConverter;


public class FileSizeConverter
	implements IStringConverter<Long>
	{
	public static final String OPT_DESC="";
	
	@Override
	public Long convert(String s) {
		final long unit = 1024;
		s=s.toLowerCase().trim();
		long factor=1;
		if(s.endsWith("k")) {
			factor  = unit;
			s=s.substring(0,s.length()-1);
			}
		else if(s.endsWith("m")) {
			factor  = unit * unit;
			s=s.substring(0,s.length()-1);
			}
		else if(s.endsWith("g")) {
			factor  = unit * unit * unit;
			s=s.substring(0,s.length()-1);
			}
		return Long.parseLong(s.trim())*factor;
		}
	
	}
