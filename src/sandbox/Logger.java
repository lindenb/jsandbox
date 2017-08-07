package sandbox;

public class Logger
	{
	private String prefix="";
	
	public static class Builder
		{
		String prefix="log";
		Builder(final Class<?> C)
			{
			this.prefix=C.getSimpleName();
			}	
		public Logger build()
			{
			final Logger L=new Logger();
			L.prefix = this.prefix;
			return L;
			}
		}
	public static Builder builder(final Class<?> C) {
		return new Builder(C);
		}
	
	public void log(final String level,final Object o,final Throwable err) {
		if(o!=null && err==null && (o instanceof Throwable))
			{
			final Throwable err2=Throwable.class.cast(o);
			log(level,err2.getMessage(),err2);
			return;
			}
		if(level!=null && !level.isEmpty()) {
			System.err.print("[");
			System.err.print(level);
			System.err.print("]");
			}
		if(this.prefix!=null && !this.prefix.isEmpty()) {
			System.err.print("[");
			System.err.print(this.prefix);
			System.err.print("]");
			}
		if(o!=null) {
			System.err.print(String.valueOf(o));
			}
		System.err.println();
		if(err!=null) err.printStackTrace();
		}
	
	public void log(final String level,final Object o) {
		log(level,o,null);
		}
	
	public void info(final Object o) {
		log("INFO",o);
		}
	public void error(final Object o) {
		log("ERROR",o);
		}
	public void warning(final Object o) {
		log("WARN",o);
		}
	public void debug(final Object o) {
		log("DEBUG",o);
		}
	}
