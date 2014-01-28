package com.kaplanrobotics.wifidirect.server;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;


import java.util.ArrayList;
import java.util.List;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class WifiDirectServer extends Activity {
	
	// WiFi Mac Address Samsumg S3 -  00:37:6D:F9:3D:9F  (currently no Jelly Bean! (no API 18))
	// Galaxy Nexus (Verizon) - 4C:BC:A5:1C:5D:19
	// Nexus 7 - 30:85:A9:4B:66:EF - but showing 32:85:A9:4B:66:EF
	public static final String NEXUS_7 = "32:85:a9:4b:66:ef";
	// Case sensitive - Lower case

	WifiP2pManager wifiManager;
	Channel wifiChannel;
	BroadcastReceiver receiver;
	IntentFilter intentFilter;
	
	// The other end (client)
	WifiP2pDevice otherDevice;
	
	private volatile boolean serverRunning = false;
	
	private volatile boolean connected = false;
	
	ServerAsyncTask serverAsyncTask;
	
	private class WifiDirectServerBroadcastReceiver extends BroadcastReceiver{

		private WifiP2pManager wifiManager;
		private Channel channel;
		private WifiDirectServer activity;
		private String otherDeviceID;

		
		public WifiDirectServerBroadcastReceiver(WifiP2pManager manager, Channel channel, WifiDirectServer activity, String otherDeviceID){			
			this.wifiManager = manager;
			this.channel = channel;
			this.activity = activity;
			this.otherDeviceID = otherDeviceID;
			
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			String action = intent.getAction();

			// Broadcast when the state of the device's Wi-Fi connection changes.
			if(action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)){
				Log.e("Server B-Recv'r: onPeersAvailable()", "WIFI P2P STATE CHANGED");
			}
			// Broadcast when you call discoverPeers(). You usually want to call requestPeers() 
			// to get an updated list of peers if you handle this intent in your application.
			else if(action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)){
				Log.e("Server B-Recv'r: onPeersAvailable()", "PEERS CHANGED");

				if(wifiManager != null){
					wifiManager.requestPeers(channel, new PeerListListener(){

						@Override
						public void onPeersAvailable(WifiP2pDeviceList peerList) {
														
							if(!connected){

								List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>(peerList.getDeviceList());
								if(peers.size() != 0){
									for(WifiP2pDevice wpd : peers)
										Log.e("Server B-Recv'r: onPeersAvailable()", "Found peer "+wpd.deviceAddress+".  Look for "+otherDeviceID);
									// Try and find our other device
									otherDevice = peerList.get(otherDeviceID);
									
									// if we've found the listed peer MAC...
									if(otherDevice != null){
										Log.e("Server B-Recv'r: onPeersAvailable()", "Found listed connectable device - try and connect");
																				
										// connect to it.
										WifiP2pConfig config = new WifiP2pConfig();										
										config.deviceAddress = otherDevice.deviceAddress;										
										
										// Connect needs to be done on both devices???
										wifiManager.connect(channel, config, new ActionListener(){
		
											@Override
											public void onFailure(int reason) {
												Log.e("Server B-Recv'r: onPeersAvailable()", "Connect Fail");										
											}
		
											@Override
											public void onSuccess() {
												connected = true;
												Log.e("Server B-Recv'r: onPeersAvailable()", "Connnect Sucess");
												serverAsyncTask = new ServerAsyncTask();
												serverAsyncTask.execute();
											}
											
										});
										Log.e("Server B-Recv'r: onPeersAvailable()", "connect attempt made");
									}
								}
								Log.e("Server B-Recv'r: onPeersAvailable()","No peers found - device list empty.  (but we're not connected)");
							}
						}
					});
				}				
			}
			// Broadcast when Wi-Fi P2P is enabled or disabled on the device.
			else if(action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)){
				Log.e("Server B-Recv'r: onPeersAvailable()", "CONNECTION CHANGED");
			}
			// Broadcast when a device's details have changed, such as the device's name.
			else if(action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)){
				Log.e("Server B-Recv'r: onPeersAvailable()", "THIS DEVICE CHANGED");

			}

		}
		
		String getStatusString(WifiP2pDevice device){
			String statusString = "";
			if(device == null)
				statusString = "NULL";
			else {
				if(device.status == WifiP2pDevice.CONNECTED) statusString = "Connected";
				if(device.status == WifiP2pDevice.INVITED) statusString = "Invited";
				if(device.status == WifiP2pDevice.FAILED) statusString = "Failed";
				if(device.status == WifiP2pDevice.AVAILABLE) statusString = "Available";
				if(device.status == WifiP2pDevice.UNAVAILABLE) statusString = "Unavailable";

			}
			return statusString;
		}
		
	}
	
//	void setupServerSocket(){
//		
//	}
	
	
	Button serverState;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		wifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		// Register the application with the Wi-Fi P2P framework
		wifiChannel = wifiManager.initialize(this, getMainLooper(), null);
		// allow the broadcast receiver to manipulate the device's Wi-Fi state and 
		// notify the activity of events (and update it accordingly)
		receiver = new WifiDirectServerBroadcastReceiver(wifiManager, wifiChannel, this, NEXUS_7);
		
		intentFilter = new IntentFilter();
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		
		serverState = (Button) findViewById(R.id.button1);
		serverState.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if(serverRunning){
					stopServer();
				}
				else{
					startServer();
				}
					
			}});
	}
	
	// Register the receiver on resume
	@Override
	protected void onResume(){
		startServer();
		super.onResume();

	}
	
	// Unregister on pause
	@Override
	protected void onPause(){
		stopServer();
		super.onPause();
	}
	
	private void startServer(){
		if(serverRunning)
			return;
		
		registerReceiver(receiver, intentFilter);
		Log.e("WiFi Direct Server - onResume()", "Discover Peers.");
		wifiManager.discoverPeers(wifiChannel, new WifiP2pManager.ActionListener(){

			@Override
			public void onFailure(int reason) {
				String failReason = "Unknown";
				if(reason == 0) failReason = "Internal Error";
				if(reason == 1) failReason = "P2P Unsupported";
				if(reason == 2) failReason = "WiFi Direct Busy";
                Toast.makeText(WifiDirectServer.this, "Discovery Failed : " + failReason, Toast.LENGTH_SHORT).show();				
			}

			@Override
			public void onSuccess() {
                Toast.makeText(WifiDirectServer.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
			}});
		serverRunning = true;
		serverState.setText("Stop Server");

	}
	private void stopServer(){
		if(!serverRunning)
			return;
		
		if(serverAsyncTask != null)
			serverAsyncTask.close();
		
		if(wifiManager != null)
			 wifiManager.cancelConnect(wifiChannel, null);
		
		unregisterReceiver(receiver);
		
			
		serverRunning = false;
		serverState.setText("Start Server");
	}


}
