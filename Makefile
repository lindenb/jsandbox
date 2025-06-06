EMPTY :=
SPACE := $(EMPTY) $(EMPTY)
.PHONY: all all_maven_jars clean_maven_jars eclipse_classpath
here.dir=$(dir $(realpath $(word $(words $(MAKEFILE_LIST)),$(MAKEFILE_LIST))))
bin.dir =${here.dir}dist
javacc.exe ?= java -cp ${HOME}/packages/javacc/target/javacc.jar  javacc
lib.dir=${here.dir}maven
src.dir=${here.dir}src/main/java
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

$(1)  : $(addsuffix .java,$(addprefix src/main/java/,$(subst .,/,$(2)))) $(3) dist/annotproc.jar
	echo "### COMPILING $(1) ######"
	mkdir -p ${tmp.dir}/META-INF ${bin.dir}
	#compile
	${JAVAC} -Xlint -d ${tmp.dir} -implicit:class -processor sandbox.annotation.processing.MyProcessor -processorpath dist/annotproc.jar -g -classpath "$$(subst $$(SPACE),:,$$(filter-out dist/annotproc.jar,$$(filter %.jar,$$^)))" -sourcepath ${src.dir} $$(filter %.java,$$^)
	# copy source
	mkdir -p  '${tmp.dir}/$(dir $(subst .,/,$(firstword $(2))))'
	cp -v  '$$(firstword $$(filter %.java,$$^))'  '${tmp.dir}/$(dir $(subst .,/,$(firstword $(2))))'
	#create META-INF/MANIFEST.MF
	echo "Manifest-Version: 1.0" > ${tmp.dir}/tmp.mf
	echo "Main-Class: $(2)" >> ${tmp.dir}/tmp.mf
	echo "Class-Path: $$(filter-out dist/annotproc.jar,$$(filter %.jar,$$^)) ${bin.dir}/$(1).jar" | fold -w 71 | awk '{printf("%s%s\n",(NR==1?"": " "),$$$$0);}' >>  ${tmp.dir}/tmp.mf
	#create jar
	${JAR} cfm ${bin.dir}/$(1).jar ${tmp.dir}/tmp.mf  -C ${tmp.dir} .
	#cleanup
	rm -rf ${tmp.dir}

endef

jakarta.mail.jar = \
	$(lib.dir)/jakarta/mail/jakarta.mail-api/2.1.3/jakarta.mail-api-2.1.3.jar

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
	$(lib.dir)/org/apache/httpcomponents/httpclient/4.5.9/httpclient-4.5.9.jar \
	$(lib.dir)/commons-codec/commons-codec/1.11/commons-codec-1.11.jar \
	$(lib.dir)/org/apache/httpcomponents/httpcore/4.4.11/httpcore-4.4.11.jar \
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

# 	$(lib.dir)/commons-lang/commons-lang/2.4/commons-lang-2.4.jar 
# 	$(lib.dir)/org/apache/velocity/velocity/1.7/velocity-1.7.jar
# 	$(lib.dir)/commons-collections/commons-collections/3.2.1/commons-collections-3.2.1.jar \

velocity.jars  =  \
	$(lib.dir)/org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar \
	$(lib.dir)/org/apache/commons/commons-lang3/3.16.0/commons-lang3-3.16.0.jar \
	$(lib.dir)/org/apache/velocity/velocity-engine-core/2.3/velocity-engine-core-2.3.jar

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

commons-math3.jar= \
	$(lib.dir)/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar



apache.poi.jar = \
	$(lib.dir)/org/apache/poi/poi/5.0.0/poi-5.0.0.jar \
	$(lib.dir)/com/zaxxer/SparseBitSet/1.2/SparseBitSet-1.2.jar \
	$(lib.dir)/commons-codec/commons-codec/1.15/commons-codec-1.15.jar \
	$(lib.dir)/org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar \
	$(lib.dir)/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar \
	$(lib.dir)/org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar \
	$(lib.dir)/org/slf4j/jcl-over-slf4j/1.7.30/jcl-over-slf4j-1.7.30.jar \
	$(lib.dir)/org/slf4j/slf4j-simple/1.7.13/slf4j-simple-1.7.13.jar \
	$(lib.dir)/org/apache/poi/poi-ooxml-lite/5.0.0/poi-ooxml-lite-5.0.0.jar \
	$(lib.dir)/org/apache/poi/poi-ooxml/5.0.0/poi-ooxml-5.0.0.jar \
	$(lib.dir)/org/apache/xmlbeans/xmlbeans/4.0.0/xmlbeans-4.0.0.jar \
	$(lib.dir)/org/apache/commons/commons-compress/1.20/commons-compress-1.20.jar

