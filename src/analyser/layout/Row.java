package analyser.layout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import util.Util;

public class Row implements Cell, Iterable<Column> {

	private static int idGenerator = 0;

	private List<Column> columns = new ArrayList<Column>();
	private int id = ++idGenerator;

	private float[] corners = null;

	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public String getText() {
		StringBuffer toReturn = new StringBuffer();
		for (Column c : columns) {
			toReturn.append(c.getText());
			toReturn.append('\n');
		}
		return toReturn.toString();
	}

	@Override
	public List<Text> getTexts() {
		List<Text> toReturn = new ArrayList<Text>();
		for (Column c : columns) {
			toReturn.addAll(c.getTexts());
		}
		return toReturn;
	}

	@Override
	public int getId() {
		return id;
	}

	public boolean addColumn(Column column) {
		if (corners == null) {
			corners = new float[] { Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE };
		}
		float[] elementCorners = column.getCorners();

		corners[0] = Util.getMin(corners[0], elementCorners[0], elementCorners[2]);
		corners[1] = Util.getMax(corners[1], elementCorners[1], elementCorners[3]);
		corners[2] = Util.getMax(corners[2], elementCorners[0], elementCorners[2]);
		corners[3] = Util.getMin(corners[3], elementCorners[1], elementCorners[3]);
		return columns.add(column);
	}

	public void addColumn(int index, Column column) {
		if (corners == null) {
			corners = new float[] { Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE };
		}
		float[] elementCorners = column.getCorners();

		corners[0] = Util.getMin(corners[0], elementCorners[0], elementCorners[2]);
		corners[1] = Util.getMax(corners[1], elementCorners[1], elementCorners[3]);
		corners[2] = Util.getMax(corners[2], elementCorners[0], elementCorners[2]);
		corners[3] = Util.getMin(corners[3], elementCorners[1], elementCorners[3]);
		columns.add(index, column);
	}

	@Override
	public Iterator<Column> iterator() {
		return columns.iterator();
	}

	@Override
	public float[] getCorners() {
		return corners;
	}

	@Override
	public String toString() {
		return getText();
	}
}
