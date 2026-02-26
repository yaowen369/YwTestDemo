/*
 * Copyright (C) 2018 Baidu, Inc. All Rights Reserved.
 */
package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model;


import com.baidu.idl.main.facesdk.FaceInfo;
import com.baidu.idl.main.facesdk.model.BDFaceImageInstance;
//import com.example.datalibrary.manager.IdentifyResult;
//import com.example.datalibrary.model.User;

import java.util.ArrayList;
import java.util.List;

import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.manager.IdentifyResult;

public class LivenessModel {

    private int faceDetectCode;
    private FaceInfo faceInfo = null;
    private FaceInfo [] faceInfos;
    private long irDetectDuration;
    private float irLivenessScore;
    private long depthtLivenessDuration;
    private float rgbLivenessScore;

    private float[] rgbLivenessScores;
    private float depthLivenessScore;
    private int liveType;
    private float[] landmarks;
    private byte[] feature;
    private int trackStatus;
    private boolean isRGBLiveStatus;
    private boolean isNIRLiveStatus;
    private boolean isDepthLiveStatus;
    private boolean multiFrame;
    private long irInfraRedDuration;
    float score ;

    private int faceSize;

    private float[] mouthMaskArray;

    public int getFaceSize() {
        return faceSize;
    }

    public void setFaceSize(int faceSize) {
        this.faceSize = faceSize;
    }
    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public long getIrInfraRedDuration() {
        return irInfraRedDuration;
    }

    public void setIrInfraRedDuration(long irInfraRedDuration) {
        this.irInfraRedDuration = irInfraRedDuration;
    }
    public long getNirInstanceTime() {
        return nirInstanceTime;
    }

    public void setNirInstanceTime(long nirInstanceTime) {
        this.nirInstanceTime = nirInstanceTime;
    }
    public long getAccurateTime() {
        return accurateTime;
    }

    public void setAccurateTime(long accurateTime) {
        this.accurateTime = accurateTime;
    }
    public long getTestBDFaceImageInstanceDuration() {
        return testBDFaceImageInstanceDuration;
    }

    public void setTestBDFaceImageInstanceDuration(long testBDFaceImageInstanceDuration) {
        this.testBDFaceImageInstanceDuration = testBDFaceImageInstanceDuration;
    }

    public boolean isMultiFrame() {
        return multiFrame;
    }

    public void setMultiFrame(boolean multiFrame) {
        this.multiFrame = multiFrame;
    }

    public boolean isRGBLiveStatus() {
        return isRGBLiveStatus;
    }

    public void setRGBLiveStatus(boolean rGBLiveStatus) {
        isRGBLiveStatus = rGBLiveStatus;
    }

    public boolean isNIRLiveStatus() {
        return isNIRLiveStatus;
    }

    public void setNIRLiveStatus(boolean nIRLiveStatus) {
        isNIRLiveStatus = nIRLiveStatus;
    }

    public boolean isDepthLiveStatus() {
        return isDepthLiveStatus;
    }

    public void setDepthLiveStatus(boolean depthLiveStatus) {
        isDepthLiveStatus = depthLiveStatus;
    }

    public int getTrackStatus() {
        return trackStatus;
    }

    public void setTrackStatus(int trackStatus) {
        this.trackStatus = trackStatus;
    }

    private float featureScore;
    private float featureCode;
    private BDFaceImageInstance bdFaceImageInstance;
    private BDFaceImageInstance bdFaceImageInstanceCrop;

    private BDFaceImageInstance bdNirFaceImageInstance;
    private BDFaceImageInstance bdDepthFaceImageInstance;

    private User user;
    private int[] shape;

    private FaceInfo[] trackFaceInfo;
    private long allDetectDuration;

    private float maskScore;
    private long maskScoreDuration;
    private long testBDFaceImageInstanceDuration; // 创建BDFaceImage耗时
    private long darkEnhanceDuration;  // 暗光检测
    private long rgbDetectDuration; // track
    private long multiFrameTime; // 多帧判断
    private long accurateTime; // detect 和质量判断
    private long rgbLivenessDuration; // rgb活体
    private long nirInstanceTime; // 创建nir BDFaceImage耗时
    private long irLivenessDuration; // nir detect
    private long irSilentLiveDuration; // nir 活体
    private long featureDuration; // feature
    private long checkDuration; // featureSearch
    private float[] trackLandmarks;