freemarker.jar=\
	$(lib.dir)/org/freemarker/freemarker/2.3.32/freemarker-2.3.32.jar

jaxb.jar=\
	$(lib.dir)/javax/xml/bind/jaxb-api/2.4.0-b180830.0359/jaxb-api-2.4.0-b180830.0359.jar

snakeyaml.jar=$(lib.dir)/org/yaml/snakeyaml/2.3/snakeyaml-2.3.jar

mime4j.jars=\
	$(lib.dir)/org/apache/james/apache-mime4j-core/0.8.11/apache-mime4j-core-0.8.11.jar \
	$(lib.dir)/commons-io/commons-io/2.18.0/commons-io-2.18.0.jar

nashorn.jars=\
	$(lib.dir)/org/openjdk/nashorn/nashorn-core/15.4/nashorn-core-15.4.jar \
	$(lib.dir)/org/ow2/asm/asm-analysis/7.3.1/asm-analysis-7.3.1.jar \
	$(lib.dir)/org/ow2/asm/asm-commons/7.3.1/asm-commons-7.3.1.jar \
	$(lib.dir)/org/ow2/asm/asm-tree/7.3.1/asm-tree-7.3.1.jar \
	$(lib.dir)/org/ow2/asm/asm-util/7.3.1/asm-util-7.3.1.jar \
	$(lib.dir)/org/ow2/asm/asm/7.3.1/asm-7.3.1.jar

all_maven_jars = $(sort $(apache.poi.jar) ${jcommander.jar} ${jgit.jars} ${velocity.jars} ${jersey-server.jars} \
	$(jakarta.mail.jar) ${apache-derby.jars} ${jena-core.jars} ${jtidy.jars} ${jetty.jars} ${servlet.api.jars} ${spring-beans.jars} ${apache.httpclient.jars} ${slf4j.jars} ${jtidy.jars} ${twitter.hbc.jars} ${apache.commons.cli} ${org.scribe.jars} ${google.gson.jars} ${sqlite3.jdbc.jar} ${emf.core.jars} ${berkeleydb.jar} ${log4j.jars} ${commons-math3.jar} ${freemarker.jar} ${jaxb.jar} ${snakeyaml.jar} $(mime4j.jars) $(nashorn.jars) )

top: jsandbox

all: 	rss2atom bouletmaton genisansbouillir treemapviewer \
	xml2xsd weatherarchive gribouille java2graph githistory  \
        atommerger pubmedtrending cookiestorefile softwarefitness \
	packageeclipsejars xslserver java2xml flickrrss \
	geneticpainting json2dom  twittergraph twitterfollow  twitter01   \
	jfxwatcher  atom500px gimpprocs2xml instagram2atom  imagemap \
	pcaviewer swap2bits


