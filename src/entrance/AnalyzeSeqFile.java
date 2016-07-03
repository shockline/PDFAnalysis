package entrance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class AnalyzeSeqFile {
	public static void main(String[] args) throws Exception {

		String[] fileList = new String[] { "中海地产公司债募集说明书-中信证券.pdf",
				"京东世纪贸易公司债募集说明书（反馈回复稿）-瑞银证券.pdf", "联想控股公司债募集说明书-银河证券.pdf",
				"兵装集团公司债募集说明书-中信建投.pdf", "神舟租车公司债募集说明书-中金公司.pdf",
				"北新建材公司债募集说明书-中信证券.pdf", "清华控股公司债募集说明书-国泰君安.pdf",
				"1 保利房地产（集团）股份有限公司公开发行2016 年公司债券（第二期）募集说明书.pdf", };
		// String[] fileList = new String[] { "data/input/兵装集团公司债募集说明书-中信建投.pdf"
		// };

		int minSupport = 2;
		int gaps = 1;
		int maxLength = 50;
		File dataDir = new File("data/");
		String dataDirAbsPath = dataDir.getCanonicalPath();

		for (String fileString : fileList) {
			File toAnalyse = new File(fileString);

			String inputFilename = dataDirAbsPath + "/output/" + toAnalyse.getName();
			String outputFilename = dataDirAbsPath + "/output/" + toAnalyse.getName() + ".output";

			Util.patternGenerator(inputFilename, outputFilename, minSupport, gaps, maxLength, "d");

			File fruOutputFile = new File(dataDirAbsPath + "/output/" + toAnalyse.getName() + ".output/translatedFS");
			if (fruOutputFile.isDirectory())
				fruOutputFile = new File(fruOutputFile, "part-r-00000");
			
			Object[] res = Util.patternFilter(fruOutputFile);
			
			Integer[] listIndex = (Integer[]) res[0];
			ArrayList<String> fruItemsList = (ArrayList<String>) res[1];
			ArrayList<String> fruSupStringList = (ArrayList<String>) res[2];
			final ArrayList<Integer> patternLengthList = (ArrayList<Integer>) res[3];

			File filterResFile = new File(
					dataDirAbsPath + "/output/" + toAnalyse.getName() + ".output/filteredFruItems");
			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(filterResFile, false), "utf-8");
			for (Integer index : listIndex) {
				osw.append(fruItemsList.get(index));
				osw.append("\n");
			}
			osw.close();
		}
		System.out.println("All finished!");
	}
}
