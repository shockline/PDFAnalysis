package analyser.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import util.Util;

public class TextPiece implements Cell, Iterable<Text> {

	private static int idGenerator = 0;

	private List<Text> texts = new ArrayList<Text>();
	private int id = ++idGenerator;

	private float[] corners = null;

	@Override
	public String getText() {
		StringBuffer toReturn = new StringBuffer();
		for (Text c : texts) {
			toReturn.append(c.getText());
			toReturn.append('\n');
		}
		return toReturn.toString();
	}

	@Override
	public List<Text> getTexts() {
		return texts;
	}

	@Override
	public int getId() {
		return id;
	}

	public boolean addText(Text text) {
		if (corners == null) {
			corners = new float[] { Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE };
		}
		float[] elementCorners = text.getCorners();

		corners[0] = Util.getMin(corners[0], elementCorners[2], elementCorners[4]);
		corners[1] = Util.getMax(corners[1], elementCorners[5], elementCorners[7]);
		corners[2] = Util.getMax(corners[2], elementCorners[2], elementCorners[4]);
		corners[3] = Util.getMin(corners[3], elementCorners[5], elementCorners[7]);
		return texts.add(text);
	}

	public void addAllText(Collection<Text> texts) {
		for (Text text : texts)
			addText(text);
	}

	public void Text(int index, Text text) {
		if (corners == null) {
			corners = new float[] { Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE };
		}
		float[] elementCorners = text.getCorners();

		corners[0] = Util.getMin(corners[0], elementCorners[2], elementCorners[4]);
		corners[1] = Util.getMax(corners[1], elementCorners[5], elementCorners[7]);
		corners[2] = Util.getMax(corners[2], elementCorners[2], elementCorners[4]);
		corners[3] = Util.getMin(corners[3], elementCorners[5], elementCorners[7]);
		texts.add(index, text);
	}

	@Override
	public Iterator<Text> iterator() {
		return texts.iterator();
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
