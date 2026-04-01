package sandbox.tools.interpolate;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.util.Precision;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.io.IOUtils;
import sandbox.lang.StringUtils;

public class Interpolator extends Launcher {
	private static final Logger LOG = Logger.builder(Interpolator.class).build();
	
	@Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
	private Path output = null;
	@Parameter(names= {"--xy","--training"},description="traing data. Tab delimited XY",required=true)
	private Path xyPath = null;

	private static class XY {
		double x;
		double y;
		XY(double x,double y) {
		this.x = x;
		this.y = y;
		}
		public double getX()
			{
			return x;
			}
		public double getY()
			{
			return y;
			}
		@Override
		public String toString()
			{
			return String.valueOf(x)+" "+y;
			}
	}
	
	@Override
	public int doWork(List<String> args)
		{
		try {
			final String input = oneFileOrNull(args);
			final List<XY> xyL = new ArrayList<XY>();
			try(BufferedReader br= IOUtils.openBufferedReaderFromPath(xyPath)) {
				br.lines().
					filter(S->!(S.startsWith("#") || StringUtils.isBlank(S))).
					forEach(R->{
						int tab = R.indexOf('\t');
						double x = Double.parseDouble(R.substring(0,tab));
						double y = Double.parseDouble(R.substring(tab+1));
						xyL.add(new XY(x,y));
						});
				}
			Collections.sort(xyL,(A,B)->Double.compare(A.getX(), B.getX()));

			int i=0;
			int k=0;
			while(i<xyL.size()) {
				int j=i+1;
				while(j < xyL.size()) {
					if(!Precision.equals(xyL.get(i).getX(),xyL.get(j).getX())) {
						break;
						}
					j++;
					}
				double[] y2= xyL.subList(i, j).stream().mapToDouble(R->R.getY()).toArray();
				xyL.get(k).x = xyL.get(i).getX();
				xyL.get(k).y = new Median().evaluate(y2);
				k++;
				i=j;
				}
			while(xyL.size()>k) xyL.remove(xyL.size()-1);
			Collections.sort(xyL,(A,B)->Double.compare(A.getY(), B.getY()));

			System.err.println(xyL);
			double[] x = xyL.stream().mapToDouble(R->R.getX()).toArray();
			double[] y = xyL.stream().mapToDouble(R->R.getY()).toArray();
			final UnivariateInterpolator univariateInterpolator = new LoessInterpolator();
			final UnivariateFunction univariateFunction= univariateInterpolator.interpolate(x, y);
			
			
			try(BufferedReader br= super.openBufferedReader(input)) {
				try(PrintWriter pw = IOUtils.openPathAsPrintWriter(output)) {
					String line;
					while((line=br.readLine())!=null) {
						if(line.startsWith("#") || StringUtils.isBlank(line)) continue;
						double v = Double.parseDouble(line);
						pw.println(v+"\t"+univariateFunction.value(v));
						}
					pw.flush();
					}
				}
			
			return 0;
			}
		catch(final Throwable err) {
			LOG.error(err);
			return -1;
			}
		}
	
	public static void main(String[] args) {
		new Interpolator().instanceMainWithExit(args);
		}
	
	}
