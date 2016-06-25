package analyser.layout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import util.Util;

public class Table implements Cell, Iterable<Row> {

	private static int idGenerator = 0;

	private List<Row> rows = new ArrayList<Row>();

	private int id = ++idGenerator;

	private float[] corners = null;

	public List<Row> getRows() {
		return rows;
	}

	@Override
	public String getText() {
		StringBuffer toReturn = new StringBuffer();
		for (Row r : rows) {
			toReturn.append(r.getText());
			toReturn.append('\n');
		}
		return toReturn.toString();
	}

	@Override
	public List<Text> getTexts() {
		List<Text> toReturn = new ArrayList<Text>();
		for (Row r : rows) {
			toReturn.addAll(r.getTexts());
		}
		return toReturn;
	}

	@Override
	public int getId() {
		return id;
	}

	public boolean addRow(Row row) {
		if (corners == null) {
			corners = new float[] { Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE };
		}
		float[] elementCorners = row.getCorners();

		corners[0] = Util.getMin(corners[0], elementCorners[0], elementCorners[2]);
		corners[1] = Util.getMax(corners[1], elementCorners[1], elementCorners[3]);
		corners[2] = Util.getMax(corners[2], elementCorners[0], elementCorners[2]);
		corners[3] = Util.getMin(corners[3], elementCorners[1], elementCorners[3]);

		return rows.add(row);
	}

	public void addRow(int index, Row row) {
		if (corners == null) {
			corners = new float[] { Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE };
		}
		float[] elementCorners = row.getCorners();

		corners[0] = Util.getMin(corners[0], elementCorners[0], elementCorners[2]);
		corners[1] = Util.getMax(corners[1], elementCorners[1], elementCorners[3]);
		corners[2] = Util.getMax(corners[2], elementCorners[0], elementCorners[2]);
		corners[3] = Util.getMin(corners[3], elementCorners[1], elementCorners[3]);

		rows.add(index, row);
	}

	@Override
	public Iterator<Row> iterator() {
		return rows.iterator();
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
