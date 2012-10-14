package com.example.android.BluetoothChat;

import com.example.android.BluetoothChat.BluetoothChatService.ConnectedThread;

import android.bluetooth.BluetoothSocket;

public class BluetoothClient {
	public String deviceName;
	public String deviceAdress;
	public ConnectedThread connectedThread;
	
	public BluetoothClient (BluetoothSocket bluetoothSocket, ConnectedThread connectedThread){
		deviceName = bluetoothSocket.getRemoteDevice().getName();
		deviceAdress = bluetoothSocket.getRemoteDevice().getAddress();
		this.connectedThread = connectedThread;
	}
}
