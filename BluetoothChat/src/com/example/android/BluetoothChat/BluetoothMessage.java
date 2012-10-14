package com.example.android.BluetoothChat;

import org.json.JSONObject;

public class BluetoothMessage {
	public String author;
	public String date;
	public String text;

	public BluetoothMessage(String author, String date, String text){
		this.author = author;
		this.date = date;
		this.text = text;

	}
	public BluetoothMessage(String string){
		try{
			JSONObject jobj = new JSONObject(string);
			this.author = jobj.getString("author");
			this.date = jobj.getString("date");
			this.text = jobj.getString("text");
		}catch (Exception e) {
			// TODO: handle exception
		}
	}



	public byte[] getJSONStr(){
		JSONObject jobj = new JSONObject();
		try{
			jobj.put("author", author);
			jobj.put("date", date);
			jobj.put("text", text);
		}catch (Exception e) {
			// TODO: handle exception
		}
		return jobj.toString().getBytes();
	}



}
