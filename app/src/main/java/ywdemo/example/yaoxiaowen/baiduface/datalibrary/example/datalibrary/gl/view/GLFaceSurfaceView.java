package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.gl.view;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewOutlineProvider;

import com.baidu.idl.main.facesdk.FaceInfo;
import com.baidu.idl.main.facesdk.model.BDFaceImageInstance;

import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.gl.shape.GLFaeShap;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.gl.shape.GLFrame;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.gl.shape.GLFramebuffer;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.gl.utils.EGLUtils;

public class GLFaceSurfaceView extends SurfaceView {

    private GLFramebuffer mFramebuffer;
    private EGLUtils mEglUtils;
    private GLFrame mFrame;
    private GLFaeShap glFaeShap;

    // 圆角半径
    private int radius = 0;

    public GLFaceSurfaceView(Context context) {
        super(context);
    }

    public GLFaceSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);


        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                Rect rect = new Rect(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                outline.setRoundRect(rect, radius);
            }
        });
        setClipToOutline(true);
    }

    public void init(int mirrorRGB){
        mFrame = new GLFrame(mirrorRGB);
        glFaeShap = new GLFaeShap();
        mFramebuffer = new GLFramebuffer();
        if (getHolder().getSurface() != null && getWidth() > 0) {
            if (mEglUtils != null) {
                mEglUtils.release();
            }
            mEglUtils = new EGLUtils();
            mEglUtils.initEGL(getHolder().getSurface());
            mFrame.initFrame();
            mFramebuffer.initFramebuffer();

        }

    }


    public void surfaceChanged(SurfaceHolder surfaceHolder) {
        Log.d("=============", "surfaceChanged");
        if (mEglUtils != null) {
            mEglUtils.release();
        }
        mEglUtils = new EGLUtils();
        mEglUtils.initEGL(surfaceHolder.getSurface());
        mFramebuffer.initFramebuffer();

    }

    public SurfaceTexture getFramebuffer(){
        return mFramebuffer.getSurfaceTexture();
    }


    public void initFrame(int width , int height){
//        mFramebuffer.initFramebuffer();
        mFrame.initFrame();
        glFaeShap.init(width , height);
    }


    public void surfaceDestroyed() {

        mFramebuffer.release();
        mFrame.release();

        if (mEglUtils != null) {
            mEglUtils.release();
            mEglUtils = null;
        }
    }


    private int i = -1;

    public void setFrame() {
        this.i = mFramebuffer.drawFrameBuffer();
        this.floats = mFramebuffer.getMatrix();
    }

    public void setSize(int width , int height){
        mFrame.setSize(getWidth() , getHeight() , width , height);
        mFrame.correctSize(width , height);
    }

    private float[] floats;

    public void onGlDraw(FaceInfo[] faceInfos , BDFaceImageInstance image , boolean revert , float[] colors) {
        if (mEglUtils == null){
            return;
        }
        showFrame(faceInfos , image , revert , colors);
        mFrame.setS(100 / 100.0f);
        mFrame.setH(0 / 360.0f);
        mFrame.setL(100 / 100.0f - 1);
        if (i == -1 &&  floats == null){
            mFrame.drawFrame(0, mFramebuffer.drawFrameBuffer() , mFramebuffer.getMatrix());
        }else {

            mFrame.drawFrame(0, i , floats);
        }
        glFaeShap.onDraw();
        mEglUtils.swap();
    }

    public void onGlDraw(FaceInfo[] faceInfos , BDFaceImageInstance image , float[] colors ,
                         boolean revert , boolean isFaeShap) {
        if (mEglUtils == null){
            return;
        }
        showFrame(faceInfos , image , revert , colors);
        mFrame.setS(100 / 100.0f);
        mFrame.setH(0 / 360.0f);
        mFrame.setL(100 / 100.0f - 1);
        if (i == -1 &&  floats == null){
            mFrame.drawFrame(0, mFramebuffer.drawFrameBuffer() , mFramebuffer.getMatrix());
        }else {

            mFrame.drawFrame(0, i , floats);
        }
        if (!isFaeShap){
            glFaeShap.onDraw();
        }
        mEglUtils.swap();
    }


    public void onGlDraw() {
        if (mEglUtils == null){
            return;
        }
        mFrame.setS(100 / 100.0f);
        mFrame.setH(0 / 360.0f);
        mFrame.setL(100 / 100.0f - 1);
        mFrame.drawFrame(0, mFramebuffer.drawFrameBuffer() , mFramebuffer.getMatrix());
        mEglUtils.swap();
    }


    public void showFrame(FaceInfo[] faceInfos , BDFaceImageInstance image , boolean revert ,  float[] colors){
        if (faceInfos == null){
            glFaeShap.cleanVertices();
            return;
        }
        if (image == null) {
            glFaeShap.cleanVertices();
            return;
        }
        if (faceInfos != null && faceInfos.length != 0) {
            FaceInfo faceInfo = faceInfos[0];
            float imageWidth = image.width / 2;
            float imageHeight = image.height / 2;
            float dx = ((faceInfo.centerX - imageWidth ) / imageWidth);
            float dy = -(faceInfo.centerY - imageHeight) / imageHeight;
            float x = (faceInfo.width / image.width) ;
            float y = (faceInfo.height / image.height) / 1.4f;
            if (revert){
                glFaeShap.setVertices(-(mFrame.getWd() * (dx + x)) , mFrame.getHd() * (dy + y) ,
                        -(mFrame.getWd() * (dx + x)) , mFrame.getHd() * (dy - y) ,
                        -(mFrame.getWd() * (dx - x)) , mFrame.getHd() * (dy + y) ,
                        -(mFrame.getWd() * (dx - x)) , mFrame.getHd() * (dy - y));
            }else {
                glFaeShap.setVertices(mFrame.getWd() * (dx - x) , mFrame.getHd() * (dy + y) ,
                        mFrame.getWd() * (dx - x) , mFrame.getHd() * (dy - y) ,
                        mFrame.getWd() * (dx + x) , mFrame.getHd() * (dy + y) ,
                        mFrame.getWd() * (dx + x) , mFrame.getHd() * (dy - y));
            }
            glFaeShap.setFaceColor(colors);
        }else {
            glFaeShap.cleanVertices();
        }
    }

}
