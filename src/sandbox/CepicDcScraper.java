package sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import com.beust.jcommander.Parameter;

public class CepicDcScraper extends Launcher {
	private static final Logger LOG = Logger.builder(CepicDcScraper.class).build();
	private static final int YEAR_START=1979;
	private static final int YEAR_END=2015;
	private static String ZONES_GEO[]={"06088","13055","31555","33063","34172","35238","44109","59350","67482"};
	private static final String default_header="Code CIM ; Libellé ; Sexe ;'Total;'<1;'1-4;'5-14;'15-24;'25-34;'35-44;'45-54;'55-64;'65-74;'75-84;'85-94;'95+;";

	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private File cookieStoreFile  = null;
	
	private CloseableHttpClient client = null;

	
	private void run(final String codeGeo,final int curr_year,PrintWriter out) {
		LOG.info("scanning "+codeGeo+" "+curr_year);
		CloseableHttpResponse resp=null;
		InputStream in=null;
		BufferedReader reader=null;
		final Pattern semicolon=Pattern.compile("[;]");
		try {
			HttpPost post = new HttpPost("http://www.cepidc.inserm.fr/cgi-bin/broker.exe");
			post.setHeader("Connection", "keep-alive");
			post.setHeader("Upgrade-Insecure-Requests", "1");
			
			final List<NameValuePair> params = new ArrayList<NameValuePair>();
			
			params.add(new BasicNameValuePair("NOMEN","910"));
			params.add(new BasicNameValuePair("AXEGEO","V"));
			params.add(new BasicNameValuePair("PREV_AXEGEO","V"));
			params.add(new BasicNameValuePair("CODE_GEO",codeGeo));
			params.add(new BasicNameValuePair("CAUSE","00"));
			params.add(new BasicNameValuePair("SEC_CAUSE","00"));
			params.add(new BasicNameValuePair("AN_DEB",String.valueOf(YEAR_START)));
			params.add(new BasicNameValuePair("AN_FIN",String.valueOf(YEAR_END)));
			params.add(new BasicNameValuePair("curr_year",String.valueOf(curr_year)));
			params.add(new BasicNameValuePair("TYPE","E"));
			params.add(new BasicNameValuePair("TYPE_SORTIE","X"));
			params.add(new BasicNameValuePair("TCAUSE","S"));
			params.add(new BasicNameValuePair("YEAR_MOOVE",""));
			params.add(new BasicNameValuePair("_SERVICE","inserm"));
			params.add(new BasicNameValuePair("_PROGRAM","INTLIB.GENERAL.ET_OUTPUT_GEN.SCL"));
			params.add(new BasicNameValuePair("_DEBUG","0"));

			
			post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			
			
			
			resp = this.client.execute(post);
			if(resp.getStatusLine().getStatusCode()!=200) {
				LOG.error("cannot fetch data. "+resp.getStatusLine());
				return;
				}
			in = resp.getEntity().getContent();
			reader = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));
			String line;
			boolean in_data=false;
			String zone=String.valueOf(codeGeo);
			while((line=reader.readLine())!=null)
				{
				if(line.isEmpty())
					{
					if(in_data) break;
					}
				if(line.startsWith("Code CIM"))
					{
					if(in_data) break;
					if(!line.equals(default_header)) {
						LOG.error("not valid header "+line);
						return;
						}
					in_data=true;
					continue;
					}
				if(!in_data) {
					if(line.startsWith("Zone:"))
						{
						zone=line.substring(5).trim();
						}
					continue;
				}
				if(!line.startsWith("'")) continue;
				// https://stackoverflow.com/questions/3322152
				line = Normalizer.normalize(line, Normalizer.Form.NFD).
						replaceAll("\\p{InCombiningDiacriticalMarks}+", "").
						replaceAll("\u0092","'");
				
				final String tokens[]=semicolon.split(line);
				if(tokens[2].equals("T")) continue; //total H/F
				
				out.print(zone);
				out.print("\t");
				out.print(curr_year);
				
				
				for(int i=0;i< tokens.length;++i)
					{
					if(tokens[i].startsWith("'")) tokens[i]=tokens[i].substring(1);					out.print("\t");
					
					out.print(tokens[i]);
					}
				out.println();
				}
			reader.close();
			}
		catch(final IOException err) {
			LOG.error(err);
			}
		finally
			{
			IOUtils.close(reader);
			IOUtils.close(in);
			IOUtils.close(resp);
			}
		}
		
	
	
	@Override
	public int doWork(final List<String> args) {
		try
			{
			
			final HttpClientBuilder builder = HttpClientBuilder.create();
			final String proxyH = System.getProperty("http.proxyHost");
			final String proxyP = System.getProperty("http.proxyPort");
			if(proxyH!=null && proxyP!=null && 
					!proxyH.trim().isEmpty() && 
					!proxyP.trim().isEmpty())
				{
				builder.setProxy(new HttpHost(proxyH, Integer.parseInt(proxyP)));
				}
			builder.setUserAgent(IOUtils.getUserAgent());
			if(this.cookieStoreFile!=null) {
				final BasicCookieStore cookies = CookieStoreUtils.readTsv(this.cookieStoreFile);
				builder.setDefaultCookieStore(cookies);
			}
			
			this.client = builder.build();
			
			final PrintWriter out=new PrintWriter(System.out);
			out.println("CodeGeo\tyear\tCode_CIM\tLibelle\tSexe\tTotal\tLT_1\t1_4\t5_14\t15_24\t25_34\t35_44\t45_54\t55_64\t65_74\t75_84\t85_94\tGT_95");
			for(final String region:ZONES_GEO) {
				for(int y=YEAR_START;y<= YEAR_END;++y)
					{
					run(region,y,out);
					if(out.checkError()) break;
					}
				if(out.checkError()) break;
				}
			out.flush();
			return 0;
			}
		catch(final Exception err)
			{
			LOG.error(err);
			return -1;
			}
		finally
			{
			IOUtils.close(this.client);
			}
		
		}	
	public static void main(final String[] args) {
		new CepicDcScraper().instanceMainWithExit(args);

	}

}
