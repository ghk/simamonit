package id.co.microvac.simamonit.util;

import id.co.microvac.simamonit.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;

public class CommandRunner {
	public static final int OUTPUT_RECEIVED = 1000;
	public static final int PROCESS_EXIT = 1001;
	
	private Handler handler;
	private Activity context;
	private String command;
	private Process process;
	private boolean alreadyRunning = false;
	private boolean processFinished = false;
	
	
	public CommandRunner(Handler handler, Activity context, String command) {
		this.handler = handler;
		this.context = context;
		this.command = command;
	}
	
	public void run(long timeout){
		if(alreadyRunning)
			throw new IllegalStateException("Process already running");
		
		try{
			process = Runtime.getRuntime().exec(command);
			Thread waitingThread = new ProcessWaitingThread(process);
			waitingThread.start();
			new OutputReadingThread(process.getInputStream()).start();
			new OutputReadingThread(process.getErrorStream()).start();
			if(timeout > 0){
				new ProcessKillingThread(process, timeout).start();
			}
		}
		catch(Exception e){
			if(context != null)
				UiUtil.showException(e, context);
			else
				e.printStackTrace();
		}
	}
	
	public boolean isProcessFinished(){
		return processFinished;
	}
	
	public void kill(){
		process.destroy();
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
        	handler.sendMessage(Message.obtain(handler, PROCESS_EXIT, exitValue));
        }
        
        public boolean isProcessFinished() {
			return processFinished;
		}
	}
	
	private class ProcessKillingThread extends Thread{
		
        private Process process;
    	private long timeout = 0;

        public ProcessKillingThread(Process process, long timeout) {
            this.process = process;
            this.timeout = timeout;
        }

        public void run() {
        	long startmillis = System.currentTimeMillis();
        	long spent = 0;
        	while(spent < timeout){
        		try{
            		Thread.sleep(timeout - spent);
            	}
            	catch(InterruptedException ie){
            	}
            	spent = System.currentTimeMillis() - startmillis;
        	}
        	
        	try{
    			process.exitValue();
    		}
    		catch(Exception e){
    			process.destroy();
    		}
        }
   
	}
}
