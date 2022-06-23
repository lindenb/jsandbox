package sandbox.tools.donotuseexcel;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;


import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.io.IOUtils;

/*
 *  gunzip -c ~/data/GRCh37.gtf.gz  | cut -f 9 | tr ";" "\n" | grep -w gene_name | cut -d '"' -f 2 | uniq | sort | uniq > genes.txt
 * 
 * 
 * 
 */

public class DoNotUseExcel extends Launcher {
	private static final Logger LOG = Logger.builder(DoNotUseExcel.class).build();

	@Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
	private Path outPath = null;
	@Parameter(names= {"-g","--genes"},description="file containing common genes names")
	private Path genePath = null;
	@Parameter(names= {"--min-gene"},description="column must contain at least 'x' genes from the gene list")
	private int min_true_gene_count=1;
	@Parameter(names= {"--debug"},description="print debugging info")
	private boolean debug_flag=false;


	private final Set<String> geneNames = new HashSet<>();
	
	
	private static class ColumnInfo {
		final String name;
		ColumnInfo(final File f,Sheet sheet,int column) {
			this.name= f.toString()+" "+sheet.getSheetName()+" column:"+(column+1);
			}
		final Set<String> found_genes = new HashSet<>();
		final Set<String> found_dates = new HashSet<>();
		int count_gene_name = 0;
		int count_non_gene_name = 0;
		int count_bad_gene_name = 0;
		}

	private void debug(Object o) {
		if(!debug_flag) return;
		LOG.debug(o);
	}
	
	private boolean isBadDate(final Date date) {
		final Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int month = cal.get(Calendar.MONTH);
		if(month ==11 ) return true;
		if(month ==3 ) return true;
		if(month ==10 ) return true;
		if(month ==8 ) return true;
		return true;//TODO
		}
	
	
    private void scan(final PrintWriter pw, String filename) {
        try {
            final File xlsFile = new File(filename);
            if(!xlsFile.exists() || !xlsFile.canRead() || !xlsFile.isFile()) {
            	LOG.warning("cannot find/read file "+xlsFile);
            	return;
            	}
            LOG.info(filename);
        	final DataFormatter dataFormatter = new DataFormatter();

            final String name= xlsFile.getName().toLowerCase();
            if(!(name.endsWith(".xls") || name.endsWith(".xlsx"))) return;
            try(FileInputStream file = new FileInputStream(xlsFile)) {
            	debug("load workbook");
            	try(final Workbook workbook = WorkbookFactory.create(file)) {
            		debug("iterate");
            		for(int idx=0;idx< workbook.getNumberOfSheets();++idx) {
                		debug("load sheet "+(idx+1));
                		final Sheet sheet= workbook.getSheetAt(idx);
	                    final List<ColumnInfo> columns = new ArrayList<>();
	                    for(Row row : sheet) {
	                    	int y=0;
	                    	for(Cell cell:row) {
	                    		debug("cell "+(y+1));
	                    		while(columns.size()<=y) {
	                    			debug("adding sheet "+(y+1));
	                    			columns.add(new ColumnInfo(xlsFile,sheet,columns.size()));
	                    			}
	                    		final ColumnInfo col = columns.get(y);
	                    		final String value;
	                    		Date date = null;
	                    		switch(cell.getCellType()) {
	                    			case STRING: value = cell.getStringCellValue().toUpperCase(); break;
	                    			case NUMERIC: {
	                    				value=null;
	                    				if(DateUtil.isCellDateFormatted(cell)) {
	                    					date= cell.getDateCellValue();
	                    					}
	                    				break;
	                    				}
	                    			case FORMULA://cont
	                    			case BOOLEAN: //cont
	                    			case _NONE://cont
	                    			case BLANK:// cont
	                    			default: value=null; break;
	                    			}
	                    		y++;
	                    		if(!StringUtils.isBlank(value)) {
		                    		if(geneNames.contains(value)) {
		                    			col.count_gene_name++;
		                    			if(col.found_genes.size()<10) {
		                    				col.found_genes.add(value);
		                    				}
		                    			}
		                    		else
		                    			{
		                    			col.count_non_gene_name++;
		                    			}
		                    		}
	                    		else if(date!=null)
	                    			{
	                    			if(isBadDate(date)) {
		                    			col.count_bad_gene_name++;
		                    			// https://stackoverflow.com/a/7100727/58082
		                    			col.found_dates.add(dataFormatter.formatCellValue(cell));
		                    			}
	                    			}
	                    		}
	                    	}//end of row
	                    for(ColumnInfo ci:columns) {
	                    	if(ci.count_bad_gene_name==0) continue;
	                    	if(ci.count_gene_name<this.min_true_gene_count) continue;
	                    	pw.print(ci.name);
	                    	pw.print("\t");
	                    	pw.print(ci.count_gene_name);
	                    	pw.print("\t");
	                    	pw.print(String.join("|", ci.found_genes));
	                    	pw.print("\t");
	                    	pw.print(ci.count_non_gene_name);
	                    	pw.print("\t");
	                    	pw.print(ci.count_bad_gene_name);
	                    	pw.print("\t");
	                    	pw.print(String.join("|", ci.found_dates));
	                    	pw.println();
	                    	}
	                    }
	                }
                }
            }
        catch(java.lang.OutOfMemoryError err) {
        	System.gc();
        	LOG.error("Out of memory for "+filename);
        	}
        catch(org.apache.poi.hssf.OldExcelFormatException err) {
        	LOG.error("old format "+filename);
        	}
        catch(Throwable err) {
        	LOG.error("Error in "+filename);
            err.printStackTrace();            
            }
        }	

	
	@Override
	public int doWork(final List<String> args) {
		try {
			if(this.genePath!=null) {
				this.geneNames.addAll(
					Files.lines(genePath).
						filter(S->!StringUtils.isBlank(S) || S.startsWith("#")).
						map(S->S.toUpperCase()).
						filter(S->!S.equals("GENE")).
						filter(S->!S.equals("NAME")).
						collect(Collectors.toSet())
					);
					
				}
			String input  = oneFileOrNull(args);
			
            try(BufferedReader br = new BufferedReader(
            		input==null?new InputStreamReader(System.in, "UTF-8")
            		: new FileReader(new File(input))
            		)) {
	            	try(PrintWriter pw=IOUtils.openPathAsPrintWriter(this.outPath)) {
		                br.lines().forEach(L->scan(pw,L));
		                pw.flush();
		                }
				}
			return 0;
			} 
		catch(final Throwable err) {
			err.printStackTrace();
			return -1;
			}
		}
	
	public static void main(String[] args)
		{
		new DoNotUseExcel().instanceMainWithExit(args);
		}

	}
