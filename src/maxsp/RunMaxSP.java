package maxsp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

public class RunMaxSP {
	private static String inputPath;
	private static String outPath;
	private static int minsup;
	private static boolean showsid;

	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub
		run(args);
	}
	
	public static void run(String[] args) throws IOException{
		parseparas(args);
		MaxSP algo = new MaxSP();
		// Load a sequence database
		SequenceDatabase sequenceDatabase = new SequenceDatabase();
		sequenceDatabase.loadFile(inputPath);
		//	sequenceDatabase.print();
		
		// if you set the following parameter to true, the sequence ids of the sequences where
		// each pattern appears will be shown in the result
		algo.setShowSequenceIdentifiers(showsid);
		
		// execute the algorithm
		algo.runAlgorithm(sequenceDatabase, outPath, minsup);
		algo.printStatistics(sequenceDatabase.size());
	}
	
	public static String fileToPath(String filename) throws UnsupportedEncodingException{
		URL url = RunMaxSP.class.getResource(filename);
		return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}
	
	public static void parseparas(String[] args){
		for(int i = 0;i < args.length; i++){
			if(args[i].equalsIgnoreCase("-i")){
				inputPath = args[i+1];
			}else if(args[i].equalsIgnoreCase("-o")){
				outPath = args[i+1];
			}else if(args[i].equalsIgnoreCase("-s")){
				minsup = Integer.valueOf(args[i+1]);
			}else if(args[i].equalsIgnoreCase("-d")){
				showsid = Boolean.valueOf(args[i+1]);
			}
		}
	}

}
