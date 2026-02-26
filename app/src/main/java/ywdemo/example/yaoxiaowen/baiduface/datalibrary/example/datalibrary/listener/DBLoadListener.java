package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener;



import java.util.List;

import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.User;

public interface DBLoadListener {

    void onStart(int successCount);

    void onLoad(int finishCount, int successCount, float progress);

    void onComplete(List<User> features , int successCount);

    void onFail(int finishCount, int successCount, List<User> features);
}
