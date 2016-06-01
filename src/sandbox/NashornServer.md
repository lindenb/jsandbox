#Motivation

*Nashorn* is lightweight javascript server based on jetty http://www.eclipse.org/jetty/ and nashorn http://www.oracle.com/technetwork/articles/java/jf14-nashorn-2126515.html

#Compilation

```
make nashornserver
```

#Options

```
 -f,--script <arg>               javascript file required.
 -P,--port <arg>                 port. Default:8080
 -p,--path <arg>                 servlet path . default: "/"
```

#Usage

```
java -jar dist/nashornserver.jar [options] -f handler.js 
```


```

#Handler

The javascript file  **must** implement a function having the signature of http://download.eclipse.org/jetty/stable-9/apidocs/org/eclipse/jetty/server/Handler.html#handle-java.lang.String-org.eclipse.jetty.server.Request-javax.servlet.http.HttpServletRequest-javax.servlet.http.HttpServletResponse-


```javascript
function handle(target,baseRequest,req,res) {
	var session = req.getSession();
	var c= session.getAttribute("count");
	if(c==null) c=0;
	c++;
	session.setAttribute("count",c);
	res.setContentType("application/json");
    res.setStatus(200);
   
	var out= res.getWriter();

	out.println(JSON.stringify({"count":c}));
	out.flush();
	out.close();
	}

```


the javascript file is **reloaded** for each http request;

