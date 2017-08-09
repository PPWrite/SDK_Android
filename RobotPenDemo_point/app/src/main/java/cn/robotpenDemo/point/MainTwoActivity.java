package cn.robotpenDemo.point;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.codingmaster.slib.S;

import java.text.DecimalFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.robotpen.model.DevicePoint;
import cn.robotpen.model.entity.SettingEntity;
import cn.robotpen.pen.adapter.RobotPenAdapter;
import cn.robotpen.pen.model.AllBatteryType;
import cn.robotpen.pen.model.Battery;
import cn.robotpenDemo.point.connect.BleConnectTwoActivity;

/**
 * Created by wang on 2017/3/3.
 */


public class MainTwoActivity extends BaseTwoActivity {

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        S.init(true,1,"S_LOG");

        mSetting = new SettingEntity(this);//获取通过设置改的值例如横竖屏、压感等
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SD card read/write permission deny!", Toast.LENGTH_SHORT).show();
            return;
        }
        gotoBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivity(new Intent(MainTwoActivity.this, BleConnectTwoActivity.class));
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPenServiceStarted() {
        super.onPenServiceStarted();
//            S.i("");
    }

    private int deviceType=0;

    @Override
    public void onConnected(int penType) {
        super.onConnected(penType);
        S.i(penType);
        this.deviceType=penType;
    }

    @Override
    public void onConnectFailed(int reasonCode) {
        super.onConnectFailed(reasonCode);
        deviceType=0;
        S.i(reasonCode);
    }



    @Override
    public void onReceiveDot(long timestamp, int x, int y, int pressure, int state) {
        super.onReceiveDot(timestamp,x,y,pressure,state);
        Log.e("test","x: "+x+"  y:"+y);
        S.i(x, y);
        DevicePoint point = DevicePoint.obtain(deviceType, x, y, pressure, (byte)state); //将传入的数据转化为点数据
        /**
         *DevicePoint 将会获取更多的信息
         *
         **/
        connectDeviceType.setText(point.getDeviceType().name());
        connectDeviceSize.setText(point.getWidth() + "/" + point.getHeight());

        penIsRoute.setText(String.valueOf(point.isRoute()));
        penPress.setText(point.getPressure() + "/" + point.getPressureValue());
        penOriginal.setText(point.getOriginalX() + "/" + point.getOriginalY());
        connectOffest.setText(point.getOffsetX() + "/" + point.getOffsetY());
        penWindows.setText(point.getWindowX(0) + "/" + point.getWindowY(0));
    }

    @OnClick(R.id.getBattery)
    void OnClick(View view){
        switch (view.getId()){
            case R.id.getBattery:
                if(adapter!=null&&deviceType!=0)
                    initBatteryView(adapter.getRemainBatteryEM().getALLBatteryType());
                else
                    Toast.makeText(MainTwoActivity.this,"请先连接书写板",Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void initBatteryView(AllBatteryType batteryLevel){
        Toast.makeText(MainTwoActivity.this,""+batteryLevel,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnected() {
        deviceType=0;
        S.i("");
    }

    @Override
    public void onMemoryFillLevel(int percent) {

    }

    @Override
    public void onRemainBattery(int percent) {
        Log.e("test","电池电量："+percent);
    }

    @Override
    public void onOfflineDataReceived(String event, boolean completed) {
        S.i(event);
    }
}
