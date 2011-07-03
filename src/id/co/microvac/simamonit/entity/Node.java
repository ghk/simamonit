package id.co.microvac.simamonit.entity;

import java.util.Map;
import org.codehaus.jackson.annotate.JsonProperty;

public class Node {
	public static final int MAX_INDEX = 9;
	public static final int MIN_INDEX = 0;
	
	
	private String name;
	private String json;
	private String error;
	
	private int index;
	private Node previous;
	
	@JsonProperty("Status")
	private String status;
	
	@JsonProperty("DataCollected")
	private String dataCollected;
	
	@JsonProperty("ProcessInfos")
	private Map<String, Map<String, Object>> processInfos;
	
	@JsonProperty("SystemInfo")
	private SystemInfo systemInfo;

	private boolean loaded;
	private boolean loading;
	private boolean expanded;


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public int getIndex() {
		return index;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public Node getPrevious() {
		return previous;
	}
	
	public void setPrevious(Node previous) {
		this.previous = previous;
	}
	
	public boolean isLoaded() {
		return loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public boolean isLoading() {
		return loading;
	}

	public void setLoading(boolean loading) {
		this.loading = loading;
	}

	public boolean isExpanded() {
		return expanded;
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
	
	public String getJson() {
		return json;
	}
	
	public void setJson(String json) {
		this.json = json;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String Status) {
		this.status = Status;
	}

	public String getDataCollected() {
		return dataCollected;
	}

	public void setDataCollected(String DataCollected) {
		this.dataCollected = DataCollected;
	}

	public Map<String, Map<String, Object>> getProcessInfos() {
		return processInfos;
	}

	public void setProcessInfos(
			Map<String, Map<String, Object>> processInfos) {
		this.processInfos = processInfos;
	}

	public SystemInfo getSystemInfo() {
		return systemInfo;
	}

	public void setSystemInfo(SystemInfo systemInfo) {
		this.systemInfo = systemInfo;
	}

}
