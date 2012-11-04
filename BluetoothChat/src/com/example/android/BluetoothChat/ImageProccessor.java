package com.example.android.BluetoothChat;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

public class ImageProccessor {
  private ImageProccessor() {};

  public static Bitmap getRoundedCornersImage(Bitmap source, int radiusPixels, int width) {

    if (source == null) {
      //we cant proccess null image, go out
      return null;
    }
    final int sourceWidth = source.getWidth();
    final int sourceHeight = source.getHeight();
    final Bitmap output = Bitmap.createBitmap(width, sourceHeight, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(output);

    final int color = 0xFF000000;
    final Paint paint = new Paint();
    paint.setColor(color);

    final Rect rect = new Rect(0, 0, width, sourceHeight+ radiusPixels);
    final RectF rectF = new RectF(rect);

    paint.setAntiAlias(true);
    canvas.drawARGB(0, 0, 0, 0);
    canvas.drawRoundRect(rectF, radiusPixels, radiusPixels, paint);

    paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
    
    int count = width/sourceWidth;
    for (int i = 0; i < count+1; i++){
      canvas.drawBitmap(source, i*sourceWidth, 0, paint);
    }
    
    
    return output;
  }
}
