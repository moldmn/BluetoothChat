package com.example.android.BluetoothChat;

import org.json.JSONObject;

public class BluetoothMessage {
	public static int TYPE_BYTES = 0;
	public static int TYPE_TEXT = 1;
	public static int TYPE_FILE_START = 2;
	public static int TYPE_FILE_END = 3;
	

	public String author;
	public String date;
	public String text;
	public int type;

	public BluetoothMessage(String author, String date, String text){
		this.author = author;
		this.date = date;
		this.text = text;
		this.type = TYPE_TEXT;
	}
	
	public BluetoothMessage(String author, String date, String text, int type){
		this.author = author;
		this.date = date;
		this.text = text;
		this.type = type;
	}
	
	public BluetoothMessage(String string){
		try{
			JSONObject jobj = new JSONObject(string);
			this.author = jobj.getString("author");
			this.date = jobj.getString("date");
			this.text = jobj.getString("text");
			this.type = jobj.getInt("type");
		}catch (Exception e) {
			this.type = TYPE_BYTES;
		}
	}



	public byte[] getJSONStr(){
		JSONObject jobj = new JSONObject();
		try{
			jobj.put("author", author);
			jobj.put("date", date);
			jobj.put("text", text);
			jobj.put("type", type);
		}catch (Exception e) {
			// TODO: handle exception
		}
		return jobj.toString().getBytes();
	}



}
