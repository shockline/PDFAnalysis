package entrance;

import java.io.File;

public class AnlysePDFFile {
	public static void main(String[] args) throws Exception {

		String[] fileList = new String[] { "data/input/京东世纪贸易公司债募集说明书（反馈回复稿）-瑞银证券.pdf",
//				"data/input/中海地产公司债募集说明书-中信证券.pdf", "data/input/联想控股公司债募集说明书-银河证券.pdf",
//				"data/input/兵装集团公司债募集说明书-中信建投.pdf", "data/input/神舟租车公司债募集说明书-中金公司.pdf",
//				"data/input/北新建材公司债募集说明书-中信证券.pdf", "data/input/清华控股公司债募集说明书-国泰君安.pdf",
//				"data/input/1 保利房地产（集团）股份有限公司公开发行2016 年公司债券（第二期）募集说明书.pdf",
				};
		// String[] fileList = new String[] { "data/input/兵装集团公司债募集说明书-中信建投.pdf"
		// };
		for (String fileString : fileList) {
			File toAnalyse = new File(fileString);
			Util.analyse(toAnalyse, "d");
		}
		System.out.println("All finished!");
	}
}
