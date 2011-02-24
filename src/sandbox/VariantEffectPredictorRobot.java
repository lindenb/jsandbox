/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	Feb-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Motivation:
 *  	Scrapper for the Ensembl Variant Effect Predictor
 * Requires:
 * 		apache httpclient library
 * Compilation:
 *       ant variantpredictor
 */
package sandbox;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.InvalidRedirectLocationException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 
 * VariantEffectPredictorRobot
 *
 */
public class VariantEffectPredictorRobot
	{
	private final URL BASE=new URL("http://www.ensembl.org/Homo_sapiens/UserData/UploadVariations");
	private static final String MOZILLA_CLIENT="Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8";
	private static final int BATCH_SIZE=750;
	private static Logger LOG=Logger.getLogger("ensembl.robot");
	private DocumentBuilder builder;
	private XPath xpath=null;
	/** httpClient */
	private HttpClient httpClient=null;
	/** id generator */
	private long id_generator=System.currentTimeMillis();
	private boolean printEnst=false;
	
	private static class Mutation
		{
		
		String tokens[];
		Set<String> genes=new HashSet<String>();
		Set<String> transcripts=new HashSet<String>();
		Set<String> consequences=new HashSet<String>();
		Set<String> ids=new HashSet<String>();
		
		Mutation(String tokens[])
			{
			this.tokens=tokens;
			for(String s:tokens[2].split("[,]"))
				{
				if(!s.equals("."))
					{
					this.ids.add(s);
					}
				}
			}
		
		public String getChrom()
			{
			String s=tokens[0];
			if(s.startsWith("chr")) s=s.substring(3);
			return s;
			}
		
		private Object[] compute()
			{
			String alt=tokens[3].toUpperCase();
			String ref=tokens[4].toUpperCase();
			boolean is_indel=false;
			if( alt.startsWith("D") ||
				alt.startsWith("I") ||	
				ref.length()!=alt.length())
				{
				is_indel=true;
				}
			int start=Integer.parseInt(tokens[1]);
			int end=start;
			end+= (ref.length()-1);
	        
			if(is_indel)
				{
				alt=alt.substring(1);
				ref=ref.substring(1);
				start++;
				if(alt.isEmpty())
					{
					alt="-";
					}
				if(ref.isEmpty())
					{
					ref="-";
					}
				}
			return new Object[]{start,end,alt,ref};
			}
		
		public int getStart()
			{
			return (Integer)compute()[0];
			}
		public int getEnd()
			{
			return (Integer)compute()[1];
			}
		public String getAlt()
			{
			return (String)compute()[2];
			}
		public String getRef()
			{
			return (String)compute()[3];
			}
		
		public String getSubmit()
			{
			return new StringBuilder(getChrom()).
					append('\t').
					append(getStart()).
					append('\t').
					append(getEnd()).
					append('\t').
					append(getRef()).
					append('/').
					append(getAlt()).
					append('\t').
					append('+').
					toString()
					;
			}
		
		public String getSignature()
			{
			StringBuilder b=new StringBuilder(getChrom()).append('_');
			int start=getStart();
			int end=getEnd();
			if(start==end)
				{
				b.append(start);
				}
			else
				{
				b.append(start);
				b.append('-');
				b.append(end);
				}
			b.append('_');
			b.append(getRef());
			b.append('/');
			b.append(getAlt());
			return b.toString();
			}
		}
	

	
	private VariantEffectPredictorRobot() throws Exception
		{
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		factory.setCoalescing(true);
		factory.setNamespaceAware(false);
		factory.setExpandEntityReferences(true);
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		factory.setIgnoringElementContentWhitespace(true);
		this.builder=factory.newDocumentBuilder();
		this.builder.setEntityResolver(new EntityResolver()
			{
			@Override
			public InputSource resolveEntity(String publicId, String systemId)
					throws SAXException, IOException
				{
				LOG.info("trying to resolve "+publicId);
				return new InputSource(new StringReader(""));
				}
			});
		
		this.xpath=XPathFactory.newInstance().newXPath();
		
		this.httpClient= new HttpClient();
		this.httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(10000);
		}
	
	/** submit data to ensembl */
	private void submitOne(List<Mutation> mutations) throws Exception
		{
		StringBuilder lines=new StringBuilder();
		for(Mutation m:mutations)
			{
			lines.append(m.getSubmit()).append('\n');
			}
		LOG.info(lines.toString());
		long request_id=(id_generator++);
		PostMethod postMethod =null;
		GetMethod methodGet =null;
		LOG.info("Aquire cookies");
		try
			{
			URL url=new URL(BASE,"/Homo_sapiens/UserData/UploadVariations");
			LOG.info(url.toString());
			methodGet=new GetMethod(url.toString());
			methodGet.setRequestHeader("User-Agent", MOZILLA_CLIENT);
			int status = this.httpClient.executeMethod(methodGet);
			if(status!=HttpStatus.SC_OK)
				{
				throw new HttpException("post returned http-code="+status+" "+HttpStatus.SC_OK+" "+url);
				}
			System.err.println(Arrays.asList(httpClient.getState().getCookies()));
			}
		finally
			{
			if(methodGet!=null) methodGet.releaseConnection();
			methodGet=null;
			}
		wait(2);
		String location=null;
		try
			{
			URL url=new URL(BASE,"/Homo_sapiens/UserData/CheckConvert");
			LOG.info(url.toString());
			postMethod=new PostMethod(url.toString());
			postMethod.addParameter(new NameValuePair("consequence_mapper", "1"));
			postMethod.addParameter(new NameValuePair("upload_format", "snp"));
			postMethod.addParameter(new NameValuePair("variation_limit", "750"));
			postMethod.addParameter(new NameValuePair("uploadto","iframe"));
			postMethod.addParameter(new NameValuePair("species","Homo_sapiens"));
			postMethod.addParameter(new NameValuePair("name","DataSet"+request_id));
			postMethod.addParameter(new NameValuePair("file",""));
			postMethod.addParameter(new NameValuePair("url",""));
			postMethod.addParameter(new NameValuePair("submit","Next >"));
			postMethod.addParameter(new NameValuePair("species","Homo_sapiens"));
			postMethod.addParameter(new NameValuePair("text",lines.toString()));
			postMethod.addParameter(new NameValuePair("lrg",""));
			postMethod.addParameter(new NameValuePair("pt",""));
			postMethod.addParameter(new NameValuePair("sv",""));
			postMethod.addParameter(new NameValuePair("vf",""));
			postMethod.addParameter(new NameValuePair("db","core"));
			postMethod.addParameter(new NameValuePair("r",""));
			postMethod.addParameter(new NameValuePair("vdb",""));
			postMethod.addParameter(new NameValuePair("fdb",""));
			postMethod.addParameter(new NameValuePair("m",""));
			postMethod.addParameter(new NameValuePair("v",""));
			postMethod.addParameter(new NameValuePair("rf",""));
			postMethod.addParameter(new NameValuePair("h",""));
			postMethod.addParameter(new NameValuePair("gt",""));
			postMethod.addParameter(new NameValuePair("g",""));
			postMethod.addParameter(new NameValuePair("t",""));
			int status = httpClient.executeMethod(postMethod);
			System.err.println(Arrays.asList(postMethod.getResponseHeaders()));
			if(status!=302)
				{
				throw new HttpException("post returned http-code="+status+" "+HttpStatus.SC_OK+" "+url);
				}
			Header header=postMethod.getResponseHeader("Location");
			if(header==null)
				{
				throw new HttpException("location header missing");
				}
			location=header.getValue();
			int  comma=location.indexOf(',');
			if(comma!=-1) location=location.substring(0,comma);
			}
		finally
			{
			if(postMethod!=null) postMethod.releaseConnection();
			postMethod=null;
			}
		
		if(location==null)
			{
			throw new HttpException("location is null");
			}
		LOG.info("location is "+location);
		try
			{
			URL url=new URL(BASE,location);
			location=null;
			LOG.info(url.toString());
			methodGet=new GetMethod(url.toString());
			methodGet.setRequestHeader("User-Agent", MOZILLA_CLIENT);
			int status = this.httpClient.executeMethod(methodGet);
			if(status!=HttpStatus.SC_OK)
				{
				throw new HttpException("post returned http-code="+status+" "+HttpStatus.SC_OK+" "+url);
				}
			System.err.println(Arrays.asList(methodGet.getResponseHeaders()));
			String html=methodGet.getResponseBodyAsString();
			int i=html.indexOf("'/Homo_sapiens");
			if(i==-1)
				{
				throw new HttpException("no /Homo_sapens in "+html);
				}
			int j=html.indexOf("'",i+1);
			if(j==-1)
				{
				throw new HttpException("no \\' in "+html);
				}
			location=html.substring(i+1,j);
			}
		finally
			{
			if(methodGet!=null) methodGet.releaseConnection();
			methodGet=null;
			}
		
		if(location==null)
			{
			throw new HttpException("location is null");
			}
		wait(5);
		try
			{
			URL url=new URL(BASE,location);
			location=null;
			LOG.info(url.toString());
			methodGet=new GetMethod(url.toString());
			methodGet.setRequestHeader("User-Agent", MOZILLA_CLIENT);
			int status = this.httpClient.executeMethod(methodGet);
			if(status!=HttpStatus.SC_OK)
				{
				throw new HttpException("post returned http-code="+status+" "+HttpStatus.SC_OK+" "+url);
				}
			InputStream in=methodGet.getResponseBodyAsStream();
			Document dom=builder.parse(in);
			in.close();
			
			Element anchor=(Element)xpath.evaluate("//a[@class='modal_link'][@href][text()='Text']", dom, XPathConstants.NODE);
			if(anchor==null)
				{
				TransformerFactory factory=TransformerFactory.newInstance();
				Transformer transformer=factory.newTransformer();
				transformer.transform(new DOMSource(dom), new StreamResult(System.err));
				throw new HttpException("anchor1 is null in "+url);
				}
			location=anchor.getAttribute("href");
			}
		finally
			{
			if(methodGet!=null) methodGet.releaseConnection();
			methodGet=null;
			}
		
		if(location==null)
			{
			throw new HttpException("location is null");
			}
		wait(5);
		try
			{
			URL url=new URL(BASE,location);
			location=null;
			LOG.info(url.toString());
			methodGet=new GetMethod(url.toString());
			methodGet.setRequestHeader("User-Agent", MOZILLA_CLIENT);
			int status = this.httpClient.executeMethod(methodGet);
			if(status!=HttpStatus.SC_OK)
				{
				throw new HttpException("post returned http-code="+status+" "+HttpStatus.SC_OK+" "+url);
				}
			InputStream in=methodGet.getResponseBodyAsStream();
			Document dom=builder.parse(in);
			in.close();
			
			Element anchor=(Element)xpath.evaluate("//a[@href][text()='DataSet"+request_id+".txt']", dom, XPathConstants.NODE);
			if(anchor==null)
				{
				TransformerFactory factory=TransformerFactory.newInstance();
				Transformer transformer=factory.newTransformer();
				transformer.transform(new DOMSource(dom), new StreamResult(System.err));
				
				throw new HttpException("anchor2 is null in "+url);
				}
			location=anchor.getAttribute("href");
			}
		finally
			{
			if(methodGet!=null) methodGet.releaseConnection();
			methodGet=null;
			}
		
		if(location==null)
			{
			throw new HttpException("location is null");
			}
		
		try
			{
			URL url=new URL(BASE,location);
			location=null;
			LOG.info(url.toString());
			methodGet=new GetMethod(url.toString());
			methodGet.setRequestHeader("User-Agent", MOZILLA_CLIENT);
			int status = this.httpClient.executeMethod(methodGet);
			if(status!=HttpStatus.SC_OK)
				{
				throw new HttpException("post returned http-code="+status+" "+HttpStatus.SC_OK+" "+url);
				}
			BufferedReader in=new BufferedReader(new InputStreamReader(methodGet.getResponseBodyAsStream()));
			String line;
			while((line=in.readLine())!=null)
				{
				LOG.info(line);
				if(line.startsWith("Uploaded Variation")) continue;
				String tokens[]=line.split("[\t]");
				for(int i=0;i< tokens.length;++i)
					{
					if(tokens[i].equals("-")) tokens[i]="";
					}
				for(Mutation mut: mutations)
					{
					if(mut.getSignature().equalsIgnoreCase(tokens[0]))
						{
						LOG.info("OK found "+mut.getSubmit());
						if(!tokens[2].isEmpty()) mut.genes.add(tokens[2]);
						if(!tokens[3].isEmpty()) mut.transcripts.add(tokens[3]);
						if(!tokens[4].isEmpty()) mut.consequences.add(tokens[4]);
						if(!tokens[8].isEmpty()) mut.ids.add(tokens[8]);
						}
					}
				
				}
			in.close();
			}
		finally
			{
			if(methodGet!=null) methodGet.releaseConnection();
			methodGet=null;
			}
		
		//cleanup
		Set<String> deleteURLS=new HashSet<String>();
		try
			{
			URL url=new URL(BASE,"/Homo_sapiens/UserData/ManageData?db=core");
			location=null;
			LOG.info(url.toString());
			methodGet=new GetMethod(url.toString());
			methodGet.setRequestHeader("User-Agent", MOZILLA_CLIENT);
			int status = this.httpClient.executeMethod(methodGet);
			if(status!=HttpStatus.SC_OK)
				{
				throw new HttpException("post returned http-code="+status+" "+HttpStatus.SC_OK+" "+url);
				}
			String html=methodGet.getResponseBodyAsString();
			int n=0;
			for(;;)
				{
				n=html.indexOf("href=\"",n);
				if(n==-1) break;
				n+=6;
				int n2=html.indexOf("\"",n);
				if(n2==-1) break;
				String href=html.substring(n,n2);
				n=n2+1;
				if(!href.contains("DeleteUpload")) continue;
				LOG.info(href);
				deleteURLS.add(href);
				}
			}
		finally
			{
			if(methodGet!=null) methodGet.releaseConnection();
			methodGet=null;
			}
		
		LOG.info(deleteURLS.toString());
		
		for(String deleteURL: deleteURLS)
			{
			try{
				URL url=new URL(BASE,deleteURL);
				LOG.info("deleting "+url.toString());
				methodGet=new GetMethod(url.toString());
				methodGet.setRequestHeader("User-Agent", MOZILLA_CLIENT);
				this.httpClient.executeMethod(methodGet);
				}
			catch(InvalidRedirectLocationException err)
				{
				//just ignore
				}
			finally
				{
				if(methodGet!=null) methodGet.releaseConnection();
				methodGet=null;
				}
			}

		wait(30);
		}
	
	private void wait(int seconds)
		{
		LOG.info("wait "+seconds+" secs");
		try{
		Thread.sleep(1000*seconds);//wait 
		} catch(Throwable err) {}
		}
	
	private void submit(List<Mutation> mutations)
		throws Exception
		{
		Exception lastException=null;
		for(int i=0;i< 10;++i)
			{
			lastException=null;
			try
				{
				submitOne(mutations);
				break;
				}
			catch (Exception e)
				{
				LOG.info("err:"+e.getMessage());
				lastException=e;
				wait(60);
				}
			}
		if(lastException!=null) throw new RuntimeException(lastException);
		for(Mutation m:mutations)
			{
			System.out.print(m.tokens[0]);
			System.out.print("\t");
			System.out.print(m.tokens[1]);
			System.out.print("\t");
			if(m.ids.isEmpty())
				{
				System.out.print(".");
				}
			else
				{
				boolean first=true;
				for(String s:m.ids)
					{
					if(!first) System.out.print(",");
					first=false;
					System.out.print(s);
					}
				}
			System.out.print("\t");
			System.out.print(m.tokens[3]);
			System.out.print("\t");
			System.out.print(m.tokens[4]);
			System.out.print("\t");
			System.out.print(m.tokens[5]);//QUAL
			System.out.print("\t");
			System.out.print(m.tokens[6]);//FILTER
			System.out.print("\t");
			System.out.print(m.tokens[7]);//INFO
			if(!m.genes.isEmpty())
				{
				System.out.print(";ENSG=");
				boolean first=true;
				for(String s:m.genes)
					{
					if(!first) System.out.print(",");
					first=false;
					System.out.print(s);
					}
				}
			if(!m.transcripts.isEmpty() && printEnst)
				{
				System.out.print(";ENST=");
				boolean first=true;
				for(String s:m.transcripts)
					{
					if(!first) System.out.print(",");
					first=false;
					System.out.print(s);
					}
				}
			if(!m.consequences.isEmpty())
				{
				System.out.print(";GC=");
				boolean first=true;
				for(String s:m.consequences)
					{
					if(!first) System.out.print(",");
					first=false;
					System.out.print(s);
					}
				}
			for(int i=8;i< m.tokens.length;++i)
				{
				System.out.print("\t");
				System.out.print(m.tokens[i]);//INFO
				}
			System.out.println();
			}
		}
	
	private void runVCF(BufferedReader in) throws Exception
		{
		LOG.info("run...");
		List<Mutation> mutations=new ArrayList<Mutation>(BATCH_SIZE);
		int nLines=0;
		String line;
		while((line=in.readLine())!=null)
			{
			if(line.startsWith("#"))
				{
				if(line.startsWith("#CHROM"))
					{
					
					}
				System.out.println(line);
				continue;
				}
			LOG.info(line);
			Mutation mutation=new Mutation(line.split("\t"));
			mutations.add(mutation);
			
			if(mutations.size()==BATCH_SIZE)
				{
				submit(mutations);
				nLines=0;
				mutations.clear();
				}
			}
		if(nLines!=0)
			{
			submit(mutations);
			}
		LOG.info("end run...");
		}
	
	public static void main(String[] args)
		{
		LOG.setLevel(Level.OFF);
		try {
			VariantEffectPredictorRobot app=new VariantEffectPredictorRobot();
			int optind=0;
			while(optind<args.length)
				{
				if(args[optind].equals("-h"))
					{
					System.out.println("Variant Effect Predictor Robot.");
					System.out.println("Pierre Lindenbaum PhD 2011.");
					System.out.println("(stdin|file.vcf)");
					return;
					}
				else if(args[optind].equals("-L"))
					{
					LOG.setLevel(Level.parse(args[++optind]));
					}
				else if(args[optind].equals("--"))
					{
					optind++;
					break;
					}
				else if(args[optind].startsWith("-"))
					{
					System.err.println("Unnown option: "+args[optind]);
					return;
					}
				else
					{
					break;
					}
				++optind;
				}
			if(optind==args.length)
				{
				app.runVCF(new BufferedReader(new InputStreamReader(System.in)));
				}
			else if(optind+1==args.length)
				{
				String inputName=args[optind++];
				LOG.info(inputName);
				BufferedReader in=new BufferedReader(new FileReader(inputName));
				app.runVCF(in);
				in.close();
				}
			else
				{
				System.err.println("Illegal number of arguments.");
				return;
				}
		} 
	catch (Exception e)
		{
		e.printStackTrace();
		}
		}
	

	}
