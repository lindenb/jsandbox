package divandb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;

import org.lindenb.berkeley.binding.JsonBinding;
import org.lindenb.njson.ArrayNode;
import org.lindenb.njson.Node;
import org.lindenb.njson.NodeBinding;
import org.lindenb.njson.ObjectNode;
import org.lindenb.util.Digest;
import org.lindenb.util.Predicate;
import org.lindenb.util.TimeUtils;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.bind.tuple.TupleBinding;
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

@SuppressWarnings("serial")
public class DivanDB
	{
	private Environment environment;
	private File homeDir;
	private Database database;
	
	
	private static class DBException extends DatabaseException
		{
		DBException(String msg) { super(msg);}
		DBException(Exception e) { super(e);}
		}
	
	/**
	 * 
	 * JsonBinding
	 *
	 */
	public static class JsonBinding
		extends NodeBinding
		implements EntryBinding<Node>
		{
		@Override
		public Node entryToObject(DatabaseEntry entry)
			{
			DataInputStream in=new DataInputStream(new ByteArrayInputStream(
					entry.getData(),
					entry.getOffset(),
					entry.getSize()
					));
			try
				{
				return readNode(in);
				}
			catch (Exception e)
				{
				throw new RuntimeException(e);
				}
			}
		public void objectToEntry(Node node, DatabaseEntry entry)
			{
			try
				{
				ByteArrayOutputStream baos=new ByteArrayOutputStream();
				DataOutputStream dos=new DataOutputStream(baos);
				writeNode(node, dos);
				dos.flush();
				entry.setData(baos.toByteArray());
				}
			catch (Exception e)
				{
				throw new RuntimeException(e);
				}
			}
		}
	
	public DivanDB()
		{
		}
	
	public Environment getEnvironment()
		{
		return environment;
		}
	
	public void open(File homeDir)
		{
		close();
		EnvironmentConfig envConfig=new EnvironmentConfig();
		envConfig.setAllowCreate(true);
		envConfig.setReadOnly(false);
		envConfig.setTransactional(true);
		this.homeDir=homeDir;
		this.environment=new Environment(this.homeDir, envConfig);
		
		DatabaseConfig cfg=new DatabaseConfig();
		cfg.setAllowCreate(true);
		cfg.setReadOnly(false);
		this.database=this.environment.openDatabase(null, "id2json", cfg);
		}
	
	
	public Node put(Node node)
		throws DatabaseException
		{
		JsonBinding binding=new JsonBinding();
		DatabaseEntry keyEntry=new DatabaseEntry();
		DatabaseEntry dataEntry=new DatabaseEntry();
		if(!node.isObject())
			{
			throw new DBException("node is not an object but "+node.getType());
			}
		ObjectNode object=node.asObject();
		Transaction txn=null;
		try
			{
			Node idnode= object.get("id");
			String id=null;
			if( idnode!=null)
				{
				if( !idnode.isString() ||
					!idnode.asString().matches("[0-9a-zA-Z_]+")
					)
					{
					throw new DBException("bad id for "+object);
					}
				id=idnode.asString().getValue();
				}
			
			txn=this.environment.beginTransaction(null, null);
			while(id==null)
				{
				id = Digest.MD5.encrypt(String.valueOf(System.currentTimeMillis())+Math.random());
				StringBinding.stringToEntry(id, keyEntry);
				if(database.get(txn, keyEntry, dataEntry, LockMode.DEFAULT)!=OperationStatus.SUCCESS)
					{
					object.put("id", id);
					break;
					}
				id=null;
				}
			
			StringBinding.stringToEntry(id, keyEntry);
			binding.objectToEntry(object, dataEntry);
			if(database.put(txn, keyEntry, dataEntry)!=OperationStatus.SUCCESS)
				{
				throw new DBException("insertion failed");
				}
			txn.commit();
			return node;
			}
		catch (DBException e)
			{
			txn.abort();
			throw e;
			}
		catch (Exception e)
			{
			txn.abort();
			throw new DBException(e);
			}
		}
	
	
	
	public long dump(
			Transaction txn,
			Predicate<Node> predicate,
			Writer out)
		throws IOException
		{
		long nCount=0L;
		Cursor c=null;
		try
			{
			JsonBinding binding=new JsonBinding();
			DatabaseEntry key=new DatabaseEntry();
			DatabaseEntry data=new DatabaseEntry();
			c=database.openCursor(txn, null);
			out.write("[");
			while((c.getNext(key, data, LockMode.DEFAULT))==OperationStatus.SUCCESS)
				{
				Node node=binding.entryToObject(data);
				if(predicate!=null && !predicate.apply(node)) continue;
				if(nCount!=0L) out.write(",");
				nCount++;
				
				node.print(out);
				}
			out.write("]");
			return nCount;
			}
		catch (Exception e)
			{
			throw new IOException(e);
			}
		finally
			{
			if(c!=null) c.close();
			}
		}
	
	public void dump(File file)
		throws IOException
		{
		PrintWriter out=null;
		try
			{
			out=new PrintWriter(file);
			out.flush();
			dump(null,null,out);
			}
		catch (Exception e)
			{
			throw new IOException(e);
			}
		finally
			{
			if(out!=null) out.close();
			}
		}
	public void close()
		{
		if(this.database!=null)
			{
			try {this.database.close();}
			catch (Exception e) {}
			this.database=null;
			}
		if(this.environment!=null)
			{
			try {this.environment.close();}
			catch (Exception e) {}
			this.environment=null;
			}
		if(this.homeDir!=null)
			{
			for(File f:this.homeDir.listFiles())
				{
				f.delete();
				}
			this.homeDir.delete();
			this.homeDir=null;
			}
		}
	}
