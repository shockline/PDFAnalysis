package pdfpattern;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import analyser.layout.Column;
import analyser.layout.Row;
import analyser.layout.Table;

public class PdfPattern {

	private String tablenamepatternString;
	private Pattern tablenamepattern;
	private boolean tablenameNoSpace = false;
	private Node tablenamepatternNode;
	private List<Field> filedList = new ArrayList<Field>();
	private int maxline = 0;

	private PdfPattern(InputStream is) {
		SAXReader reader = new SAXReader();
		Document document = null;
		try {
			document = reader.read(is);
			Node root = document.selectSingleNode("pattern");
			maxline = Integer.parseInt(((Element) root).attribute("maxline").getText().trim());
			tablenamepatternString = root.selectSingleNode("tablenamepattern").getText().trim();
			Attribute a = ((Element) root.selectSingleNode("tablenamepattern")).attribute("nospace");
			if (a != null)
				tablenameNoSpace = Boolean.parseBoolean(a.getText().trim());
			tablenamepattern = Pattern.compile(tablenamepatternString);
			List<Node> fields = root.selectSingleNode("fields").selectNodes("field");
			for (Node f : fields) {
				String fieldname = f.selectSingleNode("fieldname").getText().trim();
				String fieldpattern = f.selectSingleNode("fieldpattern").getText().trim();
				String fieldtypePattern = f.selectSingleNode("fieldtypePattern").getText().trim();

				Field tmpF = new Field(fieldname, fieldpattern, fieldtypePattern);
				Attribute aAospacehead = ((Element) f.selectSingleNode("fieldpattern")).attribute("nospace");
				if (aAospacehead != null)
					tmpF.setNospacehead(Boolean.parseBoolean(aAospacehead.getText().trim()));

				Attribute aAospacebody = ((Element) f.selectSingleNode("fieldtypePattern")).attribute("nospace");
				if (aAospacebody != null)
					tmpF.setNospacebody(Boolean.parseBoolean(aAospacebody.getText().trim()));

				filedList.add(tmpF);
			}
		} catch (Exception e) {
			System.err.println("Can't load XML from stream.");
			e.printStackTrace();
		}

	}

	public static PdfPattern compile(InputStream is) {
		return new PdfPattern(is);
	}

	public List<Table> match(Table table) {
		List<Table> toReturn = new ArrayList<Table>();
		Row r;
		int rowIndex = 0;
		for (Iterator<Row> rowIter = table.iterator(); rowIter.hasNext();) {
			r = rowIter.next();
			boolean rowMatchFlag = false;
			for (Column c : r) {
				String text = c.getText();
				if (tablenameNoSpace)
					text = text.replaceAll("\\s", "");
				if (text.contains("名股东持股情况"))
					System.out.println("flag");
				if (tablenamepattern.matcher(text).find()) {
					rowMatchFlag = true;
					break;
				}
			}
			Table toAdd = null;
			if (rowMatchFlag)
				toAdd = matchTable(table.getRows().listIterator(rowIndex + 1));
			if (toAdd != null)
				toReturn.add(toAdd);
			++rowIndex;
		}
		return toReturn;
	}

	private Table matchTable(Iterator<Row> rowIter) {
		int curRowCount = 1;
		Table toReturn = new Table();
		if (rowIter.hasNext()) {
			Row r = matchTableHeadRow(rowIter);
			if (r != null)
				toReturn.addRow(r);
			else
				return null;
		} else
			return null;

		while (curRowCount++ <= maxline && rowIter.hasNext()) {
			Row r = matchTableBodyRow(rowIter);
			if (r != null)
				toReturn.addRow(r);
			else
				return toReturn;
		}
		return toReturn;
	}

	private Row matchTableHeadRow(Iterator<Row> rowIter) {
		if (rowIter.hasNext()) {
			Row r = rowIter.next();
			Iterator<Field> fIter = filedList.iterator();
			for (Column c : r) {
				if (!fIter.hasNext())
					return null;
				if (!fIter.next().matchTableHead(c.getText()))
					return null;
			}
			return r;
		} else
			return null;
	}

	private Row matchTableBodyRow(Iterator<Row> rowIter) {
		if (rowIter.hasNext()) {
			Row r = rowIter.next();
			Iterator<Field> fIter = filedList.iterator();
			for (Column c : r) {
				if (!fIter.hasNext())
					return null;
				if (!fIter.next().matchTableBody(c.getText()))
					return null;
			}
			return r;
		} else
			return null;
	}

	public class Field {
		private String fieldname;
		private Pattern fieldPattern;
		private Pattern fieldtypePattern;
		private boolean nospacehead = false;
		private boolean nospacebody = false;

		public boolean isNospacebody() {
			return nospacebody;
		}

		public void setNospacebody(boolean nospacebody) {
			this.nospacebody = nospacebody;
		}

		public boolean isNospacehead() {
			return nospacehead;
		}

		public void setNospacehead(boolean nospacehead) {
			this.nospacehead = nospacehead;
		}

		public Field(String fieldname, String fieldPatternString, String fieldtypePatternString) {
			super();
			this.fieldname = fieldname;
			fieldPattern = Pattern.compile(fieldPatternString);
			fieldtypePattern = Pattern.compile(fieldtypePatternString);
		}

		public boolean matchTableHead(String input) {
			if (isNospacehead())
				input = input.replaceAll("\\s", "");
			Matcher m = fieldPattern.matcher(input);
			return m.find();
		}

		public boolean matchTableBody(String input) {
			if (isNospacebody())
				input = input.replaceAll("\\s", "");
			Matcher m = fieldtypePattern.matcher(input);
			return m.find();
		}

		public String getFieldname() {
			return fieldname;
		}

		public void setFieldname(String fieldname) {
			this.fieldname = fieldname;
		}
	}
}
