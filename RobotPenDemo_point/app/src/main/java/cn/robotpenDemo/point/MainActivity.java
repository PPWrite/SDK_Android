package cn.robotpenDemo.point;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.robotpen.model.DevicePoint;
import cn.robotpen.model.entity.SettingEntity;
import cn.robotpen.model.symbol.DeviceType;
import cn.robotpen.pen.callback.RobotPenActivity;
import cn.robotpen.pen.model.AllBatteryType;
import cn.robotpen.pen.model.RemoteState;
import cn.robotpen.pen.model.RobotDevice;
import cn.robotpen.utils.log.CLog;
import cn.robotpenDemo.point.connect.BleConnectActivity;

/**
 * 建议统一继承{@line RobotPenActivity} 在父类中已经将服务的绑定和解绑进行了处理
 */
public class MainActivity extends RobotPenActivity {
    Handler mHandler;
    SettingEntity mSetting;
    @BindView(R.id.connect_deviceType)
    TextView connectDeviceType;
    @BindView(R.id.connect_deviceSize)
    TextView connectDeviceSize;
    @BindView(R.id.pen_isRoute)
    TextView penIsRoute;
    @BindView(R.id.pen_press)
    TextView penPress;
    @BindView(R.id.pen_original)
    TextView penOriginal;
    @BindView(R.id.pen_windows)
    TextView penWindows;
    @BindView(R.id.connect_offest)
    TextView connectOffest;
    @BindView(R.id.gotoBle)
    Button gotoBle;
    @BindView(R.id.activity_usb)
    LinearLayout activityUsb;
    private  int width =0;
    private  int height=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        //屏幕常亮控制
        MainActivity.this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler = new Handler();
        mSetting = new SettingEntity(this);//获取通过设置改的值例如横竖屏、压感等
        gotoBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, BleConnectActivity.class));
            }
        });

        WindowManager wm = this.getWindowManager();
        width = wm.getDefaultDisplay().getWidth();
        height = wm.getDefaultDisplay().getHeight();
    }


    @Override
    public void onStateChanged(int i, String s) {
        switch (i) {
            case RemoteState.STATE_CONNECTED:
                break;
            case RemoteState.STATE_DEVICE_INFO:
                //当出现设备切换时获取到新设备信息后执行的
                checkDeviceConn();
                break;
            case RemoteState.STATE_DISCONNECTED://设备断开
                break;
        }
    }

    @Override
    public void onLargeOffLineNoteSyncFinished(String adressHead, String adressData) {

    }

    @Override
    public void onRobotKeyEvent(int e) {
        super.onRobotKeyEvent(e);
    }

    /**
     * 当服务服务连接状态回调
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
                    //判断当前设备与笔记设备是否一致
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @OnClick(R.id.getBattery)
    void OnClick(View view){
        switch (view.getId()){
            case R.id.getBattery:
                try {
                    if(getPenServiceBinder()!=null&&getPenServiceBinder().getConnectedDevice()!=null)
                        initBatteryView(getPenServiceBinder().getRemainBatteryEM().getALLBatteryType());
                    else
                        Toast.makeText(MainActivity.this,"请先连接书写板",Toast.LENGTH_SHORT).show();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
    private void initBatteryView(AllBatteryType batteryLevel){
        Toast.makeText(MainActivity.this,"当前电量："+batteryLevel,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPenServiceError(String s) {

    }

    @Override
    public void onPenPositionChanged(int deviceType, int x, int y, int presure, byte state) {
        // state  00 离开 0x10悬空 0x11按下
        super.onPenPositionChanged(deviceType, x, y, presure, state);
        DevicePoint point = DevicePoint.obtain(deviceType, x, y, presure, state); //将传入的数据转化为点数据
//        point.setIsHorizontal(true);
        connectDeviceType.setText(point.getDeviceType().name());
        connectDeviceSize.setText(point.getWidth() + "/" + point.getHeight());
        penIsRoute.setText(String.valueOf(point.isRoute()));
        penPress.setText(point.getPressure() + "/" + point.getPressureValue());// pressure 是0-1的浮点值  value是0-1023的原始值
        penOriginal.setText(point.getOriginalX() + "/" + point.getOriginalY());
        connectOffest.setText(point.getOffsetX() + "/" + point.getOffsetY());
        penWindows.setText(point.getWindowX(width) + "/" + point.getWindowY(height));

    }

    @Override
    public void onPageInfo(int i, int i1) {

    }

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

    @Override
    public void requetMemorySizeCallBack(int size) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}