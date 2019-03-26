import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

public class HybridCF {
	private static final String UCF_RESULT_PATH = "./test/UCFPredict.result";
	private static final String ICF_RESULT_PATH = "./test/ICFPredict.result";
	private static final String Hybrid_RESULT_PATH = "./test/HybridPredict.result";
	private static final String TEST_PATH = "./u1.test";
	private static final int TEST_RECORD_NUM = 20000;
	private static final int MIN_RATING = 1;
	private static final int MAX_RATING = 5;
	private static final int USER_NUM = 943;
	private static final int ITEM_NUM = 1682;
	private static final double  Tradeoff_Parameter_UCF = 0.5;

	private double r_u_i_Hybrid[][];
	Record test[];
	public static void main(String[] args) throws IOException {
		HybridCF hcf = new HybridCF();
		hcf.readIn();
		hcf.calAndShowError();
		hcf.savePreditedRating();
	}
	void calAndShowError() {
		
		double MAE = 0.0, RMSE = 0.0;
		
		double errorSum = 0.0;
		double squareSum = 0.0;
		double error = 0.0;
		
		for (Record tmp : test) {		
			error = Math.abs(tmp.r_ui - r_u_i_Hybrid[tmp.userID][tmp.itemID]);
			errorSum += error;
			squareSum += error * error;
		}
		MAE = (double)errorSum / TEST_RECORD_NUM;
		RMSE = Math.sqrt((double)squareSum / TEST_RECORD_NUM);
		
		System.out.println("HybridCF\t" + "\tRMSE:" + RMSE +  "MAE: " + MAE );
	
	}
	void readIn() throws IOException {
		test = new Record[TEST_RECORD_NUM];
		File testFile = new File(TEST_PATH) ;
		FileInputStream testFileIn = new FileInputStream(testFile);
		InputStreamReader testIn = new InputStreamReader(testFileIn);
		BufferedReader testReader =  new BufferedReader(testIn);
		for (int i = 0; i < TEST_RECORD_NUM; i++) {
			String data[] = testReader.readLine().split("\\s+");
			Record newRecord = new Record(Integer.valueOf(data[0]), Integer.valueOf(data[1]), Double.valueOf(data[2]));
			test[i] = newRecord;
		}
		testReader.close();
		testIn.close();
		testFileIn.close();
		
		r_u_i_Hybrid = new double [USER_NUM + 1][ITEM_NUM + 1];
		File ucfFile = new File(UCF_RESULT_PATH)  ;
		FileInputStream ucfFileIn = new FileInputStream(ucfFile);
		InputStreamReader ucfIn = new InputStreamReader(ucfFileIn);
		BufferedReader ucfReader =  new BufferedReader(ucfIn);
		String data[];
		String line;
		while((line = ucfReader.readLine()) != null) {
			data = line.split("\\s+");
			r_u_i_Hybrid[Integer.valueOf(data[0])][ Integer.valueOf(data[1])] = Tradeoff_Parameter_UCF * Double.valueOf(data[2]);
		}
		ucfReader.close();
		ucfIn.close();
		ucfFileIn.close();
		
		File icfFile = new File(ICF_RESULT_PATH)  ;
		FileInputStream icfFileIn = new FileInputStream(icfFile);
		InputStreamReader icfIn = new InputStreamReader(icfFileIn);
		BufferedReader icfReader =  new BufferedReader(icfIn);
		while((line = icfReader.readLine()) != null) {
			data = line.split("\\s+");
			r_u_i_Hybrid[Integer.valueOf(data[0])][ Integer.valueOf(data[1])] +=  (1.0 - Tradeoff_Parameter_UCF) * Double.valueOf(data[2]);
		}
		icfReader.close();
		icfIn.close();
		icfFileIn.close();
	}
	void savePreditedRating() throws IOException {
		File f = new File(Hybrid_RESULT_PATH);
		if (!f.exists()) f.createNewFile();		
		FileWriter fw = new FileWriter(f);
		BufferedWriter save = new BufferedWriter(fw);		
		String data;
		double result = 0;
		for (int u = 1; u <= USER_NUM; u++) {
			for (int i = 1; i <= ITEM_NUM; i++) {
					result = r_u_i_Hybrid[u][i];
					if (result != 0) {
					data = u + " " + i + " " + result + '\n';
					save.write(data);
					}
			}
		}
		save.flush();
		save.close();
	}
}
