package analyser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.faceless.pdf2.PDF;
import org.faceless.pdf2.PDFPage;
import org.faceless.pdf2.PDFParser;
import org.faceless.pdf2.PDFReader;
import org.faceless.pdf2.PageExtractor;

import analyser.layout.Column;
import analyser.layout.Row;
import analyser.layout.Table;
import analyser.layout.Text;
import analyser.layout.TextPiece;

public class Analyser {
	private static enum DfaState {
		INLINE_STATE, MULTILINE_BUFFER_STATE
	};

	private static enum DfaAlpha {
		TOP_RIGHT, MID_RIGHT, BOTTOM_MIDRIGHT, BOTTOM_LEFT;
	};

	public List<Table> analyse(File pdfFile) {
		PDFReader pdfReader = null;
		try {
			pdfReader = new PDFReader(pdfFile);
		} catch (IOException e) {
			System.err.println("���ļ�" + pdfFile + "ʱ����");
			return null;
		}
		PDF pdf = new PDF(pdfReader);
		PDFParser pdfParser = new PDFParser(pdf);
		DocAnalysingDFA ddfa = new DocAnalysingDFA(pdfParser);
		List<Table> pageTables = new ArrayList<Table>();
		pageTables.add(ddfa.analyse());
		pdf.close();
		return pageTables;
	}

	private class Position implements Comparable<Position> {
		private float left;
		private float top;

		public Position(float left, float top) {
			super();
			this.left = left;
			this.top = top;
		}

		@Override
		public int compareTo(Position o) {
			if (left < o.left)
				return -1;
			else if (left > o.left)
				return 1;
			else {
				if (top < o.top)
					return -1;
				else if (top > o.top)
					return 1;
				else
					return 0;
			}
		}
	}

	private class PageAnalysingDFA {
		private static final float COLUMN_LINE_WIDTH = 7f;
		private Iterator<Text> iter;
		private DfaState state;
		private Table curTable = new Table();
		private List<TextPiece> curRowBuffer = new ArrayList<TextPiece>();
		private TextPiece curTextPiece;
		private List<Float> lastRightScope = new ArrayList<Float>();;
		private float last2RightScope;
		private Text lastText;
		private Text curText;
		private int skipNum = 0;
		private boolean lastSpaceColumnLine;

		private int firstFoldIndex = Integer.MAX_VALUE;

		private Map<Position, Integer> displayOrderTextMap = new TreeMap<Analyser.Position, Integer>();
		private List<Text> displayOrderTextArray = new ArrayList<Text>();
		private List<Text> workTextArray = new ArrayList<Text>();

		protected PageAnalysingDFA() {
		}

		@SuppressWarnings({ "unused" })
		public PageAnalysingDFA(PageExtractor pageExtractor) {
			addWorkArray(pageExtractor);
			initIter();
		}

		@SuppressWarnings("unused")
		public PageAnalysingDFA(PageExtractor pageExtractor, int skipNum) {
			initSkipnum(skipNum);
			addWorkArray(pageExtractor);
			initIter();
		}

		protected void initSkipnum(int sn) {
			this.skipNum = sn;
		}

		protected void initIter() {
			this.iter = workTextArray.iterator();
		}

		protected void addWorkArray(PageExtractor pageExtractor) {
			float hOffset = 0f;
			addWorkArray(pageExtractor, hOffset);
		}

		protected void addWorkArray(PageExtractor pageExtractor, float hOffset) {
			List<?> tmp = (List<?>) pageExtractor.getTextUnordered();
			Iterator<? extends org.faceless.pdf2.PageExtractor.Text> iter = (Iterator<? extends org.faceless.pdf2.PageExtractor.Text>) tmp
					.iterator();
			if (tmp.size() <= skipNum)
				return;
			for (int i = 0; i < skipNum; ++i)
				iter.next();
			while (iter.hasNext())
				workTextArray.add(new Text(iter.next(), hOffset));
			for (org.faceless.pdf2.PageExtractor.Text text : pageExtractor.getTextInDisplayOrder()) {
				Text myText = new Text(text, hOffset);
				displayOrderTextArray.add(myText);
				displayOrderTextMap.put(new Position(myText.getCorners()[2], myText.getCorners()[3]),
						displayOrderTextArray.size() - 1);
			}
		}

