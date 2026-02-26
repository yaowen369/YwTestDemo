package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.gl.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.TextureView;
import android.widget.RelativeLayout;

import com.baidu.idl.main.facesdk.FaceInfo;
import com.baidu.idl.main.facesdk.model.BDFaceImageInstance;

import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.FaceColor;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.LivenessModel;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils.FaceOnDrawTexturViewUtil;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils.ImageUtils;

public class GlMantleSurfacView extends RelativeLayout {

    private boolean isDraw = false;

    private int drawLength = 200;
    public float circleRadius;
    public float circleX;
    public float circleY;
    private Context context;
    private Paint paint;
    public TextureView textureView;
    private TextureView faceTexture;
    GLFaceSurfaceView glFaceSurfaceView;

    private int videoWidth = 0;
    private int videoHeight = 0;
    public int previewWidth = 0;
    private int previewHeight = 0;
    private static int scale = 2;

    private boolean mIsRegister;   // 注册

    private boolean rgbRevert; // 检测框镜像

    private int mirrorRGB; // 摄像头展示镜像

    private boolean isMultiIdentify = false;


    public void setMultiIdentify(boolean multiIdentify){
        this.isMultiIdentify = multiIdentify;
    }

    public void setDraw(boolean draw) {
        isDraw = draw;
        postInvalidate();
    }
    public boolean isDraw() {
        return isDraw;
    }
    public GLFaceSurfaceView getGlFaceSurfaceView() {
        return glFaceSurfaceView;
    }


    public GlMantleSurfacView(Context context) {
        super(context);
        init(context);
    }

