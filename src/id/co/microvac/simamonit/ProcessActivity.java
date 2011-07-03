package id.co.microvac.simamonit;

import id.co.microvac.simamonit.entity.Node;
import id.co.microvac.simamonit.util.DataFetcher;
import id.co.microvac.simamonit.util.ParsingUtil;
import id.co.microvac.simamonit.util.UiUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ProcessActivity extends Activity {
	
	private String nodeName;
	private String processName;
	private String nodeJson;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle data = this.getIntent().getExtras();
		if(data != null){
			String tmp;
			tmp = data.getString("nodeName");
			if(tmp != null)
				nodeName = tmp;
			tmp = data.getString("processName");
			if(tmp != null)
				processName = tmp;
			tmp = data.getString("nodeJson");
			if(tmp != null)
				nodeJson = tmp;
		}
		
		if(nodeJson == null || nodeName == null || processName == null){
			Log.v("test", "empty fields, returning to node list");
			startActivity(new Intent(this, NodeListActivity.class));
		}
		else{
			setContentView(R.layout.process);
			View entryView = findViewById(R.id.entry);
			Node node = DataFetcher.convertNode(nodeName, nodeJson, 0);
			node.setExpanded(true);
			UiUtil.FillNodeEntryView(node, entryView);
			
			((TextView) findViewById(R.id.process_name)).setText(processName);
			
			ListView processInfoListView = (ListView) findViewById(R.id.process_info_entry);
			List<Map.Entry<String, Object>> processInfos = new ArrayList<Map.Entry<String,Object>>(node.getProcessInfos().get(processName).entrySet());
			ProcessInfoAdapter processInfoAdapter = new ProcessInfoAdapter(this, R.layout.process_info_entry, processInfos);
			processInfoListView.setAdapter(processInfoAdapter);
		}
	}
	
	
	private class ProcessInfoAdapter extends ArrayAdapter<Map.Entry<String, Object>> {

		private List<Map.Entry<String, Object>> infos;

		public ProcessInfoAdapter(Context context, int textViewResourceId,
				List<Map.Entry<String, Object>> infos) {
			super(context, textViewResourceId, infos);
			this.infos = infos;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = li.inflate(R.layout.process_info_entry, null);
			}
			
			try{
				Map.Entry<String, Object> info = infos.get(position);
				String key = info.getKey();
				Object value = info.getValue();
				String valueString = ""+value;
				if(key != null){
					if(value instanceof String){
						if(key.toLowerCase().contains("uptime")){
							valueString = ParsingUtil.parseTimeSpan((String) value);
						}
						if(key.toLowerCase().contains("datacollected")){
							valueString = ParsingUtil.parseDate((String) value);
						}
					}
					if(value instanceof Number){
						if(key.toLowerCase().contains("memory")){
							valueString = ParsingUtil.parseMemory((Number)value, 1);
						}
					}
				}
				((TextView) view.findViewById(R.id.key)).setText(key);
				((TextView) view.findViewById(R.id.value)).setText(valueString);
			}
			catch(Exception e){
				e.printStackTrace();
			}
			return view;
		}
	}
}