		public Table analyse() {
			if (!iter.hasNext())
				return null;
			lastText = iter.next();
			curTextPiece = new TextPiece();
			curTextPiece.addText(lastText);
			state = DfaState.INLINE_STATE;
			lastRightScope.add(lastText.getCorners()[4]);
			last2RightScope = 0.0f;

			// ///////////////////////
//			System.out.println(lastText.getText() + "|" + lastText.getCorners()[0] + "|" + lastText.getCorners()[1]
//					+ "|" + lastText.getCorners()[4] + "|" + lastText.getCorners()[5] + "|");
			// //////////////////////
			
			iLabel: while (iter.hasNext()) {
				curText = iter.next();
//				System.out.println(curText.getText() + "|" + curText.getCorners()[0] + "|" + curText.getCorners()[1]
//						+ "|" + curText.getCorners()[4] + "|" + curText.getCorners()[5] + "|");

				DfaAlpha alpha;
				if (curText.getCorners()[3] < lastText.getCorners()[1]
						|| curText.getCorners()[1] > lastText.getCorners()[3]
						|| (curText.getCorners()[3] >= lastText.getCorners()[1] && curText.getCorners()[4] < lastText
								.getCorners()[0])) {
					if (curText.getCorners()[3] + curText.getCorners()[1] > 2 * lastText.getCorners()[3])
						alpha = DfaAlpha.TOP_RIGHT;
					else {
						// int textByDisplayRowIndex =
						// displayOrderTextMap.get(new
						// Position(lastText.getCorners()[2],
						// lastText.getCorners()[3]));
						// int textByDisplayRowNextColumnIndex =
						// textByDisplayRowIndex + 1;
						// for (; textByDisplayRowNextColumnIndex <
						// displayOrderTextArray.size();
						// ++textByDisplayRowNextColumnIndex) {
						// if
						// (!displayOrderTextArray.get(textByDisplayRowNextColumnIndex).getText().equals(" "))
						// break;
						// }
						// Text thisText =
						// displayOrderTextArray.get(textByDisplayRowNextColumnIndex);
						// I wanna judge whether lastText is the last one in
						// last line.
						// if (textByDisplayRowIndex ==
						// displayOrderTextArray.size() - 1
						// || (thisText.getCorners()[3] <
						// lastText.getCorners()[1] && curText.getCorners()[0] <
						// last2RightScope))
						if (curText.getCorners()[0] < last2RightScope)
							alpha = DfaAlpha.BOTTOM_LEFT;
						else
							alpha = DfaAlpha.BOTTOM_MIDRIGHT;
					}
				} else
					alpha = DfaAlpha.MID_RIGHT;
				if (!guide(state, alpha))
					break iLabel;
				lastText = curText;
			}
			guide(state, DfaAlpha.BOTTOM_LEFT);
			return curTable;
		}

		private boolean guide(DfaState mystate, DfaAlpha myalpha) {
//			System.out.println(mystate + " " + myalpha);
			switch (mystate) {
			case INLINE_STATE:
				switch (myalpha) {
				case BOTTOM_LEFT:
					INLINE_STATEwithBOTTOM_LEFT();
					break;
				case BOTTOM_MIDRIGHT:
					INLINE_STATEwithBOTTOM_MIDRIGHT();
					break;
				case MID_RIGHT:
					INLINE_STATEwithMID_RIGHT();
					break;
				case TOP_RIGHT:
					INLINE_STATEwithTOP_RIGHT();
					break;
				default:
					System.err.println("Error state!");
					return false;
				}
				break;
			case MULTILINE_BUFFER_STATE:
				switch (myalpha) {
				case BOTTOM_LEFT:
					MULTILINE_BUFFER_STATEwithBOTTOM_LEFT();
					break;
				case BOTTOM_MIDRIGHT:
					MULTILINE_BUFFER_STATEwithBOTTOM_MIDRIGHT();
					break;
				case MID_RIGHT:
					MULTILINE_STATEwithMID_RIGHT();
					break;
				case TOP_RIGHT:
					MULTILINE_BUFFER_STATEwithTOP_RIGHT();
					break;
				default:
					System.err.println("Error state!");
					return false;
				}
				break;
			default:
				System.err.println("Error state!");
				return false;
			}
			return true;
		}

