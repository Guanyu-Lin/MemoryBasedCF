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

public class ICF {
	private static final int USER_NUM = 943;
	private static final int ITEM_NUM = 1682;
	private static final int K = 50;
	private static final int TRAINNING_RECORD_NUM = 80000;
	private static final int TEST_RECORD_NUM = 20000;
	private static final int MIN_RATING = 1;
	private static final int MAX_RATING = 5;
	private static final double threshold = 0.195;
	private static final String BASE_PATH = "./u1.base";
	private static final String TEST_PATH = "./u1.test";
	private static final String PRE_RESULT_PATH = "./test/ICFPredict.result";
	private double ave_r_u[];

	private HashMap<Integer, Set<Integer>> I_u;
	private HashMap<Integer, Set<Integer>> N_j;
	private HashMap<Integer, Set<Integer>> U_i;
	
	private double r_u_i[][];
	private double ave_r;
	private double s_k_j[][];
	
	Record trainning[];
	Record test[];
	ICF() {
		trainning = new Record[TRAINNING_RECORD_NUM];
		test = new Record[TEST_RECORD_NUM];
		ave_r_u = new double[USER_NUM + 1];
		I_u = new HashMap<Integer, Set<Integer>>();
		r_u_i = new double[USER_NUM + 1][ITEM_NUM + 1];
		s_k_j = new double[ITEM_NUM + 1][ITEM_NUM + 1];
		N_j = new HashMap<Integer, Set<Integer>>();
		U_i = new HashMap<Integer, Set<Integer>>();
	}
	public static void main(String[] args) throws IOException {
		ICF icf = new ICF();
		icf.readIn();
		icf.Initial();
		icf.savePreditedRating();
		icf.calAndShowError();
	}
	void CountSimilarityOfItems() {
		for (int k = 1; k <= ITEM_NUM; k++) {   //对每个物品k和j计算相似度
			for (int j = 1; j < k; j++) {
				if (!U_i.containsKey(k) || !U_i.containsKey(j)) {  
					continue;
				}
				Set<Integer> setkj = new HashSet<Integer>(U_i.get(k));  //得到U_k
				setkj.retainAll(U_i.get(j));  //合并U_k和U_j
				if (setkj.size() == 0) {  //不存在同时对物品k和j评分过的用户
					continue;
				}
				//sum1表示公式（8）的分子，sum2表示左下方的方差，sum3表示右下方的方差
				double sum1 = 0, sum2 = 0, sum3 = 0;
				for (Iterator<Integer> i = setkj.iterator(); i.hasNext(); ) {
					int u = i.next();
					sum1 += (r_u_i[u][k] - ave_r_u[u]) * (r_u_i[u][j] - ave_r_u[u]);
					sum2 += (r_u_i[u][k] - ave_r_u[u]) * (r_u_i[u][k] - ave_r_u[u]);
					sum3 += (r_u_i[u][j] - ave_r_u[u]) * (r_u_i[u][j] - ave_r_u[u]);
				}
				if (sum2 <= 0 || sum3 <= 0) s_k_j[k][j] = 0;
				else s_k_j[k][j] = sum1 / (Math.sqrt(sum2) * Math.sqrt(sum3));  //计算公式
				
				if (s_k_j[k][j] < this.threshold) s_k_j[k][j] = 0;
				if (s_k_j[k][j] == 0) continue;
				s_k_j[j][k] = s_k_j[k][j];
				//存储物品k的相似物品
				if (!N_j.containsKey(k)) {
					Set<Integer> tmp = new HashSet<Integer>();
					tmp.add(j);
					N_j.put(k, tmp);
				}
				else 
					N_j.get(k).add(j);
				//存储物品j的相似物品
				if (!N_j.containsKey(j)) {
					Set<Integer> tmp = new HashSet<Integer>();
					tmp.add(k);
					N_j.put(j, tmp);
				}
				else N_j.get(j).add(k); 
				
			}
		}
	}
	void Initial() {
		double u_sum_rating[] = new double [USER_NUM + 1];
		int itemNumOfU[] = new int[USER_NUM + 1];
		double sumOfRating = 0;
		for (Record tmp : trainning) {
			//记录被用户u评分过的物品I_u
			if (!I_u.containsKey(tmp.userID)) {
				HashSet<Integer> tmpSet = new HashSet<Integer>();
				I_u.put(tmp.userID, tmpSet);
			}
			I_u.get(tmp.userID).add(tmp.itemID);
			
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
		
		this.CountSimilarityOfItems();
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
			
		double MAE = 0.0, RMSE = 0.0;
		
		double errorSum = 0.0;
		double squareSum = 0.0;
		double error = 0.0;	
		for (Record tmp : test) {		
			error = Math.abs(tmp.r_ui - r_u_i[tmp.userID][tmp.itemID]);
			errorSum += error;
			squareSum += error * error;
		}
		MAE = (double)errorSum / TEST_RECORD_NUM;
		RMSE = Math.sqrt((double)squareSum / TEST_RECORD_NUM);
		
		System.out.println("ICF\t" + "\tRMSE:" + RMSE +  "MAE: " + MAE );
	
	}
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
					
					if (!I_u.containsKey(u) || !N_j.containsKey(i) || N_j.get(i).size() == 0) { 
						result = ave_r_u[u];
					}
					else {
						final int jj = i;
						TreeSet<Integer> N_u_j= new TreeSet<Integer>(new Comparator<Integer>() {
							public int compare(Integer k1, Integer k2) {  //自定义比较器，将集合中的物品按照相似度排序
								return s_k_j[jj][k1] - s_k_j[jj][k2] < 0 ? 1 : -1;
							}
						});
						N_u_j.addAll(N_j.get(i));
						N_u_j.retainAll(I_u.get(u));  //合并集合
						if (N_u_j.size() == 0) {
							result = ave_r_u[u];
						}
						else {
							double sum1 = 0, sum2 = 0;
							N_u_j.comparator();
							//选取前K个物品，当数量不足K个时尽量多取
							int t = Math.min(K, N_u_j.size());
							for (Iterator<Integer> iter = N_u_j.iterator(); iter.hasNext(); ) {
								int k = iter.next();
								//公式（9）分子计算
								sum1 += s_k_j[k][i] * r_u_i[u][k];
								//分母计算
								sum2 += (s_k_j[k][i]);
								t--;
								if (t == 0) break;
							}			
							result = sum1 / sum2;  //预测公式									
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
