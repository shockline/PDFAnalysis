package analyser.layout;

import java.util.List;


public interface Cell {
	public String getText();

	public List<Text> getTexts();

	public int getId();

	public float[] getCorners();

}
