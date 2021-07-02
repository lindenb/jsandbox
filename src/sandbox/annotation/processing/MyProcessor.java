package sandbox.annotation.processing;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import sandbox.annotation.IncludeUrl;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MyProcessor extends AbstractProcessor {
private static final Logger LOG = Logger.getLogger(MyProcessor.class.getName());
private Filer filer =null;
@Override
public synchronized void init(ProcessingEnvironment processingEnvironment) {
    super.init(processingEnvironment);
    this.filer = processingEnvironment.getFiler();
}


@Override
public Set<String> getSupportedAnnotationTypes() {
	return Arrays.asList(IncludeUrl.class).stream().
		map(C->C.getName()).
		collect(Collectors.toSet());
	}

private void copySource(final javax.lang.model.element.Element element) {
	
	}

@Override
public boolean process(Set<? extends TypeElement> annotations,
		final RoundEnvironment roundEnv)
		{
		roundEnv.getElementsAnnotatedWith(IncludeUrl.class).stream().
			filter(E->E.getKind()==ElementKind.CLASS).
			forEach(E->{
				
			LOG.info("got "+E+" "+E.getClass());
			String packageName="";
			if(E.getEnclosingElement()!=null && E.getEnclosingElement().getKind()==ElementKind.PACKAGE) {
				packageName = PackageElement.class.cast(E.getEnclosingElement()).getQualifiedName().toString();
				}
			
			LOG.info("got22 "+E.getEnclosingElement()+" "+E.getEnclosingElement().getClass());
				IncludeUrl ann= E.getAnnotation(IncludeUrl.class);
				for(String urlstr: ann.url().split("[ \t]+")) {
					LOG.info("url "+ urlstr);
					LOG.info("got "+ann.directory());
					try {
						URL url = new URL(urlstr);
						String dir="META-INF";
						if(!packageName.isEmpty()) {
							dir = dir+"/"+packageName.replace('.', '/');
							}
						
						String filename ="jeter.txt";
						
						FileObject resource =  filer.createResource(		
							StandardLocation.SOURCE_OUTPUT,"",
							dir+"/"+filename
							);
						
						Writer w= resource.openWriter();
						w.write("ok");
						w.flush();
						w.close();
						break;
						}
					catch(Exception err) {
						err.printStackTrace();
						}
					}
				});
	
		return true;
		}
	}
