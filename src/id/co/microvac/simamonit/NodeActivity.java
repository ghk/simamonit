package id.co.microvac.simamonit;

import id.co.microvac.simamonit.entity.Command;
import id.co.microvac.simamonit.entity.Node;
import id.co.microvac.simamonit.entity.ParameterizedCommand;
import id.co.microvac.simamonit.ui.FlingGestureListener;
import id.co.microvac.simamonit.util.CommandBuilder;
import id.co.microvac.simamonit.util.DataFetcher;
import id.co.microvac.simamonit.util.ParsingUtil;
import id.co.microvac.simamonit.util.UiUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class NodeActivity extends Activity {
	
	private static final String[] LOG_FILES = new String[]{
		"/var/log/simad/simad.log",
		"/var/log/simad/error/error.log",
		"/var/log/simad/patient/patient",
		"/var/log/simajobd.log",
		"/var/log/simamaintenanced.log",
		"/var/log/monitd.log",
		"/var/log/monitwebd.log",
		"/var/log/mongodb.log",
		"/var/log/sshd.log",
	};
	
	private static final String[] SHELLS = new String[]{
		"bash",
		"simajobd --client",
		"simacli.exe",
		"simamaintenanced --client",
		"monitd --client",
		"monitwebd --client",
		"mongo",
		"psql"
	};
	
	private static final String[][] SERVICE_COMMANDS = new String[][]{
		new String[]{"net start %s", "start"},
		new String[]{"net stop %s", "stop"},
		new String[]{"sc query %s", "query"},
	};
	
	private static final CharSequence[] SERVICES = new CharSequence[]{
		"simad",
		"simajobd",
		"simamaintenanced",
		"monitd",
		"monitwebd",
		"postgresql",
		"mongodb",
		"tvnserver",
		"winvnc4",
		"opensshd",
	};
	
	private static final String[][] CUSTOM_COMMANDS = new String[][]{
		new String[] {"tasklist", "tasklist"},
		new String[] {"ps ax", "ps ax"},
		new String[] {"shutdown /t 1 /r", "reboot"},
		new String[] {"shutdown /t 1 /s", "shutdown"},
		new String[] {"systeminfo", "system info"},
		new String[] {"ipconfig", "ipconfig"},
		new String[] {"df -m", "df -m"},
	};

	private static final String[][] PARAMETERIZED_COMMANDS = new String[][]{
		new String[] {"taskkill \\im %s", "taskkill", "Enter Process name (simad.exe)"},
		new String[] {"kill -9 %s", "cygwin kill", "Enter Cygwin PID"},
	};
	
	private static interface ParameterizedCommandRunner{
		void run(ParameterizedCommand command, boolean isHosted);
	}
	
	private String nodeName;
	private int nodeIndex;
	private Node node;
	
	private GestureDetector gestureDetector;
	private View.OnTouchListener touchListener;
	
	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == SimaMonit.NODE_FETCHED){
				Node node = (Node) msg.obj;
				if(node.getName().equals(nodeName) && node.getIndex() == nodeIndex){
					loadNode(node);
				}
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.node);
		SimaMonit.getInstance().addHandler(handler);
		
		gestureDetector = new GestureDetector(this, new FlingGestureListener() {
			public void onFling(boolean leftToRight) {
				boolean change = false;
				if(node != null){
					int nextIndex = -1;
					int animationOut = android.R.anim.slide_out_right;
					int animationIn = android.R.anim.slide_in_left;
					if(!leftToRight && nodeIndex - 1 >= Node.MIN_INDEX){
						nextIndex = nodeIndex - 1;
						animationOut = R.anim.slide_out_left;
						animationIn = R.anim.slide_in_right;
					}
					else if(leftToRight && nodeIndex + 1 <= Node.MAX_INDEX){
						nextIndex = nodeIndex + 1;
					}
					Log.v("next fetch", "will fetching: "+nextIndex);
					if(nextIndex != -1){
						change = true;
						Intent intent = new Intent(NodeActivity.this, NodeActivity.class);
						Bundle data = new Bundle();
						data.putString("nodeName", nodeName);
						data.putInt("nodeIndex", nextIndex);
						intent.putExtras(data);
						startActivity(intent);
						overridePendingTransition(animationIn, animationOut);
						finish();
					}
				}
				if(!change){
					findViewById(android.R.id.content).startAnimation(AnimationUtils.loadAnimation(NodeActivity.this, R.anim.shake));
				}
			}
		});
		touchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return NodeActivity.this.onTouchEvent(event);
			}
		};
		
		findViewById(R.id.scroll).setOnTouchListener(touchListener);
		Bundle data = this.getIntent().getExtras();
		if(data != null){
			String tmp;
			tmp = data.getString("nodeName");
			if(tmp != null)
				nodeName = tmp;
			nodeIndex = data.getInt("nodeIndex", nodeIndex);
		}
		if(nodeName == null){
			startActivity(new Intent(this, NodeListActivity.class));
			finish();
		}
		else{
			node = SimaMonit.getInstance().findNode(nodeName, nodeIndex);
			if(node != null){
				loadNode(node);
			}
			else{
				Node dummyNode = new Node();
				dummyNode.setName(nodeName);
				dummyNode.setExpanded(true);
				dummyNode.setLoading(true);
				View entryView = findViewById(R.id.entry);
				UiUtil.FillNodeEntryView(dummyNode, entryView);
				new Thread(new Runnable() {
					@Override
					public void run() {
						try{
							Node fetchedNode = DataFetcher.fetchNode(nodeName, nodeIndex);
							SimaMonit.getInstance().sendMessage(SimaMonit.NODE_FETCHED, fetchedNode);
						}
						catch(Exception e){
							UiUtil.showException(e, NodeActivity.this);
						}
					}
				}).start();
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		SimaMonit.getInstance().removeHandler(handler);
		super.onDestroy();
	}
	
	private void loadNode(Node node){
		this.node = node;
		View entryView = findViewById(R.id.entry);
		node.setExpanded(true);
		UiUtil.FillNodeEntryView(node, entryView);
		if(node.isLoaded()){
			configureProcesses();
			configureDisks();
			configureHeader((TextView)findViewById(R.id.process_header), (ListView)findViewById(R.id.process_entry));
			configureHeader((TextView)findViewById(R.id.disk_header), (ListView)findViewById(R.id.disk_entry));
		}
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event))
	        return true;
	    else
	    	return false;
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.node_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(node == null || !node.isLoaded())
			return true;
		
		switch(item.getItemId()){
			case R.id.logs:
				List<Command> logCommands = new ArrayList<Command>();
				for(String file: LOG_FILES){
					logCommands.add(CommandBuilder.tailLogCommand(node, file));
				}
				createCommandsDialog("Pick Log File", logCommands, true).show();
				break;
			case R.id.shells:
				List<Command> shellCommands = new ArrayList<Command>();
				for(String shell: SHELLS){
					shellCommands.add(CommandBuilder.shellCommand(node, shell));
				}
				createCommandsDialog("Pick Remote Shell", shellCommands, false).show();
				break;
			case R.id.commands:
				List<Command> customCommands = new ArrayList<Command>();
				for(String[] customCommand: CUSTOM_COMMANDS){
					customCommands.add(new Command(CommandBuilder.sshCommand(node, customCommand[0]), customCommand[1]));
				}
				createCommandsDialog("Pick Command", customCommands, true).show();
				break;
			case R.id.services:
				List<ParameterizedCommand> serviceCommands = new ArrayList<ParameterizedCommand>();
				for(String[] serviceCommand: SERVICE_COMMANDS){
					serviceCommands.add(new ParameterizedCommand(CommandBuilder.sshCommand(node, serviceCommand[0]), serviceCommand[1]));
				}
				ParameterizedCommandRunner serviceCommandRunner = new ParameterizedCommandRunner() {
					@Override
					public void run(final ParameterizedCommand command, final boolean isHosted) {
				    	AlertDialog.Builder builder = new AlertDialog.Builder(NodeActivity.this);
				    	builder.setTitle("Pick Service");
				    	builder.setItems(SERVICES, new DialogInterface.OnClickListener() {
				    	    public void onClick(DialogInterface dialog, int item) {
				    	    	String service = (String) (SERVICES[item]);
				    	    	command.setParameter(service);
				    	    	runCommand(command, isHosted);
				    	    }
				    	});
				    	builder.create().show();
					}
				};
				createParameterizedCommandsDialog("Pick Command", serviceCommands, true, serviceCommandRunner).show();
				break;
			case R.id.parameterized_commands:
				List<ParameterizedCommand> parameterizedCommands = new ArrayList<ParameterizedCommand>();
				for(String[] paremeterizedCommand: PARAMETERIZED_COMMANDS){
					parameterizedCommands.add(new ParameterizedCommand(CommandBuilder.sshCommand(node, paremeterizedCommand[0]), paremeterizedCommand[1]));
				}
				ParameterizedCommandRunner parameterizedCommandRunner = new ParameterizedCommandRunner() {
					@Override
					public void run(final ParameterizedCommand command, final boolean isHosted) {
						LayoutInflater factory = LayoutInflater.from(NodeActivity.this);
						final View textEntryView = factory.inflate(R.layout.text_entry, null);
						new AlertDialog.Builder(NodeActivity.this)
		                .setTitle("Enter parameter")
		                .setView(textEntryView)
		                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {
		                    	String parameter = ((EditText) textEntryView.findViewById(R.id.text_edit)).getText().toString();
				    	    	command.setParameter(parameter);
				    	    	runCommand(command, isHosted);
		                    }
		                })
		                .create().show();
					}
				};
				createParameterizedCommandsDialog("Pick Command", parameterizedCommands, true, parameterizedCommandRunner).show();
				break;
		}
		return true;
	}
	
    private AlertDialog createCommandsDialog(String title, final List<Command> commands, final boolean isHosted) {
    	final CharSequence[] items = new CharSequence[commands.size()];
    	for(int i = 0; i < commands.size(); i++)
    		items[i] = commands.get(i).getCaption();
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(title);
    	builder.setItems(items, new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int item) {
    	    	runCommand(commands.get(item), isHosted);
    	    }
    	});
    	AlertDialog alert = builder.create();
        return alert;
    }
    
    private AlertDialog createParameterizedCommandsDialog(String title, final List<ParameterizedCommand> commands, final boolean isHosted, final ParameterizedCommandRunner runner) {
    	final CharSequence[] items = new CharSequence[commands.size()];
    	for(int i = 0; i < commands.size(); i++)
    		items[i] = commands.get(i).getCaption();
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(title);
    	builder.setItems(items, new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int item) {
    	    	runner.run(commands.get(item), isHosted);
    	    }
    	});
    	AlertDialog alert = builder.create();
        return alert;
    }
    
    private void runCommand(Command command, boolean isHosted){
		if(isHosted){
			Intent intent = new Intent(NodeActivity.this, HostedCommandActivity.class);
			Bundle data = new Bundle();
			data.putString("command", command.getCommand());
			intent.putExtras(data);
			startActivity(intent);
		}
		else{
			Intent intent = new Intent(
					Intent.ACTION_VIEW,
					Uri.parse("exe:#"+command.getCommand()+"#"));
			startActivity(intent);
		}
    }
    
	private void configureHeader(TextView header, final ListView content){
		header.setOnTouchListener(touchListener);
		header.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if(content.getVisibility() == View.GONE)
					content.setVisibility(View.VISIBLE);
				else
					content.setVisibility(View.GONE);
			}
		});
	}
	
	private void configureProcesses(){
		ListView processListView = (ListView) findViewById(R.id.process_entry);
		List<Map.Entry<String, Map<String, Object>>> processes = new ArrayList<Map.Entry<String, Map<String,Object>>>();
		final ProcessAdapter processAdapter = new ProcessAdapter(this, R.layout.process_entry, processes);
		processListView.setAdapter(processAdapter);
		for(Map.Entry<String, Map<String, Object>> value: node.getProcessInfos().entrySet())
			processAdapter.add(value);
		
		int totalHeight = 0;
        for (int i = 0; i < processAdapter.getCount(); i++) {
            View listItem = processAdapter.getView(i, null, processListView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = processListView.getLayoutParams();
        params.height = totalHeight + (processListView.getDividerHeight() * (processAdapter.getCount() - 1));
        processListView.setLayoutParams(params);
        
		processListView
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> adapter,
							View view, int position, long arg3) {
						final Map.Entry<String,Map<String, Object>> process = processAdapter.getItem(position);
						Intent intent = new Intent(NodeActivity.this, ProcessActivity.class);
						Bundle data = new Bundle();
						data.putString("nodeName", nodeName);
						data.putString("nodeJson", node.getJson());
						data.putString("processName", process.getKey());
						intent.putExtras(data);
						startActivity(intent);
					}
				});
		processListView.setOnTouchListener(touchListener);
	}
	
	private void configureDisks(){
		ListView disksListView = (ListView) findViewById(R.id.disk_entry);
		List<Map<String, Object>> disks = new ArrayList<Map<String,Object>>();
		DiskAdapter diskAdapter = new DiskAdapter(this, R.layout.disk_entry, disks);
		disksListView.setAdapter(diskAdapter);
		for(Map<String, Object> value: node.getSystemInfo().getDisks().values())
			diskAdapter.add(value);
		
		int totalHeight = 0;
        for (int i = 0; i < diskAdapter.getCount(); i++) {
            View listItem = diskAdapter.getView(i, null, disksListView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = disksListView.getLayoutParams();
        params.height = totalHeight + (disksListView.getDividerHeight() * (diskAdapter.getCount() - 1));
        disksListView.setLayoutParams(params);
		disksListView.setOnTouchListener(touchListener);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("nodeName", nodeName);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		String tmp;
		tmp = savedInstanceState.getString("nodeName");
		if(tmp != null)
			nodeName = tmp;
		nodeIndex = savedInstanceState.getInt("nodeIndex", nodeIndex);
	}
	
	private class ProcessAdapter extends ArrayAdapter<Map.Entry<String,Map<String, Object>>> {

		private List<Map.Entry<String, Map<String,Object>>> processes;

		public ProcessAdapter(Context context, int textViewResourceId,
				List<Map.Entry<String, Map<String,Object>>> processes) {
			super(context, textViewResourceId, processes);
			this.processes = processes;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = li.inflate(R.layout.process_entry, null);
			}
			
			try{
				Map<String, Object> process = processes.get(position).getValue();
				TextView tv = ((TextView) view.findViewById(R.id.name));
				String name = process.get("Name") == null ? "-" : process.get("Name").toString();
				tv.setText(name);
				((TextView) view.findViewById(R.id.status)).setText(""+process.get("MonitoringStatus"));
				((TextView) view.findViewById(R.id.memory))
				.setText(ParsingUtil.parseMemory((Number)process.get("TotalMemory"), 1));
				((TextView) view.findViewById(R.id.uptime)).setText(ParsingUtil.parseTimeSpan((String)process.get("Uptime")));
			}
			catch(Exception e){
				e.printStackTrace();
			}
			return view;
		}
	}
	
	private class DiskAdapter extends ArrayAdapter<Map<String, Object>> {

		private List<Map<String,Object>> disks;

		public DiskAdapter(Context context, int textViewResourceId,
				List<Map<String, Object>> disks) {
			super(context, textViewResourceId, disks);
			this.disks = disks;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = li.inflate(R.layout.disk_entry, null);
			}
			
			try{
				Map<String, Object> disk = disks.get(position);
				((TextView) view.findViewById(R.id.name)).setText(disk.get("Name")+"");
				double freeSpace = ((Number)disk.get("FreeSpace")).doubleValue();
				double size = ((Number)disk.get("Size")).doubleValue();
				((TextView) view.findViewById(R.id.free)).setText(
						ParsingUtil.parseMemory(freeSpace, 1)
				);
				((TextView) view.findViewById(R.id.size)).setText(
						ParsingUtil.parseMemory(size, 1)
				);
				((TextView) view.findViewById(R.id.used)).setText(
						ParsingUtil.parseMemory(size-freeSpace, 1)
				);
			}
			catch(Exception e){
				e.printStackTrace();
			}
			return view;
		}
	}
}
