EMPTY :=
SPACE := $(EMPTY) $(EMPTY)
.PHONY: all all_maven_jars clean_maven_jars eclipse_classpath
here.dir=$(dir $(realpath $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST))))
bin.dir =${here.dir}dist
javacc.exe ?= javacc
lib.dir=${here.dir}maven
src.dir=${here.dir}src
tmp.dir=tmp
JAVAC?=javac
JAR?=jar

# 1 : name
# 2 : main.java 
# 3 : deps
define compile
## 1 : target name
## 2 : qualified main class name
## 3 : other deps

$(1)  : $(addsuffix .java,$(addprefix src/,$(subst .,/,$(2)))) $(3)
	echo "### COMPILING $(1) ######"
	mkdir -p ${tmp.dir}/META-INF ${bin.dir}
	#compile
	${JAVAC} -Xlint -d ${tmp.dir} -g -classpath "$$(subst $$(SPACE),:,$$(filter %.jar,$$^))" -sourcepath ${src.dir} $$(filter %.java,$$^)
	#create META-INF/MANIFEST.MF
	echo "Manifest-Version: 1.0" > ${tmp.dir}/tmp.mf
	echo "Main-Class: $(2)" >> ${tmp.dir}/tmp.mf
	echo "Class-Path: $$(filter %.jar,$$^) ${bin.dir}/$(1).jar" | fold -w 71 | awk '{printf("%s%s\n",(NR==1?"": " "),$$$$0);}' >>  ${tmp.dir}/tmp.mf
	#create jar
	${JAR} cfm ${bin.dir}/$(1).jar ${tmp.dir}/tmp.mf  -C ${tmp.dir} .
	#cleanup
	rm -rf ${tmp.dir}

endef

twitter.hbc.jars  = \
	$(lib.dir)/org/apache/httpcomponents/httpcore/4.4.3/httpcore-4.4.3.jar \
	$(lib.dir)/commons-codec/commons-codec/1.10/commons-codec-1.10.jar \
	$(lib.dir)/org/apache/httpcomponents/httpclient/4.2.5/httpclient-4.2.5.jar \
	$(lib.dir)/com/google/guava/guava/14.0.1/guava-14.0.1.jar \
	$(lib.dir)/com/twitter/joauth/6.0.2/joauth-6.0.2.jar \
	$(lib.dir)/com/google/code/findbugs/jsr305/1.3.9/jsr305-1.3.9.jar \
	$(lib.dir)/commons-logging/commons-logging/1.2/commons-logging-1.2.jar \
	$(lib.dir)/com/twitter/hbc-core/2.2.0/hbc-core-2.2.0.jar \
	$(lib.dir)/org/slf4j/slf4j-api/1.6.6/slf4j-api-1.6.6.jar \
	$(lib.dir)/com/google/guava/guava/15.0/guava-15.0.jar

apache.commons.cli  = \
	$(lib.dir)/commons-cli/commons-cli/1.3.1/commons-cli-1.3.1.jar

jackson-core.jars  =  \
	$(lib.dir)/com/fasterxml/jackson/core/jackson-core/2.6.3/jackson-core-2.6.3.jar

org.scribe.jars  = \
	$(lib.dir)/org/scribe/scribe/1.3.7/scribe-1.3.7.jar

google.gson.jars  = \
	$(lib.dir)/com/google/code/gson/gson/2.5/gson-2.5.jar

sqlite3.jdbc.jar  = \
	$(lib.dir)/org/xerial/sqlite-jdbc/3.8.11.1/sqlite-jdbc-3.8.11.1.jar

log4j.jars = \
	$(lib.dir)/log4j/log4j/1.2.17/log4j-1.2.17.jar

slf4j.jars  = \
	$(lib.dir)/org/slf4j/slf4j-api/1.7.13/slf4j-api-1.7.13.jar \
	$(lib.dir)/org/slf4j/slf4j-log4j12/1.7.13/slf4j-log4j12-1.7.13.jar \
	$(lib.dir)/org/slf4j/slf4j-simple/1.7.13/slf4j-simple-1.7.13.jar


