package com.example.android.BluetoothChat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.Display;
import android.widget.LinearLayout;

public class RoundedTileBackground extends LinearLayout {
  private Display display;
  
  
  
  public RoundedTileBackground(Context context) {
    this(context, null);
  }

  public RoundedTileBackground(Context context, AttributeSet attrs) {
    super(context, attrs);
    display = ((Activity)getContext()).getWindowManager().getDefaultDisplay();
    drawBg();
  }


  private void drawBg(){
    int w = display.getWidth(); 
    int r = 0;
    
    
    
    Bitmap b = ImageProccessor.getRoundedCornersImage(BitmapFactory.decodeResource(getResources(), R.drawable.ab), r, w); 

    BitmapDrawable bd = new BitmapDrawable(b);

    this.setBackgroundDrawable(bd);

  }
}
