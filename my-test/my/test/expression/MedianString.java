package my.test.expression;

import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;

import org.h2.api.AggregateFunction;

public class MedianString implements AggregateFunction {

	private ArrayList<String> list = new ArrayList<>();

	public void add(Object value) {
		list.add(value.toString());
	}

	public Object getResult() {
		return list.get(list.size() / 2);
	}

	public int getType(int[] inputType) {
		return Types.VARCHAR;
	}

	public void init(Connection conn) {
		// nothing to do
	}

}