apache.httpclient.jars  = \
	$(lib.dir)/org/apache/httpcomponents/httpclient/4.5.1/httpclient-4.5.1.jar \
	$(lib.dir)/commons-codec/commons-codec/1.10/commons-codec-1.10.jar \
	$(lib.dir)/org/apache/httpcomponents/httpcore/4.4.4/httpcore-4.4.4.jar \
	$(lib.dir)/commons-logging/commons-logging/1.2/commons-logging-1.2.jar

spring-beans.jars = \
	$(lib.dir)/org/springframework/spring-context/4.2.4.RELEASE/spring-context-4.2.4.RELEASE.jar \
	$(lib.dir)/org/springframework/spring-beans/4.2.4.RELEASE/spring-beans-4.2.4.RELEASE.jar \
	$(lib.dir)/org/springframework/spring-core/4.2.4.RELEASE/spring-core-4.2.4.RELEASE.jar \
	$(lib.dir)/org/springframework/spring-expression/4.2.4.RELEASE/spring-expression-4.2.4.RELEASE.jar \
	$(lib.dir)/commons-logging/commons-logging/1.2/commons-logging-1.2.jar \
	$(lib.dir)/aopalliance/aopalliance/1.0/aopalliance-1.0.jar

servlet.api.jars  =\
	$(lib.dir)/javax/servlet/javax.servlet-api/4.0.0-b01/javax.servlet-api-4.0.0-b01.jar


jetty.jars=\
	$(lib.dir)/javax/servlet/javax.servlet-api/4.0.0-b01/javax.servlet-api-4.0.0-b01.jar \
	$(lib.dir)/org/eclipse/jetty/jetty-http/9.3.7.v20160115/jetty-http-9.3.7.v20160115.jar \
	$(lib.dir)/org/eclipse/jetty/jetty-server/9.3.7.v20160115/jetty-server-9.3.7.v20160115.jar \
	$(lib.dir)/org/eclipse/jetty/jetty-io/9.3.7.v20160115/jetty-io-9.3.7.v20160115.jar \
	$(lib.dir)/org/eclipse/jetty/jetty-util/9.3.7.v20160115/jetty-util-9.3.7.v20160115.jar

jtidy.jars=\
	$(lib.dir)/net/sf/jtidy/jtidy/r938/jtidy-r938.jar

emf.core.jars=\
	$(lib.dir)/org/eclipse/emf/org.eclipse.emf.ecore/2.11.1-v20150805-0538/org.eclipse.emf.ecore-2.11.1-v20150805-0538.jar \
	$(lib.dir)/org/eclipse/emf/org.eclipse.emf.common/2.11.0-v20150805-0538/org.eclipse.emf.common-2.11.0-v20150805-0538.jar


jena-core.jars  =\
	$(lib.dir)/org/apache/jena/jena-core/3.1.0/jena-core-3.1.0.jar \
	$(lib.dir)/org/apache/jena/jena-base/3.1.0/jena-base-3.1.0.jar \
	$(lib.dir)/xml-apis/xml-apis/1.4.01/xml-apis-1.4.01.jar \
	$(lib.dir)/org/slf4j/slf4j-api/1.7.21/slf4j-api-1.7.21.jar \
	$(lib.dir)/org/apache/commons/commons-csv/1.4/commons-csv-1.4.jar \
	$(lib.dir)/xerces/xercesImpl/2.11.0/xercesImpl-2.11.0.jar \
	$(lib.dir)/org/apache/jena/jena-shaded-guava/3.1.0/jena-shaded-guava-3.1.0.jar \
	$(lib.dir)/org/apache/commons/commons-lang3/3.4/commons-lang3-3.4.jar \
	$(lib.dir)/org/apache/jena/jena-iri/3.1.0/jena-iri-3.1.0.jar \
	$(lib.dir)/commons-cli/commons-cli/1.3.1/commons-cli-1.3.1.jar \
	$(lib.dir)/com/github/andrewoma/dexx/collection/0.6/collection-0.6.jar

apache-derby.jars =\
	$(lib.dir)/org/apache/derby/derby/10.12.1.1/derby-10.12.1.1.jar \
	$(lib.dir)/org/apache/derby/derbyclient/10.12.1.1/derbyclient-10.12.1.1.jar \
	$(lib.dir)/org/apache/derby/derbytools/10.12.1.1/derbytools-10.12.1.1.jar