		private List<Row> mergeLShapeRow(List<TextPiece> rowBuffer) {
			List<Row> lr = new ArrayList<Row>();

			float tableTop = Float.MIN_VALUE;
			for (int i = (firstFoldIndex == Integer.MAX_VALUE ? 0 : firstFoldIndex); i < rowBuffer.size(); ++i)
				tableTop = Math.max(tableTop, rowBuffer.get(i).getCorners()[1]);

			Column c = new Column();
			int totalCount = rowBuffer.size();

			int i;
			for (i = 0; i < totalCount; ++i) {
				TextPiece tp = rowBuffer.get(i);
				if (tp.getCorners()[1] > tableTop + tp.getTexts().get(0).getFontSize()) {
					c.addAllText(tp.getTexts());
					if (!c.getTexts().isEmpty()) {
						Row r = new Row();
						r.addColumn(c);
						lr.add(r);
						c = new Column();
					}
				} else
					break;
			}
			if (i == rowBuffer.size())
				return lr;
			Row r = new Row();
			c.addAllText(rowBuffer.get(i).getTexts());
			++i;
			for (; i < totalCount; ++i) {
				TextPiece tp = rowBuffer.get(i);
				if (i != 0 && tp.getCorners()[1] < c.getCorners()[3] && tp.getCorners()[0] < c.getCorners()[2]) {
					// if (i < totalCount - 1 && i != 0) {
					// TextPiece tp2 = rowBuffer.get(i + 1);
					// if (tp.getCorners()[3] < tp2.getCorners()[1] &&
					// tp2.getCorners()[0] < c.getCorners()[2]) {
					// r.addColumn(c);
					// lr.add(r);
					// r = new Row();
					// c = new Column();
					// }
					// }
					c.addAllText(tp.getTexts());
				} else {
					r.addColumn(c);
					c = new Column();
					c.addAllText(tp.getTexts());
				}
				if (i == totalCount - 1)
					r.addColumn(c);
			}
			if (r.iterator().hasNext())
				lr.add(r);
			return lr;
		}

		private float getMaxUnderScopeFromRightScopeList(float scope) {
			float toReturn = 0.0f;
			for (Float f : lastRightScope) {
				if (f > toReturn && f < scope)
					toReturn = f;
			}
			return toReturn;
		}

		private void MULTILINE_STATEwithMID_RIGHT() {
			if (" ".equals(curText.getText())) {
				if (lastSpaceColumnLine) {
					if (curText.getCorners()[0] > lastText.getCorners()[4] + COLUMN_LINE_WIDTH) {
						last2RightScope = getMaxUnderScopeFromRightScopeList(lastText.getCorners()[0]);
						lastRightScope.clear();
						lastRightScope.add(lastText.getCorners()[4]);
						curTextPiece.addText(lastText);
						firstFoldIndex = Math.min(curRowBuffer.size(), firstFoldIndex);
						curRowBuffer.add(curTextPiece);
						curTextPiece = new TextPiece();
					}
				} else {
					// last2RightScope = lastRightScope;
					// firstFoldIndex = Math.min(curRowBuffer.size(),
					// firstFoldIndex);
					curRowBuffer.add(curTextPiece);
					curTextPiece = new TextPiece();
				}
				lastSpaceColumnLine = true;
			} else {

				// if (curText.getCorners()[0] < lastText.getCorners()[4]) {
				if (curText.getCorners()[0] < lastText.getCorners()[4] + COLUMN_LINE_WIDTH) {
					if (lastSpaceColumnLine && curRowBuffer.size() >= 1) {
						curTextPiece = curRowBuffer.get(curRowBuffer.size() - 1);
						curRowBuffer.remove(curRowBuffer.size() - 1);
					}
					curTextPiece.addText(curText);
					lastRightScope.add(curText.getCorners()[4]);

				} else {
					if (lastSpaceColumnLine)
						firstFoldIndex = Math.min(curRowBuffer.size(), firstFoldIndex);
					last2RightScope = getMaxUnderScopeFromRightScopeList(curText.getCorners()[0]);
					lastRightScope.clear();
					lastRightScope.add(curText.getCorners()[4]);
					if (!curTextPiece.getTexts().isEmpty()) {
						firstFoldIndex = Math.min(curRowBuffer.size(), firstFoldIndex);
						curRowBuffer.add(curTextPiece);
						curTextPiece = new TextPiece();
					}
					curTextPiece.addText(curText);
				}
				lastSpaceColumnLine = false;
			}
		}

