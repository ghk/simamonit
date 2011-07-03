package id.co.microvac.simamonit.entity;

public class ParameterizedCommand extends Command{
	
	private String formatText;
	private String parameter;
	
	public ParameterizedCommand(String formatText, String caption) {
		super(null, caption);
		this.formatText = formatText;
	}
	
	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
	
	@Override
	public String getCommand() {
		return String.format(formatText, parameter);
	}
}