jersey-server.jars  =  \
	$(lib.dir)/javax/annotation/javax.annotation-api/1.2/javax.annotation-api-1.2.jar \
	$(lib.dir)/javax/inject/javax.inject/1/javax.inject-1.jar \
	$(lib.dir)/javax/validation/validation-api/1.1.0.Final/validation-api-1.1.0.Final.jar \
	$(lib.dir)/javax/ws/rs/javax.ws.rs-api/2.0.1/javax.ws.rs-api-2.0.1.jar \
	$(lib.dir)/org/glassfish/hk2/external/aopalliance-repackaged/2.5.0-b10/aopalliance-repackaged-2.5.0-b10.jar \
	$(lib.dir)/org/glassfish/hk2/external/javax.inject/2.5.0-b10/javax.inject-2.5.0-b10.jar \
	$(lib.dir)/org/glassfish/hk2/hk2-api/2.5.0-b10/hk2-api-2.5.0-b10.jar \
	$(lib.dir)/org/glassfish/hk2/hk2-locator/2.5.0-b10/hk2-locator-2.5.0-b10.jar \
	$(lib.dir)/org/glassfish/hk2/hk2-utils/2.5.0-b10/hk2-utils-2.5.0-b10.jar \
	$(lib.dir)/org/glassfish/hk2/osgi-resource-locator/2.5.0-b10/osgi-resource-locator-2.5.0-b10.jar \
	$(lib.dir)/org/glassfish/jersey/bundles/repackaged/jersey-guava/2.23.2/jersey-guava-2.23.2.jar \
	$(lib.dir)/org/glassfish/jersey/core/jersey-client/2.23.2/jersey-client-2.23.2.jar \
	$(lib.dir)/org/glassfish/jersey/core/jersey-common/2.23.2/jersey-common-2.23.2.jar \
	$(lib.dir)/org/glassfish/jersey/core/jersey-server/2.23.2/jersey-server-2.23.2.jar \
	$(lib.dir)/org/glassfish/jersey/media/jersey-media-jaxb/2.23.2/jersey-media-jaxb-2.23.2.jar \
	$(lib.dir)/org/javassist/javassist/3.20.0-GA/javassist-3.20.0-GA.jar \
	$(lib.dir)/org/jmockit/jmockit/1.27/jmockit-1.27.jar


velocity.jars  =  \
	$(lib.dir)/commons-collections/commons-collections/3.2.1/commons-collections-3.2.1.jar \
	$(lib.dir)/commons-lang/commons-lang/2.4/commons-lang-2.4.jar \
	$(lib.dir)/org/apache/velocity/velocity/1.7/velocity-1.7.jar

jgit.jars  =  \
	$(lib.dir)/com/googlecode/javaewah/JavaEWAH/1.1.6/JavaEWAH-1.1.6.jar \
	$(lib.dir)/com/jcraft/jsch/0.1.54/jsch-0.1.54.jar \
	$(lib.dir)/commons-codec/commons-codec/1.10/commons-codec-1.10.jar \
	$(lib.dir)/commons-logging/commons-logging/1.2/commons-logging-1.2.jar \
	$(lib.dir)/org/apache/httpcomponents/httpclient/4.5.2/httpclient-4.5.2.jar \
	$(lib.dir)/org/apache/httpcomponents/httpcore/4.4.5/httpcore-4.4.5.jar \
	$(lib.dir)/org/eclipse/jgit/org.eclipse.jgit/4.4.1.201607150455-r/org.eclipse.jgit-4.4.1.201607150455-r.jar \
	$(lib.dir)/org/slf4j/slf4j-api/1.7.21/slf4j-api-1.7.21.jar

jcommander.jar= \
	$(lib.dir)/com/beust/jcommander/1.64/jcommander-1.64.jar

berkeleydb.jar= \
	$(lib.dir)/com/sleepycat/je/5.0.73/je-5.0.73.jar


