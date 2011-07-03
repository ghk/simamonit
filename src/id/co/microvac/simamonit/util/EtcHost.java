package id.co.microvac.simamonit.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

public class EtcHost {
	public static String ETCHOST_PATH = "/system/etc/hosts";

	private static String SUFFIX = ".sima";

	private String etcHostPath;

	private List<String> lines = new ArrayList<String>();
	private Map<String, Integer> simaLines = new HashMap<String, Integer>();

	public EtcHost() throws IOException
    {
    	this.etcHostPath = ETCHOST_PATH;
		InputStreamReader isr = new InputStreamReader(new FileInputStream(this.etcHostPath));
		BufferedReader br = new BufferedReader(isr);
		
		String line = null;
		int i = 0;
		while ((line = br.readLine()) != null)   {
			lines.add(line);
			Log.i("simamonit", "lines: "+line);
			if(IsSimaEntry(line)){
				simaLines.put(ExtractSimaEntry(line), i);
			}
			i++;
		}
		br.close();
		isr.close();
    }

	public void Update(String node, String ip) {
		if (!simaLines.containsKey(node)) {
			simaLines.put(node, lines.size());
			lines.add("");
		}
		lines.set(simaLines.get(node), ip + " " + node + SUFFIX);
	}

	public void Save(String tempFileName) throws Exception{
		try{
			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(tempFileName));
			BufferedWriter bw = new BufferedWriter(osw);
			for(String line: lines){
				bw.write(line);
				bw.newLine();
			}
			bw.flush();
			bw.close();
			osw.close();
			moveInSu(tempFileName);
		}
		finally{
		}
	}
	
	public void moveInSu(String filename) throws Exception{
		 String command = 
		  "busybox mount -o remount,rw /system \n" +
		  "cp "+filename+" "+this.etcHostPath+" \n"+
		  "busybox mount -o remount,ro /system \n";
		 executeCommand(command);
	}
	
	public static void executeCommand(String command) throws Exception{
		 Process process;
		 try {
		  Log.i("simamonit", "will execute su for ");
		  Log.i("simamonit", command);
		  process = Runtime.getRuntime().exec("su -c sh");
		  Log.i("simamonit", "su executed");
		  DataOutputStream os = new DataOutputStream(process.getOutputStream());
		  //DataInputStream osRes = new DataInputStream(process.getInputStream());
		  os.writeBytes(command); os.flush();
		  // and finally close the shell
		  os.writeBytes("exit\n"); os.flush();
		  process.waitFor();
		 } catch (IOException e) {
		  e.printStackTrace();
		  throw e;
		 } catch (InterruptedException e) {
		  e.printStackTrace();
		  throw e;
		 } 
	}

	private static String ExtractSimaEntry(String content) {
		if (content.contains("#"))
			content = content.substring(0, content.indexOf("#"));

		String[] splitted = content.split(" ");
		return splitted[1].substring(0, splitted[1].length() - SUFFIX.length());
	}

	private static boolean IsSimaEntry(String content) {
		if (content.contains("#"))
			content = content.substring(0, content.indexOf("#"));

		if (content.trim().length() == 0)
			return false;

		String[] splitted = content.split(" ");
		if (splitted.length < 2)
			return false;
		return splitted[1].endsWith(SUFFIX);
	}

}