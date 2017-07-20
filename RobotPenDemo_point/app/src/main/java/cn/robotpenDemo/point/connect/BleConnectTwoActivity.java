package cn.robotpenDemo.point.connect;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.codingmaster.slib.S;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.robotpen.model.DevicePoint;
import cn.robotpen.model.entity.DeviceEntity;
import cn.robotpen.model.symbol.DeviceType;
import cn.robotpen.pen.adapter.RobotPenAdapter;
import cn.robotpen.pen.callback.RobotPenActivity;
import cn.robotpen.pen.model.RobotDevice;
import cn.robotpen.pen.scan.RobotScanCallback;
import cn.robotpen.pen.scan.RobotScannerCompat;
import cn.robotpenDemo.point.BaseTwoActivity;
import cn.robotpenDemo.point.MainTwoActivity;
import cn.robotpenDemo.point.R;

/**
 * Created by wang on 2017/3/3.
 */

public class BleConnectTwoActivity extends BaseTwoActivity {
    private final UUID SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    @BindView(R.id.statusText)
    TextView statusText;
    @BindView(R.id.listview)
    ListView listview;
    @BindView(R.id.scanBut)
    Button scanBut;
    @BindView(R.id.disconnectBut)
    Button disconnectBut;
    @BindView(R.id.listFrame)
    LinearLayout listFrame;
    @BindView(R.id.deviceUpdate)
    Button deviceUpdate;
    @BindView(R.id.deviceSync)
    Button deviceSync;

    boolean isConnected = false;
    private PenAdapter mPenAdapter;
    //    SharedPreferences lastSp;
    SharedPreferences pairedSp;
    ProgressDialog mProgressDialog;
    RobotDevice mRobotDevice;//连接上的设备
    String mNewVersion; //从网络获取的最新版本号
    /**
     * 上次配对信息
     */
    public static final String SP_LAST_PAIRED = "last_paired_device";
    /**
     * 记录配对信息0
     */
    public static final String SP_PAIRED_DEVICE = "sp_paird";
    /**
     * 关键字
     */
    public static final String SP_PAIRED_KEY = "address";
    /**
     * 固件升级URL
     */
    public static final String FIREWARE_FILE_HOST = "http://dl.robotpen.cn/fw/";
    public static final int SUCCESS = 0;
    public static final int ERRORCODE = 1;
    public static final int FAILURE = 2;
    public static final int UPDATESUCCESS = 3;
    public static final int UPDATEFAILURE = 4;
    /**
     * 当有扫描结果时的回调
     */
    RobotScannerCompat robotScannerCompat;
    RobotScanCallback scanCallback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_connect);
        ButterKnife.bind(this);
        mPenAdapter = new PenAdapter(BleConnectTwoActivity.this);
        //获取存储存储
