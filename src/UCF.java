import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.event.TreeExpansionEvent;

public class UCF {
	private static final int USER_NUM = 943;
	private static final int ITEM_NUM = 1682;
	private static final int K = 50;
	private static final int TRAINNING_RECORD_NUM = 1586126;
	private static final int TEST_RECORD_NUM = 20000;
	private static final int MIN_RATING = 1;
	private static final int MAX_RATING = 5;
	private static final double threshold = 0.05;
	private static final String BASE_PATH = "./u1.base";
	private static final String TEST_PATH = "./u1.test";
	//private static final String PRE_RESULT_PATH = "./UCFPredict.result";
	private static final String PRE_RESULT_PATH = "./test/UCFPredict.result";
	private double ave_r_u[];
	private HashMap<Integer, Set<Integer>> I_u;
	private HashMap<Integer, Set<Integer>> N_u;
	private HashMap<Integer, Set<Integer>> U_i;
	private double r_u_i[][];
	private double ave_r;
	private double s_w_u[][];
	Record trainning[];
	Record test[];
	UCF() {
		trainning = new Record[TRAINNING_RECORD_NUM];
		test = new Record[TEST_RECORD_NUM];
		ave_r_u = new double[USER_NUM + 1];
		I_u = new HashMap<Integer, Set<Integer>>();
		r_u_i = new double[USER_NUM + 1][ITEM_NUM + 1];
		s_w_u = new double[USER_NUM + 1][USER_NUM + 1];
		N_u = new HashMap<Integer, Set<Integer>>();
		U_i = new HashMap<Integer, Set<Integer>>();
	}
	public static void main(String[] args) throws IOException {
		UCF ucf = new UCF();
		ucf.readIn();
		ucf.Initial();
		ucf.savePreditedRating();
		//ucf.saveAve_r_u();
		ucf.calAndShowError();
	}
	void CountSimilarityOfUsers() {
		for (int w = 1; w <= USER_NUM; w++) {  //计算每一个用户u和 用户w之间的相似度
			for (int u = 1; u < w; u++) {			
				Set<Integer> setwu = new HashSet<Integer>(I_u.get(w));//得到I_w
				setwu.retainAll(I_u.get(u));  //得到I_u
				if (setwu.size() == 0) {  //不存在用户u和用户w同时评分过的物品
					s_w_u[w][u] = 0;
					continue;
				}
				double sum1 = 0, sum2 = 0, sum3 = 0;
				for (Iterator<Integer> i = setwu.iterator(); i.hasNext(); ) {
					int k = i.next();
					sum1 += (r_u_i[u][k] - ave_r_u[u]) * (r_u_i[w][k] - ave_r_u[w]);
					sum2 += (r_u_i[u][k] - ave_r_u[u]) * (r_u_i[u][k] - ave_r_u[u]);
					sum3 += (r_u_i[w][k] - ave_r_u[w]) * (r_u_i[w][k] - ave_r_u[w]);
				}
				if (sum2 <= 0 || sum3 <= 0) s_w_u[w][u] = 0;
				else  s_w_u[w][u] = sum1 / (Math.sqrt(sum2) * Math.sqrt(sum3));
				if (s_w_u[w][u] < this.threshold) s_w_u[w][u] = 0;  //阈值
				if (s_w_u[w][u] == 0) continue;
	
				s_w_u[u][w] = s_w_u[w][u];
				
				//存储用户w的相似用户
				if (!N_u.containsKey(w)) {
					Set<Integer> tmp = new HashSet<Integer>();
					tmp.add(u);
					N_u.put(w, tmp);
				}
				else 
					N_u.get(w).add(u);
				
				//存储用户u的相似用户
				if (!N_u.containsKey(u)) {
					Set<Integer> tmp = new HashSet<Integer>();
					tmp.add(w);
					N_u.put(u, tmp);
				}
				else N_u.get(u).add(w); 
				
			}
		}
	}
	void Initial() {
		double u_sum_rating[] = new double [USER_NUM + 1];
		int itemNumOfU[] = new int[USER_NUM + 1];
		double sumOfRating = 0;
		for (Record tmp : trainning) {
			if (!I_u.containsKey(tmp.userID)) {
				HashSet<Integer> tmpSet = new HashSet<Integer>();
				I_u.put(tmp.userID, tmpSet);
			}
			I_u.get(tmp.userID).add(tmp.itemID);
			
			//存储标记物品j的用户u
			if (!U_i.containsKey(tmp.itemID)) {
				HashSet<Integer> tmpSet = new HashSet<Integer>();
				U_i.put(tmp.itemID, tmpSet);
			}
			U_i.get(tmp.itemID).add(tmp.userID);
			
			r_u_i[tmp.userID][tmp.itemID] = tmp.r_ui;
			u_sum_rating[tmp.userID] += tmp.r_ui;
			itemNumOfU[tmp.userID]++;
			sumOfRating += tmp.r_ui;
			
		}
		ave_r = sumOfRating / TRAINNING_RECORD_NUM;
		for (int u = 1; u <= USER_NUM; u++) {
			if (itemNumOfU[u] == 0) ave_r_u[u] = ave_r;
			else ave_r_u[u] = u_sum_rating[u] / itemNumOfU[u];
		}
		
		this.CountSimilarityOfUsers();
	}
	void readIn() throws IOException {
		File baseFile = new File(BASE_PATH) ;
		FileInputStream baseFileIn = new FileInputStream(baseFile);
		InputStreamReader baseIn = new InputStreamReader(baseFileIn);
		BufferedReader baseReader =  new BufferedReader(baseIn);
		for (int i = 0; i < TRAINNING_RECORD_NUM; i++) {
			String data[] = baseReader.readLine().split("\\s+");
			Record newRecord = new Record(Integer.valueOf(data[0]), Integer.valueOf(data[1]), Double.valueOf(data[2]));
			trainning[i] = newRecord;
		}
		baseReader.close();
		baseIn.close();
		baseFileIn.close();
		
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
		
	}
	void calAndShowError() {
		
		double predicted_rating[] = null;	
		double MAE = 0.0, RMSE = 0.0;
		
		double errorSum = 0.0;
		double squareSum = 0.0;
		double error = 0.0;
		predicted_rating = this.ave_r_u;
		
		for (Record tmp : test) {		
			error = Math.abs(tmp.r_ui - r_u_i[tmp.userID][tmp.itemID]);
			errorSum += error;
			squareSum += error * error;
		}
		MAE = (double)errorSum / TEST_RECORD_NUM;
		RMSE = Math.sqrt((double)squareSum / TEST_RECORD_NUM);
		
		System.out.println("UCF\t" + "\tRMSE:" + RMSE +  "MAE: " + MAE );
	
	}
	/*void saveAve_r_u() throws IOException {
		File f = new File(AVE_RESULT_PATH);
		if (!f.exists()) f.createNewFile();		
		FileWriter fw = new FileWriter(f);
		BufferedWriter save = new BufferedWriter(fw);
		String data;
		for (int u = 1; u <= USER_NUM; u++) {
			data = u + " " + ave_r_u[u];
			save.write(data);
		}
	}*/
	void savePreditedRating() throws IOException {
		File f = new File(PRE_RESULT_PATH);
		if (!f.exists()) f.createNewFile();		
		FileWriter fw = new FileWriter(f);
		BufferedWriter save = new BufferedWriter(fw);		
		String data;
		for (int u = 1; u <= USER_NUM; u++) {
			for (int i = 1; i <= ITEM_NUM; i++) {
				if (r_u_i[u][i] == 0) {
					double result = 0;
					if (!U_i.containsKey(i) || N_u.get(u).size() == 0) { 
						result = ave_r_u[u];
					}
					else {
						final int uu = u;
						TreeSet<Integer> N_i_u= new TreeSet<Integer>(new Comparator<Integer>() {
							public int compare(Integer w1, Integer w2) {  //自定义比较器，将集合中的用户按照相似度排序
								return s_w_u[uu][w1] - s_w_u[uu][w2] < 0 ? 1 : -1;
							}
						});
						N_i_u.addAll(N_u.get(u));  
						N_i_u.retainAll(U_i.get(i));  //合并集合
						if (N_i_u.size() == 0) {
							result = ave_r_u[u];
						}
						else {
							double sum1 = 0, sum2 = 0;
							//N_i_u.comparator();
							//遍历前K个用户，当不足K个时，尽量多取
							int t = Math.min(K, N_i_u.size());
							for (Iterator<Integer> iter = N_i_u.iterator(); iter.hasNext(); ) {
								int w = iter.next();
								sum1 += s_w_u[w][u] * (r_u_i[w][i] - ave_r_u[w]);
								sum2 += Math.abs(s_w_u[w][u]);
								t--;
								if (t == 0) break;
							}
							 result = ave_r_u[u] + sum1 / sum2;  //预测公式，用户u的平均评分在读取数据时计算
							
								
						}	
					}
				
					if (result < MIN_RATING) result = MIN_RATING;
					else if (result > MAX_RATING) result = MAX_RATING;
					r_u_i[u][i] = result;
					data = u + " " + i + " " + result + '\n';
					save.write(data);
				}
			
			}
		
		}
		save.flush();
		save.close();
	}
}
