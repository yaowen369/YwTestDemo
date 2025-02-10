package ywdemo.example.yaoxiaowen;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.ImageView;



public class SpecialQrCodeActivity extends Activity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_special_qr_code);
        // 定义二维码尺寸
        int qrSize = 200;
        // 创建一个空白的 Bitmap
        Bitmap bitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        // 设置背景颜色为白色
        canvas.drawColor(Color.WHITE);
        // 创建画笔
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        // 绘制圆形表示协议部分
        int circleSize = 50;
        int circleX = 20;
        int circleY = 20;
        int circleRadius = circleSize / 2;
        canvas.drawCircle(circleX + circleRadius, circleY + circleRadius, circleRadius, paint);
        // 绘制方形表示域名部分
        int squareSize = 80;
        int squareX = 100;
        int squareY = 100;
        canvas.drawRect(squareX, squareY, squareX + squareSize, squareY + squareSize, paint);
        // 获取 ImageView 并显示生成的 Bitmap
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(bitmap);
    }
}