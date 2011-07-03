package id.co.microvac.simamonit.entity;

public class Command {
	private String command;
	private String caption;
	
	public Command(String command, String caption) {
		super();
		this.command = command;
		this.caption = caption;
	}
	
	public Command(String commandAndCaption) {
		this(commandAndCaption, commandAndCaption);
	}
	
	public String getCaption() {
		return caption;
	}
	
	public String getCommand() {
		return command;
	}
	
	@Override
	public String toString() {
		return caption;
	}
}
