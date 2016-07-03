package entrance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import seg.NlpirLib;
import seg.NlpirMethod;
import analyser.Analyser;
import analyser.layout.Column;
import analyser.layout.Row;
import analyser.layout.Table;
import de.mpii.fsm.driver.FsmDriver;

public class Util {

	public static Pattern numbericPattern = Pattern.compile("[\\d,\\.]+(\\d|万|亿)元");
	public static Pattern percentPattern = Pattern.compile("[\\d,\\.]+%");
	public static Pattern zeroPattern = Pattern.compile("([^\\d])0([^\\d])");
	public static Pattern dateTimePattern = Pattern
			.compile("(([0-9]{4})[-/\\.年])(第[一二三四]季度)?[末初前后]?(([01]?[0-9])[-/\\.月])?([0-3]?[0-9]{1}[日])?");

	static {
		String path = System.getProperty("user.dir");
		boolean flag = NlpirMethod.NLPIR_Init(path + "/ictdata", 1, "");

		if (flag) {
			System.out.println("nlpir初始化成功");
		} else {
			System.out.println("nlpir初始化失败：" + NlpirLib.Instance.NLPIR_GetLastErrorMsg());
			System.exit(1);
		}
	}

	public static void analyse(File toAnalyse, String mode) throws Exception {
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

					StringTokenizer st = new StringTokenizer(columnText, "，。；");
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
				chString = chString.replace("*", "\\*").replace("&", "\\&").replace("\\", "\\\\").replace("-", "\\-")
						.replace("+", "\\+").replace("?", "\\?").replace("[", "\\[").replace("]", "\\]")
						.replace("(", "\\(").replace(")", "\\)").replace("{", "\\{").replace("}", "\\}");
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
		int gaps = 0;
		int maxLength = 50;

		String inputFilename = dataDirAbsPath + "/output/" + toAnalyse.getName() + "/data.txt";
		String outputFilename = dataDirAbsPath + "/output/" + toAnalyse.getName() + ".output/";

		patternGenerator(inputFilename, outputFilename, minSupport, gaps, maxLength, mode);

		File fruOutputFile = new File(dataDirAbsPath + "/output/" + toAnalyse.getName() + ".output/translatedFS");

		Object[] res = patternFilter(fruOutputFile);

		Integer[] listIndex = (Integer[]) res[0];
		ArrayList<String> fruItemsList = (ArrayList<String>) res[1];
		ArrayList<String> fruSupStringList = (ArrayList<String>) res[2];
		final ArrayList<Integer> patternLengthList = (ArrayList<Integer>) res[3];

		File sentenceCorrespondingOutputFile = new File(
				dataDirAbsPath + "/output/" + toAnalyse.getName() + ".output/sentence");
		OutputStreamWriter sentenceFw = new OutputStreamWriter(
				new FileOutputStream(sentenceCorrespondingOutputFile, false), "utf-8");
		int outputCount = 0;
		boolean[] validList = new boolean[fruSupStringList.size()];
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
					sb.append("[\\s\\S]{0," + ((gaps + 1) * maxLength * 3) + "}");
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
					// sentenceFw.append(sentence);
					// sentenceFw.append("\n");
					sentenceFw.append(flattenList.get(countSentence));
					sentenceFw.append("\n");
				}
				++countSentence;
			}
			++outputCount;
			System.out.print(".");
			if (outputCount % 100 == 0)
				System.out.println();
		}
		System.out.println();
		System.out.println(outputCount);

		sentenceFw.close();
	}

	public static void patternGenerator(String inputFile, String outputFile, int minSupport, int gaps, int maxLength,
			String mode) throws Exception {
		if ("s".equals(mode)) {
			String[] args = new String[] { "-i", inputFile, "-o", outputFile, "-s", Integer.toString(minSupport), "-g",
					Integer.toString(gaps), "-l", Integer.toString(maxLength), "-t", "m", "-m", "s" };
			FsmDriver.main(args);
		} else {
			File inputF = new File(inputFile);
			sendFile("/pdftemp/" + inputFile, inputFile);
			String[] args = new String[] { "-i", "/pdftemp/" + inputF, "-o", "/pdftemp/" + inputF + ".output", "-s",
					Integer.toString(minSupport), "-g", Integer.toString(gaps), "-l", Integer.toString(maxLength), "-t",
					"m", "-m", "d" };
			 FsmDriver.main(args);

			File outputF = new File(outputFile + "/translatedFS");
			if (outputF.exists())
				outputF.delete();
			// String cmd = "hadoop fs -get \"" + "/pdftemp/" + inputF +
			// ".output/*\" \"" + outputFile + "\"";
			// Runtime.getRuntime().exec(cmd);
			downloadFile("/pdftemp/" + inputF + ".output/translatedFS/part-r-00000", outputFile + "/translatedFS");
		}

	}

	public static boolean downloadFile(String hadfile, String localPath) {
		try {
			Configuration conf = new Configuration();
			FileSystem localFS = FileSystem.getLocal(conf);
			FileSystem hadoopFS = FileSystem.get(conf);
			Path hadPath = new Path(hadfile);
			FSDataOutputStream fsOut = localFS.create(new Path(localPath));
			FSDataInputStream fsIn = hadoopFS.open(hadPath);
			byte[] buf = new byte[1024];
			int readbytes = 0;
			while ((readbytes = fsIn.read(buf)) > 0) {
				fsOut.write(buf, 0, readbytes);
			}
			fsIn.close();
			fsOut.close();

			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean sendFile(String path, String localfile) {
		Configuration conf = new Configuration();
		File file = new File(localfile);
		if (!file.isFile()) {
			System.out.println(file.getName());
			return false;
		}
		try {
			FileSystem localFS = FileSystem.getLocal(conf);
			FileSystem hadoopFS = FileSystem.get(conf);
			Path hadPath = new Path(path);
			hadoopFS.delete(new Path(path + "/" + file.getName()), true);
			FSDataOutputStream fsOut = hadoopFS.create(new Path(path + "/" + file.getName()), true);
			FSDataInputStream fsIn = localFS.open(new Path(localfile));
			byte[] buf = new byte[1024];
			int readbytes = 0;
			while ((readbytes = fsIn.read(buf)) > 0) {
				fsOut.write(buf, 0, readbytes);
			}
			fsIn.close();
			fsOut.close();

			FileStatus[] hadfiles = hadoopFS.listStatus(hadPath);
			for (FileStatus fs : hadfiles) {
				System.out.println(fs.toString());
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static Object[] patternFilter(File fruOutputFile) throws FileNotFoundException {
		System.out.println("Correspond sentences.");
		if (fruOutputFile.isDirectory())
			fruOutputFile = new File(fruOutputFile, "part-r-00000");

		Scanner fin = new Scanner(fruOutputFile);
		final ArrayList<String> fruItemsList = new ArrayList<String>();
		ArrayList<String> fruSupStringList = new ArrayList<String>();
		final ArrayList<Integer> patternLengthList = new ArrayList<Integer>();
		while (fin.hasNextLine()) {
			String line = fin.nextLine();
			int tabIndex = line.indexOf('\t');
			String fruItems = line.substring(0, tabIndex);
			if (!fruItems.contains("numberxx") && !fruItems.contains("timexx"))
				continue;
			String fruSupString = line.substring(tabIndex + 1);

			StringTokenizer st = new StringTokenizer(fruItems, " ");
			int patternLength = st.countTokens();

			fruItemsList.add(fruItems);
			fruSupStringList.add(fruSupString);
			patternLengthList.add(patternLength);
		}

		Integer[] listIndex = new Integer[fruSupStringList.size()];
		for (int i = 0; i < listIndex.length; ++i)
			listIndex[i] = i;
		Arrays.sort(listIndex, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				int c = -Integer.compare(patternLengthList.get(o1), patternLengthList.get(o2));
				if (c == 0)
					return fruItemsList.get(o1).compareTo(fruItemsList.get(o2));
				else
					return c;
			}
		});
		fin.close();
		System.out.println("Pattern Number: " + fruItemsList.size());
		System.out.println();

		return new Object[] { listIndex, fruItemsList, fruSupStringList, patternLengthList };
	}

}
