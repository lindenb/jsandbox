package sandbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.tidy.Tidy;


public class WeatherArchive extends AbstractApplication {
	private static final String MIN_DATE_STR="1963-01-01";
	public static enum Unit {
		celcius,hours,mm,hourday;
	}
	
	public static class Value {
		double value;
		Unit unit;
		Value(double value,Unit unit) {
			this.value=value;
			this.unit=unit;
		}
		@Override
		public String toString() {
			return String.valueOf(this.value)+" ("+unit+")";
			}
	}
	
	public static class Echeance {
		Value when;
		String desc;
		Echeance(Value when,String desc) {
			this.when=when;
			this.desc=desc;
		}
		@Override
		public String toString() {
			return String.valueOf(this.when)+" ("+desc+")";
			}
	}
	
	private static GregorianCalendar parseYYYMMDD(String yyyymmdd) {
		Pattern date=Pattern.compile("([12][0-9][0-9][0-9])"+"([01][0-9])"+"([1203][0-9])");
		Matcher matcher = date.matcher(yyyymmdd.replace("-", "").replace("/", ""));
		if(!matcher.matches()) throw new IllegalArgumentException("Bad date: "+yyyymmdd);
		int year = Integer.parseInt(matcher.group(1));
		int month = Integer.parseInt(matcher.group(2));
		int dayofMonth = Integer.parseInt(matcher.group(3));
		return new  GregorianCalendar(year, month-1, dayofMonth);
	}
	
	public static class Record {
		final GregorianCalendar cal;
		final String placeId;
		Value temperatureMin = null;
		Value temperatureMax = null;
		Value sunlight = null;
		Value rain = null;
		List<Echeance> echeances = new ArrayList<Echeance>();
		
		
		
		Record(final String placeId,final GregorianCalendar cal) {
			this.placeId=placeId;
			this.cal=cal;
		}
		
		
		String getURL() throws IOException {
			return "http://www.m"+/* hello */"ete"+/* hello */"ofra"+
					"nce."+"com/"+
					"cli"+"mat/me"+
					"teo-"+/* hello */"da"+"te-pa"+/*world*/"ssee?lie"+
					"uId="+URLEncoder.encode(placeId,"UTF-8")+ "&lie"+"uType=VIL"+
					"LE_FRA"+
					"NCE&date="+
					String.format("%02d", this.cal.get(GregorianCalendar.DAY_OF_MONTH))+"-"+
					String.format("%02d", 1+this.cal.get(GregorianCalendar.MONTH))+"-"+
					String.format("%04d", this.cal.get(GregorianCalendar.YEAR))
					;
		}
		
		
		private void sql(PrintStream out) {
			String when = String.format("%04d", this.cal.get(GregorianCalendar.YEAR))+"-"+
					String.format("%02d", 1+this.cal.get(GregorianCalendar.MONTH))+"-"+
					String.format("%02d", this.cal.get(GregorianCalendar.DAY_OF_MONTH))
					;
			out.println("delete from wrecord where placeId=\'"+this.placeId+"\' and date=\'"+when+"\';");
			out.print("insert into wrecord(placeId,date,minT,minTu,maxT,maxTu,sunlight,sunlightu,rain,rainu) values (\'"+this.placeId+"\',\'"+when+"\' ");
			if(this.temperatureMin==null) {
				out.print(",NULL,NULL");
			} else
				 
			{
				out.print(","+this.temperatureMin.value+",\'"+this.temperatureMin.unit.name()+"\'");
			}
			if(this.temperatureMax==null) {
				out.print(",NULL,NULL");
			} else
				 
			{
				out.print(","+this.temperatureMax.value+",\'"+this.temperatureMax.unit.name()+"\'");
			}
			
			if(this.sunlight==null) {
				out.print(",NULL,NULL");
			} else
				 
			{
				out.print(","+this.sunlight.value+",\'"+this.sunlight.unit.name()+"\'");
			}
			if(this.rain==null) {
				out.print(",NULL,NULL");
			} else
				 
			{
				out.print(","+this.rain.value+",\'"+this.rain.unit.name()+"\'");
			}
			out.println(");");
			
			
			out.println("delete from wech where placeId=\'"+this.placeId+"\' and date=\'"+when+"\';");
			for(Echeance ech: this.echeances) {
				out.println("insert into wech(placeId,date,evtwhen,evtwhenu,evtwhat) values (\'"+this.placeId+"\',"
						+ "\'"+when+"',"+ech.when.value +",'"+ech.when.unit.name() +"','"+ech.desc+"\');");
			}

		}
		
