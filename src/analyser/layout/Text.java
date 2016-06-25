package analyser.layout;

public class Text {
	private float[] corners = new float[8];
	private String text;
	private float fontSize;

	public Text(org.faceless.pdf2.PageExtractor.Text text, float hOffset) {
		String tmp = text.getText().trim();
		if (!tmp.isEmpty()) {
			this.text = tmp;
			int start = text.getText().indexOf(this.text);
			text = text.getSubText(start, this.text.length());
		} else{
			text = text.getSubText(0, 1);
			this.text = text.getText();
		}
		corners = text.getCorners().clone();
		corners[1] += hOffset;
		corners[3] += hOffset;
		corners[5] += hOffset;
		corners[7] += hOffset;
		fontSize = text.getFontSize();
	}

	public float[] getCorners() {
		return corners;
	}

	public String getText() {
		return text;
	}

	public float getFontSize() {
		return fontSize;
	}
}
