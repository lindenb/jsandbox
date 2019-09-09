package sandbox.jcommander;

import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.converters.IParameterSplitter;

public class NoSplitter implements IParameterSplitter {
@Override
public List<String> split(String s) {
	return Arrays.asList(s);
	}
}
