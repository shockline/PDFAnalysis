import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
	public static Pattern percentPattern = Pattern.compile("[\\d,\\.]+%");
	public static Pattern zeroPattern = Pattern.compile("([^\\d])0([^\\d])");
	public static Pattern dateTimePattern = Pattern
			.compile("(([0-9]{4})[-/\\.年])(第[一二三四]季度)?[末初前后]?(([01]?[0-9])[-/\\.月])?([0-3]?[0-9]{1}[日])?");

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

		File dataDir = new File("data/");
		String dataDirAbsPath = dataDir.getCanonicalPath();

		for (Table table : lt) {
			for (Row row : table) {
				for (Column column : row) {
					String columnText = column.getText().replace("\n", "").replace("\r", "");

					StringTokenizer st = new StringTokenizer(columnText, "。");
					while (st.hasMoreTokens()) {
						String sentence0 = st.nextToken();
						sentence0 = sentence0.trim();
						if (sentence0.isEmpty())
							continue;

						String sentence = sentence0;

						Matcher m = numbericPattern.matcher(sentence);
						sentence = m.replaceAll(" numberxx ");

						m = percentPattern.matcher(sentence);
						sentence = m.replaceAll(" numberxx ");

						m = zeroPattern.matcher(sentence);
						sentence = m.replaceAll("$1 numberxx $2");
						
						m = dateTimePattern.matcher(sentence);
						sentence = m.replaceAll(" timexx ");

						if (!sentence.contains(" numberxx ") && !sentence.contains(" timexx "))
							continue;

						flattenList.add(sentence0);
						lt_replaced.add(sentence);
					}
				}
			}
		}

		Map<String, Integer> dic = new HashMap<String, Integer>();
		Map<Integer, String> inverseDic = new HashMap<Integer, String>();
		int charCount = 1;

		System.out.println("build seq database.");

		File newDir = new File(dataDirAbsPath + "/output/" + toAnalyse.getName());
		if (!newDir.exists())
			newDir.mkdirs();

		OutputStreamWriter seqfw = new OutputStreamWriter(
				new FileOutputStream(dataDirAbsPath + "/output/" + toAnalyse.getName() + "/data.txt", false), "utf-8");

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

		int minSupport = 12;
		int gaps = 2;
		int maxLength = 20;

		String[] args = new String[] { "-i", dataDirAbsPath + "/output/" + toAnalyse.getName(), "-o",
				dataDirAbsPath + "/output/" + toAnalyse.getName() + ".output", "-s", Integer.toString(minSupport), "-g",
				Integer.toString(gaps), "-l", Integer.toString(maxLength), "-t", "m", "-m", "s" };
		FsmDriver.main(args);

		System.out.println("Correspond sentences.");
		File fruOutputFile = new File(dataDirAbsPath + "/output/" + toAnalyse.getName() + ".output/translatedFS");
		if (fruOutputFile.isDirectory())
			fruOutputFile = new File(fruOutputFile, "part-r-00000");
		File sentenceCorrespondingOutputFile = new File(
				dataDirAbsPath + "/output/" + toAnalyse.getName() + ".output/sentence");
		OutputStreamWriter sentenceFw = new OutputStreamWriter(
				new FileOutputStream(sentenceCorrespondingOutputFile, false), "utf-8");

		Scanner fin = new Scanner(fruOutputFile);
		ArrayList<String> fruItemsList = new ArrayList<String>();
		ArrayList<String> fruSupStringList = new ArrayList<String>();
		final ArrayList<Integer> patternLengthList = new ArrayList<Integer>();
		while (fin.hasNextLine()) {
			String line = fin.nextLine();
			int tabIndex = line.indexOf('\t');
			String fruItems = line.substring(0, tabIndex);
			if (!fruItems.contains("number") && !fruItems.contains("percent"))
				continue;
			String fruSupString = line.substring(tabIndex + 1);

			StringTokenizer st = new StringTokenizer(fruItems, " ");
			int patternLength = st.countTokens();

			fruItemsList.add(fruItems);
			fruSupStringList.add(fruSupString);
			patternLengthList.add(patternLength);
		}

		Integer[] listIndex = new Integer[fruSupStringList.size()];
		boolean[] validList = new boolean[fruSupStringList.size()];
		for (int i = 0; i < listIndex.length; ++i)
			listIndex[i] = i;
		Arrays.sort(listIndex, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return -Integer.compare(patternLengthList.get(o1), patternLengthList.get(o2));
			}
		});
		System.out.println("Pattern Number: " + fruItemsList.size());

		int outputCount = 0;
		for (int i = 0; i < fruItemsList.size(); ++i) {
			String fruItems = fruItemsList.get(listIndex[i]);
			String fruSupString = fruSupStringList.get(listIndex[i]);

			StringTokenizer st = new StringTokenizer(fruItems, " ");
			int tokenTotal = st.countTokens();
			int totenCount = 0;
			StringBuffer sb = new StringBuffer();
			StringBuffer sbPattern = new StringBuffer();
			while (st.hasMoreTokens()) {
				String nextToken = st.nextToken();
				sb.append(nextToken);
				sbPattern.append(nextToken);
				++totenCount;
				if (totenCount != tokenTotal) {
					sb.append("[\\s\\S]{0," + (gaps * 5) + "}");
					sbPattern.append("[\\s\\S]*?");
				}
			}

			Pattern regxPattern = Pattern.compile(sb.toString());
			Pattern regxCorsPattern = Pattern.compile(sbPattern.toString());
			boolean flag = false;
			for (int j = 0; j < i; ++j) {
				if (validList[listIndex[j]])
					continue;
				String fCors = fruItemsList.get(listIndex[j]);
				Matcher mp = regxCorsPattern.matcher(fCors);
				if (mp.find()) {
					flag = true;
					validList[listIndex[i]] = true;
					break;
				}
			}
			if (flag)
				continue;

			sentenceFw.append("---------Pattern---------\n");
			sentenceFw.append("Supprt: ");
			sentenceFw.append(fruSupString);
			sentenceFw.append("\nPattern: ");
			sentenceFw.append(fruItems);
			sentenceFw.append("\n");
			int countSentence = 0;
			for (String sentence : lt_replaced) {
				Matcher m = regxPattern.matcher(sentence);
				// if(sentence.contains("%") || sentence.contains("percent"))
				// System.out.println("Check!");
				if (m.find()) {
					sentenceFw.append(sentence);
					sentenceFw.append("\n");
					sentenceFw.append(flattenList.get(countSentence));
					sentenceFw.append("\n");
				}
				++countSentence;
			}
			++outputCount;
			System.out.println(".");
			if (outputCount % 10 == 0)
				System.out.println();
		}
		System.out.println();
		fin.close();
		sentenceFw.close();
	}

}