all_maven_jars = $(sort ${jcommander.jar} ${jgit.jars} ${velocity.jars} ${jersey-server.jars} ${apache-derby.jars} ${jena-core.jars} ${jtidy.jars} ${jetty.jars} ${servlet.api.jars} ${spring-beans.jars} ${apache.httpclient.jars} ${slf4j.jars} ${jtidy.jars} ${twitter.hbc.jars} ${apache.commons.cli} ${org.scribe.jars} ${google.gson.jars} ${sqlite3.jdbc.jar} ${emf.core.jars} ${berkeleydb.jar})



all: 	rss2atom bouletmaton genisansbouillir treemapviewer \
	xml2xsd weatherarchive gribouille java2graph githistory nashornserver \
        saxscript atommerger pubmedtrending cookiestorefile softwarefitness \
	htmlinxml packageeclipsejars xslserver java2xml mosaicofpictures flickrrss \
	geneticpainting json2dom json2xml twittergraph twitterfollow miniivy twitter01 aksum images2base64 \
	jfxwatcher mywordle atom500px gimpprocs2xml instagram2atom fileserver xmlpath imagemap


$(eval $(call compile,miniivy,sandbox.MiniIvy,${jcommander.jar}))
$(eval $(call compile,mywordle,sandbox.MyWordle,${jcommander.jar}))
$(eval $(call compile,jfxwatcher,sandbox.JFXWatcher,))
$(eval $(call compile,twitter01,sandbox.Twitter01, ${twitter.hbc.jars}))
$(eval $(call compile,twitterfollow,sandbox.TwitterFollow, ${apache.commons.cli} ${org.scribe.jars} ${google.gson.jars}))
$(eval $(call compile,twitteruserlookup,sandbox.TwitterUserLookup, ${apache.commons.cli} ${org.scribe.jars} ${google.gson.jars}))
$(eval $(call compile,twittergraph,sandbox.TwitterGraph, ${sqlite3.jdbc.jar} ${jcommander.jar} ${org.scribe.jars} ${google.gson.jars}))
$(eval $(call compile,json2xml,sandbox.Json2Xml,${google.gson.jars}))
$(eval $(call compile,json2dom,sandbox.Json2Dom,${google.gson.jars}))
$(eval $(call compile,geneticpainting,sandbox.GeneticPainting,${apache.commons.cli}))
$(eval $(call compile,flickrrss,sandbox.FlickrRss,${apache.commons.cli} ${slf4j.jars} ${org.scribe.jars}))
$(eval $(call compile,mosaicofpictures,sandbox.MosaicOfPictures,${jcommander.jar}))
$(eval $(call compile,java2xml,sandbox.Java2Xml,${apache.commons.cli} ${slf4j.jars}))
$(eval $(call compile,xslserver,sandbox.XslHandler,${google.gson.jars} ${apache.commons.cli} ${slf4j.jars} ${jetty.jars} ${apache.httpclient.jars}  ${jtidy.jars} ${spring-beans.jars}))
$(eval $(call compile,divandb,sandbox.DivanDB,${google.gson.jars} ${apache.commons.cli} ${slf4j.jars} ${jetty.jars} ${apache.httpclient.jars}  ${jtidy.jars}))
$(eval $(call compile,packageeclipsejars,sandbox.PackageEclipseJars,${apache.commons.cli}))
$(eval $(call compile,htmlinxml,sandbox.HtmlInXml,${jcommander.jar}  ${jtidy.jars}))
$(eval $(call compile,pubmedtrending,sandbox.PubmedTrending,${apache.commons.cli} ${slf4j.jars} ))
$(eval $(call compile,atommerger,sandbox.AtomMerger, ${jcommander.jar}))
$(eval $(call compile,cookiestorefile,sandbox.CookieStoreUtils,${apache.httpclient.jars}))
$(eval $(call compile,softwarefitness,sandbox.SoftwareFitness,${apache.commons.cli} ${slf4j.jars}))
$(eval $(call compile,bmcaltmetrics,sandbox.BmcAltmetrics,${apache.commons.cli} ${slf4j.jars}))
$(eval $(call compile,rgddigest,sandbox.RGDDigest,${apache.commons.cli} ${slf4j.jars} ${jtidy.jars} ${apache.httpclient.jars} ))
$(eval $(call compile,saxscript,sandbox.SAXScript,${apache.commons.cli} ${slf4j.jars} ${google.gson.jars} ))
$(eval $(call compile,nashornserver,sandbox.NashornServer,${apache.commons.cli} ${slf4j.jars} ${jetty.jars} ))
$(eval $(call compile,githistory,sandbox.GitHistory,${apache.commons.cli}  ))
$(eval $(call compile,xml2xsd,sandbox.XmlToXsd,${apache.commons.cli}  ))
$(eval $(call compile,java2graph,sandbox.Java2Graph,${apache.commons.cli}  ))
$(eval $(call compile,gribouille,sandbox.Gribouille,${jcommander.jar}  ))
$(eval $(call compile,weatherarchive,sandbox.WeatherArchive,${apache.commons.cli} ${slf4j.jars}  ${jtidy.jars}  ${apache.httpclient.jars} ))
$(eval $(call compile,velocityjson,sandbox.VelocityJson,${apache.commons.cli}  ${velocity.jars} ${gogle.gson.jars}  ))
$(eval $(call compile,treemapviewer,sandbox.TreeMapViewer,  ))
$(eval $(call compile,comicstrip,sandbox.ComicsStrip, ))
$(eval $(call compile,genisansbouillir,sandbox.GeniSansBouillir,${jcommander.jar} ${apache.httpclient.jars}  ${jtidy.jars}))
$(eval $(call compile,bouletmaton,sandbox.BouletMaton,${jcommander.jar}))
$(eval $(call compile,aksum,sandbox.Aksum,))
$(eval $(call compile,images2base64,sandbox.ImagesToBase64,${jcommander.jar}))
$(eval $(call compile,rss2atom,sandbox.RssToAtom,${jcommander.jar}))
$(eval $(call compile,atom500px,sandbox.Atom500px,${jcommander.jar}))
$(eval $(call compile,insta2atom,sandbox.InstagramToAtom,${jcommander.jar} ${apache.httpclient.jars} ${google.gson.jars}))
$(eval $(call compile,fileserver,sandbox.FileServer,${jcommander.jar} ${jetty.jars}))
$(eval $(call compile,atomxhtml,sandbox.AtomXhtmlContent,${jcommander.jar} ${jtidy.jars}))
$(eval $(call compile,xmlpath,sandbox.XmlPath,${jcommander.jar} ${jtidy.jars}))
$(eval $(call compile,imagemap,sandbox.ImageMap,${jcommander.jar}))
$(eval $(call compile,dccomicsscraper,sandbox.DcComicsScraper,${jcommander.jar} ${google.gson.jars} ${jtidy.jars} ${apache.httpclient.jars} ${berkeleydb.jar}))
$(eval $(call compile,image2ascii,sandbox.ImageToAscii,${jcommander.jar}))
$(eval $(call compile,cepicdcscraper,sandbox.CepicDcScraper,${jcommander.jar} ${apache.httpclient.jars}))


