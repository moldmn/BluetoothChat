/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.BluetoothChat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import org.apache.http.util.ByteArrayBuffer;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService {
	// Debugging
	private static final String TAG = "BluetoothChatService";
	private static final boolean D = true;

	// Name for the SDP record when creating server socket
	private static final String NAME_SECURE = "BluetoothChatSecure";
	private static final String NAME_INSECURE = "BluetoothChatInsecure";

	// Unique UUID for this application
	private static final UUID MY_UUID_SECURE =
			UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
	private static final UUID MY_UUID_INSECURE =
			UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

	// Member fields
	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	private AcceptThread mSecureAcceptThread;
	private ConnectThread mConnectThread;
	private int mState;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0;       // we're doing nothing
	public static final int STATE_LISTEN = 1;     // now listening for incoming connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
	public static final int STATE_CONNECTED = 3;  // now connected to a remote device
	

	public boolean accepting;

	public boolean isServer = true;

	private Context bc;

	private ArrayList<BluetoothClient> bluetoothClients = new ArrayList<BluetoothClient>();
	private ArrayList<BluetoothMessage> messages = new ArrayList<BluetoothMessage>();

	/**
	 * Constructor. Prepares a new BluetoothChat session.
	 * @param context  The UI Activity Context
	 * @param handler  A Handler to send messages back to the UI Activity
	 */
	public BluetoothChatService(Context context, Handler handler) {
		bc = context;
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
	}

	public void addMessageToHistory(BluetoothMessage m){
		messages.add(m);
		if (messages.size()>11){
			messages.remove(0);
		}
	}

	public void setServer(boolean b){
		isServer = b;
		accepting = b;

		if (isServer){
			if (mSecureAcceptThread == null) {
				mSecureAcceptThread = new AcceptThread();
			}

			if (!mSecureAcceptThread.isAlive()){
				mSecureAcceptThread.start();
				setState(STATE_LISTEN);
			}
		}else{ 
			messages.clear();
		}
	}

	public void startServer(){

		stop();
		setServer(true);

	}


	/**
	 * Set the current state of the chat connection
	 * @param state  An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;

		// Give the new state to the Handler so the UI Activity can update
		mHandler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	/**
	 * Return the current connection state. */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Start the chat service. Specifically start AcceptThread to begin a
	 * session in listening (server) mode. Called by the Activity onResume() */
	public synchronized void start() {
		if (D) Log.d(TAG, "start");

		// Cancel any thread attempting to make a connection


		// Cancel any thread currently running a connection

		setServer(true);

		//setState(STATE_LISTEN);

		// Start the thread to listen on a BluetoothServerSocket
		//		if (mSecureAcceptThread == null) {
		//			mSecureAcceptThread = new AcceptThread();
		//			mSecureAcceptThread.start();
		//		}
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * @param device  The BluetoothDevice to connect
	 * @param secure Socket Security type - Secure (true) , Insecure (false)
	 */
	public synchronized void connect(BluetoothDevice device) {
		if (D) Log.d(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		}

		// Cancel any thread currently running a connection

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * @param socket  The BluetoothSocket on which the connection was made
	 * @param device  The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice
			device, final String socketType) {
		if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

		// Cancel any thread currently running a connection


		// Cancel the accept thread because we only want to connect to one device

		// Start the thread to manage the connection and perform transmissions
		ConnectedThread _mConnectedThread = new ConnectedThread(socket, socketType);
		_mConnectedThread.start();

		//creating new client
		BluetoothClient bc = new BluetoothClient(socket,_mConnectedThread);

		bluetoothClients.add(bc);


		// Send the name of the connected device back to the UI Activity
		Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothChat.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);

		bc.connectedThread.sendLogs(); //write(messages.get(i).getJSONStr());		

	}

	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		if (D) Log.d(TAG, "stop");

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mSecureAcceptThread != null) {
			mSecureAcceptThread.cancel();
			mSecureAcceptThread = null;
		}
		for (int i = 0; i < bluetoothClients.size(); i++){
			bluetoothClients.get(i).connectedThread.cancel();
			bluetoothClients.get(i).connectedThread = null;
		}
		bluetoothClients.clear();
		messages.clear();

		setState(STATE_NONE);
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * @param out The bytes to write
	 * @see ConnectedThread#write(byte[])
	 */
	public void write(byte[] out) {
		// Create temporary object
		// Synchronize a copy of the ConnectedThread
		//synchronized (this) {
		//	if (mState != STATE_CONNECTED) return;
		//  r = mConnectedThread;

		//}
		// Perform the write unsynchronized
		Log.d("write", "Send message");

		for (int i = 0 ; i < bluetoothClients.size(); i++){
			bluetoothClients.get(i).connectedThread.write(out);
		}

		mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, out).sendToTarget();

	}

	public void writeFile(Uri uri) {
		try{

			for (int i = 0 ; i < bluetoothClients.size(); i++){
				bluetoothClients.get(i).connectedThread.writeFile(uri);
			}
		}catch (Exception e){

		}


		//mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, out).sendToTarget();

	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothChat.TOAST, "Unable to connect device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		// Start the service over to restart listening mode
		BluetoothChatService.this.start();
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {
		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothChat.TOAST, "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		// Start the service over to restart listening mode
		BluetoothChatService.this.start();
	}

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted
	 * (or until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;
		private String mSocketType;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;
			mSocketType = "Secure";

			// Create a new listening server socket
			try {

				tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
						MY_UUID_SECURE);
			} catch (IOException e) {
				Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		public void run() {
			if (D) Log.d(TAG, "Socket Type: " + mSocketType +
					"BEGIN mAcceptThread" + this);
			setName("AcceptThread" + mSocketType);

			BluetoothSocket socket = null;

			accepting = true;

			// Listen to the server socket if we're not connected
			while (accepting) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
					break;
				}

				// If a connection was accepted
				if (socket != null) {
					synchronized (BluetoothChatService.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							connected(socket, socket.getRemoteDevice(),
									mSocketType);
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// Either not ready or already connected. Terminate new socket.
							connected(socket, socket.getRemoteDevice(),
									mSocketType);
							break;
						}
					}
				}
			}
			if (D) Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

		}

		public void cancel() {
			if (D) Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
			}
		}
	}


	/**
	 * This thread runs while attempting to make an outgoing connection
	 * with a device. It runs straight through; the connection either
	 * succeeds or fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		private String mSocketType;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;
			mSocketType = "Secure";

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {

				tmp = device.createRfcommSocketToServiceRecord(
						MY_UUID_SECURE);
			} catch (IOException e) {
				Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
			setName("ConnectThread" + mSocketType);

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() " + mSocketType +
							" socket during connection failure", e2);
				}
				connectionFailed();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothChatService.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice, mSocketType);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device.
	 * It handles all incoming and outgoing transmissions.
	 */
	public class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket, String socketType) {
			Log.d(TAG, "create ConnectedThread: " + socketType);
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;

			boolean fileMode = false;
			ByteArrayBuffer file = null;
			long fileSize = 0;
			long totalSize = 0;
			String fileName = "tmp";
			String fileAuthor = "";

			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);

					// construct a string from the buffer
					String writeMessage = new String(buffer);
					BluetoothMessage m = new BluetoothMessage(writeMessage);

					if (m.type == BluetoothMessage.TYPE_TEXT) {
						// Send the obtained bytes to the UI Activity
						mHandler.obtainMessage(BluetoothChat.MESSAGE_READ, bytes, -1, m).sendToTarget();
					} else if (m.type == BluetoothMessage.TYPE_FILE_START) {
						fileMode = true;
						fileSize = 0;
						totalSize = Long.parseLong(m.date);// Integer.parseInt(m.date);
						fileName = m.text;
						fileAuthor = m.author;
						file = new ByteArrayBuffer((int)totalSize);
						continue;
					}


					if (fileMode){
						file.append(buffer, 0, bytes);
						fileSize += bytes;
						if (fileSize >= totalSize) {
							fileMode = false;
							File savedFile = new File (Environment.getExternalStorageDirectory().getPath()+"/"+ ( (!fileName.isEmpty()) ? fileName : "tmp.txt"));
							FileOutputStream fos = new FileOutputStream(savedFile);
				            fos.write(file.toByteArray());
				            fos.close();
				            
				    		Date d = new Date();
							String date = BluetoothChat.pad(d.getHours()) + ":"+ BluetoothChat.pad(d.getMinutes())+ ":"+ BluetoothChat.pad(d.getSeconds());
				            
				            BluetoothMessage bm = new BluetoothMessage(fileAuthor, date, "file "+fileName+" sent");
				            
				            mHandler.obtainMessage(BluetoothChat.MESSAGE_READ, bytes, -1, bm).sendToTarget();
				            
				            Uri uri = Uri.fromFile(savedFile);
				            if (bluetoothClients.size()>1){
								for (int i = 0; i < bluetoothClients.size(); i++){
									if (!bluetoothClients.get(i).deviceAdress.equals(mmSocket.getRemoteDevice().getAddress())){
										bluetoothClients.get(i).connectedThread.writeFile(uri);
									}
								}
							}
				        }
					}else{
						String str = new String(buffer, 0, bytes);

						if (bluetoothClients.size()>1){
							for (int i = 0; i < bluetoothClients.size(); i++){
								if (!bluetoothClients.get(i).deviceAdress.equals(mmSocket.getRemoteDevice().getAddress())){
									bluetoothClients.get(i).connectedThread.write(str.getBytes());
								}
							}
						}

					}
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * @param buffer  The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
				mmOutStream.flush();	
				// Share the sent message back to the UI Activity

			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		
		
		public void writeFile(Uri uri) throws IOException, InterruptedException {
			File f = new File(uri.toString());
			
			String fileName = f.getName();
			InputStream inputStream = bc.getContentResolver().openInputStream(uri);
			
			long fileSize = inputStream.available();
			
			BluetoothMessage m = new BluetoothMessage(BluetoothAdapter.getDefaultAdapter().getName(), String.valueOf(fileSize), fileName, BluetoothMessage.TYPE_FILE_START);
			write(m.getJSONStr());
			
			sleep(100);
			
			ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();			

			int buffersize = 1024;
			byte[] buffer = new byte[buffersize];

			int len = 0;
			while((len = inputStream.read(buffer)) != -1){
				byteBuffer.write(buffer, 0, len);
			}

			Log.d(TAG, "sending data to connected thread");
			write(byteBuffer.toByteArray());
		}

		public void sendLogs(){
			for (int i = 0; i < messages.size(); i++){
				write(messages.get(i).getJSONStr());
				try {
					sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}
