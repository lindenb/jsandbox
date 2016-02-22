/**

Les donnees de bouletmaton sont isssue de  http://www.zanorg.net/bouletmaton/
(c) Boulet et Kek / bouletcorp.com  et zanorg.com


**//
package sandbox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


public class BouletMaton {
	  private static final int WIDTH =  550;
	  private static final int HEIGHT = 646;
	  private enum Part {
		  Boucle(){
				@Override boolean match(String s) { return s.startsWith("boucle");} 
		  },
		  Eye() {
			@Override boolean match(String s) { return s.startsWith("ye");} 
		  };
		  
		  abstract boolean match(String s);
	  }

private static final List<String> PICTS= Arrays.asList("boucle10","boucle1","boucle2","boucle3","boucle4","boucle5",
		"boucle6","boucle7","boucle8","boucle9","chapo10","chapo11","chapo12",
		"chapo13","chapo14","chapo15","chapo16","chapo17","chapo18","chapo19",
		"chapo1","chapo2","chapo3","chapo4","chapo5","chapo6","chapo7",
		"chapo8","chapo9","col10","col11","col1","col2","col3",
		"col4","col5","col6","col7","col8","col9","coul10/bar10",
		"coul10/bar11","coul10/bar12","coul10/bar13","coul10/bar14","coul10/bar1","coul10/bar2","coul10/bar3",
		"coul10/bar4","coul10/bar5","coul10/bar6","coul10/bar7","coul10/bar8","coul10/bar9","coul10/coi10",
		"coul10/coi11","coul10/coi12","coul10/coi13","coul10/coi14","coul10/coi15","coul10/coi16","coul10/coi17",
		"coul10/coi18","coul10/coi19","coul10/coi1","coul10/coi20","coul10/coi21","coul10/coi22","coul10/coi23",
		"coul10/coi24","coul10/coi25","coul10/coi26","coul10/coi27","coul10/coi28","coul10/coi29","coul10/coi2",
		"coul10/coi30","coul10/coi31","coul10/coi32","coul10/coi33","coul10/coi3","coul10/coi4","coul10/coi5",
		"coul10/coi6","coul10/coi7","coul10/coi8","coul10/coi9","coul11/bar10","coul11/bar11","coul11/bar12",
		"coul11/bar13","coul11/bar14","coul11/bar1","coul11/bar2","coul11/bar3","coul11/bar4","coul11/bar5",
		"coul11/bar6","coul11/bar714","coul11/bar715","coul11/bar716","coul11/bar717","coul11/bar718","coul11/bar719",
		"coul11/bar720","coul11/bar721","coul11/bar722","coul11/bar723","coul11/bar724","coul11/bar725","coul11/bar726",
		"coul11/bar7","coul11/bar8","coul11/bar9","coul11/coi10","coul11/coi11","coul11/coi12","coul11/coi13",
		"coul11/coi14","coul11/coi15","coul11/coi16","coul11/coi17","coul11/coi18","coul11/coi19","coul11/coi1",
		"coul11/coi20","coul11/coi21","coul11/coi22","coul11/coi23","coul11/coi24","coul11/coi25","coul11/coi26",
		"coul11/coi27","coul11/coi28","coul11/coi29","coul11/coi2","coul11/coi30","coul11/coi31","coul11/coi32",
		"coul11/coi33","coul11/coi3","coul11/coi4","coul11/coi5","coul11/coi6","coul11/coi7","coul11/coi8",
		"coul11/coi9","coul12/bar10","coul12/bar11","coul12/bar12","coul12/bar13","coul12/bar14","coul12/bar1",
		"coul12/bar2","coul12/bar3","coul12/bar4","coul12/bar5","coul12/bar6","coul12/bar7","coul12/bar8",
		"coul12/bar9","coul12/coi10","coul12/coi11","coul12/coi12","coul12/coi13","coul12/coi14","coul12/coi15",
		"coul12/coi16","coul12/coi17","coul12/coi18","coul12/coi19","coul12/coi1","coul12/coi20","coul12/coi21",
		"coul12/coi22","coul12/coi23","coul12/coi24","coul12/coi25","coul12/coi26","coul12/coi27","coul12/coi28",
		"coul12/coi29","coul12/coi2","coul12/coi30","coul12/coi31","coul12/coi32","coul12/coi33","coul12/coi3",
		"coul12/coi4","coul12/coi5","coul12/coi6","coul12/coi7","coul12/coi8","coul12/coi9","coul1/bar10",
		"coul1/bar11","coul1/bar12","coul1/bar13","coul1/bar14","coul1/bar1","coul1/bar2","coul1/bar3",
		"coul1/bar4","coul1/bar5","coul1/bar6","coul1/bar7","coul1/bar8","coul1/bar9","coul1/coi10",
		"coul1/coi11","coul1/coi12","coul1/coi13","coul1/coi14","coul1/coi15","coul1/coi16","coul1/coi17",
		"coul1/coi18","coul1/coi19","coul1/coi1","coul1/coi20","coul1/coi21","coul1/coi22","coul1/coi23",
		"coul1/coi24","coul1/coi25","coul1/coi26","coul1/coi27","coul1/coi28","coul1/coi29","coul1/coi2",
		"coul1/coi30","coul1/coi31","coul1/coi32","coul1/coi33","coul1/coi3","coul1/coi4","coul1/coi5",
		"coul1/coi6","coul1/coi7","coul1/coi8","coul1/coi9","coul2/bar10","coul2/bar11","coul2/bar12",
		"coul2/bar13","coul2/bar14","coul2/bar1","coul2/bar2","coul2/bar3","coul2/bar4","coul2/bar5",
		"coul2/bar6","coul2/bar7","coul2/bar8","coul2/bar9","coul2/coi10","coul2/coi11","coul2/coi12",
		"coul2/coi13","coul2/coi14","coul2/coi15","coul2/coi16","coul2/coi17","coul2/coi18","coul2/coi19",
		"coul2/coi1","coul2/coi20","coul2/coi21","coul2/coi22","coul2/coi23","coul2/coi24","coul2/coi25",
		"coul2/coi26","coul2/coi27","coul2/coi28","coul2/coi29","coul2/coi2","coul2/coi30","coul2/coi31",
		"coul2/coi32","coul2/coi33","coul2/coi3","coul2/coi4","coul2/coi5","coul2/coi6","coul2/coi7",
		"coul2/coi8","coul2/coi9","coul3/bar10","coul3/bar11","coul3/bar12","coul3/bar13","coul3/bar14",
		"coul3/bar1","coul3/bar2","coul3/bar3","coul3/bar4","coul3/bar5","coul3/bar6","coul3/bar7",
		"coul3/bar8","coul3/bar9","coul3/coi10","coul3/coi11","coul3/coi12","coul3/coi13","coul3/coi14",
		"coul3/coi15","coul3/coi16","coul3/coi17","coul3/coi18","coul3/coi19","coul3/coi1","coul3/coi20",
		"coul3/coi21","coul3/coi22","coul3/coi23","coul3/coi24","coul3/coi25","coul3/coi26","coul3/coi27",
		"coul3/coi28","coul3/coi29","coul3/coi2","coul3/coi30","coul3/coi31","coul3/coi32","coul3/coi33",
		"coul3/coi3","coul3/coi4","coul3/coi5","coul3/coi6","coul3/coi7","coul3/coi8","coul3/coi9",
		"coul4/bar10","coul4/bar11","coul4/bar12","coul4/bar13","coul4/bar14","coul4/bar1","coul4/bar2",
		"coul4/bar3","coul4/bar4","coul4/bar5","coul4/bar6","coul4/bar7","coul4/bar8","coul4/bar9",
		"coul4/coi10","coul4/coi11","coul4/coi12","coul4/coi13","coul4/coi14","coul4/coi15","coul4/coi16",
		"coul4/coi17","coul4/coi18","coul4/coi19","coul4/coi1","coul4/coi20","coul4/coi21","coul4/coi22",
		"coul4/coi23","coul4/coi24","coul4/coi25","coul4/coi26","coul4/coi27","coul4/coi28","coul4/coi29",
		"coul4/coi2","coul4/coi30","coul4/coi31","coul4/coi32","coul4/coi33","coul4/coi3","coul4/coi4",
		"coul4/coi5","coul4/coi6","coul4/coi7","coul4/coi8","coul4/coi9","coul5/bar10","coul5/bar11",
		"coul5/bar12","coul5/bar13","coul5/bar14","coul5/bar1","coul5/bar2","coul5/bar3","coul5/bar4",
		"coul5/bar5","coul5/bar6","coul5/bar7","coul5/bar8","coul5/bar9","coul5/coi10","coul5/coi11",
		"coul5/coi12","coul5/coi13","coul5/coi14","coul5/coi15","coul5/coi16","coul5/coi17","coul5/coi18",
		"coul5/coi19","coul5/coi1","coul5/coi20","coul5/coi21","coul5/coi22","coul5/coi23","coul5/coi24",
		"coul5/coi25","coul5/coi26","coul5/coi27","coul5/coi28","coul5/coi29","coul5/coi2","coul5/coi30",
		"coul5/coi31","coul5/coi32","coul5/coi33","coul5/coi3","coul5/coi4","coul5/coi5","coul5/coi6",
		"coul5/coi7","coul5/coi8","coul5/coi9","coul6/bar10","coul6/bar11","coul6/bar12","coul6/bar13",
		"coul6/bar14","coul6/bar1","coul6/bar2","coul6/bar3","coul6/bar4","coul6/bar5","coul6/bar6",
		"coul6/bar7","coul6/bar8","coul6/bar9","coul6/coi10","coul6/coi11","coul6/coi12","coul6/coi13",
		"coul6/coi14","coul6/coi15","coul6/coi16","coul6/coi17","coul6/coi18","coul6/coi19","coul6/coi1",
		"coul6/coi20","coul6/coi21","coul6/coi22","coul6/coi23","coul6/coi24","coul6/coi25","coul6/coi26",
		"coul6/coi27","coul6/coi28","coul6/coi29","coul6/coi2","coul6/coi30","coul6/coi31","coul6/coi32",
		"coul6/coi33","coul6/coi3","coul6/coi4","coul6/coi5","coul6/coi6","coul6/coi7","coul6/coi8",
		"coul6/coi9","coul7/bar10","coul7/bar11","coul7/bar12","coul7/bar13","coul7/bar14","coul7/bar1",
		"coul7/bar2","coul7/bar3","coul7/bar4","coul7/bar5","coul7/bar6","coul7/bar7","coul7/bar8",
		"coul7/bar9","coul7/coi10","coul7/coi11","coul7/coi12","coul7/coi13","coul7/coi14","coul7/coi15",
		"coul7/coi16","coul7/coi17","coul7/coi18","coul7/coi19","coul7/coi1","coul7/coi20","coul7/coi21",
		"coul7/coi22","coul7/coi23","coul7/coi24","coul7/coi25","coul7/coi26","coul7/coi27","coul7/coi28",
		"coul7/coi29","coul7/coi2","coul7/coi30","coul7/coi31","coul7/coi32","coul7/coi33","coul7/coi3",
		"coul7/coi4","coul7/coi5","coul7/coi6","coul7/coi7","coul7/coi8","coul7/coi9","coul8/bar10",
		"coul8/bar11","coul8/bar12","coul8/bar13","coul8/bar14","coul8/bar1","coul8/bar2","coul8/bar3",
		"coul8/bar4","coul8/bar5","coul8/bar6","coul8/bar7","coul8/bar8","coul8/bar9","coul8/coi10",
		"coul8/coi11","coul8/coi12","coul8/coi13","coul8/coi14","coul8/coi15","coul8/coi16","coul8/coi17",
		"coul8/coi18","coul8/coi19","coul8/coi1","coul8/coi20","coul8/coi21","coul8/coi22","coul8/coi23",
		"coul8/coi24","coul8/coi25","coul8/coi26","coul8/coi27","coul8/coi28","coul8/coi29","coul8/coi2",
		"coul8/coi30","coul8/coi31","coul8/coi32","coul8/coi33","coul8/coi3","coul8/coi4","coul8/coi5",
		"coul8/coi6","coul8/coi7","coul8/coi8","coul8/coi9","coul9/bar10","coul9/bar11","coul9/bar12",
		"coul9/bar13","coul9/bar14","coul9/bar1","coul9/bar2","coul9/bar3","coul9/bar4","coul9/bar5",
		"coul9/bar6","coul9/bar7","coul9/bar8","coul9/bar9","coul9/coi10","coul9/coi11","coul9/coi12",
		"coul9/coi13","coul9/coi14","coul9/coi15","coul9/coi16","coul9/coi17","coul9/coi18","coul9/coi19",
		"coul9/coi1","coul9/coi20","coul9/coi21","coul9/coi22","coul9/coi23","coul9/coi24","coul9/coi25",
		"coul9/coi26","coul9/coi27","coul9/coi28","coul9/coi29","coul9/coi2","coul9/coi30","coul9/coi31",
		"coul9/coi32","coul9/coi33","coul9/coi3","coul9/coi4","coul9/coi5","coul9/coi6","coul9/coi7",
		"coul9/coi8","coul9/coi9","fleche","indic","loading","lunette10","lunette11",
		"lunette12","lunette13","lunette14","lunette15","lunette16","lunette1","lunette2",
		"lunette3","lunette4","lunette5","lunette6","lunette7","lunette8","lunette9",
		"peau1/bou10","peau1/bou11","peau1/bou12","peau1/bou13","peau1/bou14","peau1/bou15","peau1/bou16",
		"peau1/bou17","peau1/bou18","peau1/bou19","peau1/bou1","peau1/bou20","peau1/bou21","peau1/bou22",
		"peau1/bou23","peau1/bou24","peau1/bou25","peau1/bou26","peau1/bou27","peau1/bou28","peau1/bou29",
		"peau1/bou2","peau1/bou30","peau1/bou31","peau1/bou32","peau1/bou33","peau1/bou34","peau1/bou35",
		"peau1/bou36","peau1/bou37","peau1/bou38","peau1/bou39","peau1/bou3","peau1/bou40","peau1/bou41",
		"peau1/bou42","peau1/bou43","peau1/bou44","peau1/bou45","peau1/bou46","peau1/bou47","peau1/bou48",
		"peau1/bou49","peau1/bou4","peau1/bou50","peau1/bou51","peau1/bou52","peau1/bou53","peau1/bou54",
		"peau1/bou55","peau1/bou56","peau1/bou57","peau1/bou58","peau1/bou59","peau1/bou5","peau1/bou60",
		"peau1/bou61","peau1/bou62","peau1/bou6","peau1/bou7","peau1/bou8","peau1/bou9","peau1/cor10",
		"peau1/cor11","peau1/cor12","peau1/cor13","peau1/cor14","peau1/cor15","peau1/cor16","peau1/cor17",
		"peau1/cor18","peau1/cor19","peau1/cor1","peau1/cor20","peau1/cor21","peau1/cor22","peau1/cor23",
		"peau1/cor24","peau1/cor25","peau1/cor26","peau1/cor27","peau1/cor2","peau1/cor3","peau1/cor4",
		"peau1/cor5","peau1/cor6","peau1/cor7","peau1/cor8","peau1/cor9","peau1/divers10","peau1/divers11",
		"peau1/divers12","peau1/divers13","peau1/divers1","peau1/divers2","peau1/divers3","peau1/divers4","peau1/divers5",
		"peau1/divers6","peau1/divers7","peau1/divers8","peau1/divers9","peau1/nez10","peau1/nez11","peau1/nez12",
		"peau1/nez13","peau1/nez14","peau1/nez15","peau1/nez16","peau1/nez17","peau1/nez18","peau1/nez19",
		"peau1/nez1","peau1/nez20","peau1/nez21","peau1/nez22","peau1/nez23","peau1/nez24","peau1/nez25",
		"peau1/nez26","peau1/nez2","peau1/nez3","peau1/nez4","peau1/nez5","peau1/nez6","peau1/nez7",
		"peau1/nez8","peau1/nez9","peau1/ore1","peau1/ore2","peau1/ore3","peau1/ore4","peau1/ore5",
		"peau1/ore6","peau1/ore7","peau1/sec10","peau1/sec11","peau1/sec12","peau1/sec1","peau1/sec2",
		"peau1/sec3","peau1/sec4","peau1/sec5","peau1/sec6","peau1/sec7","peau1/sec8","peau1/sec9",
		"peau1/tete10","peau1/tete11","peau1/tete12","peau1/tete13","peau1/tete14","peau1/tete15","peau1/tete16",
		"peau1/tete17","peau1/tete18","peau1/tete19","peau1/tete1","peau1/tete20","peau1/tete21","peau1/tete22",
		"peau1/tete23","peau1/tete24","peau1/tete25","peau1/tete26","peau1/tete27","peau1/tete28","peau1/tete29",
		"peau1/tete2","peau1/tete30","peau1/tete31","peau1/tete32","peau1/tete33","peau1/tete34","peau1/tete35",
		"peau1/tete36","peau1/tete37","peau1/tete38","peau1/tete39","peau1/tete3","peau1/tete40","peau1/tete41",
		"peau1/tete42","peau1/tete43","peau1/tete44","peau1/tete45","peau1/tete46","peau1/tete47","peau1/tete48",
		"peau1/tete49","peau1/tete4","peau1/tete50","peau1/tete51","peau1/tete52","peau1/tete53","peau1/tete54",
		"peau1/tete5","peau1/tete6","peau1/tete7","peau1/tete8","peau1/tete9","peau2/bou10","peau2/bou11",
		"peau2/bou12","peau2/bou13","peau2/bou14","peau2/bou15","peau2/bou16","peau2/bou17","peau2/bou18",
		"peau2/bou19","peau2/bou1","peau2/bou20","peau2/bou21","peau2/bou22","peau2/bou23","peau2/bou24",
		"peau2/bou25","peau2/bou26","peau2/bou27","peau2/bou28","peau2/bou29","peau2/bou2","peau2/bou30",
		"peau2/bou31","peau2/bou32","peau2/bou33","peau2/bou34","peau2/bou35","peau2/bou36","peau2/bou37",
		"peau2/bou38","peau2/bou39","peau2/bou3","peau2/bou40","peau2/bou41","peau2/bou42","peau2/bou43",
		"peau2/bou44","peau2/bou45","peau2/bou46","peau2/bou47","peau2/bou48","peau2/bou49","peau2/bou4",
		"peau2/bou50","peau2/bou51","peau2/bou52","peau2/bou53","peau2/bou54","peau2/bou55","peau2/bou56",
		"peau2/bou57","peau2/bou58","peau2/bou59","peau2/bou5","peau2/bou60","peau2/bou61","peau2/bou62",
		"peau2/bou6","peau2/bou7","peau2/bou8","peau2/bou9","peau2/cor10","peau2/cor11","peau2/cor12",
		"peau2/cor13","peau2/cor14","peau2/cor15","peau2/cor16","peau2/cor17","peau2/cor18","peau2/cor19",
		"peau2/cor1","peau2/cor20","peau2/cor21","peau2/cor22","peau2/cor23","peau2/cor24","peau2/cor25",
		"peau2/cor26","peau2/cor27","peau2/cor2","peau2/cor3","peau2/cor4","peau2/cor5","peau2/cor6",
		"peau2/cor7","peau2/cor8","peau2/cor9","peau2/divers10","peau2/divers11","peau2/divers12","peau2/divers13",
		"peau2/divers1","peau2/divers2","peau2/divers3","peau2/divers4","peau2/divers5","peau2/divers6","peau2/divers7",
		"peau2/divers8","peau2/divers9","peau2/nez10","peau2/nez11","peau2/nez12","peau2/nez13","peau2/nez14",
		"peau2/nez15","peau2/nez16","peau2/nez17","peau2/nez18","peau2/nez19","peau2/nez1","peau2/nez20",
		"peau2/nez21","peau2/nez22","peau2/nez23","peau2/nez24","peau2/nez25","peau2/nez26","peau2/nez2",
		"peau2/nez3","peau2/nez4","peau2/nez5","peau2/nez6","peau2/nez7","peau2/nez8","peau2/nez9",
		"peau2/ore1","peau2/ore2","peau2/ore3","peau2/ore4","peau2/ore5","peau2/ore6","peau2/ore7",
		"peau2/sec10","peau2/sec11","peau2/sec12","peau2/sec1","peau2/sec2","peau2/sec3","peau2/sec4",
		"peau2/sec5","peau2/sec6","peau2/sec7","peau2/sec8","peau2/sec9","peau2/tete10","peau2/tete11",
		"peau2/tete12","peau2/tete13","peau2/tete14","peau2/tete15","peau2/tete16","peau2/tete17","peau2/tete18",
		"peau2/tete19","peau2/tete1","peau2/tete20","peau2/tete21","peau2/tete22","peau2/tete23","peau2/tete24",
		"peau2/tete25","peau2/tete26","peau2/tete27","peau2/tete28","peau2/tete29","peau2/tete2","peau2/tete30",
		"peau2/tete31","peau2/tete32","peau2/tete33","peau2/tete34","peau2/tete35","peau2/tete36","peau2/tete37",
		"peau2/tete38","peau2/tete39","peau2/tete3","peau2/tete40","peau2/tete41","peau2/tete42","peau2/tete43",
		"peau2/tete44","peau2/tete45","peau2/tete46","peau2/tete47","peau2/tete48","peau2/tete49","peau2/tete4",
		"peau2/tete50","peau2/tete51","peau2/tete52","peau2/tete53","peau2/tete54","peau2/tete5","peau2/tete6",
		"peau2/tete7","peau2/tete8","peau2/tete9","peau3/bou10","peau3/bou11","peau3/bou12","peau3/bou13",
		"peau3/bou14","peau3/bou15","peau3/bou16","peau3/bou17","peau3/bou18","peau3/bou19","peau3/bou1",
		"peau3/bou20","peau3/bou21","peau3/bou22","peau3/bou23","peau3/bou24","peau3/bou25","peau3/bou26",
		"peau3/bou27","peau3/bou28","peau3/bou29","peau3/bou2","peau3/bou30","peau3/bou31","peau3/bou32",
		"peau3/bou33","peau3/bou34","peau3/bou35","peau3/bou36","peau3/bou37","peau3/bou38","peau3/bou39",
		"peau3/bou3","peau3/bou40","peau3/bou41","peau3/bou42","peau3/bou43","peau3/bou44","peau3/bou45",
		"peau3/bou46","peau3/bou47","peau3/bou48","peau3/bou49","peau3/bou4","peau3/bou50","peau3/bou51",
		"peau3/bou52","peau3/bou53","peau3/bou54","peau3/bou55","peau3/bou56","peau3/bou57","peau3/bou58",
		"peau3/bou59","peau3/bou5","peau3/bou60","peau3/bou61","peau3/bou62","peau3/bou6","peau3/bou7",
		"peau3/bou8","peau3/bou9","peau3/cor10","peau3/cor11","peau3/cor12","peau3/cor13","peau3/cor14",
		"peau3/cor15","peau3/cor16","peau3/cor17","peau3/cor18","peau3/cor19","peau3/cor1","peau3/cor20",
		"peau3/cor21","peau3/cor22","peau3/cor23","peau3/cor24","peau3/cor25","peau3/cor26","peau3/cor27",
		"peau3/cor2","peau3/cor3","peau3/cor4","peau3/cor5","peau3/cor6","peau3/cor7","peau3/cor8",
		"peau3/cor9","peau3/divers10","peau3/divers11","peau3/divers12","peau3/divers13","peau3/divers1","peau3/divers2",
		"peau3/divers3","peau3/divers4","peau3/divers5","peau3/divers6","peau3/divers7","peau3/divers8","peau3/divers9",
		"peau3/nez10","peau3/nez11","peau3/nez12","peau3/nez13","peau3/nez14","peau3/nez15","peau3/nez16",
		"peau3/nez17","peau3/nez18","peau3/nez19","peau3/nez1","peau3/nez20","peau3/nez21","peau3/nez22",
		"peau3/nez23","peau3/nez24","peau3/nez25","peau3/nez26","peau3/nez2","peau3/nez3","peau3/nez4",
		"peau3/nez5","peau3/nez6","peau3/nez7","peau3/nez8","peau3/nez9","peau3/ore1","peau3/ore2",
		"peau3/ore3","peau3/ore4","peau3/ore5","peau3/ore6","peau3/ore7","peau3/sec10","peau3/sec11",
		"peau3/sec12","peau3/sec1","peau3/sec2","peau3/sec3","peau3/sec4","peau3/sec5","peau3/sec6",
		"peau3/sec7","peau3/sec8","peau3/sec9","peau3/tete10","peau3/tete11","peau3/tete12","peau3/tete13",
		"peau3/tete14","peau3/tete15","peau3/tete16","peau3/tete17","peau3/tete18","peau3/tete19","peau3/tete1",
		"peau3/tete20","peau3/tete21","peau3/tete22","peau3/tete23","peau3/tete24","peau3/tete25","peau3/tete26",
		"peau3/tete27","peau3/tete28","peau3/tete29","peau3/tete2","peau3/tete30","peau3/tete31","peau3/tete32",
		"peau3/tete33","peau3/tete34","peau3/tete35","peau3/tete36","peau3/tete37","peau3/tete38","peau3/tete39",
		"peau3/tete3","peau3/tete40","peau3/tete41","peau3/tete42","peau3/tete43","peau3/tete44","peau3/tete45",
		"peau3/tete46","peau3/tete47","peau3/tete48","peau3/tete49","peau3/tete4","peau3/tete50","peau3/tete51",
		"peau3/tete52","peau3/tete53","peau3/tete54","peau3/tete5","peau3/tete6","peau3/tete7","peau3/tete8",
		"peau3/tete9","peau4/bou10","peau4/bou11","peau4/bou12","peau4/bou13","peau4/bou14","peau4/bou15",
		"peau4/bou16","peau4/bou17","peau4/bou18","peau4/bou19","peau4/bou1","peau4/bou20","peau4/bou21",
		"peau4/bou22","peau4/bou23","peau4/bou24","peau4/bou25","peau4/bou26","peau4/bou27","peau4/bou28",
		"peau4/bou29","peau4/bou2","peau4/bou30","peau4/bou31","peau4/bou32","peau4/bou33","peau4/bou34",
		"peau4/bou35","peau4/bou36","peau4/bou37","peau4/bou38","peau4/bou39","peau4/bou3","peau4/bou40",
		"peau4/bou41","peau4/bou42","peau4/bou43","peau4/bou44","peau4/bou45","peau4/bou46","peau4/bou47",
		"peau4/bou48","peau4/bou49","peau4/bou4","peau4/bou50","peau4/bou51","peau4/bou52","peau4/bou53",
		"peau4/bou54","peau4/bou55","peau4/bou56","peau4/bou57","peau4/bou58","peau4/bou59","peau4/bou5",
		"peau4/bou60","peau4/bou61","peau4/bou62","peau4/bou6","peau4/bou7","peau4/bou8","peau4/bou9",
		"peau4/cor10","peau4/cor11","peau4/cor12","peau4/cor13","peau4/cor14","peau4/cor15","peau4/cor16",
		"peau4/cor17","peau4/cor18","peau4/cor19","peau4/cor1","peau4/cor20","peau4/cor21","peau4/cor22",
		"peau4/cor23","peau4/cor24","peau4/cor25","peau4/cor26","peau4/cor27","peau4/cor2","peau4/cor3",
		"peau4/cor4","peau4/cor5","peau4/cor6","peau4/cor7","peau4/cor8","peau4/cor9","peau4/divers10",
		"peau4/divers11","peau4/divers12","peau4/divers13","peau4/divers1","peau4/divers2","peau4/divers3","peau4/divers4",
		"peau4/divers5","peau4/divers6","peau4/divers7","peau4/divers8","peau4/divers9","peau4/nez10","peau4/nez11",
		"peau4/nez12","peau4/nez13","peau4/nez14","peau4/nez15","peau4/nez16","peau4/nez17","peau4/nez18",
		"peau4/nez19","peau4/nez1","peau4/nez20","peau4/nez21","peau4/nez22","peau4/nez23","peau4/nez24",
		"peau4/nez25","peau4/nez26","peau4/nez2","peau4/nez3","peau4/nez4","peau4/nez5","peau4/nez6",
		"peau4/nez7","peau4/nez8","peau4/nez9","peau4/ore1","peau4/ore2","peau4/ore3","peau4/ore4",
		"peau4/ore5","peau4/ore6","peau4/ore7","peau4/sec10","peau4/sec11","peau4/sec12","peau4/sec1",
		"peau4/sec2","peau4/sec3","peau4/sec4","peau4/sec5","peau4/sec6","peau4/sec7","peau4/sec8",
		"peau4/sec9","peau4/tete10","peau4/tete11","peau4/tete12","peau4/tete13","peau4/tete14","peau4/tete15",
		"peau4/tete16","peau4/tete17","peau4/tete18","peau4/tete19","peau4/tete1","peau4/tete20","peau4/tete21",
		"peau4/tete22","peau4/tete23","peau4/tete24","peau4/tete25","peau4/tete26","peau4/tete27","peau4/tete28",
		"peau4/tete29","peau4/tete2","peau4/tete30","peau4/tete31","peau4/tete32","peau4/tete33","peau4/tete34",
		"peau4/tete35","peau4/tete36","peau4/tete37","peau4/tete38","peau4/tete39","peau4/tete3","peau4/tete40",
		"peau4/tete41","peau4/tete42","peau4/tete43","peau4/tete44","peau4/tete45","peau4/tete46","peau4/tete47",
		"peau4/tete48","peau4/tete49","peau4/tete4","peau4/tete50","peau4/tete51","peau4/tete52","peau4/tete53",
		"peau4/tete54","peau4/tete5","peau4/tete6","peau4/tete7","peau4/tete8","peau4/tete9","sec1",
		"sec2","sec3","sec4","sec5","so10","so11","so12",
		"so13","so14","so15","so16","so17","so18","so19",
		"so1","so20","so21","so2","so3","so4","so5",
		"so6","so7","so8","so9","tache1","tache2","tache3",
		"tache4","_vide","ye10","ye11","ye12","ye13","ye14",
		"ye15","ye16","ye17","ye18","ye19","ye1","ye20",
		"ye21","ye22","ye23","ye24","ye25","ye26","ye27",
		"ye28","ye29","ye2","ye30","ye31","ye32","ye33",
		"ye34","ye35","ye36","ye37","ye38","ye39","ye3",
		"ye40","ye41","ye42","ye43","ye44","ye45","ye46",
		"ye47","ye48","ye4","ye5","ye6","ye7","ye8",
		"ye9");

