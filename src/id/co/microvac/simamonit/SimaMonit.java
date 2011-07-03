package id.co.microvac.simamonit;

import id.co.microvac.simamonit.entity.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Application;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SimaMonit extends Application{
	
	public static final int NODE_NAMES_FETCHED = 1;
	public static final int NODE_FETCHED = 2;
	
	private static SimaMonit instance;
	
	private List<Node> nodes;
	private Set<Handler> handlers;
	private boolean nodeFetched;
	
	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		nodes = new ArrayList<Node>();
		handlers = new HashSet<Handler>();
	}
	
	public static SimaMonit getInstance() {
		return instance;
	}
	
	public List<Node> getNodes() {
		return nodes;
	}
	
	public boolean isNodeFetched() {
		return nodeFetched;
	}
	
	public void setNodeFetched(boolean nodeFetched) {
		this.nodeFetched = nodeFetched;
	}
	
	public synchronized void addHandler(Handler handler){
		handlers.add(handler);
	}
	
	public synchronized void removeHandler(Handler handler) {
		handlers.remove(handler);
	}
	
	public synchronized void sendMessage(int what, Object obj) {
		switch(what){
			case NODE_NAMES_FETCHED:
				@SuppressWarnings("unchecked")
				HashSet<String> nodeNames = (HashSet<String>) obj;
				for(int i = nodes.size() - 1; i >= 0; i--){
					Node temp = nodes.get(i);
					if(nodeNames.contains(temp.getName())){
						nodes.remove(i);
					}
				}
				for(String nodeName: nodeNames){
					Log.v("node name fetched", "add nodes: "+nodeName);
					Node node = new Node();
					node.setName(nodeName);
					nodes.add(node);
				}
				break;
			case NODE_FETCHED:
				Node node = (Node) obj;
				Node dummyNode = null;
				int i = 0;
				for(i = nodes.size() - 1; i >= 0; i--){
					Node temp = nodes.get(i);
					if(temp.getName().equals(node.getName())){
						dummyNode = temp;
						break;
					}
				}
				if(dummyNode != null){
					if(node.getIndex() == 0){
						node.setPrevious(dummyNode.getPrevious());
						nodes.remove(dummyNode);
						nodes.add(i, node);
					}
					else{
						i = 0;
						Node currentNode = dummyNode;
						while(i < node.getIndex() - 1){
							if(currentNode.getPrevious() == null){
								Node prev = new Node();
								node.setName(currentNode.getName());
								currentNode.setPrevious(prev);
							}
							currentNode = currentNode.getPrevious();
							i++;
						}
						if(currentNode.getPrevious() != null)
							node.setPrevious(currentNode.getPrevious().getPrevious());
						currentNode.setPrevious(node);
					}
				}
				else{
					if(node.getIndex() == 0){
						nodes.add(node);
					}
				}
				break;
		}
		for(Handler handler: handlers)
			handler.sendMessage(Message.obtain(handler, what, obj));
	}
	
	public Node findNode(String nodeName, int nodeIndex){
		Log.v("node", "finding node index: "+nodeIndex+" ,name: "+nodeName);
		int i = 0;
		for(i = nodes.size() - 1; i >= 0; i--){
			Node temp = nodes.get(i);
			if(temp.getName().equals(nodeName)){
				int index = 0;
				while(index < nodeIndex && temp.getPrevious() != null){
					temp =  temp.getPrevious();
					index++;
				}
				return index == nodeIndex ? temp : null;
			}
		}
		return null;
	}
	
}
