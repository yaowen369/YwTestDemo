package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener;

public interface QualityListener {
    public void onQualitySuccess();
    public void onQualityFail(String detectFail , String occlusionFail);
}
