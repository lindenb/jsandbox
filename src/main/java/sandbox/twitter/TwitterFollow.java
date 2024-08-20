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

import java.math.BigInteger;
import java.util.List;



import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import sandbox.twitter.AbstractTwitterApplication.Consumer;




public class TwitterFollow
	extends AbstractTwitterApplication
	{
	private String verb=null;
	private TwitterFollow()
		{
		}
	
	@Override
	protected void fillOptions(final Options options)
		{
		options.addOption(Option.builder("followers").
				hasArg(false).
				required(false).
				longOpt("followers").
				type(String.class).
				desc("search for 'followers'").
				build()
				);
		
		super.fillOptions(options);
		}
	
	@Override
	protected Status decodeOptions(CommandLine cmd)
		{
		info("here");
		if(cmd.hasOption("followers"))
			{
			this.verb = "followers";
			}
		if(this.verb==null)
			{
			System.err.println("following or followers not specified");
			return Status.EXIT_ERROR;
			}
		return super.decodeOptions(cmd);
		}

	
	@Override
	protected int execute(CommandLine cmd)
		{
		info("here");
		List<String> args = cmd.getArgList();

		if(  args.size()==1)
			{
			String screen_nameor_user_id = args.get(0);
			connect();
			try
				{
				savePreferences();
				final Consumer<BigInteger> consumer = new Consumer<BigInteger>()
					{
					@Override
					public void accept(BigInteger o)
						{
						System.out.println(o);
						}
					};
				if(verb.equals("followers"))
					{
					this.listFollowers(screen_nameor_user_id,consumer);
					}
				savePreferences();
				}
			catch (Exception e) {
				error(e);
				return -1;
				}
			
			return 0;
			}
		else
			{
			error("Illegal number of Arguments.");
			return -1;
			}
		}
	
	public static void main(String[] args) throws Exception
		{
		new TwitterFollow().instanceMainWithExit(args);
		}
	}