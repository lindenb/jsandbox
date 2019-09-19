/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	Jan-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * WWW:
 * 	http://plindenbaum.blogspot.com
 *
 */

package sandbox.twitter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.cli.CommandLine;

import sandbox.twitter.AbstractTwitterApplication.UserEntity;




public class TwitterUserLookup
	extends AbstractTwitterApplication
	{
	private TwitterUserLookup()
		{
		}
	
	private void run(BufferedReader r) throws Exception
		{
		Set<BigInteger> ids=new HashSet<>();
		for(;;)
			{
			String line=r.readLine();
			if(line==null || ids.size()==100)
				{
				for(UserEntity user :super.usersLookup(ids))
					{
					System.out.print(user.getIdStr());
					System.out.print("\t");
					System.out.print(user.getScreenName());
					System.out.print("\t");
					System.out.print(user.getName());
					System.out.println();
					}
				if(line==null) break;
				ids.clear();
				}
			
			if(line.isEmpty() || line.startsWith("#")) continue;
			try
				{
				BigInteger userId=new BigInteger(line);
				ids.add(userId);
				}
			catch (Exception e)
				{
				
				warning(e);
				
				}
			}
		}
	
	@Override
	protected int execute(CommandLine cmd)
		{
		List<String> args = cmd.getArgList();
		try
			{
			connect();
			savePreferences();
			if(  args.isEmpty())
				{
				BufferedReader r=new BufferedReader(new InputStreamReader(System.in));
				run(r);
				r.close();
				}
			else for(String s:args)
				{
				BufferedReader r=new BufferedReader(new FileReader(s));
				run(r);
				r.close();
				}
			savePreferences();
			return 0;
			}
		catch (Exception e) {
			error(e);
			return -1;
			}			
		}
	
	public static void main(String[] args) throws Exception
		{
		new TwitterUserLookup().instanceMainWithExit(args);
		}
	}