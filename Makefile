bin.dir = dist

all: $(bin.dir)/minyivy.jar
	mkdir -p tmp 
	javac -d tmp -sourcepath src sandbox/FlickrRss.java

$(bin.dir)/minyivy.jar : src/sandbox/MiniIvy.java
	mkdir -p tmp $(dir $@)
	javac -d tmp -sourcepath src  src/sandbox/MiniIvy.java
	jar cvf $@ -C tmp .
	rm -rf tmp
