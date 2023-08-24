package sandbox.tools.tonic;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


import sandbox.iterator.AbstractIterator;
import sandbox.util.stream.MyCollectors;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@FunctionalInterface
interface XYPredicate {
	public boolean test(int x,int y);
	}


abstract class Matrix {
	public abstract void set(int x,int y,int v);
	public abstract int get(int x,int y);
	public abstract int getWidth();
	public abstract int getHeight();
	void print(PrintStream out) {
		for(int y=0;y< getHeight();y++) {
			for(int x=0;x<getWidth();++x) {
				out.print(get(x,y));
				}
			out.println();
			}
		}
	}


public class Tonic {
private final static int EMPTY =-1;
private final Random random = new Random();

private class Area {
	final Set<Integer> indexes=new HashSet();
}

private class MatrixImpl extends Matrix {
	final int[] data;
	private final int width;
	private final int height;
	
	MatrixImpl(int width,int height) {
		this.width = width;
		this.height = height;
		this.data = new int[width*height];
		Arrays.fill(this.data,EMPTY);
		}
	
	protected IntPredicate toIndexPreficate(final XYPredicate pred) {
		return IDX -> pred.test(toX(IDX),toY(IDX));
		}
	
	private int toIndex(int x, int y) {
		return y*getWidth()+x;
	}
	
	public int toX(int index) {
		return index%getWidth();
		}
	public int toY(int index) {
		return index/getWidth();
		}
	@Override
	public int getWidth() { return this.width;}
	@Override
	public int getHeight() { return this.height;}
	
	public void set(int index,int v) {
		this.data[index]=v;
	}
	
	public int get(int index) {
		return this.data[index];
	}
	IntStream stream(final XYPredicate pred) {
		return stream(toIndexPreficate(pred));
		}
	IntStream stream(final IntPredicate pred) {
		return stream().filter(pred);
		}
	IntStream stream() {
		return IntStream.rangeClosed(0, size());
		}
	int size() { return data.length;}
	@Override
	public void set(int x,int y,int v) {
		set(toIndex(x,y),v);
		}
	@Override
	public int get(int x,int y) {
		return get(toIndex(x,y));
		}
	
	void createAreaAt(int x, int y) {
		if(get(x,y)!=EMPTY) throw new IllegalStateException();
		
		}
	IntStream getNeighbours(int index) {
		final int[] array = new int[8];
		int k=0;
		final int x1 = toX(index);
		final int y1 = toY(index);
		for(int i=-1;i<=1;i++) {
			if(x1+i<0) continue;
			if(x1+i>=getWidth()) continue;
			for(int j=-1;j<=1;j++) {
				if(i==j) continue;
				if(y1+j<0) continue;
				if(y1+j>=getHeight()) continue;
				array[k++]=toIndex(x1+i, y1+j);
				}
			}
		return Arrays.stream(array, 0, k);
		}


	}

void make(final MatrixImpl matrix) {
	final Set<Integer> zeroToNine = Collections.unmodifiableSet(
			IntStream.of(0,1,2,3,4,5,6,7,8).
			mapToObj(I->I).
			collect(Collectors.toSet()));
	
	final Map<Integer,Integer> filled=new HashMap<>();
	while(filled.size() < matrix.size()) {
		Optional<Integer> start= matrix.
			stream(P->matrix.get(P)==EMPTY).
			mapToObj(I->I).
			collect(MyCollectors.shuffle()).
			findAny();
		if(!start.isPresent()) break;
		Area area=new Area();
		area.indexes.add(start.get());
		final Set<Integer> usedNumbers = matrix.getNeighbours(start.get()).
			 mapToObj(IDX->matrix.get(IDX)).
			 filter(V->!V.equals(EMPTY)).
		 	 collect(Collectors.toSet());
		final Set<Integer> remain = new HashSet<Integer>(zeroToNine);
		remain.removeAll(usedNumbers);
		final Optional<Integer> chooseNum = remain.stream().collect(MyCollectors.shuffle(random)).findFirst();
		if(!chooseNum.isPresent()) continue;
	
		filled.put(start.get(),chooseNum.get());
		while(area.indexes.size()<9) {
			List<Integer> L=new ArrayList<>(area.indexes);
			Collections.shuffle(L, random);
			int k;
			for(k=0;k< L.size();k++) {
				final Optional<Integer> opt2 = matrix.getNeighbours(L.get(k)).
					mapToObj(I->I).
					filter(IDX->!filled.containsKey(IDX)).
					collect(MyCollectors.shuffle(random)).
					findAny();
				if(!opt2.isPresent()) continue;
				
				}
			if(k==L.size()) break;
			}
		}
	}

private void doWork() {
	final MatrixImpl matrix = new MatrixImpl(
		15+random.nextInt(10),
		15+random.nextInt(10)
		);
	matrix.createAreaAt(
			random.nextInt(matrix.getWidth()),
			random.nextInt(matrix.getHeight())
			);
	
	matrix.print(System.out);
	}


public static void main(final String[] args) {
	new Tonic().doWork();
	}
}
