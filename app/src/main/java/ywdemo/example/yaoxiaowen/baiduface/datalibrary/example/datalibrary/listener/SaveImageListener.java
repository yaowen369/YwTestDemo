package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener;

import java.util.List;

/**
 * 图片保存监听器
 * 用于监听图片保存操作的完成状态和结果
 */
public interface SaveImageListener {

    /**
     * 图片保存成功回调
     * @param savedCount 保存的图片数量
     * @param filePaths 已保存图片的完整路径列表
     * @param timestamp 保存时间戳（格式：yyyy-MM-dd_HH-mm-ss）
     */
    void onSaveSuccess(int savedCount, List<String> filePaths, String timestamp);

    /**
     * 图片保存失败回调
     * @param errorCode 错误码
     * @param errorMsg 错误信息
     */
    void onSaveFailed(int errorCode, String errorMsg);

    /**
     * 未满足保存条件回调
     * 当RGB或Depth活体得分不满足阈值时调用
     * @param rgbScore RGB活体得分
     * @param depthScore Depth活体得分
     * @param threshold 保存阈值
     */
    void onConditionNotMet(float rgbScore, float depthScore, float threshold);
}
