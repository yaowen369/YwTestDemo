package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.builder;

import android.content.Context;

import com.baidu.idl.main.facesdk.model.BDFaceInstance;

public abstract class ModelBuilder<T> {
    public abstract void init(BDFaceInstance bdFaceInstance);
    public abstract void init();
    public abstract void initModel(Context context);
    public abstract T getExample();
//    public abstract void initFastModel(Context context);
//    public abstract void initAccurateModel(Context context);
//    public abstract void initQualityModel(Context context);
//    public abstract void initAttrbuteModel(Context context);
//    public abstract void initBestImageModel(Context context);
}
