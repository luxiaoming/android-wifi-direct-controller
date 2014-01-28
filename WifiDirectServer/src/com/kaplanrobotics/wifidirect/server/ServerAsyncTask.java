package com.kaplanrobotics.wifidirect.server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * A simple server socket that accepts connection and writes some data on
 * the stream.
 */

public class ServerAsyncTask extends AsyncTask<Void, Void, String> {

	public static final String TAG = "ServerAsyncTask";
	
//    private Context context;
//    private TextView statusText;

    /**
     * @param context
     * @param statusText
     */
//    public ServerAsyncTask(Context context, View statusText) {
//        this.context = context;
//        this.statusText = (TextView) statusText;
//    }

	
	ServerSocket serverSocket;
	InputStream inputStream;
	
    @Override
    protected String doInBackground(Void... params) {
        try {
        	
        	// Open the server socket
            serverSocket = new ServerSocket(8988);
            Log.e(TAG, "Server: Socket opened");
            
            // Connect to the client
            Socket client = serverSocket.accept();
            Log.e(TAG, "Server: connection done");
            
            
            inputStream = null;
            
            if(client.isConnected()){
            
            	Log.e(TAG, "CONNECTED TO CLIENT");
                
            	
                // Grab the streaming client output
                inputStream = client.getInputStream();
                // Maybe do an outputstream here later, if for nothing else than to send status back to the client                
            
                // As long as the connection is OK, listen
                while(client.isConnected()){
                	
                	Log.e(TAG, "Try and read");

                	
                	// listen and read
                	read(inputStream);
                	
                
                	
                }
            
            }
         
            // Clean up the stream and the socket
            if(inputStream != null)
            	inputStream.close();
            serverSocket.close();        
                    
        } 
        catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
        
        return "Done.";
    }
    
    public void close(){
    	if(serverSocket != null && !serverSocket.isClosed())
			try {
				serverSocket.close();
				if(inputStream != null)
					inputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    	
    }
    
    private void read(InputStream inputStream){
    	// Open a reader on the given input stream
    	BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
    	
    	// Storage for incoming messages
    	String inputLine;
    	
    	try {
    		// Try and read 
			while ((inputLine = in.readLine()) != null) {
				 Log.e(TAG, "Message: "+inputLine);
			}
		} 
    	catch (IOException e) {
			e.printStackTrace();
		}
    	
        
    }
}


