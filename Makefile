bin.dir = dist
javacc.exe ?= javacc

all: $(bin.dir)/miniivy.jar $(bin.dir)/avdl2xml.jar

$(bin.dir)/miniivy.jar : src/sandbox/MiniIvy.java
	mkdir -p tmp/META-INF $(dir $@)
	echo "Manifest-Version: 1.0" > tmp/META-INF/MANIFEST.MF
	echo "Main-Class: sandbox.MiniIvy" >> tmp/META-INF/MANIFEST.MF
	javac -d tmp -sourcepath src  src/sandbox/MiniIvy.java
	jar cvfm $@ tmp/META-INF/MANIFEST.MF -C tmp .
	rm -rf tmp

$(bin.dir)/avdl2xml.jar: ./src/sandbox/Avdl2Xml.jj
	mkdir -p tmp $(dir $@)
	${javacc.exe} -OUTPUT_DIRECTORY=tmp/sandbox -TOKEN_MANAGER_USES_PARSER=true $<
	javac -d tmp -sourcepath tmp $(addprefix tmp/sandbox/,$(addsuffix .java,Avdl2XmlConstants Avdl2Xml Avdl2XmlTokenManager ParseException SimpleCharStream Token TokenMgrError))
	jar cvf $@ -C tmp .
	#rm -rf tmp
	$(foreach F,alleleAnnotationmethods  alleleAnnotations  annotateAllelemethods beacon  genotypephenotype common metadata, echo "${F}" && curl -L -o "${F}.avdl" "https://raw.githubusercontent.com/ga4gh/schemas/master/src/main/resources/avro/${F}.avdl" && cat   "${F}.avdl" | java -cp $@ sandbox.Avdl2Xml  | xmllint --format - ; )

common.avdl :
	curl -o $@ -L "https://raw.githubusercontent.com/ga4gh/schemas/master/src/main/resources/avro/$@"
