/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	May-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Motivation:
 *   	create pivot table
 * Compilation:
 *        ant pivot
 * Usage:
 *        java -jar pivot.jar -h
 */
package sandbox;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


@SuppressWarnings({"rawtypes","unchecked"})
public class Pivot
	{
	private String htmlStylsheet=null;
	private boolean print_horizontal_total=true;
	private boolean print_vertical_total=true;
	@SuppressWarnings("unused")
	private String nilValue="NULL";
	boolean firstRowIsHeader=true;
	private Pattern delimiter=Pattern.compile("[\t]");
	private String headers[]=null;
	private List<Object[]> spreadsheet=new ArrayList<Object[]>();

	private Column aggregateColumn=null;
	//
	private List<Column> allColumns=new ArrayList<Pivot.Column>();
	private List<Column> leftColumns=new ArrayList<Pivot.Column>();
	private Map<Token,List<Integer>> leftTokens=new TreeMap<Token,List<Integer>>();
	private List<Column> topColumns=new ArrayList<Pivot.Column>();
	private Map<Token,List<Integer>> topTokens=new TreeMap<Token,List<Integer>>();
	private Aggregate aggregates[]=new Aggregate[]
	    {
	    new AggregateCount(),
	    new AggregateMin(),
	    new AggregateMax(),
	    new AggregateDistinct(),
	    new AggregateDistinctCount(),
	    new AggregateSum()
	    };
	private Aggregate selectedAggregate=aggregates[0];

	private static interface Aggregate
		{
		public void reset();
		public String getId();
		public String getDescription();
		public void add(Object o);
		public void write(XMLStreamWriter w) throws XMLStreamException;
		}

	private static class AggregateCount implements Aggregate
		{
		int count=0;
		@Override
		public void reset()
			{
			count=0;
			}

		@Override
		public String getId()
			{
			return "count";
			}

		@Override
		public String getDescription()
			{
			return "Count non-null data";
			}

		@Override
		public void add(Object o)
			{
			count+=(o==null?0:1);
			}

		@Override
		public void write(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeCharacters(String.valueOf(count));
			}
		}


	private class AggregateMin implements Aggregate
		{
		Object value=null;
		@Override
		public void reset()
			{
			value=null;
			}

		@Override
		public String getId()
			{
			return "min";
			}

		@Override
		public String getDescription()
			{
			return "minimum of non-null data";
			}

		@Override
		public void add(Object o)
			{
		    if(o==null) return;
		    if(this.value==null ||
		    	aggregateColumn.comparator.compare(o,this.value)<0)
		    	{
		    	this.value=o;
		    	}
			}

		@Override
		public void write(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeCharacters(String.valueOf(this.value));
			}
		}

	private class AggregateMax implements Aggregate
		{
		Object value=null;
		@Override
		public void reset()
			{
			value=null;
			}

		@Override
		public String getId()
			{
			return "max";
			}

		@Override
		public String getDescription()
			{
			return "maximum of non-null data";
			}

		@Override
		public void add(Object o)
			{
		    if(o==null) return;
		    if(this.value==null ||
		    	aggregateColumn.comparator.compare(o,this.value)>0)
		    	{
		    	this.value=o;
		    	}
			}

		@Override
		public void write(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeCharacters(String.valueOf(this.value));
			}
		}


	private class AggregateDistinct implements Aggregate
		{
		Set<Object> value=null;
		@Override
		public void reset()
			{
			value=new TreeSet<Object>(aggregateColumn.comparator);
			}

		@Override
		public String getId()
			{
			return "distinct";
			}

		@Override
		public String getDescription()
			{
			return "distinct non-null data";
			}

		@Override
		public void add(Object o)
			{
		    if(o==null) return;
		    this.value.add(o);
			}

		@Override
		public void write(XMLStreamWriter w) throws XMLStreamException
			{
			if(this.value.isEmpty()) return;
			w.writeStartElement("ul");
			for(Object v: value)
				{
				w.writeStartElement("li");
				w.writeCharacters(String.valueOf(v));
				w.writeEndElement();
				}
			w.writeEndElement();
			}
		}

	private class AggregateDistinctCount implements Aggregate
		{
		Set<Object> value=null;
		@Override
		public void reset()
			{
			value=new TreeSet<Object>(aggregateColumn.comparator);
			}

		@Override
		public String getId()
			{
			return "count-distinct";
			}

		@Override
		public String getDescription()
			{
			return "count distinct non-null data";
			}

		@Override
		public void add(Object o)
			{
		    if(o==null) return;
		    this.value.add(o);
			}

		@Override
		public void write(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeCharacters(String.valueOf(this.value.size()));
			}
		}

	private class AggregateSum implements Aggregate
		{
		BigDecimal value=null;
		@Override
		public void reset()
			{
			value= new BigDecimal(0);
			}

		@Override
		public String getId()
			{
			return "sum";
			}

		@Override
		public String getDescription()
			{
			return "sum of numeric data";
			}

		@Override
		public void add(Object o)
			{
		    if(o==null || !(o instanceof Number)) return;
		    this.value= this.value.add(new BigDecimal(Number.class.cast(o).doubleValue()));
			}

		@Override
		public void write(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeCharacters(String.valueOf(this.value));
			}
		}


	private static interface StringParser
		{
		public Object parse(String s);
		}

	private class Column
		{
		int index=-1;
		StringParser parser;

		Comparator comparator;
		public String getLabel()
			{
			if(!firstRowIsHeader) return "$"+(index+1);
			return (index>=headers.length  ? "$"+(index+1) : headers[index]);
			}
		public int getIndex()
			{
			return this.index;
			}
		}

	private class Token
		implements Comparable<Token>
		{
		private int nLine=0;
		private List<Column> columns;

		Token(int nLine,List<Column> columns)
			{
			this.nLine=nLine;
			this.columns=columns;
			}

		private Object get(Column c)
			{
			return getValueAt(this.nLine,c.getIndex());
			}



		@Override
		public int compareTo(Token o)
				{
				for(Column c: this.columns)
					{
					Object o1= get(c);
					Object o2= o.get(c);
					if(o1==null)
						{
						if(o2==null) return 0;
						return 1;
						}
					if(o2==null) return -1;
					int i = c.comparator.compare(o1, o2);
					if(i!=0) return i;
					}
				return 0;
				}
		@Override
		public boolean equals(Object obj)
			{
			if(obj==this) return true;
			if(obj==null || !(obj instanceof Token)) return false;
			Token other=Token.class.cast(obj);
			return compareTo(other)==0;
			}
		@Override
		public String toString()
			{
			List<Object> b=new ArrayList<Object>(this.columns.size());
			for(Column c: this.columns)
				{
				b.add(get(c));
				}
			return b.toString();
			}
		}

	private Object getValueAt(int rowIndex,int colIndex)
		{
		Object[] l=spreadsheet.get(rowIndex);
		return (colIndex>=l.length  ? null : l[colIndex]);
		}

	private List<Column> parseColumns(String s)
		{
		 List<Column> array=new ArrayList<Column>();
		 for(String s2:s.split("[,]"))
			 {
			 if(s2.isEmpty()) continue;
			 array.add(parseColumn(s2));
			 }
		 if(array.isEmpty()) throw new IllegalArgumentException("bad columns definitions: "+s);
		 return array;
		}

	private Column defaultColumn(int index0)
		{
		Column column=new Column();
		column.index=index0;
		column.parser=new StringParser()
			{
			@Override
			public Object parse(String s)
				{
				return s;
				}
			};
		column.comparator=new Comparator<String>()
			{
			@Override
			public int compare(String o1, String o2)
				{
				return o1.compareTo(o2);
				}
			};
		return column;
		}

	private Column parseColumn(String s)
		{
		Column column=new Column();
		int i=0;
		while(i<s.length() && Character.isDigit(s.charAt(i)))
			{
			++i;
			}
		if(i==0) throw new IllegalArgumentException("Cannot find column number in "+s);
		column.index = Integer.parseInt(s.substring(0, i));
		if(column.index<=0) throw new IllegalArgumentException("Cannot column index should be >0 in "+s);
		column.index--;
		s=s.substring(i);
		if(s.equals("i"))
			{

			column.parser=new StringParser()
				{
				@Override
				public Object parse(String s)
					{
					return new Integer(s);
					}
				};
			column.comparator=new Comparator<Integer>()
				{
				@Override
				public int compare(Integer o1, Integer o2)
					{
					return o1.compareTo(o2);
					}
				};
			}
		else if(s.equals("s") || s.isEmpty())
			{
			column.parser=new StringParser()
				{
				@Override
				public Object parse(String s)
					{
					return s;
					}
				};
			column.comparator=new Comparator<String>()
				{
				@Override
				public int compare(String o1, String o2)
					{
					return o1.compareTo(o2);
					}
				};
			}
		else if(s.equals("d"))
			{
			column.parser=new StringParser()
				{
				@Override
				public Object parse(String s)
					{
					return new Double(s);
					}
				};
			column.comparator=new Comparator<Double>()
				{
				@Override
				public int compare(Double o1, Double o2)
					{
					return o1.compareTo(o2);
					}
				};
			}
		else if(s.equals("si"))
			{
			column.parser=new StringParser()
				{
				@Override
				public Object parse(String s)
					{
					return s;
					}
				};
			column.comparator=new Comparator<String>()
				{
				@Override
				public int compare(String o1, String o2)
					{
					return o1.compareToIgnoreCase(o2);
					}
				};
			}
		else
			{
			throw new IllegalArgumentException("bad column declaration: "+s);
			}
		return column;
		}

	private boolean isNil(String s)
		{
		return 	s.isEmpty() ||
				s.equalsIgnoreCase("NULL") ||
				s.equalsIgnoreCase("nil") ||
				s.equalsIgnoreCase("N/A") ||
				s.equalsIgnoreCase("NA")
				;
		}

	private void readTable(BufferedReader in) throws IOException
		{
		if(firstRowIsHeader)
			{
			String line=in.readLine();
			if(line==null) throw new IOException("Cannot read first header line");
			this.headers = delimiter.split(line);
			}
		String line;
		while((line=in.readLine())!=null)
			{
			String tokens[]=delimiter.split(line);
			Object array[]=new Object[tokens.length];
			for(int i=0;i< tokens.length;++i)
				{
				if(isNil(tokens[i]) ||
				    allColumns.size()<=i ||
				   allColumns.get(i)==null)
					{
					array[i]=null;
					}
				else
					{
					array[i]=allColumns.get(i).parser.parse(tokens[i]);
					}
				}
			this.spreadsheet.add(array);
			//left
			Token token =new Token(this.spreadsheet.size()-1,leftColumns);
			List<Integer> lines= this.leftTokens.get(token);
			if(lines==null)
				{
				lines=new ArrayList<Integer>();
				this.leftTokens.put(token,lines);
				}
			lines.add(this.spreadsheet.size()-1);

			if(!topColumns.isEmpty())
				{
				token =new Token(this.spreadsheet.size()-1,topColumns);
				lines= this.topTokens.get(token);
				if(lines==null)
					{
					lines=new ArrayList<Integer>();
					this.topTokens.put(token,lines);
					}
				lines.add(this.spreadsheet.size()-1);
				}

			}
		}
	private void aggregate(XMLStreamWriter out,Token left,Token top)
		throws XMLStreamException
		{
		this.selectedAggregate.reset();
		if(left==null && top==null )
			{
			for(int rowIndex=0;rowIndex< this.spreadsheet.size();rowIndex++)
				{
				Object v=getValueAt(rowIndex, this.aggregateColumn.getIndex());
				this.selectedAggregate.add(v);
				}
			}
		else if(left!=null && top==null)
			{
			for(Integer i:this.leftTokens.get(left))
				{
				Object v=getValueAt(i, this.aggregateColumn.getIndex());
				this.selectedAggregate.add(v);
				}
			}
		else if(left==null && top!=null)
			{
			for(Integer i:this.topTokens.get(top))
				{
				Object v=getValueAt(i, this.aggregateColumn.getIndex());
				this.selectedAggregate.add(v);
				}
			}
		else
			{
			int indexInTopList=0;
			List<Integer> leftList= this.leftTokens.get(left);
			List<Integer> topList= this.topTokens.get(top);
			for(Integer index:leftList)
				{
				while(indexInTopList< topList.size() &&
					 topList.get(indexInTopList)< index)
					 {
					 indexInTopList++;
					 }
				if( indexInTopList< topList.size() &&
					topList.get(indexInTopList)== index)
					{
					Object v=getValueAt(index, this.aggregateColumn.getIndex());
					this.selectedAggregate.add(v);
					}
				}
			}
		this.selectedAggregate.write(out);
		this.selectedAggregate.reset();
		}



	private void printHTML(XMLStreamWriter out) throws XMLStreamException
	    {
		out.writeStartElement("html");
		out.writeAttribute("xmlns","http://www.w3.org/1999/xhtml");
		out.writeStartElement("head");

		out.writeStartElement("title");
		out.writeCharacters("Pivot");
		out.writeEndElement();




		if(htmlStylsheet!=null)
			{
			out.writeStartElement("link");
			out.writeAttribute("rel","stylesheet");
			out.writeAttribute("type","text/css");
			out.writeAttribute("media","all");
			out.writeAttribute("href",htmlStylsheet);
			out.writeEndElement();//link
			}
		else
			{
			out.writeStartElement("style");
			out.writeAttribute("type","text/css");
			out.writeCharacters("body\t{\n"+
					"\tbackground-color:white;\n"+
					"\tfont-size:12px;\n"+
					"\t}\n"+
					"table\t{\n"+
					"\twidth:100%;\n"+
					"\tborder-collapse:collapse;\n"+
					"\tfont-family: sans-serif;\n"+
					"\t}\n"+
					"table, th, td { border: 1px solid black; }\n"+
					".top-token, .left-token {\n"+
					"\tbackground-color:lightgray;\n"+
					"\tcolor:blue;\n"+
					"\t}\n"+
					".top-column, .left-row {\n"+
					"\tbackground-color:lightgray;\n"+
					"\tcolor:gray;\n"+
					"\t}");
			out.writeEndElement();//style
			}


		out.writeEndElement();//head

		out.writeStartElement("body");
		out.writeStartElement("table");
		out.writeAttribute("class", "pivot-table");
		out.writeStartElement("thead");


	    /**********************************************
	     *
	     * Titles TOP
	     *
	     **********************************************/






	    for(int x=0;x< topColumns.size();++x)
	        {
	        Column topColumn = topColumns.get(x);
	    	out.writeStartElement("tr");
	        //add empty columns for left_labels
	        for(int i=0;i< leftColumns.size();++i)
	            {
	            out.writeEmptyElement("th");
	            }

	        out.writeStartElement("th");
	        out.writeAttribute("class", "top-label");
	        out.writeCharacters(topColumn.getLabel());
	        out.writeEndElement();//th



	       for(Token topT: this.topTokens.keySet())
	            {
	            out.writeStartElement("th");
	            out.writeAttribute("class", "top-token");
		        out.writeCharacters(String.valueOf(topT.get(topColumn)));
		        out.writeEndElement();//th
	            }

	        out.writeEmptyElement("th");//add an extra column will be used for 'Total' on the right, fill with col name
	        out.writeEndElement();//TR
	        }


	    /**********************************************
	     *
	     * add one extra line that will contains left header labels
	     *
	     **********************************************/
	    out.writeStartElement("tr");
	    for(Column leftC: leftColumns)
	        {
	        out.writeStartElement("th");
	        out.writeAttribute("class", "left-label");
	        out.writeCharacters(leftC.getLabel());
	        out.writeEndElement();
	        }

	    out.writeEmptyElement("th");

	    for(long i=0;i< topTokens.size();++i)
	        {
	        out.writeStartElement("th");
	        out.writeAttribute("class", "top-column");
	        out.writeCharacters(String.valueOf(i+1));
	        out.writeEndElement();
	        }
	    if(print_horizontal_total)
			{
			out.writeStartElement("th");
			out.writeAttribute("class", "total-label");
			out.writeCharacters("Total");
			out.writeEndElement();
			}
	    out.writeEndElement();//tr
	    out.writeEndElement();//thead

	    out.writeStartElement("tbody");

	    //loop over the distinct rows
	    int nRows=0;
	    for(Token leftToken:this.leftTokens.keySet())
	        {
	    	out.writeStartElement("tr");

	        for(Column leftColumn:this.leftColumns)
	            {
	            out.writeStartElement("th");
	            out.writeAttribute("class", "left-token");
	            out.writeCharacters(String.valueOf(leftToken.get(leftColumn)));
	            out.writeEndElement();//th
	            }
	        out.writeStartElement("th");
	        out.writeAttribute("class", "left-row");
	        out.writeCharacters(String.valueOf(++nRows));
	        out.writeEndElement();//th

	        for(Token topToken:this.topTokens.keySet())
	            {
	            out.writeStartElement("td");
	            aggregate(out,leftToken,topToken);
	            out.writeEndElement();//td
	            }

	        if(this.print_horizontal_total)
		        {
		        out.writeStartElement("td");
	            aggregate(out,leftToken,null);
	            out.writeEndElement();//td
		        }

		    out.writeEndElement();//tr
	        }
	    out.writeEndElement();//tbody

	    //bottom total
	    if(this.print_vertical_total)
		    {
		    out.writeStartElement("tfoot");
	    	out.writeStartElement("tr");
	        for(int i=0;i< leftColumns.size();++i)
	            {
	            out.writeEmptyElement("th");
	            }
	        out.writeStartElement("th");
	        out.writeCharacters("Total");
	        out.writeEndElement();//th

	        for(Token topToken: this.topTokens.keySet())
	        	{
	        	out.writeStartElement("td");
	        	aggregate(out,null,topToken);
	            out.writeEndElement();//td
	            }

	        out.writeStartElement("td");
	        aggregate(out,null,null);
	        out.writeEndElement();

	        out.writeEndElement();//tr
	        out.writeEndElement();//tfoot
		    }



	    out.writeEndElement();//table
	    out.writeEndElement();//body
	    out.writeEndElement();//html
	    }


	private static int parseColumnIndex(String s)
		{
		int index= Integer.parseInt(s.trim());
		if(index<=0) throw new IllegalArgumentException("bad column index="+s);
		index--;
		return index;
		}

	private static List<Integer> parseColumnIndexes(String s)
		{
		List<Integer> listLeft=new ArrayList<Integer>();
		for(String c:s.split("[,]"))
			{
			if(c.isEmpty()) continue;
			listLeft.add(parseColumnIndex(c));
			}
		return listLeft;
		}

	/**
	 * Main method
	 * @param args
	 */
	public static void main(String[] args)
		{
		Pivot pivot= new Pivot();
		List<Integer> listLeft=new ArrayList<Integer>();
		List<Integer> listTop=new ArrayList<Integer>();
		Map<Integer,Column> index2col=new HashMap<Integer,Column>();
		int aggregateIndex=-1;

		try
			{
			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h"))
					{
					System.out.println("Pivot [options] (<File>|stdin)");
					System.err.println("Pierre Lindenbaum PhD 2011");
	                System.out.println("Author: Pierre Lindenbaum PhD. 2009");
	                System.out.println(" -h help (this screen)");
	                System.out.println(" -L \'column1,column2,column3,...\' columns for left. (required)");
	                System.out.println(" -T \'column1,column2,column3,...\' columns for top. (optional)");
	                System.out.println(" -A \'column.\' aggregate column (optional)");

	                System.out.println(" -C columns types: \'column1:type1,column2:type2,column3:type3,...\' with type in:");
	                System.out.println(" -D <aggregate-id> for agregate.");
	                for(Aggregate a:pivot.aggregates)
	                	{
	                	System.out.println("    "+a.getId()+" "+a.getDescription());
	                	}
	                System.out.println(" -p <regex> pattern used to break the input into tokens default:TAB");
	                System.out.println(" -i case insensitive");
	                System.out.println(" -t trim each column");
	                System.out.println(" -null <string> value for null");
	                System.out.println(" -f first line is NOT the header");
	                System.out.println(" -css <uri> stylesheet href");
	                return;
					}
				else if(args[optind].equals("-css"))
					{
					pivot.htmlStylsheet=args[++optind];
					}
				else if(args[optind].equals("-C"))
					{
					for(Column c:pivot.parseColumns(args[++optind]))
						{
						index2col.put(c.getIndex(),c);
						}

					}
				else if(args[optind].equals("-L"))
					{
					listLeft= parseColumnIndexes(args[++optind]);
					}
				else if(args[optind].equals("-T"))
	                {
	            	listTop= parseColumnIndexes(args[++optind]);
	                }
				else if(args[optind].equals("-A"))
	                {
	            	aggregateIndex= parseColumnIndex(args[++optind]);
	                }
	            else if(args[optind].equals("-D"))
	                {
	            	String aggName=args[++optind];
	            	pivot.selectedAggregate=null;
	            	for(Aggregate a: pivot.aggregates)
	            		{
	            		if(aggName.equals(a.getId()))
	            			{
	            			pivot.selectedAggregate=a;
	            			}
	            		}
	            	if(pivot.selectedAggregate==null)
	            		{
	            		System.err.println("Cannot find aggregate id:"+aggName);
	            		return;
	            		}
	                }
	            else if(args[optind].equals("-p"))
	                {
	                pivot.delimiter= Pattern.compile(args[++optind]);
	                }
	            else if(args[optind].equals("-null"))
		            {
		            pivot.nilValue=args[++optind];
		            }
	            else if(args[optind].equals("-f"))
		            {
		            pivot.firstRowIsHeader=!pivot.firstRowIsHeader;
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

			if(aggregateIndex==-1)
				{
				System.err.println("Aggregate column is undefined");
				return;
				}

			if(listLeft.isEmpty())
				{
				System.err.println("Left columns undefined");
				return;
				}


			Set<Integer> usedIndexes=new HashSet<Integer>();
			usedIndexes.addAll(listLeft);
			usedIndexes.addAll(listTop);
			usedIndexes.add(aggregateIndex);

			for(Integer i: usedIndexes)
				{
				while(pivot.allColumns.size()<=i)
					{
					pivot.allColumns.add(null);
					}
				if(index2col.containsKey(i))
					{

					pivot.allColumns.set(i,index2col.get(i));
					}
				else
					{
					pivot.allColumns.set(i,pivot.defaultColumn(i));
					}
				}
			for(Integer i:listLeft)
				{
				pivot.leftColumns.add(pivot.allColumns.get(i));
				}

			for(Integer i:listTop)
				{
				pivot.topColumns.add(pivot.allColumns.get(i));
				}

			pivot.aggregateColumn=pivot.allColumns.get(aggregateIndex);

			if(optind==args.length)
		        {
		        pivot.readTable(new BufferedReader(new InputStreamReader(System.in)));
		        }
			else if(optind+1==args.length)
		        {
		        String filename=args[optind++];
		    	BufferedReader in=new BufferedReader(new FileReader(filename));
		        pivot.readTable(in);
		        in.close();
		        }
		    else
		        {
		        throw new IllegalArgumentException("Too many arguments");
		        }
			XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
			XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out,"UTF-8");
			w.writeStartDocument("UTF-8","1.0");
			pivot.printHTML(w);
			w.writeEndDocument();
			w.flush();
			}
	catch(Throwable err)
		{
		err.printStackTrace();
		}
	}

}
