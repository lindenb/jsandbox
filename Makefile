EMPTY :=
SPACE := $(EMPTY) $(EMPTY)
.PHONY: all all_maven_jars
bin.dir = dist
javacc.exe ?= javacc
lib.dir=maven
src.dir=src
jtidy.jars=$(lib.dir)/net/sf/jtidy/jtidy/r938/jtidy-r938.jar
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
	${JAVAC} -d ${tmp.dir} -g -classpath "$$(subst $$(SPACE),:,$$(filter %.jar,$$^))" -sourcepath ${src.dir}:${generated.dir}/java $$(filter %.java,$$^)
	#create META-INF/MANIFEST.MF
	echo "Manifest-Version: 1.0" > ${tmp.dir}/tmp.mf
	echo "Main-Class: $(2)" >> ${tmp.dir}/tmp.mf
	echo "Class-Path: $$(realpath $$(filter %.jar,$$^)) ${dist.dir}/$(1).jar" | fold -w 71 | awk '{printf("%s%s\n",(NR==1?"": " "),$$$$0);}' >>  ${tmp.dir}/tmp.mf
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

slf4j.jars  = \
	$(lib.dir)/org/slf4j/slf4j-api/1.7.13/slf4j-api-1.7.13.jar \
	$(lib.dir)/org/slf4j/slf4j-simple/1.7.13/slf4j-simple-1.7.13.jar


apache.httpclient.jars  = \
	$(lib.dir)/org/apache/httpcomponents/httpclient/4.5.1/httpclient-4.5.1.jar \
	$(lib.dir)/commons-codec/commons-codec/1.10/commons-codec-1.10.jar \
	$(lib.dir)/org/apache/httpcomponents/httpcore/4.4.4/httpcore-4.4.4.jar \
	$(lib.dir)/commons-logging/commons-logging/1.2/commons-logging-1.2.jar

spring-beans.jars = \
	$(lib.dir)/org/springframework/spring-beans/4.2.3.RELEASE/spring-beans-4.2.3.RELEASE.jar \
	$(lib.dir)/org/springframework/spring-core/4.2.3.RELEASE/spring-core-4.2.3.RELEASE.jar \
	$(lib.dir)/commons-logging/commons-logging/1.2/commons-logging-1.2.jar

servlet.api.jars  =\
	$(lib.dir)/javax/servlet/javax.servlet-api/4.0.0-b01/javax.servlet-api-4.0.0-b01.jar


all_maven_jars = $(sort ${servlet.api.jars} ${spring-beans.jars} ${apache.httpclient.jars} ${slf4j.jars} ${jtidy.jars} ${twitter.hbc.jars} ${apache.commons.cli} ${org.scribe.jars} ${google.gson.jars} ${sqlite3.jdbc.jar})


all: mosaicofpictures flickrrss geneticpainting json2xml twittergraph twitterfollow miniivy twitter01



$(eval $(call compile,miniivy,sandbox.MiniIvy,))
$(eval $(call compile,twitter01,sandbox.Twitter01, ${twitter.hbc.jars}))
$(eval $(call compile,twitterfollow,sandbox.TwitterFollow, ${apache.commons.cli} ${org.scribe.jars} ${google.gson.jars}))
$(eval $(call compile,twitteruserlookup,sandbox.TwitterUserLookup, ${apache.commons.cli} ${org.scribe.jars} ${google.gson.jars}))
$(eval $(call compile,twittergraph,sandbox.TwitterGraph, ${sqlite3.jdbc.jar} ${apache.commons.cli} ${org.scribe.jars} ${google.gson.jars}))
$(eval $(call compile,json2xml,sandbox.Json2Xml,${google.gson.jars}))
$(eval $(call compile,geneticpainting,sandbox.GeneticPainting,${apache.commons.cli}))
$(eval $(call compile,flickrrss,sandbox.FlickrRss,${apache.commons.cli} ${slf4j.jars} ${org.scribe.jars}))
$(eval $(call compile,mosaicofpictures,sandbox.MosaicOfPictures,${apache.commons.cli} ${slf4j.jars}))

$(bin.dir)/avdl2xml.jar: ./src/sandbox/Avdl2Xml.jj
	mkdir -p tmp $(dir $@)
	${javacc.exe} -OUTPUT_DIRECTORY=tmp/sandbox -TOKEN_MANAGER_USES_PARSER=true $<
	javac -d tmp -sourcepath tmp $(addprefix tmp/sandbox/,$(addsuffix .java,Avdl2XmlConstants Avdl2Xml Avdl2XmlTokenManager ParseException SimpleCharStream Token TokenMgrError))
	jar cvf $@ -C tmp .
	#rm -rf tmp
	$(foreach F,alleleAnnotationmethods  alleleAnnotations  annotateAllelemethods beacon  genotypephenotype common metadata, echo "${F}" && curl -L -o "${F}.avdl" "https://raw.githubusercontent.com/ga4gh/schemas/master/src/main/resources/avro/${F}.avdl" && cat   "${F}.avdl" | java -cp $@ sandbox.Avdl2Xml  | xmllint --format - ; )

common.avdl :
	curl -o $@ -L "https://raw.githubusercontent.com/ga4gh/schemas/master/src/main/resources/avro/$@"


${all_maven_jars}  : 
	mkdir -p $(dir $@) && wget -O "$@" "http://central.maven.org/maven2/$(patsubst ${lib.dir}/%,%,$@)"

