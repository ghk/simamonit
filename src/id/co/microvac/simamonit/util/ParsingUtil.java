package id.co.microvac.simamonit.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ParsingUtil {
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("E, dd MMM yyyy hh:mm:ss z");
	private static final SimpleDateFormat outFormat = new SimpleDateFormat("dd MMM yyyy");
	
	public static String parseMemory(Number number, int times){
		double val = number.doubleValue();
		val *= times;
		if(val < 1024){
			return String.format("%1$.0f bytes", val);
		}
		else if(val < 1024 * 1024){
			return String.format("%1$.2f KiB", val / 1024);
		}
		else if(val < 1024 * 1024 * 1024){
			return String.format("%1$.2f MiB", val / (1024 * 1024));
		}
		else {
			return String.format("%1$.2f GiB", val / (1024 * 1024 * 1024));
		}
	}
	
	public static String parseDate(String source){
		try{
			Date date = dateFormat.parse(source);
			String fallbackResult = outFormat.format(date);
			Date now = new Date();
			long diff = (now.getTime() - date.getTime())/1000;
			if(diff < 0){
				return fallbackResult;
			} else if (diff < 60){
				return String.format("%d seconds ago", diff);
			} else if (diff < 3600){
				long minutes = diff / 60;
				long seconds = diff % 60;
				return String.format("%d minutes %d seconds ago", minutes, seconds);
			} else if (diff < 3600 * 24){
				diff /= 60;
				long hours = diff / 60;
				long minutes = diff % 60;
				return String.format("%d hours %d minutes ago", hours, minutes);
			} else if (diff < 3600 * 7 * 30){
				diff /= 3600;
				long days = diff / 24;
				long hours = diff % 24;
				return String.format("%d days %d hours ago", days, hours);
			}
			return fallbackResult;
		}catch(ParseException pe){
			return source;
		}
	}
	
	public static String parseTimeSpan(String source) {
		try{
			int dotIndex = source.indexOf(".");
			int colonIndex = source.indexOf(":");
			if(dotIndex != -1 && dotIndex < colonIndex ){
				int days = Integer.parseInt(source.substring(0, dotIndex));
				int hours = Integer.parseInt(source.substring(dotIndex+1, colonIndex));
				return days+" days "+hours+" hours";
			}
			else{
				int nextColonIndex = source.indexOf(":", colonIndex+1);
				int hours = Integer.parseInt(source.substring(0, colonIndex));
				int minutes = Integer.parseInt(source.substring(colonIndex+1, nextColonIndex));
				return hours+" hours " + minutes + " minutes";
			}
		}
		catch(Exception e){
			e.printStackTrace();
			return "-";
		}
	}
}
