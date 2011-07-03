package id.co.microvac.simamonit;

import id.co.microvac.simamonit.entity.Node;
import id.co.microvac.simamonit.util.DataFetcher;
import id.co.microvac.simamonit.util.EtcHost;
import id.co.microvac.simamonit.util.UiUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class NodeListActivity extends Activity {
	
	private ListView nodeListView;
	private NodeAdapter nodeAdapter;
	
	private List<Node> nodes;
	
	private Handler handler = new Handler(){
		public void handleMessage(Message msg) {
			switch(msg.what){
				case SimaMonit.NODE_NAMES_FETCHED:
					nodeAdapter.notifyDataSetChanged();
					@SuppressWarnings("unchecked")
					HashSet<String> nodeNames = (HashSet<String>) msg.obj;
					fetchNodes(nodeNames);
					break;
				case SimaMonit.NODE_FETCHED:
					Node node = (Node) msg.obj;
					if(node.getIndex() == 0)
						nodeAdapter.notifyDataSetChanged();
					break;
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SimaMonit.getInstance().addHandler(handler);
		Log.v("node", "create");
		setContentView(R.layout.node_list);
		nodes = SimaMonit.getInstance().getNodes();
		nodeListView = (ListView) findViewById(R.id.nodeEntry);
	}
	
	@Override
	protected void onDestroy() {
		SimaMonit.getInstance().removeHandler(handler);
		super.onDestroy();
	}
	
	@Override
	protected void onResume() {
		if(nodeAdapter == null){
			nodeAdapter = new NodeAdapter(this, R.layout.node_entry, nodes);
		}
		
		if(nodeListView.getAdapter() == null)
		{
			nodeListView.setAdapter(nodeAdapter);
			nodeListView
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> adapter,
							View view, int position, long arg3) {
						final Node node = nodeAdapter.getItem(position);
						Intent intent = new Intent(NodeListActivity.this, NodeActivity.class);
						Bundle data = new Bundle();
						data.putString("nodeName", node.getName());
						data.putInt("nodeIndex", node.getIndex());
						intent.putExtras(data);
						startActivityForResult(intent, 0);
					}
				});
		}
		
		
		boolean willFetch = false;
		synchronized (SimaMonit.getInstance()) {
			if(!SimaMonit.getInstance().isNodeFetched()){
				SimaMonit.getInstance().setNodeFetched(true);
				willFetch = true;
			}
		}
		if(willFetch){
			Log.v("fetch", "first run!");
			fetchNodeNames();
		}
		super.onResume();
	}
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.node_list_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case R.id.refresh:
				fetchNodeNames();
				break;
			case R.id.update_host:
				updateHost();
				break;
		}
		return true;
	}
	
	private void updateHost(){
		try{
			EtcHost etcHost = new EtcHost();
			for(Node node: nodes){
				etcHost.Update(node.getName(), node.getSystemInfo().getIpAddress());
			}
			FileOutputStream fos = openFileOutput("hosttemp", Context.MODE_WORLD_WRITEABLE);
			fos.write(1);
			fos.close();
			File file = getFileStreamPath("hosttemp");
			etcHost.Save(file.getAbsolutePath());
		}
		catch(Exception e){
			e.printStackTrace();
			showException(e);
		}
	}
	
	private void fetchNodeNames() {
		findViewById(R.id.nodeEntry).setVisibility(View.GONE);
		findViewById(R.id.feching_node_names).setVisibility(View.VISIBLE);
		new Thread(new Runnable() {
			public void run() {
				Set<String> nodeNames = new HashSet<String>();
				try {
					nodeNames = DataFetcher.fetchNodeNames();
				} catch (Exception e) {
					e.printStackTrace();
					showException(e);
				} finally {
					runOnUiThread(new Runnable() {
						public void run() {
							findViewById(R.id.feching_node_names).setVisibility(View.GONE);
							findViewById(R.id.nodeEntry).setVisibility(View.VISIBLE);
						};
					});
					SimaMonit.getInstance().sendMessage(SimaMonit.NODE_NAMES_FETCHED, nodeNames);
				} 				
			}
		}).start();
	}
	
	private void fetchNodes(final Set<String> nodeNames) {
		new Thread(new Runnable() {
			public void run() {
				for (final String nodeName : nodeNames) {
					Node dummyNode = null;
					for(int i = nodeAdapter.getCount() - 1; i >= 0; i--){
						Node temp = nodeAdapter.getItem(i);
						if(temp.getName().equals(nodeName)){
							dummyNode = temp;
							break;
						}
					}
					
					if(dummyNode.isLoaded())
						continue;
					
					dummyNode.setLoading(true);
					SimaMonit.getInstance().sendMessage(SimaMonit.NODE_FETCHED, dummyNode);
					
					try {
						final Node node = DataFetcher.fetchNode(nodeName, 0);
						SimaMonit.getInstance().sendMessage(SimaMonit.NODE_FETCHED, node);
					} catch (final Exception e) {
						dummyNode.setError("Error: " + e.getMessage());
						SimaMonit.getInstance().sendMessage(SimaMonit.NODE_FETCHED, dummyNode);
						e.printStackTrace();
						showException(e);
					}
				}
			}
		}).start();
	}
	private void showException(final Exception e) {
		UiUtil.showException(e, this);
	}

	private class NodeAdapter extends ArrayAdapter<Node> {

		private List<Node> nodes;

		public NodeAdapter(Context context, int textViewResourceId,
				List<Node> nodes) {
			super(context, textViewResourceId, nodes);
			this.nodes = nodes;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = li.inflate(R.layout.node_entry, null);
			}
			
			Node node = nodes.get(position);
			UiUtil.FillNodeEntryView(node, view);
			return view;
		}
	}
}