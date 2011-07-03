package id.co.microvac.simamonit;

import id.co.microvac.simamonit.util.UiUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class HostedCommandActivity extends Activity {
	
	private static final int OUTPUT_RECEIVED = 1000;
	
	private String command;
	private Process process;
	private ProcessWaitingThread waitingThread;
	
	private Handler handler = new Handler(){
		public void dispatchMessage(Message msg) {
			switch(msg.what){
				case OUTPUT_RECEIVED:
					String line = (String) msg.obj;
					((TextView)findViewById(R.id.output)).append(line+"\n");
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle data = this.getIntent().getExtras();
		if(data != null){
			command = data.getString("command");
			setContentView(R.layout.command);
        	((Button)findViewById(R.id.status)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(waitingThread != null && !waitingThread.isProcessFinished())
						process.destroy();
				}
			});
			runCommand();
		}
		else{
			finish();
		}
	}
	
	@Override
	protected void onDestroy() {
		if(process != null){
			try{
				process.destroy();
			}
			catch(Exception e){
			}
		}
		super.onDestroy();
	}
	
	private void runCommand(){
		try{
			process = Runtime.getRuntime().exec(command);
			waitingThread = new ProcessWaitingThread(process);
			waitingThread.start();
			new OutputReadingThread(process.getInputStream()).start();
			new OutputReadingThread(process.getErrorStream()).start();
		}
		catch(Exception e){
			UiUtil.showException(e, this);
		}
	}
	
	private class OutputReadingThread extends Thread{
        private InputStream input;

        public OutputReadingThread(InputStream input) {
            this.input = input;
        }

        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            try {
                String line = null;
                while ((line = reader.readLine()) != null) {
                	handler.sendMessage(Message.obtain(handler, OUTPUT_RECEIVED, line));
                }
            } catch (IOException ioe) {
            } finally {
                try {
                    reader.close();
                    input.close();
                } catch (Exception e) {
                }
            }
        }
	}
	
	private class ProcessWaitingThread extends Thread{
		
        private Process process;
    	private boolean processFinished = false;
    	private int exitValue = 0;

        public ProcessWaitingThread(Process process) {
            this.process = process;
        }

        public void run() {
        	while(!processFinished)
        	{
        		try{
        			exitValue = process.exitValue();
        			processFinished = true;
        		}
        		catch(Exception e){
        			try{
	        			exitValue = process.waitFor();
	        			processFinished = true;
        			}
        			catch(InterruptedException ie){
        			}
        		}
        	}
        	runOnUiThread(new Runnable() {
				@Override
				public void run() {
		        	((Button)findViewById(R.id.status)).setText("Finished ("+exitValue+")");
				}
			});
        }
        
        public boolean isProcessFinished() {
			return processFinished;
		}
	}
}