$(eval $(call compile,jfxwatcher,sandbox.JFXWatcher,))
$(eval $(call compile,twitter01,sandbox.Twitter01, ${twitter.hbc.jars}))
$(eval $(call compile,twitterfollow,sandbox.TwitterFollow, ${apache.commons.cli} ${org.scribe.jars} ${google.gson.jars}))
$(eval $(call compile,twitteruserlookup,sandbox.TwitterUserLookup, ${apache.commons.cli} ${org.scribe.jars} ${google.gson.jars}))
$(eval $(call compile,twittergraph,sandbox.TwitterGraph, ${sqlite3.jdbc.jar} ${jcommander.jar} ${org.scribe.jars} ${google.gson.jars}))
$(eval $(call compile,json2dom,sandbox.Json2Dom,${google.gson.jars}))
$(eval $(call compile,timelinemaker,sandbox.TimeLineMaker,${google.gson.jars} ${jcommander.jar}))
$(eval $(call compile,geneticpainting,sandbox.tools.geneticpaint.GeneticPainting,${jcommander.jar}))
$(eval $(call compile,flickrrss,sandbox.FlickrRss,${apache.commons.cli} ${slf4j.jars} ${org.scribe.jars}))
$(eval $(call compile,montagegif,sandbox.tools.montagegif.MontageGif,${jcommander.jar}))
$(eval $(call compile,java2xml,sandbox.Java2Xml,${jcommander.jar}))
$(eval $(call compile,html2tty,sandbox.HtmlToTTY,${jcommander.jar}))
$(eval $(call compile,xslserver,sandbox.XslHandler,${google.gson.jars} ${apache.commons.cli} ${slf4j.jars} ${jetty.jars} ${apache.httpclient.jars}  ${jtidy.jars} ${spring-beans.jars}))
$(eval $(call compile,divandb,sandbox.DivanDB,${google.gson.jars} ${apache.commons.cli} ${slf4j.jars} ${jetty.jars} ${apache.httpclient.jars}  ${jtidy.jars}))
$(eval $(call compile,packageeclipsejars,sandbox.PackageEclipseJars,${apache.commons.cli}))
$(eval $(call compile,pubmedtrending,sandbox.PubmedTrending,${apache.commons.cli} ${slf4j.jars} ))
$(eval $(call compile,atommerger,sandbox.tools.feed.AtomMerger, ${jcommander.jar}))
$(eval $(call compile,cookiestorefile,sandbox.CookieStoreUtils,${apache.httpclient.jars}))
$(eval $(call compile,softwarefitness,sandbox.SoftwareFitness,${apache.commons.cli} ${slf4j.jars}))
$(eval $(call compile,bmcaltmetrics,sandbox.BmcAltmetrics,${apache.commons.cli} ${slf4j.jars}))
$(eval $(call compile,rgddigest,sandbox.RGDDigest,${apache.commons.cli} ${slf4j.jars} ${jtidy.jars} ${apache.httpclient.jars} ))
$(eval $(call compile,nashornserver,sandbox.NashornServer,${apache.commons.cli} ${slf4j.jars} ${jetty.jars} ))
$(eval $(call compile,filesaveserver,sandbox.http.FileSaveServer,${jcommander.jar} ${slf4j.jars} ${jetty.jars} ${apache.httpclient.jars} ${log4j.jars}))
$(eval $(call compile,urlsurveyserver,sandbox.http.UrlSurveyServer,${jcommander.jar} ${slf4j.jars} ${jetty.jars} ${apache.httpclient.jars} ${log4j.jars}))
$(eval $(call compile,githistory,sandbox.GitHistory,${apache.commons.cli}  ))
$(eval $(call compile,xml2xsd,sandbox.tools.xmls2xsd.XmlToXsd,${jcommander.jar}))
$(eval $(call compile,java2graph,sandbox.Java2Graph,${apache.commons.cli}  ))
$(eval $(call compile,gribouille,sandbox.Gribouille,${jcommander.jar}  ))
$(eval $(call compile,weatherarchive,sandbox.WeatherArchive,${apache.commons.cli} ${slf4j.jars}  ${jtidy.jars}  ${apache.httpclient.jars} ))
$(eval $(call compile,velocityjson,sandbox.tools.velocityjson.VelocityJson,${jcommander.jar}  ${velocity.jars} ${google.gson.jars}  ))
$(eval $(call compile,treemapviewer,sandbox.tools.treemap.TreeMapViewer,  ))
$(eval $(call compile,treemapmaker,sandbox.tools.treemap.TreeMapMaker,${jcommander.jar} ${apache.httpclient.jars}))
$(eval $(call compile,comicstrip,sandbox.ComicsStrip, ))
$(eval $(call compile,genisansbouillir,sandbox.GeniSansBouillir,${jcommander.jar} ${apache.httpclient.jars}  ${jtidy.jars}))
$(eval $(call compile,bouletmaton,sandbox.BouletMaton,${jcommander.jar}))
$(eval $(call compile,rss2atom,sandbox.tools.feed.RssToAtomApp,${jcommander.jar} ${jtidy.jars}))
$(eval $(call compile,atom500px,sandbox.Atom500px,${jcommander.jar}))
$(eval $(call compile,insta2atom,sandbox.ig.InstagramToAtom,${jcommander.jar} ${apache.httpclient.jars} ${google.gson.jars}))
$(eval $(call compile,insta2json,sandbox.ig.InstagramToJson,${jcommander.jar} ${apache.httpclient.jars} ${google.gson.jars}))
$(eval $(call compile,insta2gexf,sandbox.ig.InstagramGraph,${jcommander.jar} ${apache.httpclient.jars} ${jtidy.jars}))
$(eval $(call compile,atomxhtml,sandbox.AtomXhtmlContent,${jcommander.jar} ${jtidy.jars}))
$(eval $(call compile,imagemap,sandbox.ImageMap,${jcommander.jar}))
$(eval $(call compile,dccomicsscraper,sandbox.DcComicsScraper,${jcommander.jar} ${google.gson.jars} ${jtidy.jars} ${apache.httpclient.jars} ${berkeleydb.jar}))
$(eval $(call compile,image2ascii,sandbox.tools.img2ascii.ImageToAscii,${jcommander.jar}))
$(eval $(call compile,cepicdcscraper,sandbox.CepicDcScraper,${jcommander.jar} ${apache.httpclient.jars}))
$(eval $(call compile,htmlparser,sandbox.HtmlParser,${jcommander.jar} ${jtidy.jars}))
$(eval $(call compile,osm2svg,sandbox.OsmToSvg,${jcommander.jar}))
$(eval $(call compile,img2palette,sandbox.tools.img2palette.ImageToPalette,${jcommander.jar}))
$(eval $(call compile,halftone,sandbox.drawing.Halftone,${jcommander.jar}))
$(eval $(call compile,igpano,sandbox.ig.InstaPanorama,${jcommander.jar}))
$(eval $(call compile,drawinggrid,sandbox.tools.drawinggrid.DrawingGrid,${jcommander.jar}))
$(eval $(call compile,drawclip,sandbox.tools.drawclip.DrawClip,${jcommander.jar}))
$(eval $(call compile,flickr2atom,sandbox.tools.flickr.FlickrToAtom,${jcommander.jar} ${google.gson.jars} ${apache.httpclient.jars}))
$(eval $(call compile,wigglegrid,sandbox.drawing.WiggleGrid,${jcommander.jar}))
$(eval $(call compile,hatching01,sandbox.drawing.Hatching01,${jcommander.jar}))
$(eval $(call compile,randomdots01,sandbox.drawing.RandomDots01,${jcommander.jar}))
$(eval $(call compile,swij2guile,sandbox.swij.SwijToGuile,${jcommander.jar} ./src/main/java/sandbox/swij/SwijParser.java))
$(eval $(call compile,htmltidy,sandbox.html.HtmlTidy,${jcommander.jar} ${jtidy.jars}))
$(eval $(call compile,xmlstream,sandbox.xml.tools.xmlstream.XmlStream,${jcommander.jar}))
$(eval $(call compile,rdoc,sandbox.tools.rdoc.RDocGenerator,${jcommander.jar}))
$(eval $(call compile,ig2table,sandbox.tools.ig.IgToTable,${jcommander.jar} ${google.gson.jars} ${apache.httpclient.jars}))
$(eval $(call compile,makegrid,sandbox.tools.kirby.MakeGrid,${jcommander.jar}))
$(eval $(call compile,owltool,sandbox.tools.owl.OWLTool,${jcommander.jar}))
$(eval $(call compile,ffcache,sandbox.tools.ffcache.FirefoxCache,${jcommander.jar}))
$(eval $(call compile,pcaviewer,sandbox.tools.pca.PcaViewer,${commons-math3.jar}))
$(eval $(call compile,atom2html,sandbox.tools.feed.AtomToHtml,${jcommander.jar} ${apache.httpclient.jars}  ${jtidy.jars}))
$(eval $(call compile,igdigest,sandbox.tools.ig.IgDigest,${jcommander.jar}))
$(eval $(call compile,interpolator,sandbox.tools.interpolate.Interpolator,${jcommander.jar} ${commons-math3.jar}))
$(eval $(call compile,xmlcipher,sandbox.tools.xmlcipher.XmlCipher,${jcommander.jar}))
$(eval $(call compile,donotuseexcel,sandbox.tools.donotuseexcel.DoNotUseExcel,${jcommander.jar} ${apache.poi.jar}))
$(eval $(call compile,mastodongraph,sandbox.tools.mastodongraph.MastodonGraph,${jcommander.jar} ${apache.httpclient.jars} ${google.gson.jars}))
$(eval $(call compile,gimppatterns,sandbox.tools.gimppat.GimpPatterns,${jcommander.jar}))
$(eval $(call compile,test,sandbox.tools.xml2ppt.XmlToPPT,${jcommander.jar} ${apache.poi.jar}))
$(eval $(call compile,tonic,sandbox.tools.tonic.Tonic,${jcommander.jar}))
$(eval $(call compile,xml2jni,sandbox.tools.jni.XmlToJNI,${jcommander.jar} ${freemarker.jar}))
$(eval $(call compile,rdftemplate,sandbox.tools.rdftemplate.RDFTemplate,${jcommander.jar} ${jena-core.jars}))
$(eval $(call compile,rdf2graph,sandbox.tools.rdf2graph.RdfToGraph,${jcommander.jar} ${jena-core.jars}))
$(eval $(call compile,streamplot,sandbox.tools.streamplot.StreamPlot, ${jcommander.jar} ./src/main/java/sandbox/tools/streamplot/parser/StreamPlotParser.java))
$(eval $(call compile,pojogenerator,sandbox.tools.pojogenerator.PojoGenerator, ${jcommander.jar} ./src/main/java/sandbox/tools/pojogenerator/parser/PojoParser.java))
$(eval $(call compile,jaxb2java,sandbox.tools.jaxb2java.JaxbToJava, ${jcommander.jar} ${jaxb.jar}))
$(eval $(call compile,jeter,sandbox.tools.comicsbuilder.v1.ComicsBuilderV1,${jcommander.jar} ${nashorn.jars}))
$(eval $(call compile,jsandbox,sandbox.tools.central.SandboxCentral, ${jcommander.jar} ${jtidy.jars}  ${google.gson.jars} ${velocity.jars} ${snakeyaml.jar}  ${jetty.jars} ${apache.httpclient.jars} $(mime4j.jars) $(nashorn.jars)) )


