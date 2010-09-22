package divandb;

import java.awt.print.Book;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.lindenb.njson.ArrayNode;
import org.lindenb.njson.BoolNode;
import org.lindenb.njson.JSONParser;
import org.lindenb.njson.Node;
import org.lindenb.njson.ObjectNode;
import org.lindenb.njson.ParseException;
import org.lindenb.njson.StringNode;
import org.lindenb.util.StringUtils;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Transaction;

@SuppressWarnings("serial")
public class DivanServlet extends HttpServlet
	{
    private Long maxInputLength=1000L;
    
    public Long getMaxInputLength()
		{
		return maxInputLength;
		}
    
    @Override
    public void init(ServletConfig config) throws ServletException
    	{
    	super.init(config);
    	String str= config.getInitParameter("input.max-length");
    	if(str==null) this.maxInputLength=new Long(str);
    	}
    
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
		{
		DivanDB database=DivanDB.class.cast(getServletContext().getAttribute("divan.database"));
		int status=HttpServletResponse.SC_CREATED;
		Throwable error=null;
		Writer out=resp.getWriter();
		ObjectNode reply=new ObjectNode();
		Transaction txn=null;
		try
			{
			txn=database.getEnvironment().beginTransaction(null, null);
			JSONParser parser=new JSONParser(req.getInputStream());
			Node node=parser.any();
			parser.eof();
			
			txn.commit();
			}
		catch (ParseException e)
			{
			if(txn!=null) txn.abort();
			status=HttpServletResponse.SC_BAD_REQUEST;
			error=e;
			}
		catch (Exception e)
			{
			if(txn!=null) txn.abort();
			error=e;
			}
		finally
			{
			resp.setStatus(status);
			resp.setContentType("application/json");
			if(error!=null)
				{
				reply.put("ok",true);
				}
			else
				{
				
				reply.put("ok",true);
				}
			reply.print(out);
			out.flush();
			out.close();
			}
		}
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
		{
		PrintWriter o=resp.getWriter();
		o.print("Hello World"); 
		o.print(" contentType:"+req.getContentType());
		o.print(" getContextPath:"+req.getContextPath());
		o.print(" getPathInfo:"+req.getPathInfo());
		o.print(" getRequestURI:"+req.getRequestURI());
		o.print(" getServletPath:"+req.getServletPath());
		o.print(" getQueryString:"+req.getQueryString());
		o.flush();
		o.close();
		}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
		{
		doGet(req, resp);
		}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
		{
		
		}
	}