    private List<IdentifyResult> identifyResults = new ArrayList<IdentifyResult>();

    public boolean isQualityCheck() {
        return isQualityCheck;
    }

    public void setQualityCheck(boolean qualityCheck) {
        isQualityCheck = qualityCheck;
    }

    private boolean isQualityCheck; // 是否通过质量检测
    private String isQualityDetect; // 人脸角度 模糊 光照未通过提示

    private String isQualityOcclusion; // 遮挡未通过提示

    public String getQualityDetect() {
        return isQualityDetect;
    }

    public void setQualityDetect(String qualityDetect) {
        isQualityDetect = qualityDetect;
    }

    public String getQualityOcclusion() {
        return isQualityOcclusion;
    }

    public void setQualityOcclusion(String qualityOcclusion) {
        isQualityOcclusion = qualityOcclusion;
    }

    public float[] getTrackLandmarks() {
        return trackLandmarks;
    }

    public void setTrackLandmarks(float[] trackLandmarks) {
        this.trackLandmarks = trackLandmarks;
    }

    public long getIrSilentLiveDuration() {
        return irSilentLiveDuration;
    }

    public void setIrSilentLiveDuration(long irSilentLiveDuration) {
        this.irSilentLiveDuration = irSilentLiveDuration;
    }


    public long getMultiFrameTime() {
        return multiFrameTime;
    }

    public void setMultiFrameTime(long multiFrameTime) {
        this.multiFrameTime = multiFrameTime;
    }


    public long getDarkEnhanceDuration() {
        return darkEnhanceDuration;
    }

    public void setDarkEnhanceDuration(long darkEnhance) {
        this.darkEnhanceDuration = darkEnhance;
    }

    public long getMaskScoreDuration() {
        return maskScoreDuration;
    }

    public void setMaskScoreDuration(long maskScoreDuration) {
        this.maskScoreDuration = maskScoreDuration;
    }

    public float getMaskScore() {
        return maskScore;
    }

    public void setMaskScore(float maskScore) {
        this.maskScore = maskScore;
    }

    public BDFaceImageInstance getBdFaceImageInstance() {
        return bdFaceImageInstance;
    }

    public void setBdFaceImageInstance(BDFaceImageInstance bdFaceImageInstance) {
        this.bdFaceImageInstance = bdFaceImageInstance;
    }

    public BDFaceImageInstance getBdFaceImageInstanceCrop() {
        return bdFaceImageInstanceCrop;
    }

    public void setBdFaceImageInstanceCrop(BDFaceImageInstance bdFaceImageInstance) {
        this.bdFaceImageInstanceCrop = bdFaceImageInstance;
    }

    public long getAllDetectDuration() {
        return allDetectDuration;
    }

    public void setAllDetectDuration(long allDetectDuration) {
        this.allDetectDuration = allDetectDuration;
    }


    public BDFaceImageInstance getBdNirFaceImageInstance() {
        return bdNirFaceImageInstance;
    }

    public void setBdNirFaceImageInstance(BDFaceImageInstance bdNirFaceImageInstance) {
        this.bdNirFaceImageInstance = bdNirFaceImageInstance;
    }

    public BDFaceImageInstance getBdDepthFaceImageInstance() {
        return bdDepthFaceImageInstance;
    }

    public void setBdDepthFaceImageInstance(BDFaceImageInstance bdDepthFaceImageInstance) {
        this.bdDepthFaceImageInstance = bdDepthFaceImageInstance;
    }
    public byte[] getFeature() {
        return feature;
    }

    public void setFeature(byte[] feature) {
        this.feature = feature;
    }

    public float[] getLandmarks() {
        return landmarks;
    }

    public void setLandmarks(float[] landmarks) {
        this.landmarks = landmarks;
    }

    public int[] getShape() {
        return shape;
    }

    public void setShape(int[] shape) {
        this.shape = shape;
    }


    public FaceInfo[] getTrackFaceInfo() {
        return trackFaceInfo;
    }

    public void setTrackFaceInfo(FaceInfo[] trackFaceInfo) {
        this.trackFaceInfo = trackFaceInfo;
    }


