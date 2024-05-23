package ywdemo.example.yaoxiaowen;

import androidx.fragment.app.Fragment;

public interface IModuleInterface {
    // App启动 Application初始化时， 回调各个业务线的
    void onInit();

    String getTag();
    String getName();
    // 业务线id
    String getBizId();
    // 得到首页的Fragment
    Fragment getHomeFragment();
    // 得到 订单详情页的 Fragment
    String getOrderRouter();

    // 是否全屏幕
    boolean isFullScreen();


    // 首页选择城市的回调
    void onHomeCityChange(Object cityModel);
    // 定位成功的回调
    void onLocationSuccessListener(Object caocaoAddressInfo);

    void onLogin();
    void onLogout();
}