    public GlMantleSurfacView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GlMantleSurfacView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public GlMantleSurfacView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }


    public void onGlDraw(){
        if (glFaceSurfaceView != null){
            glFaceSurfaceView.onGlDraw();
        }
    }


    public void onGlDraw(FaceInfo[] faceInfos , BDFaceImageInstance image , FaceColor colors ) {
        if (glFaceSurfaceView != null){
            glFaceSurfaceView.onGlDraw(faceInfos , image , colors.getColors() , rgbRevert , isDraw || mIsRegister);
        } else {

            Canvas canvas = faceTexture.lockCanvas();
            if (canvas == null) {
                faceTexture.unlockCanvasAndPost(canvas);
                return;
            };
            if (faceInfos == null || faceInfos.length == 0) {
                // 清空canvas
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                faceTexture.unlockCanvasAndPost(canvas);
                return;
            }
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            // FaceInfo faceInfo = faceInfos[0];
            for (int i = 0; i < faceInfos.length; i++) {
                FaceInfo faceInfo = faceInfos[i];
                RectF rectF = new RectF();
                rectF.set(FaceOnDrawTexturViewUtil.getFaceRectTwo(faceInfo));
                // 检测图片的坐标和显示的坐标不一样，需要转换。
                FaceOnDrawTexturViewUtil.mapFromOriginalRect(rectF,
                        faceTexture, image);
                // 人脸框颜色
                paint.setColor(colors.getColor());
                // 绘制人脸框
                FaceOnDrawTexturViewUtil.drawRect(canvas,
                        rectF, paint, 5f, 50f , 25f);
                if (!isMultiIdentify) {
                    break;
                }
            }

//            RectF rectF = new RectF();
//            rectF.set(FaceOnDrawTexturViewUtil.getFaceRectTwo(faceInfo));
//            // 检测图片的坐标和显示的坐标不一样，需要转换。
//            FaceOnDrawTexturViewUtil.mapFromOriginalRect(rectF,
//                    faceTexture, image);
//            // 人脸框颜色
//            paint.setColor(colors.getColor());
//            // 绘制人脸框
//            FaceOnDrawTexturViewUtil.drawRect(canvas,
//                    rectF, paint, 5f, 50f , 25f);
            // 清空canvas
            faceTexture.unlockCanvasAndPost(canvas);
        }
    }


    public void onMaskGlDraw(FaceInfo[] faceInfos , BDFaceImageInstance image , FaceColor colors, LivenessModel livenessModel,
                             String maskTip) {
        if (glFaceSurfaceView != null){
            glFaceSurfaceView.onGlDraw(faceInfos , image , colors.getColors() , rgbRevert , isDraw || mIsRegister);
        } else {

            Canvas canvas = faceTexture.lockCanvas();
            if (canvas == null) {
                faceTexture.unlockCanvasAndPost(canvas);
                return;
            };
            if (faceInfos == null || faceInfos.length == 0) {
                // 清空canvas
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                faceTexture.unlockCanvasAndPost(canvas);
                return;
            }
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//            FaceInfo faceInfo = faceInfos[0];

            for (int i = 0; i < faceInfos.length; i++) {
                FaceInfo faceInfo = faceInfos[i];
                RectF rectF = new RectF();
                rectF.set(FaceOnDrawTexturViewUtil.getFaceRectTwo(faceInfo));
                // 检测图片的坐标和显示的坐标不一样，需要转换。
                FaceOnDrawTexturViewUtil.mapFromOriginalRect(rectF,
                        faceTexture, image);
                // 人脸框颜色
                paint.setColor(colors.getColor());

                // 绘制人脸框
                boolean drawMask = false;
                if (livenessModel != null){
                    float [] maskScore = livenessModel.getMouthMaskArray();
                    if (maskScore != null){
                        float score = maskScore[i];
                        if (score >= 0.8){
                            drawMask = true;
                        }
                    }
                }

                if (drawMask){
                    FaceOnDrawTexturViewUtil.drawMaskRect(canvas,
                            rectF, paint, 5f, 50f , 25f, maskTip);
                }
                else{
                    FaceOnDrawTexturViewUtil.drawRect(canvas,
                            rectF, paint, 5f, 50f , 25f);
                }
            }

            // 清空canvas
            faceTexture.unlockCanvasAndPost(canvas);
        }
    }

    public void setDrawHeightLength(int drawLength) {
        this.drawLength = drawLength;
    }


    private void init(Context context){
        this.context = getContext();
        setWillNotDraw(false);
    }


    public void setFrame(){
        if (glFaceSurfaceView == null){
            return;
        }
        glFaceSurfaceView.setFrame();
    }

    public void initSurface(Boolean rgbRevert , int mirrorRGB , boolean isOpenGl){
        this.rgbRevert = rgbRevert;
        this.mirrorRGB = mirrorRGB;
        if (isOpenGl){
            glFaceSurfaceView = new GLFaceSurfaceView(context);
            addView(glFaceSurfaceView);
            glFaceSurfaceView.init(mirrorRGB);
        }else {

            textureView = new TextureView(getContext());
            faceTexture = new TextureView(getContext());
            faceTexture.setOpaque(false);
            faceTexture.setKeepScreenOn(true);
            if (rgbRevert){
                faceTexture.setRotationY(180);
            }
            paint = new Paint();
            addView(textureView);
            addView(faceTexture);
        }
    }


    private void setTextureLayout(){
        if (videoWidth == 0 || videoHeight == 0 || previewWidth == 0 || previewHeight == 0 || textureView == null) {
            return;
        }

        if (previewWidth * videoHeight > previewHeight * videoWidth) {
            int scaledChildHeight = videoHeight * previewWidth / videoWidth;
            textureView.layout(0, (previewHeight - scaledChildHeight) / scale,
                    previewWidth, (previewHeight + scaledChildHeight) / scale);
            faceTexture.layout(0, (previewHeight - scaledChildHeight) / scale,
                    previewWidth, (previewHeight + scaledChildHeight) / scale);
        } else {
            int scaledChildWidth = videoWidth * previewHeight / videoHeight;
            textureView.layout((previewWidth - scaledChildWidth) / scale, 0,
                    (previewWidth + scaledChildWidth) / scale, previewHeight);
            faceTexture.layout((previewWidth - scaledChildWidth) / scale, 0,
                    (previewWidth + scaledChildWidth) / scale, previewHeight);

        }
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        previewWidth = getWidth();
        previewHeight = getHeight();
        setTextureLayout();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (isDraw) {
            Path path = new Path();
            // 设置裁剪的圆心坐标，半径
            path.addCircle(getWidth() / 2,
                    (getHeight() - ImageUtils.dip2px(context , drawLength)) / 2, getWidth() / 3, Path.Direction.CCW);
            // 裁剪画布，并设置其填充方式
            // canvas.clipPath(path, Region.Op.REPLACE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                canvas.clipPath(path);
            } else {
                canvas.clipPath(path, Region.Op.REPLACE); // REPLACE、UNION 等
            }
            // 圆的半径
            circleRadius = getWidth() / 3;
            // 圆心的X坐标
            circleX = (getRight() - getLeft()) / 2;
            // 圆心的Y坐标
            circleY = (getBottom() - getTop()) / 2 ;
        }

        if (mIsRegister) {
            Path path = new Path();
            // 设置裁剪的圆心坐标，半径
            path.addCircle(getWidth() / 2, getHeight() / 2, getWidth() / 3, Path.Direction.CCW);
            // 裁剪画布，并设置其填充方式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                canvas.clipPath(path);
            } else {
                canvas.clipPath(path, Region.Op.REPLACE); // REPLACE、UNION 等
            }
            // 圆的半径
            circleRadius = getWidth() / 3;
            // 圆心的X坐标
            circleX = (getRight() - getLeft()) / 2;
            // 圆心的Y坐标
            circleY = (getBottom() - getTop()) / 2;
        }
        super.onDraw(canvas);
    }


    public void setIsRegister(boolean isRegister) {
        mIsRegister = isRegister;
        invalidate();
    }
    public TextureView getTextureView() {
        return textureView;
    }
    private Handler handler = new Handler(Looper.getMainLooper());


    public void setPreviewSize(int width, int height) {
        if (this.videoWidth == width && this.videoHeight == height) {
            return;
        }
        this.videoWidth = width;
        this.videoHeight = height;
        handler.post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });

    }

    public boolean getIsRegister() {
        return mIsRegister;
    }

    public boolean isRgbRevert() {
        return rgbRevert;
    }
    public int getMirrorRGB() {
        return mirrorRGB;
    }



}