		private void MULTILINE_BUFFER_STATEwithTOP_RIGHT() {
			last2RightScope = getMaxUnderScopeFromRightScopeList(curText.getCorners()[0]);
			lastRightScope.clear();
			lastRightScope.add(curText.getCorners()[4]);
			if (!curTextPiece.getTexts().isEmpty()) {
				firstFoldIndex = Math.min(curRowBuffer.size(), firstFoldIndex);
				curRowBuffer.add(curTextPiece);
				curTextPiece = new TextPiece();
			}
			curTextPiece.addText(curText);
			lastSpaceColumnLine = false;
		}

		private void MULTILINE_BUFFER_STATEwithBOTTOM_MIDRIGHT() {
			// last2RightScope = lastRightScope;
			lastRightScope.add(curText.getCorners()[4]);
			if (!curTextPiece.getTexts().isEmpty() && " ".equals(lastText.getText())) {
				curRowBuffer.add(curTextPiece);
				curTextPiece = new TextPiece();
				List<Row> ll = mergeLShapeRow(curRowBuffer);
				for (Row r : ll)
					curTable.addRow(r);
				curRowBuffer.clear();
			}
			curTextPiece.addText(curText);
			lastSpaceColumnLine = false;
		}

		private void MULTILINE_BUFFER_STATEwithBOTTOM_LEFT() {
			last2RightScope = 0f;
			lastRightScope.clear();
			lastRightScope.add(curText.getCorners()[4]);

			if (!curTextPiece.getTexts().isEmpty() && " ".equals(lastText.getText())) {
				curRowBuffer.add(curTextPiece);
				curTextPiece = new TextPiece();
			}
			List<Row> ll = mergeLShapeRow(curRowBuffer);
			for (Row r : ll)
				curTable.addRow(r);
			curRowBuffer.clear();
			curTextPiece.addText(curText);
//			state = DfaState.INLINE_STATE;
			lastSpaceColumnLine = false;
			firstFoldIndex = Integer.MAX_VALUE;
		}

		private void INLINE_STATEwithTOP_RIGHT() {
			last2RightScope = getMaxUnderScopeFromRightScopeList(curText.getCorners()[0]);
			lastRightScope.clear();
			lastRightScope.add(curText.getCorners()[4]);

			if (!curTextPiece.getTexts().isEmpty()) {
				firstFoldIndex = Math.min(curRowBuffer.size(), firstFoldIndex);
				curRowBuffer.add(curTextPiece);
				curTextPiece = new TextPiece();
			}
			curTextPiece.addText(curText);
			state = DfaState.MULTILINE_BUFFER_STATE;
			lastSpaceColumnLine = false;
		}

