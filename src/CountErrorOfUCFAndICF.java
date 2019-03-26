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
//
public class CountErrorOfUCFAndICF {
	private static final String UCF_RESULT_PATH = "./test/UCFPredict.result";
	private static final String ICF_RESULT_PATH = "./test/ICFPredict.result";
	private static final String Hybrid_RESULT_PATH = "./test/HybridPredict.result";
	private static final String TEST_PATH = "./u1.test";
	private static final String ERROR_PATH = "./test/error.result";
	private static final int TEST_RECORD_NUM = 20000;
	private static final int MIN_RATING = 1;
	private static final int MAX_RATING = 5;
	private static final int USER_NUM = 943;
	private static final int ITEM_NUM = 1682;
	private static final double  Tradeoff_Parameter_UCF = 0.5;
	private double UCF_Predict_R[][];
	private double ICF_Predict_R[][];
	private double Hybrid_Predict_R[][];
	Record test[];
	public static void main(String[] args) throws IOException {
		CountErrorOfUCFAndICF error = new CountErrorOfUCFAndICF();
		error.readIn();
		error.calAndSaveError();
		
	}
	void calAndSaveError() throws IOException {
		File f = new File(ERROR_PATH);
		if (!f.exists()) f.createNewFile();		
		FileWriter fw = new FileWriter(f);
		BufferedWriter save = new BufferedWriter(fw);
		String data;
			
		double MAE = 0.0, RMSE = 0.0;
		
		double errorSum = 0.0;
		double squareSum = 0.0;
		double error = 0.0;
		
		for (Record tmp : test) {		
			error = Math.abs(tmp.r_ui - UCF_Predict_R[tmp.userID][tmp.itemID]);
			errorSum += error;
			squareSum += error * error;
		}
		MAE = (double)errorSum / TEST_RECORD_NUM;
		RMSE = Math.sqrt((double)squareSum / TEST_RECORD_NUM);
		data = "UCF\t" + "RMSE: " + RMSE +  "\tMAE: " + MAE + '\n'; 
		
		errorSum = 0.0;
		squareSum = 0.0;
		error = 0.0;
		
		for (Record tmp : test) {		
			error = Math.abs(tmp.r_ui - ICF_Predict_R[tmp.userID][tmp.itemID]);
			errorSum += error;
			squareSum += error * error;
		}
		MAE = (double)errorSum / TEST_RECORD_NUM;
		RMSE = Math.sqrt((double)squareSum / TEST_RECORD_NUM);
		data = data + "ICF\t" + "RMSE: " + RMSE +  "\tMAE: " + MAE + '\n';
		
		errorSum = 0.0;
		squareSum = 0.0;
		error = 0.0;
		
		for (Record tmp : test) {		
			error = Math.abs(tmp.r_ui - Hybrid_Predict_R[tmp.userID][tmp.itemID]);
			errorSum += error;
			squareSum += error * error;
		}
		MAE = (double)errorSum / TEST_RECORD_NUM;
		RMSE = Math.sqrt((double)squareSum / TEST_RECORD_NUM);
		data = data + "Hybrid\t" + "RMSE: " + RMSE +  "\tMAE: " + MAE + '\n';
		
		save.write(data);
		
		save.flush();
		save.close();
	
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
		
		UCF_Predict_R = new double [USER_NUM + 1][ITEM_NUM + 1];
		File ucfFile = new File(UCF_RESULT_PATH)  ;
		FileInputStream ucfFileIn = new FileInputStream(ucfFile);
		InputStreamReader ucfIn = new InputStreamReader(ucfFileIn);
		BufferedReader ucfReader =  new BufferedReader(ucfIn);
		String data[];
		String line;
		while((line = ucfReader.readLine()) != null) {
			data = line.split("\\s+");
			UCF_Predict_R[Integer.valueOf(data[0])][ Integer.valueOf(data[1])] = Double.valueOf(data[2]);
		}
		ucfReader.close();
		ucfIn.close();
		ucfFileIn.close();
		
		ICF_Predict_R = new double [USER_NUM + 1][ITEM_NUM + 1];
		File icfFile = new File(ICF_RESULT_PATH)  ;
		FileInputStream icfFileIn = new FileInputStream(icfFile);
		InputStreamReader icfIn = new InputStreamReader(icfFileIn);
		BufferedReader icfReader =  new BufferedReader(icfIn);
		
		while((line = icfReader.readLine()) != null) {
			data = line.split("\\s+");
			ICF_Predict_R[Integer.valueOf(data[0])][ Integer.valueOf(data[1])] = Double.valueOf(data[2]);
		}
		icfReader.close();
		icfIn.close();
		icfFileIn.close();
		
		
		Hybrid_Predict_R = new double [USER_NUM + 1][ITEM_NUM + 1];
		File hcfFile = new File(Hybrid_RESULT_PATH)  ;
		FileInputStream hcfFileIn = new FileInputStream(hcfFile);
		InputStreamReader hcfIn = new InputStreamReader(hcfFileIn);
		BufferedReader hcfReader =  new BufferedReader(hcfIn);
		
		while((line = hcfReader.readLine()) != null) {
			data = line.split("\\s+");
			Hybrid_Predict_R[Integer.valueOf(data[0])][ Integer.valueOf(data[1])] = Double.valueOf(data[2]);
		}
		hcfReader.close();
		hcfIn.close();
		hcfFileIn.close();
		
				
	}
	
}