		private static final URL imageUrl(String s) {
			try {
				return new URL("http://www.zanorg.net/bouletmaton/assets/"+s+".png");
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
	
		private static class PersonFrame extends JFrame {
			private static final long serialVersionUID = 1L;
			private JPanel drawingArea =null;
			private Map<Part,ImageIcon> eyeIcon=new HashMap<>();
			
			private void insertMenu(final JMenu owner,final String path,final String command)
				{
				int n=path.length(),slash=path.length();
				for(int i=0;i< path.length();++i)
					{
					char c = path.charAt(i);
					if(slash==path.length() && c=='/') slash=i;
					else if(i>0 && n==path.length() && Character.isDigit(c)) n=i;
					}
				if(n!=path.length() || slash!=path.length())
					{
					int x=Math.min(n,slash);
					System.err.println(x+" "+path+" ("+command+") "+n+" "+slash+" "+x);
					String menuName= path.substring(0,x);

					JMenu menu=null;
					for(int i=0;i< owner.getItemCount();++i)
						{
						
						if(owner.getItem(i) instanceof JMenu && 
								owner.getItem(i).getText().equals(menuName))
							{
							
							menu=JMenu.class.cast(owner.getItem(i));
							break;
							}
						}
					if(menu==null) menu=new JMenu(menuName);
					owner.add(menu);
					if(x==slash) x++;
					insertMenu(menu, path.substring(x),command);
					}
				else
					{
					owner.add(new JMenuItem(path));
					}
				}
			
			PersonFrame() {
				this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				this.addWindowListener(new WindowAdapter() {
					
				});
				final JMenuBar bar = new JMenuBar();
				setJMenuBar(bar);
				JMenu menu = new JMenu("Components");
				bar.add(menu);
				for(final String s:PICTS) {
					insertMenu(menu,s,s);

					}
				final Dimension dimension = new Dimension(WIDTH, HEIGHT);
				this.drawingArea=new JPanel() {
					@Override
					protected void paintComponent(Graphics g) {
						paintDrawingArea(Graphics2D.class.cast(g));
						}
					};
					this.drawingArea.setOpaque(true);
				this.setContentPane(this.drawingArea);
				this.drawingArea.setSize(dimension);
				this.drawingArea.setMinimumSize(dimension);
				}
			
		private void paintDrawingArea(Graphics2D g) {
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(0,0,WIDTH,HEIGHT);
			if(this.eyeIcon!=null)
				{/*
				System.err.println("Print "+eyeIcon.getDescription()+" "+
						eyeIcon.getImageLoadStatus()+" "+eyeIcon.getIconWidth()+" "+eyeIcon.getIconWidth());
				this.eyeIcon.paintIcon(this.drawingArea, g, 0, 0);
				*/
				}
		}

			
			
		}

		public static void main(String[] args) throws Exception {
		final PersonFrame f=new PersonFrame();	
		f.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
					f.pack();
					f.setVisible(true);
			}
		});
		}
}