		private void INLINE_STATEwithMID_RIGHT() {
			if (" ".equals(curText.getText())) {
				if (lastSpaceColumnLine) {
					if (curText.getCorners()[0] > lastText.getCorners()[4] + COLUMN_LINE_WIDTH) {
						last2RightScope = getMaxUnderScopeFromRightScopeList(lastText.getCorners()[0]);
						lastRightScope.clear();
						lastRightScope.add(lastText.getCorners()[4]);
						curTextPiece.addText(lastText);
						firstFoldIndex = Math.min(curRowBuffer.size(), firstFoldIndex);
						curRowBuffer.add(curTextPiece);
						curTextPiece = new TextPiece();
					}
				} else {
					// last2RightScope = lastRightScope;
					// firstFoldIndex = Math.min(curRowBuffer.size(),
					// firstFoldIndex);
					curRowBuffer.add(curTextPiece);
					curTextPiece = new TextPiece();
				}
				lastSpaceColumnLine = true;
			} else {
				// if (curText.getCorners()[0] < lastText.getCorners()[4]) {
				if (curText.getCorners()[0] < lastText.getCorners()[4] + COLUMN_LINE_WIDTH) {
					if (lastSpaceColumnLine && curRowBuffer.size() >= 1) {
						curTextPiece = curRowBuffer.get(curRowBuffer.size() - 1);
						curRowBuffer.remove(curRowBuffer.size() - 1);
					}
					curTextPiece.addText(curText);
					lastRightScope.add(curText.getCorners()[4]);
				} else {
					if (lastSpaceColumnLine)
						firstFoldIndex = Math.min(curRowBuffer.size(), firstFoldIndex);
					last2RightScope = getMaxUnderScopeFromRightScopeList(curText.getCorners()[0]);
					lastRightScope.clear();
					lastRightScope.add(curText.getCorners()[4]);
					firstFoldIndex = Math.min(firstFoldIndex, firstFoldIndex);
					if (!curTextPiece.getTexts().isEmpty()) {
						firstFoldIndex = Math.min(curRowBuffer.size(), firstFoldIndex);
						curRowBuffer.add(curTextPiece);
						curTextPiece = new TextPiece();
					}
					curTextPiece.addText(curText);
				}
				lastSpaceColumnLine = false;
			}
		}

		private void INLINE_STATEwithBOTTOM_MIDRIGHT() {
			// last2RightScope = lastRightScope;
			lastRightScope.add(curText.getCorners()[4]);

			if (!curTextPiece.getTexts().isEmpty()) {
				curRowBuffer.add(curTextPiece);
				curTextPiece = new TextPiece();
			}
			curTextPiece.addText(curText);
			state = DfaState.MULTILINE_BUFFER_STATE;
			lastSpaceColumnLine = false;
			firstFoldIndex = Integer.MAX_VALUE;
		}

		private void INLINE_STATEwithBOTTOM_LEFT() {
			Row curRow = new Row();
			Column curColumn = new Column();

			if (!curTextPiece.getTexts().isEmpty()) {
				curRowBuffer.add(curTextPiece);
				curTextPiece = new TextPiece();
			}
			curTextPiece.addText(curText);
			for (TextPiece tp : curRowBuffer) {
				curColumn.addAllText(tp.getTexts());
				curRow.addColumn(curColumn);
				curColumn = new Column();
			}
			curRowBuffer.clear();
			curTable.addRow(curRow);

			curTextPiece = new TextPiece();
			curTextPiece.addText(curText);

			last2RightScope = 0;
			lastRightScope.clear();
			lastRightScope.add(curText.getCorners()[4]);
			lastSpaceColumnLine = false;
		}
	}

	private class DocAnalysingDFA extends PageAnalysingDFA {
		public DocAnalysingDFA(PDFParser pdfParser) {
			float hTotalOffset = 0f;

			for (PageExtractor pageE : pdfParser.getPageExtractors()) {
				hTotalOffset += pageE.getPage().getHeight();
			}

			for (PageExtractor pageE : pdfParser.getPageExtractors()) {
				hTotalOffset -= pageE.getPage().getHeight();
				initSkipnum(1);
				addWorkArray(pageE, hTotalOffset);
			}
			initIter();
		}

		public Table analyse() {
			Table toProcess = super.analyse();
			columnMerge(toProcess);
			return toProcess;
		}

		private void columnMerge(Table toProcess) {
			// TODO Auto-generated method stub

		}
	}
}
