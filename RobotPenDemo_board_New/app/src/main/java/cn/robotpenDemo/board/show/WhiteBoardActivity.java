package cn.robotpenDemo.board.show;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.robotpen.model.DevicePoint;
import cn.robotpen.model.entity.note.NoteEntity;
import cn.robotpen.model.symbol.DeviceType;
import cn.robotpen.pen.callback.RobotPenActivity;
import cn.robotpen.pen.model.RemoteState;
import cn.robotpen.pen.model.RobotDevice;
import cn.robotpen.utils.FileUtils;
import cn.robotpen.utils.log.CLog;
import cn.robotpen.views.widget.WhiteBoardView;
import cn.robotpenDemo.board.MyApplication;
import cn.robotpenDemo.board.R;

public class WhiteBoardActivity extends RobotPenActivity
        implements WhiteBoardView.WhiteBoardInterface {//BaseConnectPenServiceActivity<PenPositionAndEventCallback>

    @BindView(R.id.clearnScreen)
    Button clearnScreen;
    @BindView(R.id.whiteBoardView)
    WhiteBoardView whiteBoardView;

    DeviceType mDeDeviceType = DeviceType.P1;//默认连接设备为P1 当与连接设备有冲突时则需要进行切换
    int isRubber;//是否是橡皮
    float mPenWeight = 2;//笔粗细
    int mPenColor = Color.BLACK;//笔颜色
    String mNoteKey = NoteEntity.KEY_NOTEKEY_TMP;
    Handler mHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_white_board);
        ButterKnife.bind(this);
        mHandler = new Handler();
        whiteBoardView.setIsTouchWrite(true);//默认用手输入
        whiteBoardView.setDaoSession(MyApplication.getInstance().getDaoSession());
        whiteBoardView.setLoadIgnorePhoto(false);
        whiteBoardView.setBgColor(Color.WHITE);
//      whiteBoardView.setIsTouchSmooth(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        whiteBoardView.initDrawArea();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (whiteBoardView != null) {
          whiteBoardView.cleanScreen();  // 清屏
            whiteBoardView.dispose();
            whiteBoardView = null;
        }
    }
    /**
     * 笔服务连接状态回调
     * 成功不成功都调用
     * @param name
     * @param service
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        checkDeviceConn();
    }

    public void checkDeviceConn() {
        if (getPenServiceBinder() != null) {
            try {
                RobotDevice device = getPenServiceBinder().getConnectedDevice();
                if (device != null) {
                    DeviceType type = DeviceType.toDeviceType(device.getDeviceVersion());
                    whiteBoardView.setIsTouchWrite(false);
                    //判断当前设备与笔记设备是否一致
                    if (whiteBoardView.getFrameSizeObject().getDeviceType() != type) {
                        mDeDeviceType = type;
                        mNoteKey = NoteEntity.KEY_NOTEKEY_TMP + "_" + mDeDeviceType.name();
                    }
                }else {
                    whiteBoardView.setIsTouchWrite(true);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }else {
            whiteBoardView.setIsTouchWrite(true);
        }
        //都需要刷新白板
        whiteBoardView.initDrawArea();
    }

    @OnClick(R.id.clearnScreen)
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.clearnScreen://                imageViewtmp.setImageBitmap(getBitmap(whiteBoardView.getDrawAreaView()));
                whiteBoardView.cleanScreen();
                break;
        }
    }

    public boolean isScreenLanscape() {
        Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation ; //获取屏幕方向
        if(ori == mConfiguration.ORIENTATION_LANDSCAPE){
            return true;//横屏
        }else if(ori == mConfiguration.ORIENTATION_PORTRAIT){
            return false;//竖屏
        }
        return false;
    }

    @Override
    public DeviceType getDeviceType() {
        return mDeDeviceType;
    }

    @Override
    public float getPenWeight() {
        return mPenWeight;
    }

    @Override
    public int getPenColor() {
        return mPenColor;
    }

    @Override
    public float getIsRubber() {
        return isRubber;
    }

    @Override
    public boolean getIsPressure() {
        return true;
    }

    @Override
    public boolean getIsHorizontal() {
        return isScreenLanscape();
    }

    @Override
    public long getCurrUserId() {
        return 0;
    }

    @Override
    public String getNoteKey() {
        return mNoteKey;
    }

    @Override
    public String getNewNoteName() { //修改右下角页码名称
        return "123";
    }

    Bitmap bitmap;
    @Override
    public boolean onEvent(WhiteBoardView.BoardEvent boardEvent, Object o) {
        switch (boardEvent) {
            case BOARD_AREA_COMPLETE: //白板区域加载完成
                whiteBoardView.beginBlock();
                break;
            case ERROR_DEVICE_TYPE: //检测到连接设备更换
                break;
            case ERROR_SCENE_TYPE: //横竖屏更换
                break;
        }
        return true;
    }

    @Override
    public boolean onMessage(String s, Object o) {
        return false;
    }

    @Override
    public void onPageInfoUpdated(String s) {

    }


    @Override
    public void onStateChanged(int i, String s) {
        switch (i) {
            case RemoteState.STATE_CONNECTED:
                break;
            case RemoteState.STATE_DEVICE_INFO: //当出现设备切换时获取到新设备信息后执行的
                whiteBoardView.setIsTouchWrite(false);// 设备连接成功，改为用笔输入
                checkDeviceConn();
                break;
            case RemoteState.STATE_DISCONNECTED://设备断开
                whiteBoardView.setIsTouchWrite(true);// 设备断开，允许用手输入
                break;
        }
    }

    @Override
    public void onPenServiceError(String s) {

    }


    @Override
    public void onPenPositionChanged(int deviceType, int x, int y, int presure, byte state) {
        // state  00 离开 0x10悬空 0x11按下
        super.onPenPositionChanged(deviceType, x, y, presure, state);
//        CLog.w(String.format("the xc:%d --->x is : %d ----->y is :%d ---->the pressure:%d-----> the state:%s", deviceType, x, y, presure, String.valueOf(state)));
        //TEST 测试数据
        if(isRubber==0) {// isRubber==0  现在没用橡皮察 止选择橡皮擦的时候，不小心触碰笔，绘制笔迹。
            DevicePoint p = DevicePoint.obtain(deviceType, x, y, presure, state);
//            whiteBoardView.drawLine(p);//白板的绘制必须手动执行
            DeviceType type = DeviceType.toDeviceType(deviceType);
            whiteBoardView.drawDevicePoint(type,x,y,presure,state);
        }
    }


    // 上报笔记页码信息： currentPage 当前页码， totalPage 总页码。
    @Override
    public void onPageInfo(int currentPage, int totalPage) {

    }

    // 上报插入页信息： pageNumber 当前页码， category 当前页码所属的笔记。
    @Override
    public void onPageNumberAndCategory(int pageNumber, int category) {
        CLog.d("插入页码："+pageNumber+" 插入的页码类别："+category);
    }

    @Override
    public void onSupportPenPressureCheck(boolean flag) {

    }

    @Override
    public void onCheckPressureing() {

    }

    @Override
    public void onCheckPressurePen() {

    }

    @Override
    public void onCheckPressureFinish(int flag) {

    }

    @Override
    public void onCheckModuleUpdate() {

    }

    @Override
    public void onCheckModuleUpdateFinish(byte[] data) {

    }

    Bitmap bmp;
    public Bitmap getBitmap( View view){
        if(bmp!=null){
            bmp.recycle();
            bmp=null;
        }
        bmp= Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bmp);
        view.draw(canvas);
        return bmp;
    }
}
