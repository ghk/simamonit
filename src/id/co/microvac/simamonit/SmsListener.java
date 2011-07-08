package id.co.microvac.simamonit;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import id.co.microvac.simamonit.entity.Node;
import id.co.microvac.simamonit.util.CommandBuilder;
import id.co.microvac.simamonit.util.CommandRunner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class SmsListener extends BroadcastReceiver {

    private static final String LOG_TAG = "SMSApp";
    private static final String PREFIX = "#sima ";
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
    private static final long COMMAND_TIMEOUT = 30000;
    private static final int MAX_REPLY_LENGTH = 300;
            

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            Bundle bundle = intent.getExtras();
            Object messages[] = (Object[]) bundle.get("pdus");
    		for (int n = 0; n < messages.length; n++) {
    			processMessage(context, SmsMessage.createFromPdu((byte[]) messages[n]));
    		}
        }
    }
    
    private void processMessage(final Context context, final SmsMessage message){
    	String command = getCommand(message);
    	if(command != null){
    		Toast.makeText(context,
    				"Will run command: " + command, Toast.LENGTH_LONG).show();
	    	final StringBuilder sb = new StringBuilder();
			Handler handler = new Handler(){
				@Override
				public void handleMessage(Message msg) {
					switch(msg.what){
					case CommandRunner.OUTPUT_RECEIVED:
						String line = (String) msg.obj;
						sb.append(line+"\n");
						break;
					case CommandRunner.PROCESS_EXIT:
						int exitValue = (Integer) msg.obj;
						sendReply(context, message, sb, exitValue);
						break;
	    			}
				}
			};
			
			try{
				CommandRunner runner = new CommandRunner(handler, null, command);
				runner.run(COMMAND_TIMEOUT);
			}
			catch(Exception e){
				e.printStackTrace();
			}
    	}
    	Toast.makeText(context,
				"Received SMS: " + message.getMessageBody(), Toast.LENGTH_LONG).show();
    }
    
    private void sendReply(Context context, SmsMessage received, StringBuilder output, int exitValue){
    	try{
	    	String message = output.length() > MAX_REPLY_LENGTH ? output.toString() : output.substring(0, MAX_REPLY_LENGTH - 1);
	    	message+= "\n["+exitValue+"]";
	    	message = removeDuplicateWhitespace(message).toString();
	    	Toast.makeText(context,
				"Will reply, recipient: "+received.getOriginatingAddress()+" message: " + message, Toast.LENGTH_LONG).show();
	    	SmsManager sm = SmsManager.getDefault();
	    	ArrayList<String> multipart = sm.divideMessage(message);
	    	sm.sendMultipartTextMessage(received.getOriginatingAddress(), null, multipart, null, null);
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}

    }
    
    public static CharSequence removeDuplicateWhitespace(CharSequence inputStr) {
        String patternStr = "\\s+";
        String replaceStr = " ";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(inputStr);
        return matcher.replaceAll(replaceStr);
    }

	private String getCommand(SmsMessage message){
		String messageBody = message.getMessageBody();
		if(messageBody.startsWith(PREFIX)){
			messageBody = messageBody.substring(PREFIX.length());
			int spaceIndex = messageBody.indexOf(' ');
			if(spaceIndex != -1 && spaceIndex + 1 < messageBody.length()){
				String nodeName = messageBody.substring(0, spaceIndex).trim();
				String commandText = messageBody.substring(spaceIndex + 1); 
				if(nodeName.length() != 0){
					Node node = new Node();
					node.setName(nodeName);
					return CommandBuilder.sshCommand(node, commandText);
				}
			}
		}
		return null;
	}
}
