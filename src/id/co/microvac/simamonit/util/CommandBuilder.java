package id.co.microvac.simamonit.util;

import id.co.microvac.simamonit.entity.Command;
import id.co.microvac.simamonit.entity.Node;

public class CommandBuilder {
	public static String debianCommand(String command){
		return "/system/bin/debian -u "+command;
	}
	
	public static String debianBtepCommand(String command){
		return "/system/bin/debian -u \""+command+"\"";
	}
	
	public static String sshCommand(Node node, String command){
		return debianCommand("ssh "+node.getName()+".sima "+command);
	}
	
	public static Command tailLogCommand(Node node, String file){
		String command = debianCommand("ssh -t -t "+node.getName()+".sima tail -f "+file);
		return new Command(command, file);
	}
	
	public static Command shellCommand(Node node, String shell){
		String command = debianBtepCommand("ssh -t "+node.getName()+".sima "+shell);
		return new Command(command, shell);
	}
	
}
