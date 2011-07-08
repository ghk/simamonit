package id.co.microvac.simamonit;

import id.co.microvac.simamonit.util.CommandRunner;
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
	
	private CommandRunner commandRunner;
	private String command;
	
	private Handler handler = new Handler(){
		public void dispatchMessage(Message msg) {
			switch(msg.what){
				case CommandRunner.OUTPUT_RECEIVED:
					String line = (String) msg.obj;
					((TextView)findViewById(R.id.output)).append(line+"\n");
					break;
				case CommandRunner.PROCESS_EXIT:
					int exitValue = (Integer) msg.obj;
					((Button)findViewById(R.id.status)).setText("Finished ("+exitValue+")");
					break;
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
					if(commandRunner != null && !commandRunner.isProcessFinished())
						commandRunner.kill();
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
		if(commandRunner != null){
			try{
				commandRunner.kill();
			}
			catch(Exception e){
			}
		}
		super.onDestroy();
	}
	
	private void runCommand(){
		commandRunner = new CommandRunner(handler, this, command);
		commandRunner.run(0);
	}
	
}
