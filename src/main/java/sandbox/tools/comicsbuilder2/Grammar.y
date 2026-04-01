

%{

package xx;
%union {
	String s;
	int i;
	}

%}

%token PAGE  WGET
%token<int> INTEGER
%token<String> STRING
%%


input: header directives

header: | PAGE '(' INTEGER ',' INTEGER ')' ';' ;

directives: directive | directives directive;

directive: WGET '(' STRING ')'  {
	String s = $3.s
	// HELLO WORLD
	};

%%
