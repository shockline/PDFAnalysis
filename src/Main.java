import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

		String[] args = new String[] { "-i", "data/output/" + toAnalyse.getName(), "-o",
				"data/output/" + toAnalyse.getName() + ".output", "-s", "2", "-g", "100", "-l", "3" };
		FsmDriver.main(args);

		// System.out.println("fruquent analyse.");
		// int minsup = 2;
		// boolean showsid = true;
		// MaxSP algo = new MaxSP();
		// algo.setShowSequenceIdentifiers(showsid);
		//
		// // execute the algorithm
		// SequentialPatterns sps = algo.runAlgorithm(sequenceDatabase, null,
		// minsup);
		// algo.printStatistics(sequenceDatabase.size());
		//
		// OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(
		// "data/output/" + toAnalyse.getName() + ".fru", false), "GBK");
		// List<List<SequentialPattern>> lls = sps.getLevels();
		// lls = lls.subList(2, lls.size());
		// for (int i = lls.size() - 1; i >= 2; --i) {
		// List<SequentialPattern> spl = lls.get(i);
		// for (SequentialPattern sp : spl) {
		// fw.append("Pattern: ");
		// for (Itemset itemSet : sp.getItemsets()) {
		// // System.out.print("(");
		// for (Integer item : itemSet.getItems()) {
		// System.out.print(inverseDic.get(item));
		// System.out.print(",");
		// fw.append(inverseDic.get(item));
		// fw.append(",");
		// }
		// // System.out.print("),");
		// }
		// System.out.println();
		// fw.append("\n");
		// Set<Integer> seqIds = sp.getSequenceIDs();
		// for (Integer id : seqIds) {
		// System.out.println(lt_replaced.get(id));
		// fw.append(lt_replaced.get(id));
		// fw.append("\n");
		// }
		// System.out.println("---------------------------");
		// fw.append("---------------------------\n");
		// }
		// }
		// fw.close();
		// System.out.println("All finished!");
	}

}
