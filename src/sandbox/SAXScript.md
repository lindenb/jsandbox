#Motivation

*SAXScript* is an event-driven **SAX** parser java program invoking some **javascript** callbacks.
It can be used to quickly write a piece of code to parse a huge XML file.

#Compilation

```
make saxscript
```

#Options

```
 -e,--expression <arg>           read javascript script from argument
 -f,--script <arg>               read javascript script from file
 -j,--json <arg>                 insert JSON document in the javascript
                                 context as 'userData' using google gson
                                 library. Default is null json element.
 -notns,--notns                  SAX parser is NOT namespace aware
 -valid,--valid                  SAX parser is validating

```

#Callbacks

```
function startDocument()
        {println("Start doc");}
function endDocument()
        {println("End doc");}
function startElement(uri,localName,name,atts)
        {
        print(""+__FILENAME__+" START uri: "+uri+" localName:"+localName);
        for(var i=0;atts!=undefined && i< atts.getLength();++i)
                {
                print(" @"+atts.getQName(i)+"="+atts.getValue(i));
                }
        println("");
        }
function characters(s)
        {println("Characters :" +s);}
function endElement(uri,localName,name)
        {println("END: uri: "+uri+" localName:"+localName);}


#Example

The following script scan a pubmed+XML document and count the number of time each journal (tag is "MedlineTA") was cited.

```javascript

/** current title */
var title=null;
/** map title->count */
var title2count = {};

 /** called when a new element is found, if it's  MedlineTA create a string*/
function startElement(uri,localName,name,atts)
        {
        if(name=="MedlineTA") title="";
        }

/** in text node and in  MedlineTA ? append the text to the title */
function characters(s)
        {
        if(title!=null) title+=s;
        }

/** end of element, if it's MedlineTA, add the title to the hash-table */
function endElement(uri,localName,name)
        {
        if(title!=null)
                {
                var c=title2count[title];
                title2count[title]=(c==null?1:c+1);
                title=null;
                }
        }
 /** called when the end of the document is found, print the result */
function endDocument()
        {
        for(var key in title2count)
                {
                print(key+"\t"+ title2count[key]);
                }
        }
```

Run:

```bash
$ xmllint --format "http://www.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=25,100,200,201,202,203&retmode=xml"  | \
	java -jar dist/saxscript.jar -f ~/script.js
	
Arzneimittelforschung	1
Birth Defects Orig Artic Ser	1
Comp Biochem Physiol C	4
```

