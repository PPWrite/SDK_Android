package cn.robotpenDemo.point;

import android.app.Application;

import cn.robotpen.pen.RobotPenService;

/**
 * Created by dadou on 2017/1/20.
 */

public class MyApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTerminate() {
        // 程序终止的时候执行
        super.onTerminate();
    }

}