		private void scan(Node node) {
			final String EGRAVE= new String(Character.toChars(195)) + new String(Character.toChars(169)) ;// "&Atilde;&copy;";//\u00E3\u00A9";
			final String ECIRC =  new String(Character.toChars(195)) + new String(Character.toChars(170));// \195\169
			final String EAIG =  new String(Character.toChars(195)) + new String(Character.toChars(168));// \195\169

			if(node==null) return;
			if(node.getNodeType()==Node.TEXT_NODE )
				{
				final String CELCIUS=new String(Character.toChars(194)) + new String(Character.toChars(176)) +"C" ;//
				final String HOUR= "h";
				final String MILIMETERS= "mm";
				final String TMPMIN="Temp"+EGRAVE+"rature mi"+"nimale de"+" la journ"+EGRAVE+"e :";
				final String TMPMAX="Temp"+EGRAVE+"rature max"+"imale de la"+" journ"+EGRAVE+"e :";
				final String SUNLIGHT="Dur"+EGRAVE+"e d'ensolei"+"llement de la journ"+EGRAVE+"e :";
				final String RAIN="Hauteur des pr"+EGRAVE+"cipitations :";
				String data = Text.class.cast(node).getData();
				if(data!=null)
					{
					data=data.trim();
					if(data.startsWith(TMPMIN) && data.endsWith(CELCIUS)) {
						data=data.substring(TMPMIN.length(),data.length()-CELCIUS.length()).trim();
						this.temperatureMin = new Value(Double.parseDouble(data), Unit.celcius);
						LOG.info("min tmp "+this.temperatureMin);
					}
					else if(data.startsWith(TMPMAX) && data.endsWith(CELCIUS)) {
						data=data.substring(TMPMAX.length(),data.length()-CELCIUS.length()).trim();
						this.temperatureMax = new Value(Double.parseDouble(data), Unit.celcius);
						LOG.info("max tmp "+this.temperatureMax);
					}
					else if(data.startsWith(SUNLIGHT) && data.endsWith(HOUR)) {
						data=data.substring(SUNLIGHT.length(),data.length()-HOUR.length()).trim();
						this.sunlight = new Value(Double.parseDouble(data), Unit.hours);
						LOG.info("sunlight "+this.sunlight);
					}
					else if(data.startsWith(RAIN) && data.endsWith(MILIMETERS)) {
						data=data.substring(RAIN.length(),data.length()-MILIMETERS.length()).trim();
						this.rain = new Value(Double.parseDouble(data), Unit.mm);
						LOG.info("rain "+this.rain);
					}
					
					
					}
			
				}
			if(node.getNodeType()==Node.ELEMENT_NODE && node.getNodeName().equals("em")) {
				String desc = Text.class.cast(node.getFirstChild()).getData();
				Element div1 = Element.class.cast(node.getParentNode());
				if(div1.getAttribute("class").equals("weat"+"herDes"+"cription") && desc!=null) {
					desc=desc.trim();
					Node c2=div1;
					while(c2!=null) {
						
						if(c2.getNodeType()==Node.ELEMENT_NODE &&
							c2.getNodeName().equals("h3") &&
							c2.hasChildNodes() &&
							c2.getFirstChild().getNodeType()==Node.TEXT_NODE) {
							String hours =  Text.class.cast(c2.getFirstChild()).getData();
							if(hours!=null && hours.endsWith("h")) {
								hours=hours.substring(0,hours.length()-1).trim();
								Value when = new Value(Integer.parseInt(hours), Unit.hourday);
								desc = desc.replaceAll(ECIRC, "e").replaceAll(EGRAVE, "e").replaceAll(EAIG, "e");
								//for(int i=0;i< desc.length();++i) System.err.print(desc.charAt(i)+" "+desc.codePointAt(i)+"\n");
								Echeance echeance=new Echeance(when, desc );
								LOG.info("echeance "+echeance);
								this.echeances.add(echeance);
								}
							break;
							}
						
						c2=c2.getPreviousSibling();
					}
				}
			}
			
			
			for(Node c=node.getFirstChild();c!=null;c=c.getNextSibling())
				{	
				scan(c);
				}
		}

	}
	
