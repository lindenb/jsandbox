javacc=java -cp /home/lindenb/packages/javacc/target/javacc.jar javacc
MiniLang2Parser.java : Grammar.jj
	$(javacc) -OUTPUT_DIRECTORY=$(dir $@) $<