##$(eval $(call compile,autolexyacc,sandbox.AutoLexYacc,  ))


$(bin.dir)/avdl2xml.jar: ./src/sandbox/Avdl2Xml.jj
	mkdir -p tmp $(dir $@)
	${javacc.exe} -OUTPUT_DIRECTORY=tmp/sandbox -TOKEN_MANAGER_USES_PARSER=true $<
	javac -d tmp -sourcepath tmp $(addprefix tmp/sandbox/,$(addsuffix .java,Avdl2XmlConstants Avdl2Xml Avdl2XmlTokenManager ParseException SimpleCharStream Token TokenMgrError))
	jar cvf $@ -C tmp .
	#rm -rf tmp
	$(foreach F,alleleAnnotationmethods  alleleAnnotations  annotateAllelemethods beacon  genotypephenotype common metadata, echo "${F}" && curl -L -o "${F}.avdl" "https://raw.githubusercontent.com/ga4gh/schemas/master/src/main/resources/avro/${F}.avdl" && cat   "${F}.avdl" | java -cp $@ sandbox.Avdl2Xml  | xmllint --format - ; )

src/sandbox/AutoLexYacc.java : src/sandbox/AutoLexYacc.jj
	${javacc.exe} -OUTPUT_DIRECTORY=$(dir $@) $<	


