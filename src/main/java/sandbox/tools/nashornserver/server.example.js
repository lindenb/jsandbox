function handle(target, base, request , response) {
	var out = response.writer;
   	out.println("Hello, world!");
    	out.close();
	}
