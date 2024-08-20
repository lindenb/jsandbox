package sandbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class TsvToSchema
	{
	private long countLines=0;
	private char delim='\t';
	private List<Column> columns=null;
	private static enum Type { BOOLEAN,BYTE,CHAR,SHORT,INT,BIGINTEGER,LONG,FLOAT,DOUBLE,BIGDECIMAL,STRING};
	private static class Column
		{
		String name;
		Type type=Type.BOOLEAN;
		boolean signed=false;
		boolean nillable=false;
		boolean empty=false;
		
		Set<String> values=new HashSet<String>();
		int length=0;
		}
	
	private void dump() throws XMLStreamException
		{
		XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
		XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out,"UTF-8");
		w.writeStartDocument("UTF-8","1.0");
		w.writeStartElement("schema");
		w.writeAttribute("columns", String.valueOf(this.columns.size()));
		w.writeAttribute("rows", String.valueOf(this.countLines));
		for(Column c:this.columns)
			{
			w.writeStartElement("column");
			w.writeAttribute("name", c.name);
			w.writeAttribute("nillable", String.valueOf(c.nillable));
			w.writeAttribute("empty", String.valueOf(c.empty));
			if(c.type!=Type.STRING && c.type!=Type.CHAR)
				{
				w.writeAttribute("unsigned", String.valueOf(!c.signed));
				}

			
			switch(c.type)
				{
				case BOOLEAN: w.writeAttribute("type","java.lang.Boolean");break;
				case BYTE: w.writeAttribute("type","java.lang.Byte");break;
				case CHAR: w.writeAttribute("type","java.lang.Character");break;
				case SHORT: w.writeAttribute("type","java.lang.Short");break;
				case INT: w.writeAttribute("type","java.lang.Integer");break;
				case LONG: w.writeAttribute("type","java.lang.Long");break;
				case BIGINTEGER: w.writeAttribute("type","java.math.BigInteger");break;
				case FLOAT: w.writeAttribute("type","java.lang.Float");break;
				case DOUBLE: w.writeAttribute("type","java.lang.Double");break;
				case BIGDECIMAL: w.writeAttribute("type","java.math.BigDecimal");break;
				default: w.writeAttribute("type","java.lang.String");break;
				}
			if(c.type.equals(Type.STRING) && c.values.size()<256)
				{
				for(String s:c.values)
					{
					w.writeStartElement("enum");
					w.writeCharacters(s);
					w.writeEndElement();
					}
				}
			
			w.writeEndElement();
			}
		
		w.writeEndElement();
		w.writeEndDocument();
		}
	
	private void scan(BufferedReader in) throws IOException
		{
		
		
		String line;
		while((line=in.readLine())!=null)
			{
			++countLines;
			List<String> tokens=new ArrayList<String>();
			int prev=0;
			int t=0;
			while(t<=line.length())
				{
				if(t==line.length() ||  this.delim==line.charAt(t))
					{
					tokens.add(line.substring(prev,t));
					if(t==line.length()) break;
					prev=t+1;
					t=prev;
					continue;
					}
				++t;
				}
			if(columns==null)
				{
				columns=new ArrayList<TsvToSchema.Column>(tokens.size());
				for(int i=0;i< tokens.size();++i)
					{
					Column c=new Column();
					c.name="column"+(i+1);
					columns.add(c);
					}
				}
			else while(this.columns.size()< tokens.size());
				{
				Column c=new Column();
				c.name="column"+(this.columns.size()+1);
				c.empty=true;
				columns.add(c);
				}
			
					
				
				
			for(int i=0;i< columns.size();++i)
				{
				Column c=columns.get(i);
				String v=tokens.get(i);
				if(v.equalsIgnoreCase("null") || v.equalsIgnoreCase("nill"))
					{
					c.nillable=true;
					continue;
					}
				else if(v.isEmpty())
					{
					c.empty=true;
					continue;
					}
				
				if(c.values.size()<255)
					{
					c.values.add(v);
					}
				
				
				for(;;)
					{
					Type old=c.type;
					switch(c.type)
						{
						case BOOLEAN:
								{
								if(!(v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false")))
									{
									c.type=Type.BYTE;
									}
								break;
								}
						case BYTE:
							{
							try	{	
								byte b=Byte.parseByte(v);
								if(b<0) c.signed=true;
								}
							catch(Exception err)
								{
								c.type=Type.CHAR;
								}
							break;
							}
						case CHAR:
							{
							if(v.length()!=1) 
								{
								c.type=Type.SHORT;
								break;
								}
							break;
							}
						case SHORT:
							{
							try	{	
								short o=Short.parseShort(v);
								if(o<0) c.signed=true;
								}
							catch(Exception err)
								{
								c.type=Type.INT;
								}
							break;
							}
						case INT:
							{
							try	{	
								int o=Integer.parseInt(v);
								if(o<0) c.signed=true;
								}
							catch(Exception err)
								{
								c.type=Type.LONG;
								}
							break;
							}
						case LONG:
							{
							try	{	
								long o=Long.parseLong(v);
								if(o<0) c.signed=true;
								}
							catch(Exception err)
								{
								c.type=Type.BIGINTEGER;
								}
							break;
							}
						case BIGINTEGER:
							{
							try	{	
								BigInteger o=new BigInteger(v);
								if(o.compareTo(BigInteger.ZERO)<0) c.signed=true;
								c.length=Math.max(c.length, v.length());
								}
							catch(Exception err)
								{
								c.type=Type.FLOAT;
								}
							break;
							}
						case FLOAT:
							{
							try	{	
								float o=Float.parseFloat(v);
								if(o<0) c.signed=true;
								}
							catch(Exception err)
								{
								c.type=Type.DOUBLE;
								}
							break;
							}
						case DOUBLE:
							{
							try	{	
								double o=Double.parseDouble(v);
								if(o<0) c.signed=true;
								}
							catch(Exception err)
								{
								c.type=Type.BIGDECIMAL;
								}
							break;
							}
						case BIGDECIMAL:
							{
							try	{	
								BigDecimal o=new BigDecimal(v);
								if(o.compareTo(BigDecimal.ZERO)<0) c.signed=true;
								c.length=Math.max(c.length, v.length());
								}
							catch(Exception err)
								{
								c.type=Type.STRING;
								}
							break;
							}
						case STRING://through
						default:
							{
							
							c.length=Math.max(c.length, v.length());
							}
						}
					if(old.equals(c.type)) break;
					}
				}
			}
		System.out.println("create table __TABLE__NAME\n\t(");
		for(int i=0;i< this.columns.size();++i)
			{
			System.out.print("\t");
			if(i+1<this.columns.size()) System.out.print(",");
			Column c=this.columns.get(i);
			System.out.print(c.name);
			System.out.print(" ");
			switch(c.type)
				{
				case BOOLEAN:
				case SHORT:
					{
					System.out.print(" SMALLINT");
					break;
					}
				case CHAR:
					{
					break;	
					}
				case INT:
					{
					System.out.print(" INTEGER");
					break;
					}
				case LONG:
					{
					System.out.print(" BIGINT");
					break;
					}
				case BIGINTEGER:
					{
					System.out.print(" VARCHAR("+(c.length+")"));
					break;
					}
				case FLOAT:
					{
					System.out.print(" FLOAT");
					break;
					}
				case DOUBLE:
					{
					System.out.print(" DOUBLE PRECISION");
					break;
					}
				case BIGDECIMAL:
					{
					System.out.print(" VARCHAR("+(c.length+")"));
					break;
					}
				default:
					{
					System.out.print(" VARCHAR("+(c.length+")"));
					}
				}
			System.out.println();
			}
		System.out.println("\t);");
		}
	
	/* private Set<Integer> split(String s)
		{
		Set<Integer> set=new HashSet<Integer>();
		for(String i:s.split("[,]"))
			{
			if(s.isEmpty()) continue;
			int index=Integer.parseInt(i);
			set.add(index);
			while(this.columns.size() < index)
				{
				Column c=new Column();
				c.name="column"+index;
				this.columns.add(c);
				}
			}
		
		return set;
		}*/
	
	
	
	private void run(String[] args)
			throws Exception
			{
			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Pierre Lindenbaum PhD. 2013");
					System.err.println("Options:");
					System.err.println(" -h help; This screen.");
					return;
					}
				else if(args[optind].equals("-d") && optind+1<args.length)
					{
					this.delim=args[++optind].charAt(0);
					}
				else if(args[optind].equals("--"))
					{
					optind++;
					break;
					}
				else if(args[optind].startsWith("-"))
					{
					System.err.println("Unknown option "+args[optind]);
					return;
					}
				else 
					{
					break;
					}
				++optind;
				}
			scan(new BufferedReader(new InputStreamReader(System.in)));
			dump();
			}
		public static void main(String[] args) throws Exception
			{
			new TsvToSchema().run(args);
			}
		
	}
