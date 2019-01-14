package sandbox;


import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Twitter01 {

  public static void main(String args[]) throws InterruptedException {
    BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);
    StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
    // add some track terms
    endpoint.trackTerms(Lists.newArrayList("twitterapi", "#fail"));

    Authentication auth = new OAuth1("aIo5g0boczprOBULkF2SgsbKv",
			"pyIfoIc3UWVmFmkYMw8IBSxMFQEgqwVHe64IzMNJsh2meEXOTk",
			"2447612058-7BQcQ5HbheIIWHbPVuqO8433vBvz43sjElBmOSM",
			"1TNvAOX7b2qqU4Wh8VWzm2wImFTiX7rc2kSvhTus1MOjs");
    // Authentication auth = new BasicAuth(username, password);

    // Create a new BasicClient. By default gzip is enabled.
    Client client = new ClientBuilder()
      .hosts(Constants.STREAM_HOST)
      .endpoint(endpoint)
      .authentication(auth)
      .processor(new StringDelimitedProcessor(queue))
      .build();

    // Establish a connection
    client.connect();

    // Do whatever needs to be done with messages
    for (int msgRead = 0; msgRead < 1000; msgRead++) {
      String msg = queue.take();
      System.out.println(msg);
    }

    client.stop();

  }
}