    public int getFaceDetectCode() {
        return faceDetectCode;
    }

    public void setFaceDetectCode(int faceDetectCode) {
        this.faceDetectCode = faceDetectCode;
    }

    public FaceInfo getFaceInfo() {
        return faceInfo;
    }

    public FaceInfo[] getFaceInfos() {
        return faceInfos;
    }

    public void setFaceInfo(FaceInfo faceInfo) {
        this.faceInfo = faceInfo;
    }

    public void setFaceInfos(FaceInfo[] faceInfos){
        this.faceInfos = faceInfos;
    }

    public long getRgbDetectDuration() {
        return rgbDetectDuration;
    }

    public void setRgbDetectDuration(long rgbDetectDuration) {
        this.rgbDetectDuration = rgbDetectDuration;
    }

    public long getIrDetectDuration() {
        return irDetectDuration;
    }

    public void setIrDetectDuration(long irDetectDuration) {
        this.irDetectDuration = irDetectDuration;
    }

    public long getRgbLivenessDuration() {
        return rgbLivenessDuration;
    }

    public void setRgbLivenessDuration(long rgbLivenessDuration) {
        this.rgbLivenessDuration = rgbLivenessDuration;
    }

    public float getIrLivenessScore() {
        return irLivenessScore;
    }

    public void setIrLivenessScore(float irLivenessScore) {
        this.irLivenessScore = irLivenessScore;
    }

    public long getIrLivenessDuration() {
        return irLivenessDuration;
    }

    public void setIrLivenessDuration(long irLivenessDuration) {
        this.irLivenessDuration = irLivenessDuration;
    }

    public long getDepthtLivenessDuration() {
        return depthtLivenessDuration;
    }

    public void setDepthtLivenessDuration(long depthtLivenessDuration) {
        this.depthtLivenessDuration = depthtLivenessDuration;
    }

    public float getRgbLivenessScore() {
        return rgbLivenessScore;
    }

    public float[] getRgbLivenessScores(){
        return rgbLivenessScores;
    }

    public void setRgbLivenessScore(float rgbLivenessScore) {
        this.rgbLivenessScore = rgbLivenessScore;
    }

    public void setRgbLivenessScores(float [] rgbLivenessScores){
        this.rgbLivenessScores = rgbLivenessScores;
    }

    public float getDepthLivenessScore() {
        return depthLivenessScore;
    }

    public void setDepthLivenessScore(float depthLivenessScore) {
        this.depthLivenessScore = depthLivenessScore;
    }

    public int getLiveType() {
        return liveType;
    }

    public void setLiveType(int liveType) {
        this.liveType = liveType;
    }

    public float getFeatureScore() {
        return featureScore;
    }

    public void setFeatureScore(float featureScore) {
        this.featureScore = featureScore;
    }

    public long getFeatureDuration() {
        return featureDuration;
    }

    public void setFeatureDuration(long featureDuration) {
        this.featureDuration = featureDuration;
    }

    public long getCheckDuration() {
        return checkDuration;
    }

    public void setCheckDuration(long checkDuration) {
        this.checkDuration = checkDuration;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    /**
     * 获取人脸识别结果
     * @return
     */
    public List<IdentifyResult> getIdentifyResults() {
        return identifyResults;
    }

    /**
     * 设置人脸识别结果
     * @param identifyResult
     */
    public void addIdentifyResult(final IdentifyResult identifyResult) {
        String userName = identifyResult.user.getUserName();
        int size = identifyResults.size();
        for (int i = 0; i < size; i++) {
            IdentifyResult result = identifyResults.get(i);
            if (result != null && userName.equals(result.user.getUserName())) {
                identifyResults.remove(result);
                break;
            }
        }
        this.identifyResults.add(identifyResult);
    }

    /**
     * 清除人脸识别结果
     */
    public void clearIdentifyResults(){
        identifyResults.clear();
    }

    public float getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(float featureCode) {
        this.featureCode = featureCode;
    }

    public void setMouthMaskArray(float [] mouthMaskArray){
        this.mouthMaskArray = mouthMaskArray;
    }

    public float[] getMouthMaskArray(){
        return mouthMaskArray;
    }
}