gimpprocs2xml: ./src/sandbox/GimpProcedures.jj
	mkdir -p ${tmp.dir}/sandbox ${tmp.dir}/META-INF ${bin.dir}
	${javacc.exe} -OUTPUT_DIRECTORY=tmp/sandbox $<
	javac -d ${tmp.dir} ${tmp.dir}/sandbox/*.java
	mkdir -p ${tmp.dir}/META-INF 
	echo "Manifest-Version: 1.0" > ${tmp.dir}/tmp.mf
	echo "Main-Class: sandbox.GimpProcParser" >> ${tmp.dir}/tmp.mf
	${JAR} cfm ${bin.dir}/gimpprocs2xml.jar ${tmp.dir}/tmp.mf  -C ${tmp.dir} .
	rm -rf ${tmp.dir}	

textlet : src/sandbox/TextletParser.jj
	${javacc.exe} -OUTPUT_DIRECTORY=src/sandbox $<	
	javac -sourcepath src src/sandbox/Textlet.java
	echo "aa (<%@ include file='azda'  %>) aa <%= 2 %> <%!  int i=0; public int getI() { return this.i;} %>" | java -cp src sandbox.Textlet

x : src/sandbox/EmfModelParser.jj ${emf.core.jars}
	${javacc.exe} -OUTPUT_DIRECTORY=src/sandbox $<	
	javac -cp $(subst $(SPACE),:,${emf.core.jars}) -sourcepath src src/sandbox/EmfModel.java
	echo "package x1 { class x2 {int a int b String c } }" | java -cp $(subst $(SPACE),:,${emf.core.jars}):src sandbox.EmfModel

common.avdl :
	curl -o $@ -L "https://raw.githubusercontent.com/ga4gh/schemas/master/src/main/resources/avro/$@"

${all_maven_jars}  : 
	mkdir -p $(dir $@) && wget -O "$(addsuffix .tmp.jar,$@)" "http://central.maven.org/maven2/$(patsubst ${lib.dir}/%,%,$@)" && mv "$(addsuffix .tmp.jar,$@)" $@

eclipse_classpath:
	echo "$(realpath ${all_maven_jars})" | tr " " "\n" | awk '{printf("\t<classpathentry kind=\"lib\" path=\"%s\"/>\n",$$1);}'

download_maven_jars : ${all_maven_jars}

clean_maven_jars :
	rm -f ${all_maven_jars}
	
	
## JNLP

ifneq (${webstart.remotedir},)	

define sign_with_jarsigner
	jarsigner $(if ${FASTJARSIGN},,-tsa http://timestamp.digicert.com ) \
		-keystore .secret.keystore \
		-keypass "$(if ${keytool.keypass},${keytool.keypass},KEYTOOLPASS)" \
		-storepass "$(if ${keytool.storepass},${keytool.storepass},KEYTOOLSTOREPASS)" \
		"$(1)" secret ;
endef


scp-webstart: compile-webstart
	scp -r webstart/* "${webstart.remotedir}"


compile-webstart : .secret.keystore treemapviewer
	rm -rf webstart
	mkdir -p webstart
	cp dist/treemapviewer.jar webstart/
	sed 's/__BASE__/webstart.remotedir/' src/sandbox/TreeMapViewer.jnlp > webstart/TreeMapViewer.jnlp
	$(call sign_with_jarsigner,webstart/treemapviewer.jar)
	chmod 755 webstart/*.html webstart/*.jar webstart/*.jnlp


endif
	
	
.secret.keystore :
	-keytool -genkeypair -keystore $@ -alias secret \
	       	-keypass "$(if ${keytool.keypass},${keytool.keypass},KEYTOOLPASS)" \
		-storepass "$(if ${keytool.storepass},${keytool.storepass},KEYTOOLSTOREPASS)" \
		-dname "CN=Pierre Lindenbaum, OU=INSERM, O=INSERM, L=Nantes, ST=Nantes, C=Fr"

