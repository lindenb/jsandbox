/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	June-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * Reference:
 *   http://plindenbaum.blogspot.com/2011/06/couchdb-like-application-using-apache.html
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Motivation:
 * 		a couchdb-like server. Prextext to learn JETTY
 * Compilation:
 * 		  #edit or create the build.properties, set bdb.je.jar and jetty.dir
 *        cd jsandbox; ant divandb
 * Usage:
 *        java -jar dist/divandb.jar
 */
package sandbox;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ajax.JSON;

import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

@SuppressWarnings({ "unchecked", "serial","rawtypes" })
public class DivanDB extends HttpServlet
	{
	private static final String STORAGE_ATTRIBUTE="divandb.storage";
	
	/** string compartor for ordering the keys in bdb */
	public static class StringComparator
		implements Comparator<byte[]>
		{
		@Override
		public int compare(byte[] o1, byte[] o2)
			{
			String s1=StringBinding.entryToString(new DatabaseEntry(o1));
			String s2=StringBinding.entryToString(new DatabaseEntry(o2));
			return s1.compareTo(s2);
			}
		}
	/** a berkeley-db String/String datastore */ 
	private static class BDBStorage
		{
		/** bdb environment */
	    private Environment environment=null;
	    /** string/string database */
	    private Database database=null;
	    
	    private BDBStorage()
	    	{
	    	}
	    /** open environment & database */
		private void open(File dbHome) throws DatabaseException
			{
			EnvironmentConfig envConfig= new EnvironmentConfig();
			envConfig.setAllowCreate(true);
			envConfig.setReadOnly(false);
			envConfig.setTransactional(true);
			this.environment= new Environment(dbHome, envConfig);
			DatabaseConfig cfg= new DatabaseConfig();
			cfg.setAllowCreate(true);
			cfg.setReadOnly(false);
			cfg.setTransactional(true);
			cfg.setBtreeComparator(StringComparator.class);
			this.database= this.environment.openDatabase(null,"divandb",cfg);
			}
		 /** close environment & database */
		private void close()
			{
			if(this.database!=null)
				{
				try {this.database.close();} catch(Exception err){}
				this.database=null;
				}
			if(this.environment!=null)
				{
				try {this.environment.cleanLog();} catch(Exception err){}
				try {this.environment.compress();} catch(Exception err){}
				try {this.environment.close();} catch(Exception err){}
				this.environment=null;
				}
			}
		@Override
		protected void finalize() throws Throwable
			{
			try {close();} catch(Throwable err) {}
			super.finalize();
			}
		}
	

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
		{
		BDBStorage storage=(BDBStorage)this.getServletContext().getAttribute(STORAGE_ATTRIBUTE);
		resp.setContentType("application/json");
		
		String id=req.getRequestURI().substring(1+req.getContextPath().length());
		DatabaseEntry key=new DatabaseEntry();
		DatabaseEntry data=new DatabaseEntry();
		PrintWriter out=null;
		
		
		/** no id ? we want to list everything */
	    if(id.isEmpty())
	    	{
	    	int countFound=0;
	    	int countPrinted=0;
	    	out=resp.getWriter();
	    	resp.setStatus(HttpServletResponse.SC_OK);
	    	Cursor c=null;
	    	Transaction txn=null;
	    	Integer limit=null;
	    	Integer startIndex=null;
	    	try
	    		{
	    		txn=storage.environment.beginTransaction(null, null);
	    		c=storage.database.openCursor(txn, null);
	    		boolean first=true;
	    		out.print("[");
	    		String startkey=req.getParameter("startkey");
	    		String endkey=req.getParameter("endkey");
	    		if(req.getParameter("limit")!=null)
	    			{
	    			limit=Integer.parseInt(req.getParameter("limit"));
	    			}
	    		if(req.getParameter("start")!=null)
	    			{
	    			startIndex=Integer.parseInt(req.getParameter("start"));
	    			}
	    		for(;;)
	    			{
	    			
	    			if(limit!=null && countPrinted>=limit)
	    				{
	    				break;
	    				}
	    			OperationStatus status;
	    			/* first cursor call */ 
	    			if(first)
	    				{
	    				/* search start key if any */
	    				if(startkey!=null)
	    					{
	    					StringBinding.stringToEntry(startkey, key);
	    					status=c.getSearchKeyRange(key, data, LockMode.DEFAULT);
	    					}
	    				else
	    					{
	    					status=c.getNext(key, data, LockMode.DEFAULT);
	    					}
	    				first=false;
	    				}
	    			else /* not the first call */
	    				{
	    				status=c.getNext(key, data, LockMode.DEFAULT);
	    				}
	    			/* eof met */
	    			if(status!=OperationStatus.SUCCESS) break;
	    		
	    			/* check end key if any */
	    			if(endkey!=null)
	    				{
	    				String keyVal=StringBinding.entryToString(key);
	    				if(keyVal.compareTo(endkey)>0) break;
	    				}
	    			++countFound;
	    			if(startIndex!=null && countFound<startIndex)
	    				{
	    				continue;
	    				}
	    			
	    			
	    			if(countPrinted>0) out.print(",");
	    			out.print(StringBinding.entryToString(data));
	    			countPrinted++;
	    			}
	    		
	    		out.println("]");
	    		c.close();
	    		c=null;
	    		txn.commit();
	    		}
	    	catch(Exception err)
	    		{
	    		if(c!=null) c.close();
	    		c=null;
	    		if(txn!=null) txn.abort();
	    		throw new ServletException(err);
	    		}
	    	finally
	    		{
	    		if(c!=null) c.close();
	    		}
	    	}
	    else //an id was specified
	    	{
	    	//fill key entry
	    	StringBinding.stringToEntry(id, key);
	    	//get value
	    	if(storage.database.get(null, key, data, LockMode.DEFAULT)!=OperationStatus.SUCCESS)
	    		{
	    		//not found
	    		resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
	    		out=resp.getWriter();
	    		out.println("null");
	    		}
	    	else
	    		{
	    		//ok, found
	    		resp.setStatus(HttpServletResponse.SC_OK);
	    		out=resp.getWriter();
	    	    out.println(StringBinding.entryToString(data));
	    		}
	    	}
	    out.flush();
	    out.close();
	    }
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
		{
		Set<String> ids=new TreeSet<String>();
		String errorMessage=null;
		BDBStorage storage=(BDBStorage)this.getServletContext().getAttribute(STORAGE_ATTRIBUTE);
		InputStream in=req.getInputStream();
		Transaction txn=null;
		try
			{
			//begin transaction
			txn=storage.environment.beginTransaction(null, null);
			String charset=req.getCharacterEncoding();
			if(charset==null) charset="UTF-8";
			Object o=JSON.parse(new InputStreamReader(in,charset));
			if(o==null ) throw new IllegalArgumentException("nil object");
			List objects;
			if(o.getClass().isArray())
				{
				objects=Arrays.asList((Object[])o);
				}
			else if(o instanceof Map)
				{
				objects=new ArrayList();
				objects.add(o);
				}
			else if(o instanceof List)
				{
				objects=(List)o;
				}
			else
				{
				throw new IllegalArgumentException("not an array or an object");
				}
			/* loop over this list */
			for(Object o2:objects)
				{
				if(o2==null ) throw new NullPointerException("nil object");
				String id;
				/* json object */
				if(o2 instanceof Map)
					{
					Map map=(Map)o2;
					if(!map.containsKey("id")) throw new IllegalArgumentException("id missing");
					Object ido=map.get("id");
					if(ido==null) throw new IllegalArgumentException("nil id");
					id=String.valueOf(ido);
					}
				/* json string */
				else if(o2 instanceof String)
					{
					id=String.class.cast(o2);
					}
				else
					{
					throw new IllegalArgumentException("not an object or a string: "+o2);
					}
				// fill the entry key
				DatabaseEntry key=new DatabaseEntry();
				StringBinding.stringToEntry(id, key);
				//remove 
				if(storage.database.delete(txn, key)==OperationStatus.SUCCESS)
					{
					ids.add(id);
					}
				}
			//we're done.
			txn.commit();
			}
		catch (Exception e)
			{
			if(txn!=null) txn.abort();
			errorMessage=e.getMessage();
			if(errorMessage==null) errorMessage=e.getClass().getSimpleName();
			}
		finally
			{
			if(in!=null) try{ in.close();} catch(Exception err){}
			}
		resp.setContentType("application/json");
		resp.setStatus(errorMessage==null?HttpServletResponse.SC_OK:HttpServletResponse.SC_BAD_REQUEST);
		Map<String,Object> response=new LinkedHashMap<String,Object>();
		response.put("ok", (errorMessage==null?"true":"false"));
		response.put("id",ids.toArray());
		if(errorMessage!=null) response.put("message",errorMessage);
		PrintWriter out=resp.getWriter();
		out.println(JSON.toString(response));
		out.flush();
		out.close();
		}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
		{
		Set<String> ids=new TreeSet<String>();
		DatabaseEntry key=new DatabaseEntry();
		DatabaseEntry data=new DatabaseEntry();
		String errorMessage=null;
		BDBStorage storage=(BDBStorage)this.getServletContext().getAttribute(STORAGE_ATTRIBUTE);
		InputStream in=req.getInputStream();
		Transaction txn=null;
		try
			{
			txn=storage.environment.beginTransaction(null, null);
			String charset=req.getCharacterEncoding();
			if(charset==null) charset="UTF-8";
			Object json = JSON.parse(new InputStreamReader(in,charset));
			if(json==null ) throw new IllegalArgumentException("nil object");
			List objects;
			if(json.getClass().isArray())
				{
				objects=Arrays.asList((Object[])json);
				}
			else if(json instanceof Map)
				{
				objects=new ArrayList();
				objects.add(json);
				}
			else if(json instanceof List)
				{
				objects=(List)json;
				}
			else
				{
				throw new IllegalArgumentException("not an array or an object");
				}
			for(Object o2:objects)
				{
				if(o2==null || !(o2 instanceof Map)) throw new IllegalArgumentException("not a json object");
				Map map=(Map)o2;
				map.put("_timestamp", String.valueOf(System.currentTimeMillis()));
				//the JSON object already contains a field 'id'
				if(map.containsKey("id"))
					{
					Object ido=map.get("id");
					if(ido==null) throw new IllegalArgumentException("nil id");
					String id=String.valueOf(ido);
					if(id.isEmpty())  throw new IllegalArgumentException("empty id in "+map);
					StringBinding.stringToEntry(id, key);
					StringBinding.stringToEntry(JSON.toString(map),data);//back to string
					if(storage.database.put(txn, key, data)!=OperationStatus.SUCCESS)
						{
						throw new RuntimeException("BDB.error: Cannot insert "+id);
						}
					ids.add(id);
					}
				//generate an id
				else 
					{
					Random r=new Random(System.currentTimeMillis());
					
					for(;;)
						{
						String id=String.valueOf("id"+Math.abs(r.nextInt()));
						StringBinding.stringToEntry(id, key);
						//id already in database ?
						if(storage.database.get(txn,key,data,LockMode.DEFAULT)==OperationStatus.SUCCESS) continue;
						//add id to this object
						map.put("id", id);
						StringBinding.stringToEntry(JSON.toString(map),data);//back to string
						//put ,key must NOT exist
						if(storage.database.putNoOverwrite(txn, key, data)!=OperationStatus.SUCCESS)
							{
							throw new RuntimeException("BDB.error: Cannot insert "+id);
							}
						ids.add(id);
						break;
						}
						
					}
				}
			txn.commit();
			}
		catch (Exception e)
			{
			if(txn!=null) txn.abort();
			e.printStackTrace();
			errorMessage=e.getMessage();
			if(errorMessage==null) errorMessage=e.getClass().getSimpleName();
			}
		finally
			{
			if(in!=null) try{ in.close();} catch(Exception err){}
			}		
		resp.setContentType("application/json");
		resp.setStatus(errorMessage==null?HttpServletResponse.SC_CREATED:HttpServletResponse.SC_BAD_REQUEST);
		Map<String,Object> response=new LinkedHashMap<String,Object>();
		response.put("ok", (errorMessage==null?"true":"false"));
		response.put("id",ids.toArray());
		if(errorMessage!=null) response.put("message",errorMessage);
		PrintWriter out=resp.getWriter();
		out.println(JSON.toString(response));
		out.flush();
		out.close();
		}
	
	
	public static void main(String[] args)
		{
		File bdbDir=new File(System.getProperty("java.io.tmpdir"),"bdb");
		try
			{
			int port=8080;
			final BDBStorage storage=new BDBStorage();
			DivanDB app=new DivanDB();
			Runtime.getRuntime().addShutdownHook(new Thread()
				{
				@Override
				public void run()
					{
					System.err.println("Closing BerkeleyDB environement.");
					storage.close();
					}
				});
			
			
			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Pierre Lindenbaum PhD. 2011");
					System.err.println("Options:");
					System.err.println(" -h help; This screen.");
					System.err.println(" -P <port> default:"+port);
					System.err.println(" -d <berkeley.db.dir> default:"+bdbDir);
					return;
					}
				else if(args[optind].equals("-P"))
					{
					port=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-d"))
					{
					bdbDir=new File(args[++optind]);
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
			if(args.length!=optind)
				{
				System.err.println("Illegal number of arguments.");
				return;
				}
			
			
	        storage.open(bdbDir);
	        ServletContextHandler context = new ServletContextHandler();
	        context.setAttribute(
	        		STORAGE_ATTRIBUTE,
	        		storage
	        		);
	        
	        context.addServlet(new ServletHolder(app),"/*");
	        context.setContextPath("/divandb");
	        context.setResourceBase(".");

	        /* create a new server */
			Server server = new Server(port);
			/* context */
			server.setHandler(context);
			
			/* start server */
			server.start();
			/* loop forever */
			server.join();
			}
		catch(Throwable err)
			{
			err.printStackTrace();
			}
		finally
			{
			
			}
		}
	}
