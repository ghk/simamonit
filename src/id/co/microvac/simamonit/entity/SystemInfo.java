package id.co.microvac.simamonit.entity;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

public class SystemInfo {
	
	@JsonProperty("AvailableMemory")
	private double availableMemory;
	
	@JsonProperty("CpuUsage")
	private double cpuUsage;
	
	@JsonProperty("IpAddress")
	private String ipAddress;
	
	@JsonProperty("Disks")
	private Map<String, Map<String, Object>> disks;
	
	public double getAvailableMemory() {
		return availableMemory;
	}
	public void setAvailableMemory(double availableMemory) {
		this.availableMemory = availableMemory;
	}
	public double getCpuUsage() {
		return cpuUsage;
	}
	public void setCpuUsage(double cpuUsage) {
		this.cpuUsage = cpuUsage;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public Map<String, Map<String, Object>> getDisks() {
		return disks;
	}
	public void setDisks(Map<String, Map<String, Object>> disks) {
		this.disks = disks;
	}
	
}
