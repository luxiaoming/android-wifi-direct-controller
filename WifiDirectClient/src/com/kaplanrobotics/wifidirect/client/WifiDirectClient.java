package com.kaplanrobotics.wifidirect.client;


import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import android.net.Uri;
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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class WifiDirectClient extends Activity {
	
	// WiFi Mac Address Samsumg S3 -  00:37:6D:F9:3D:9F  (currently no Jelly Bean! (no API 18))
	// Galaxy Nexus (Verizon) - 4C:BC:A5:1C:5D:19
	// Nexus 7 - 30:85:A9:4B:66:EF - but showing 32:85:A9:4B:66:EF
	public static final String GALAXY_NEXUS = "32:85:a9:4b:66:ef";  // The server
	// Case sensitive - Lower case

	WifiP2pManager wifiManager;
	Channel wifiChannel;
	BroadcastReceiver receiver;
	IntentFilter intentFilter;
	
	WifiP2pDevice otherDevice;

	
	Button sendDataButton;
	EditText dataText;
	
	volatile boolean connected = false;
	
	private class WifiDirectClientBroadcastReceiver extends BroadcastReceiver{

		private WifiP2pManager wifiManager;
		private Channel channel;
		private WifiDirectClient activity;
		private String otherDeviceID;

		public WifiDirectClientBroadcastReceiver(WifiP2pManager manager, Channel channel, WifiDirectClient activity, String otherDeviceID){			
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
							// TODO Auto-generated method stub
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
									
									// Connect needs to be done on both devices
									wifiManager.connect(channel, config, new ActionListener(){
	
										@Override
										public void onFailure(int reason) {
											Log.e("Server B-Recv'r: onPeersAvailable()", "Connect Fail");										
										}
	
										@Override
										public void onSuccess() {
											connected = true;
											Log.e("Server B-Recv'r: onPeersAvailable()", "Connnect Sucess");
										}
										
									});
									Log.e("Server B-Recv'r: onPeersAvailable()", "connect attempt made");
								}
							}
						}});

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
	}
	
	Button restartWifiDirectButton;
	
	volatile boolean clientRunning = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi_direct_client);
		
		wifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		// Register the application with the Wi-Fi P2P framework
		wifiChannel = wifiManager.initialize(this, getMainLooper(), null);
		// allow the broadcast receiver to manipulate the device's Wi-Fi state and 
		// notify the activity of events (and update it accordingly)
		receiver = new WifiDirectClientBroadcastReceiver(wifiManager, wifiChannel, this, GALAXY_NEXUS);
		
		intentFilter = new IntentFilter();
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		
		restartWifiDirectButton = (Button) findViewById(R.id.button2);
		restartWifiDirectButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if(clientRunning){
					stopClient();
				}
				else{
					startClient();
				}
					
			}});
		
		
		dataText = (EditText) findViewById(R.id.editText1); 
		sendDataButton = (Button) findViewById(R.id.button1);
		sendDataButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {				
				sendData(Integer.parseInt(dataText.getText().toString()));
			}});
	}
	
	// Register the receiver on resume
	@Override
	protected void onResume(){
		super.onResume();
		startClient();

	}
	
	protected void startClient(){
		if(clientRunning)
			return;
		
		registerReceiver(receiver, intentFilter);
		Log.e("WiFi Direct Client startClient()", "Discover Peers.");
		wifiManager.discoverPeers(wifiChannel, new WifiP2pManager.ActionListener(){

			@Override
			public void onFailure(int reason) {
				String failReason = "Unknown";
				if(reason == 0) failReason = "Internal Error";
				if(reason == 1) failReason = "P2P Unsupported";
				if(reason == 2) failReason = "WiFi Direct Busy";
                Toast.makeText(WifiDirectClient.this, "Discovery Failed : " + failReason, Toast.LENGTH_SHORT).show();				
			}

			@Override
			public void onSuccess() {
                Toast.makeText(WifiDirectClient.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
			}});
		restartWifiDirectButton.setText("Stop WifiDirect");
		clientRunning = true;
	}
	
	protected void stopClient(){
		if(!clientRunning)
			return;
		unregisterReceiver(receiver);
		restartWifiDirectButton.setText("Start WifiDirect");
		clientRunning = false;
	}
	
	// Unregister on pause
	@Override
	protected void onPause(){
		super.onPause();
		unregisterReceiver(receiver);
	}

	void sendData(int dataValue){
		
		if(!connected) return;
		
		Context context = this.getApplicationContext();
		String host;
		int port;
		int len;
		Socket socket = new Socket();
		byte buf[]  = new byte[1024];
		
		try {
		    /**
		     * Create a client socket with the host,
		     * port, and timeout information.
		     */
		    socket.bind(null);
		    //socket.connect((new InetSocketAddress(host, port)), 500);
		    socket.connect((new InetSocketAddress( 8988)), 500);
		    
		    /**
		     * Create a byte stream from a JPEG file and pipe it to the output stream
		     * of the socket. This data will be retrieved by the server device.
		     */
		    //ContentResolver cr = context.getContentResolver();
		    //InputStream inputStream = null;
		    //inputStream = cr.openInputStream(Uri.parse("path/to/picture.jpg"));
		    OutputStream outputStream = socket.getOutputStream();		    
		    DataOutputStream dataOutputStream = new DataOutputStream(outputStream);		    
		    dataOutputStream.writeInt(dataValue);
		    dataOutputStream.close();
		    outputStream.close();

		    //inputStream.close();
		} catch (FileNotFoundException e) {
		    //catch logic
		} catch (IOException e) {
		    //catch logic
		}

		/**
		 * Clean up any open sockets when done
		 * transferring or if an exception occurred.
		 */
		finally {
		    if (socket != null) {
		        if (socket.isConnected()) {
		            try {
		                socket.close();
		            } catch (IOException e) {
		                //catch logic
		            }
		        }
		    }
		}
		
	}
	
}