##$(eval $(call compile,autolexyacc,sandbox.AutoLexYacc,  ))

./src/main/java/sandbox/swij/SwijParser.java : ./src/main/java/sandbox/swij/Swij.jj
	${javacc.exe} -OUTPUT_DIRECTORY=$(dir $@) $<

./src/main/java/sandbox/expr1/parser/Expr1Parser.java: ./src/main/java/sandbox/expr1/parser/Parser.jj
	${javacc.exe} -OUTPUT_DIRECTORY=$(dir $@) $<

dist/annotproc.jar: src/main/java/sandbox/annotation/processing/MyProcessor.java
	rm -rf tmp
	mkdir -p tmp $(dir $@)
	javac -d tmp -sourcepath src/main/java $< 
	javac -d tmp -sourcepath src/main/java $<
	jar cvf $@ -C tmp .
	rm -rf tmp


$(bin.dir)/avdl2xml.jar: ./src/main/java/sandbox/Avdl2Xml.jj
	mkdir -p tmp $(dir $@)
	${javacc.exe} -OUTPUT_DIRECTORY=tmp/sandbox -TOKEN_MANAGER_USES_PARSER=true $<
	javac -d tmp -sourcepath tmp $(addprefix tmp/sandbox/,$(addsuffix .java,Avdl2XmlConstants Avdl2Xml Avdl2XmlTokenManager ParseException SimpleCharStream Token TokenMgrError))
	jar cvf $@ -C tmp .
	#rm -rf tmp
	$(foreach F,alleleAnnotationmethods  alleleAnnotations  annotateAllelemethods beacon  genotypephenotype common metadata, echo "${F}" && curl -L -o "${F}.avdl" "https://raw.githubusercontent.com/ga4gh/schemas/master/src/main/resources/avro/${F}.avdl" && cat   "${F}.avdl" | java -cp $@ sandbox.Avdl2Xml  | xmllint --format - ; )