	@Override
	protected void fillOptions(Options options) {
		options.addOption(Option.builder("d").longOpt("date").hasArg().desc("uniqe date YYYY-MM-DD. (->ignore -s and -e )").build());
		options.addOption(Option.builder("s").longOpt("date-start").hasArg().desc("date-start YYYY-MM-DD. Min is "+MIN_DATE_STR).build());
		options.addOption(Option.builder("e").longOpt("date-end").hasArg().desc("date-date-end YYYY-MM-DD").build());
		options.addOption(Option.builder("p").longOpt("placeid").hasArg().desc("placeid. Multiple separated by comma. e.g: 441090").build());
		options.addOption(Option.builder("w").longOpt("wait").hasArg().desc("wait 'w' seconds between each call.").build());
		super.fillOptions(options);
	}
	
	
	
	
	@Override
	protected int execute(final CommandLine cmd) 
	{
	final Random rand= new Random();
	int waitMilliSecs=10000;
	CloseableHttpClient client = null;
		try 
		{
			GregorianCalendar dateBegin = null;
			GregorianCalendar dateEnd = new GregorianCalendar();
			Set<String> placeIds = new HashSet<>();
			
			if(cmd.hasOption("w")) {
				waitMilliSecs = Integer.parseInt(cmd.getOptionValue("w"))*1000;
				}
			
			if(!cmd.hasOption("p")) {
				LOG.severe("Place was not specified. e.g: 441090");
				return -1;
				}
			else
				{
				for(String s:cmd.getOptionValue("p").split("[, ]")) {
					if(s.trim().isEmpty()) continue;
					placeIds.add(s);
					}
				}
			if(placeIds.isEmpty()) {
				LOG.severe("No place was found");
				return -1;
			}
			
			if(cmd.hasOption("d")) {
				dateBegin=dateEnd=parseYYYMMDD(cmd.getOptionValue("d"));
				}
			else if(cmd.hasOption("s")) {
				dateBegin= parseYYYMMDD(cmd.getOptionValue("s"));
				if(cmd.hasOption("e")) {
					dateEnd = parseYYYMMDD(cmd.getOptionValue("e"));
					}
				}
			else if(cmd.hasOption("e")) {
				dateEnd= parseYYYMMDD(cmd.getOptionValue("e"));
				if(cmd.hasOption("s")) {
					dateBegin = parseYYYMMDD(cmd.getOptionValue("s"));
					}
				else
					{
					dateBegin = parseYYYMMDD(MIN_DATE_STR);
					}
				}
			else
				{
				dateBegin = parseYYYMMDD(MIN_DATE_STR);
				}
			
			HttpHost proxy=new HttpHost("cache.ha.univ-nantes.fr", 3128);
			client = HttpClientBuilder.create().
					setProxy(proxy).
					setUserAgent("Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:47.0) Gecko/20100101 Firefox/47.0").
					build();
			System.out.println("create table if not exists wrecord ("
					+ "placeId VARCHAR(10) NOT NULL,"
					+ "date  VARCHAR(10) NOT NULL,"
					+ "minT REAL,minTu VARCHAR(10),"
					+ "maxT REAL,maxTu VARCHAR(10),"
					+ "sunlight REAL ,sunlightu VARCHAR(10),"
					+ "rain REAL,rainu VARCHAR(10));");
			System.out.println("create table if not exists wech ("
					+ "placeId VARCHAR(10) NOT NULL,"
					+ "date  VARCHAR(10) NOT NULL,"
					+ "evtwhen REAL,evtwhenu VARCHAR(10),"
					+ "evtwhat VARCHAR(20));");
			System.out.println("BEGIN TRANSACTION;");
			do {
				for(String placeId:placeIds) {
					scan(client,placeId,dateBegin);
					Thread.sleep(rand.nextInt(1+waitMilliSecs));
				}
				
				dateBegin.add(Calendar.DAY_OF_MONTH, 1);
			} while(dateEnd.after(dateBegin));
	
			System.out.println("COMMIT;");
			client.close();client=null;
		return 0;
		}	 
		catch(Exception err) {
			err.printStackTrace();
		return -1;
		}
		finally {
			IOUtils.close(client);
		}
	}

	
	protected void scan(final CloseableHttpClient client,final String placeId,GregorianCalendar cal) throws IOException {
		CloseableHttpResponse response = null;
		InputStream bodyIn =null;
		try 
		{
			
			Record record=new Record(placeId,cal);
			String url = record.getURL();//"http://www.meteofrance.com/climat/meteo-date-passee?lieuId=441090&lieuType=VILLE_FRANCE&date=13-07-2016";
			LOG.info(url);
			response = client.execute(new HttpGet(url));
			bodyIn = response.getEntity().getContent();
			final Tidy tidy = new Tidy();
			tidy.setInputEncoding("ISO-8859-1");
			tidy.setXmlOut(true);
			tidy.setShowErrors(0);
			tidy.setQuiet(true);
			tidy.setShowWarnings(false);
			Document dom = tidy.parseDOM(bodyIn,null);
			bodyIn.close();bodyIn=null;
			record.scan(dom);
			//TransformerFactory.newInstance().newTransformer().transform(new DOMSource(dom), new StreamResult(System.err));
			
			
			response.close();response=null;
			if(!(record.temperatureMax==null &&
				 record.temperatureMin==null && 
				 record.sunlight==null && 
				 record.rain==null && 
				 record.echeances.isEmpty())) {
				record.sql(System.out);
				}
		}	 
		
		finally {
			IOUtils.close(bodyIn);
			IOUtils.close(response);
		}
	}
public static void main(String[] args) {
	new WeatherArchive().instanceMainWithExit(args);
}
}
