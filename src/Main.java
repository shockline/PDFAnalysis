import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import maxsp.Itemset;
import maxsp.MaxSP;
import maxsp.SequenceDatabase;
import maxsp.SequentialPattern;
import maxsp.SequentialPatterns;
import seg.NlpirLib;
import seg.NlpirMethod;
import analyser.Analyser;
import analyser.layout.Column;
import analyser.layout.Row;
import analyser.layout.Table;
import de.mpii.fsm.driver.FsmDriver;

public class Main {

	public static Pattern numbericPattern = Pattern.compile("[\\d,\\.]+(\\d|万|亿)元");
	public static Pattern percentPattern = Pattern.compile("[\\d,\\.]+\\d%");

	public static void main(String[] args) throws Exception {
		String path = System.getProperty("user.dir");
		boolean flag = NlpirMethod.NLPIR_Init(path + "/ictdata", 1, "");

		if (flag) {
			System.out.println("nlpir初始化成功");
		} else {
			System.out.println("nlpir初始化失败：" + NlpirLib.Instance.NLPIR_GetLastErrorMsg());
			System.exit(1);
		}

		// String[] fileList = new String[] {
		// "data/input/京东世纪贸易公司债募集说明书（反馈回复稿）-瑞银证券.pdf",
		// "data/input/1 保利房地产（集团）股份有限公司公开发行2016 年公司债券（第二期）募集说明书.pdf",
		// "data/input/北新建材公司债募集说明书-中信证券.pdf",
		// "data/input/中海地产公司债募集说明书-中信证券.pdf" };
		String[] fileList = new String[] { "data/input/京东世纪贸易公司债募集说明书（反馈回复稿）-瑞银证券.pdf" };
		for (String fileString : fileList) {
			File toAnalyse = new File(fileString);
			analyse(toAnalyse);
		}
		System.out.println("All finished!");
	}

	public static void analyse(File toAnalyse) throws Exception {
		Analyser a = new Analyser();
		List<Table> lt = a.analyse(toAnalyse);

		List<String> flattenList = new ArrayList<String>();
		List<String> lt_replaced = new ArrayList<String>();
		for (Table table : lt) {
			for (Row row : table) {
				for (Column column : row) {
					String columnText = column.getText().replace("\n", "").replace("\r", "");

					StringTokenizer st = new StringTokenizer(columnText, "。");
					while (st.hasMoreTokens()) {
						String sentence = st.nextToken();
						sentence = sentence.trim();
						if (sentence.isEmpty())
							continue;

						flattenList.add(sentence);

						Matcher m = numbericPattern.matcher(sentence);
						sentence = m.replaceAll("number");

						m = percentPattern.matcher(sentence);
						sentence = m.replaceAll("percent");

						lt_replaced.add(sentence);
					}
				}
			}
		}

		Map<String, Integer> dic = new HashMap<String, Integer>();
		Map<Integer, String> inverseDic = new HashMap<Integer, String>();
		int charCount = 1;

		System.out.println("build seq database.");

		File newDir = new File("data/output/" + toAnalyse.getName());
		if (!newDir.exists())
			newDir.mkdirs();

		OutputStreamWriter seqfw = new OutputStreamWriter(
				new FileOutputStream("data/output/" + toAnalyse.getName() + "/data.txt", false), "utf-8");

		// SequenceDatabase sequenceDatabase = new SequenceDatabase();
		int count = 0;
		for (String sentence : lt_replaced) {
			String segmentSentence = NlpirMethod.NLPIR_ParagraphProcess(sentence, 0);
			StringTokenizer st = new StringTokenizer(segmentSentence, " ");

			String[] sList = new String[st.countTokens()];

			int i = -1;
			while (st.hasMoreTokens()) {
				++i;
				String chString = st.nextToken();
				int value = -1;
				if (dic.containsKey(chString))
					value = dic.get(chString);
				else {
					dic.put(chString, charCount);
					inverseDic.put(charCount, chString);
					value = charCount;
					++charCount;
				}

				sList[i] = chString;
				// sList[2 * i + 1] = "-1";
			}
			// sList[sList.length - 1] = "-2";
			seqfw.append("s" + count + "\t");

			for (int index = 0; index < sList.length; ++index) {
				seqfw.append(sList[index]);
				if (index != sList.length - 1)
					seqfw.append("\t");
			}
			seqfw.append("\n");
			// sequenceDatabase.addSequence(sList);
			++count;
			// if (count != sequenceDatabase.getSequences().size())
			// System.out.println("WARNING: Some empty tokens are detected.");
		}
		seqfw.close();
		System.out.println(count);

		int minSupport = 2;
		int gaps = 2;
		int maxLength = 10;

		String[] args = new String[] { "-i", "data/output/" + toAnalyse.getName(), "-o",
				"data/output/" + toAnalyse.getName() + ".output", "-s", Integer.toString(minSupport), "-g",
				Integer.toString(gaps), "-l", Integer.toString(maxLength), "-t", "c" };
		FsmDriver.main(args);

		File fruOutputFile = new File("data/output/" + toAnalyse.getName() + ".output/translatedFS");
		
		File sentenceCorrespondingOutputFile = new File("data/output/" + toAnalyse.getName() + ".output/sentence");
		OutputStreamWriter sentenceFw = new OutputStreamWriter(
				new FileOutputStream(sentenceCorrespondingOutputFile, false), "utf-8");
		
		Scanner fin = new Scanner(fruOutputFile);
		while (fin.hasNextLine()) {
			String line = fin.nextLine();
			int tabIndex = line.indexOf('\t');
			String fruItems = line.substring(0, tabIndex);
			String fruSupString = line.substring(tabIndex + 1);
			int fruSup = Integer.parseInt(fruSupString);

			StringTokenizer st = new StringTokenizer(line, " ");
			int tokenTotal = st.countTokens();
			int totenCount = 0;
			StringBuffer sb = new StringBuffer();
			while (st.hasMoreTokens()) {
				sb.append(st.nextToken());
				++totenCount;
				if (totenCount != tokenTotal)
					sb.append("[\\s\\S]{0,2}");
			}
			
			Pattern regxPattern = Pattern.compile(sb.toString());
			
			sentenceFw.append("---------Pattern---------\nPattern: ");
			sentenceFw.append(fruItems);
			sentenceFw.append("\n");
			for (String sentence : lt_replaced) {
				Matcher m = regxPattern.matcher(sentence);
				if(m.matches()){
					sentenceFw.append(sentence);
					sentenceFw.append("\n");
				}
			}
		}

		fin.close();
		sentenceFw.close();
	}

}