//      lastSp = this.getSharedPreferences(SP_LAST_PAIRED, MODE_PRIVATE);
        pairedSp = this.getSharedPreferences(SP_PAIRED_DEVICE, MODE_PRIVATE);

        listview.setAdapter(mPenAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int index, long arg3) {
                //停止搜索
                stopScan();
                DeviceEntity device = mPenAdapter.getItem(index);
                String addr = device.getAddress();
                try {
                    Log.e("test","开始连接："+addr);
                    adapter.connect(addr);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });
        scanCallback = new BleConnectTwoActivity.MyScanCallback(this);
        robotScannerCompat = new RobotScannerCompat(scanCallback);
        isConnected=false;

    }

    @Override
    public void onPenServiceStarted() {
        super.onPenServiceStarted();
        initSuccess();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }



    private void initSuccess() {
        try {
            if(adapter.getRobotServiceBinder()!=null&&adapter.getRobotServiceBinder().getConnectedDevice()!=null){
                isConnected=true;
                statusText.setText("已连接设备: " + adapter.getRobotServiceBinder().getConnectedDevice().getName());
                disconnectBut.setVisibility(View.VISIBLE);
                scanBut.setVisibility(View.GONE);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @OnClick({R.id.scanBut, R.id.disconnectBut, R.id.deviceSync, R.id.deviceUpdate})
    void OnClick(View view) {
        switch (view.getId()) {
            case R.id.scanBut:
                checkPermission();
                break;
            case R.id.disconnectBut:
                try {
                    adapter.disConnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.deviceSync:
//                    adapter.startSyncOffLineNote();
                try {
                    if(adapter.getRobotServiceBinder()!=null&&adapter.getRobotServiceBinder().getConnectedDevice()!=null){
                        int num = adapter.getRobotServiceBinder().getConnectedDevice().getOfflineNoteNum();
                        if (num > 0) {
                            deviceSync.setVisibility(View.VISIBLE);
                            AlertDialog.Builder alert = new AlertDialog.Builder(BleConnectTwoActivity.this);
                            alert.setTitle("提示");
                            alert.setMessage("共有" + num + "条数据可以同步！");
                            alert.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try{
                                        adapter.startSyncOffLineNote();
                                    }catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    dialog.dismiss();
                                }
                            });
                            alert.setNegativeButton("取消", null);
                            alert.show();
                        }else {
                            Toast.makeText(this, "无离线笔记", Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        Toast.makeText(this, "请先连接蓝牙", Toast.LENGTH_SHORT).show();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    /**--------------
     * 设备连接部分
     -----------------*/
    /**
     * 校验蓝牙是否打开
     * 6.0以上使用蓝牙的相关权限是否具备
     * ACCESS_COARSE_LOCATION 必须校验
     */
    public void checkPermission() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "对不起，您的设备不支持蓝牙,即将退出", Toast.LENGTH_SHORT).show();
            finish();
        } else if (!mBluetoothAdapter.isEnabled()) {//蓝牙未开启
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, 0xb);
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mPenAdapter.clearItems();
            mPenAdapter.notifyDataSetChanged();
            S.i("");
            startScan();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
    }

    @Override
    public void onConnected(int i) {
        Log.e("test","onConnected");
        isConnected=true;
        mHandler.sendEmptyMessage(0x1001);
    }

    @Override
    public void onDisconnected() {
        Log.e("test","onDisconnected");
        isConnected=false;
        mHandler.sendEmptyMessage(0x1000);
    }

    @Override
    public void onConnectFailed(int i) {
        super.onConnectFailed(i);
        Log.e("test","onConnectFailed: "+i);
    }

    @Override
    public void onOfflineDataReceived(String s, boolean b) {
        super.onOfflineDataReceived(s, b);
        Log.e("test","onOfflineDataReceived s:"+s +"     b :"+b);
    }

    @Override
    public void onOffLineNoteSyncFinished(String json, byte[] data) {
        super.onOffLineNoteSyncFinished(json, data);
        if (data != null && data.length >= 5) {
            int num = 0, step = 1;
            List<DevicePoint> points = new ArrayList<>();
            DeviceType type = DeviceType.toDeviceType(mRobotDevice.getDeviceVersion());
            DevicePoint point;
            for (int i = 0; i <= data.length - 5; i += step) {
                try {
                    point = new DevicePoint(type, data, i);
                    Log.e("test","x:"+point.getOriginalX()+" y:"+point.getOriginalY()+" pressure :"+point.getPressureValue());
                    step = 5;//5字节为一个点数据
                } catch (Exception e) {
                    step = 1;//查找下一个有效字节
                    e.printStackTrace();
                    continue;
                }

                if (point.isLeave()) {
                    //结束点
                    num++;
                    Log.v("Sync", String.format("第%d笔,共%d个点", num, points.size()));
                    points.clear();
                } else {
                    points.add(point);
                }
            }
            Toast.makeText(this, "共计同步了 " + num + " 笔数据", Toast.LENGTH_SHORT).show();
        }
        Log.e("test","onOffLineNoteSyncFinished");
    }

    @Override
    public void onOfflienSyncProgress(String key, int total, int progress) {
        super.onOfflienSyncProgress(key, total, progress);
        Log.e("test","onOfflienSyncProgress  key:"+key+" total: "+total+" progress:"+progress);
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0x1000:
                    scanBut.setVisibility(View.VISIBLE);
                    disconnectBut.setVisibility(View.GONE);
                    statusText.setText("未连接设备!");
                    break;
                case 0x1001:
                    disconnectBut.setVisibility(View.VISIBLE);
                    scanBut.setVisibility(View.GONE);
                    try {
                        if(adapter.getRobotServiceBinder()!=null&&adapter.getRobotServiceBinder().getConnectedDevice()!=null){
                            statusText.setText("已连接设备: " + adapter.getRobotServiceBinder().getConnectedDevice().getName());
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isConnected=false;
    }

    public void addRobotDevice2list(BluetoothDevice bluetoothDevice) {
        DeviceEntity device = new DeviceEntity(bluetoothDevice);
        mPenAdapter.addItem(device);
        mPenAdapter.notifyDataSetChanged();
    }

    static class MyScanCallback extends RobotScanCallback {
        WeakReference<BleConnectTwoActivity> act;

        public MyScanCallback(BleConnectTwoActivity a) {
            act = new WeakReference<BleConnectTwoActivity>(a);
        }

        @Override
        public void onResult(BluetoothDevice bluetoothDevice, int i, boolean b) {
            BleConnectTwoActivity myact=act.get();
            if(myact!=null) {
                myact.addRobotDevice2list(bluetoothDevice);
            }
        }

        @Override
        public void onFailed(int i) {

        }
    }

    /**
     * 开始扫描Ble设备--带过滤
     */
    public void startScan() {
        S.i("");
        robotScannerCompat.startScan();
    }

    /**
     * 停止扫描Ble设备
     */
    public void stopScan() {
        robotScannerCompat.stopScan();
    }

    @Override
    public void onBackPressed() {
       this.finish();
    }


}
