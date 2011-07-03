package id.co.microvac.simamonit.util;

import id.co.microvac.simamonit.entity.Node;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import android.util.Log;

public class DataFetcher {

	private static String BASE_URL = "http://sima.microvac.co.id/monit";
	private static ObjectMapper mapper = new ObjectMapper();
	static{
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	public static Node fetchNode(String nodeName, int index) throws Exception {
		String fileName = index == 0 ? "latest" : String.valueOf(index);
		String nodeJson =fetch(BASE_URL+"/reports/"+nodeName+"/"+fileName+".json");
		return convertNode(nodeName, nodeJson, index);
	}
	
	public static Node convertNode(String nodeName, String nodeJson, int index){
		try{
			Node node = mapper.readValue(nodeJson, Node.class);
			node.setName(nodeName);
			node.setLoaded(true);
			node.setLoading(false);
			node.setJson(nodeJson);
			node.setIndex(index);
			Log.i("nodes", nodeJson);
			return node;
		}
		catch(Exception e){
			if(e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException(e);
		}
	}

	public static Set<String> fetchNodeNames() throws Exception {
		try{
			String nodes =fetch(BASE_URL+"/nodes.json");
			String[] strings = mapper.readValue(nodes, String[].class);
			Log.i("nodes", strings.toString());
			HashSet<String> results = new HashSet<String>();
			for(String nodeName: strings)
				results.add(nodeName);
			return results;
		}
		catch(Exception e){
			if(e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException(e);
		}
	}

	public static String fetch(String url) throws Exception {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		URI uri = new URI(url);
		HttpGet method = new HttpGet(uri);
		HttpResponse response = httpClient.execute(method);
		if(response.getStatusLine().getStatusCode() != 200)
			throw new RuntimeException("HTTP Error: "+response.getStatusLine().getStatusCode()+ " on "+url);
		InputStream data = response.getEntity().getContent();
		try {
			final char[] buffer = new char[0x10000];
			StringBuilder out = new StringBuilder();
			Reader in = new InputStreamReader(data, "UTF-8");
			int read;
			do {
				read = in.read(buffer, 0, buffer.length);
				if (read > 0) {
					out.append(buffer, 0, read);
				}
			} while (read >= 0);
			return out.toString();

		} finally {
			try {
				data.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
