package id.co.microvac.simamonit.util;

import id.co.microvac.simamonit.R;
import id.co.microvac.simamonit.entity.Node;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class UiUtil {
	public static void showException(final Exception e, final Activity context) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				AlertDialog alertDialog = new AlertDialog.Builder(context)
						.create();
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				alertDialog.setMessage(sw.toString());
				alertDialog.show();
			}
		};
		context.runOnUiThread(runnable);
	}

	public static void FillNodeEntryView(Node node, View view) {
		TextView nt = (TextView) view.findViewById(R.id.name);
		nt.setText(node.getName());
		TextView dateT = (TextView) view.findViewById(R.id.date);
		TextView statusT = (TextView) view.findViewById(R.id.status);
		TextView ipT = (TextView) view.findViewById(R.id.ip);
		TextView cpuT = (TextView) view.findViewById(R.id.cpu_usage);
		TextView memT = (TextView) view.findViewById(R.id.avail_memory);
		ImageView icon = (ImageView) view.findViewById(R.id.icon);
		int iconId = 0;
		if(node.isLoaded() && "ok".equals(node.getStatus())){
			statusT.setVisibility(View.GONE);
		}
		else{
			statusT.setVisibility(View.VISIBLE);
		}
		if (node.isLoaded()) {
			statusT.setText(node.getStatus());
			dateT.setText(ParsingUtil.parseDate(node.getDataCollected()));
			ipT.setText(node.getSystemInfo().getIpAddress());
			cpuT.setText("CPU: " + node.getSystemInfo().getCpuUsage()+"%");
			memT.setText("Free: "
					+ ParsingUtil.parseMemory(node.getSystemInfo().getAvailableMemory(), 1024*1024));
			iconId = "ok".equals(node.getStatus()) ? R.drawable.ok
					: R.drawable.problem;
		} else {
			dateT.setText("");
			ipT.setText("");
			cpuT.setText("");
			memT.setText("");
			if (node.getError() == null) {
				if (node.isLoading()) {
					statusT.setText("Loading...");
					iconId = R.drawable.loading;
				} else {
					statusT.setText("Queued");
					iconId = 0;
				}
			} else {
				statusT.setText(node.getError());
				iconId = R.drawable.problem;
			}
		}
		icon.setImageResource(iconId);
		/*
		view.findViewById(R.id.content).setVisibility(
				node.isExpanded() ? View.VISIBLE : View.GONE);
				*/
	}
}
