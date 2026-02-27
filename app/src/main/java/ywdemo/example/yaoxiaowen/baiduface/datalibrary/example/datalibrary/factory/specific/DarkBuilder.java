package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.specific;

import android.content.Context;

import com.baidu.idl.main.facesdk.FaceDarkEnhance;
import com.baidu.idl.main.facesdk.callback.Callback;
import com.baidu.idl.main.facesdk.model.BDFaceInstance;

import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.builder.ModelBuilder;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.SdkInitListener;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.GlobalSet;
//import com.example.datalibrary.factory.builder.ModelBuilder;
//import com.example.datalibrary.listener.SdkInitListener;
//import com.example.datalibrary.model.GlobalSet;

public class DarkBuilder extends ModelBuilder<FaceDarkEnhance> {

    private FaceDarkEnhance faceDarkEnhance;
    private SdkInitListener listener;
    public DarkBuilder(SdkInitListener listener){
        this.listener = listener;
    }
    @Override
    public void init(BDFaceInstance bdFaceInstance) {
        if (bdFaceInstance == null){
            faceDarkEnhance = new FaceDarkEnhance();
        }else {
            faceDarkEnhance = new FaceDarkEnhance(bdFaceInstance);
        }
    }

    @Override
    public void init() {
        faceDarkEnhance = new FaceDarkEnhance();

    }

    @Override
    public void initModel(Context context) {
        faceDarkEnhance.initFaceDarkEnhance(context,
                GlobalSet.DARK_ENHANCE_MODEL, new Callback() {
                    @Override
                    public void onResponse(int code, String response) {
                        if (code != 0 && listener != null) {
                            listener.initModelFail(code, response);
                        }
                    }
                });
    }

    @Override
    public FaceDarkEnhance getExample() {
        return faceDarkEnhance;
    }

    public FaceDarkEnhance getFaceDarkEnhance() {
        return faceDarkEnhance;
    }
}