${tmp.dir}/WEB-INF/js/jquery.js:
	mkdir -p $(dir $@)
	wget -O $@ "http://code.jquery.com/jquery-latest.js"


define runjavacc

$$(addsuffix .java,$$(basename $(1))) : $(1)
	${javacc.exe} -JDK_VERSION=17 -OUTPUT_DIRECTORY=$$(dir $$@) $$<

endef

$(eval $(call runjavacc,./src/main/java/sandbox/colors/parser/ColorParser.jj))
$(eval $(call runjavacc,./src/main/java/sandbox/tools/pojogenerator/parser/PojoParser.jj))


src/main/java/sandbox/tools/streamplot/parser/StreamPlotParser.java : src/main/java/sandbox/tools/streamplot/parser/StreamPlotGrammar.jj
	$(javacc.exe) -OUTPUT_DIRECTORY=$(dir $@) $<


gimpprocs2xml: ./src/main/java/sandbox/GimpProcedures.jj
	mkdir -p ${tmp.dir}/sandbox ${tmp.dir}/META-INF ${bin.dir}
	${javacc.exe} -OUTPUT_DIRECTORY=tmp/sandbox $<
	javac -d ${tmp.dir} ${tmp.dir}/sandbox/*.java
	mkdir -p ${tmp.dir}/META-INF
	echo "Manifest-Version: 1.0" > ${tmp.dir}/tmp.mf
	echo "Main-Class: sandbox.GimpProcParser" >> ${tmp.dir}/tmp.mf
	${JAR} cfm ${bin.dir}/gimpprocs2xml.jar ${tmp.dir}/tmp.mf  -C ${tmp.dir} .
	rm -rf ${tmp.dir}

textlet : src/main/java/sandbox/TextletParser.jj
	${javacc.exe} -OUTPUT_DIRECTORY=src/main/java/sandbox $<
	javac -sourcepath src/main/java src/main/java/sandbox/Textlet.java
	echo "aa (<%@ include file='azda'  %>) aa <%= 2 %> <%!  int i=0; public int getI() { return this.i;} %>" | java -cp src/main/java sandbox.Textlet

x : ./src/main/java/sandbox/swing/xml/BaseSwingXmlContext.java

./src/main/java/sandbox/swing/xml/BaseSwingXmlContext.java : src/main/xslt/swingxmlcontext.xsl \
	./src/main/java/sandbox/swing/xml/AbstractSwingXmlContext.java \
	./src/main/java/sandbox/swing/xml/SwingXmlContext.java
	java -jar dist/jsandbox.jar java2xml --super \
			$(addprefix java.awt.,Color Dimension Point BorderLayout BasicStroke Font) \
			$(addprefix javax.swing.,JSplitPane JTable JTree JScrollPane JTabbedPane JButton JPanel JFrame JDialog JMenu JMenuBar JMenuItem JTextArea JTextField JComboBox JScrollBar JSeparator JToggleButton JSlider JRadioButton JProgressBar JPopupMenu JPasswordField JList JLabel JDesktopPane JCheckBox JCheckBoxMenuItem border.TitledBorder border.EmptyBorder) |\
		xsltproc src/main/xslt/swingxmlcontext.xsl -  > ./src/main/java/sandbox/swing/xml/BaseSwingXmlContext.java
	$(MAKE) jsandbox

common.avdl :
	curl -o $@ -L "https://raw.githubusercontent.com/ga4gh/schemas/master/src/main/resources/avro/$@"

${all_maven_jars}  : 
	mkdir -p $(dir $@) && wget -O "$(addsuffix .tmp.jar,$@)" "https://repo1.maven.org/maven2/$(patsubst ${lib.dir}/%,%,$@)" && mv "$(addsuffix .tmp.jar,$@)" $@

eclipse_classpath:
	echo "$(realpath ${all_maven_jars})" | tr " " "\n" | awk '{printf("\t<classpathentry kind=\"lib\" path=\"%s\"/>\n",$$1);}'

download_maven_jars : ${all_maven_jars}

clean_maven_jars :
	rm -f ${all_maven_jars}




