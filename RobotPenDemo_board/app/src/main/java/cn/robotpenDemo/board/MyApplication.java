package cn.robotpenDemo.board;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.StrictMode;

import cn.robotpen.model.db.DBConfig;
import cn.robotpen.model.db.DaoMaster;
import cn.robotpen.model.db.DaoSession;
import cn.robotpen.pen.RobotPenService;

/**
 * Created by dadou on 2017/1/20.
 */
public class MyApplication extends Application {

    public RobotPenService robotPenService;
    private static MyApplication instance = null;
    private DaoSession daoSession;

    public static MyApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

      /*  robotPenService = new RobotPenServiceImpl(this.getBaseContext());
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //权限处理
            return;
        }
        robotPenService.startRobotPenService(this.getBaseContext(), true);//true为在通知栏显示通知 false将不在通知栏显示*/
    }

    /**
     * 获取笔服务
     * @return
     */
    public RobotPenService getRobotPenService() {
        return this.robotPenService;
    }
    /*
     * 统一创建session
     * @return
     */
    public DaoSession getDaoSession(){
        if(null==daoSession){
            SQLiteDatabase db = new DaoMaster.DevOpenHelper(instance, DBConfig.DB_NAME).getWritableDatabase();
            this.daoSession = new DaoMaster(db).newSession();
        }
        return daoSession;
    }

    @Override
    public void onTerminate() {
        // 程序终止的时候执行
        super.onTerminate();
    }
}
