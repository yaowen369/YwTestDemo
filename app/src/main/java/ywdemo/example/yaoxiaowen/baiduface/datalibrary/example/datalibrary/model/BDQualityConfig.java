package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model;

public class BDQualityConfig {
    public BDQualityConfig(float blur , float illum , float gesture ,
                           float leftEye , float rightEye ,
                           float nose , float mouth , float leftCheek ,
                           float rightCheek , float chinContour){
        this.blur = blur;
        this.illum = illum;
        this.gesture = gesture;
        this.leftEye = leftEye;
        this.rightEye = rightEye;
        this.nose = nose;
        this.mouth = mouth;
        this.leftCheek = leftCheek;
        this.rightCheek = rightCheek;
        this.chinContour = chinContour;
    }
    // 模糊度设置，默认0.8。取值范围[0~1]，0是最清晰，1是最模糊
    public float blur;
    // 光照设置，默认0.8.取值范围[0~1], 数值越大，光线越强
    public float illum;
    // 姿态阈值
    public float gesture;
    // 左眼被遮挡的阈值，默认0.8
    public float leftEye;
    // 右眼被遮挡的阈值，默认0.8
    public float rightEye;
    // 鼻子被遮挡的阈值，默认0.8
    public float nose;
    // 嘴巴被遮挡的阈值，默认0.8
    public float mouth;
    // 左脸颊被遮挡的阈值，默认0.8
    public float leftCheek;
    // 右脸颊被遮挡的阈值，默认0.8
    public float rightCheek;
    // 下巴被遮挡阈值，默认为0.8
    public float chinContour;
}
