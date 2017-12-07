package cn.robotpenDemo.board.connect;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.robotpen.model.DevicePoint;
import cn.robotpen.model.entity.DeviceEntity;
import cn.robotpen.model.symbol.DeviceType;
import cn.robotpen.pen.RobotPenService;
import cn.robotpen.pen.RobotPenServiceImpl;
import cn.robotpen.pen.callback.RobotPenActivity;
import cn.robotpen.pen.model.RemoteState;
import cn.robotpen.pen.model.RobotDevice;
import cn.robotpen.pen.scan.RobotScanCallback;
import cn.robotpen.pen.service.RobotRemotePenService;
import cn.robotpen.utils.log.CLog;
import cn.robotpenDemo.board.R;

import static cn.robotpen.pen.RobotPenServiceImpl.EXTR_FROM_RECEIVER;


public class BleConnectActivity extends RobotPenActivity {

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


    private PenAdapter mPenAdapter;
    SharedPreferences lastSp;
    SharedPreferences pairedSp;
    BluetoothAdapter mBluetoothAdapter;
    ProgressDialog mProgressDialog;
    RobotDevice mRobotDevice;//连接上的设备
    String mNewVersion; //从网络获取的最新版本号
    /**
     * 上次配对信息
     */
    public static final String SP_LAST_PAIRED = "last_paired_device";
    /**
     * 记录配对信息
     */
    public static final String SP_PAIRED_DEVICE = "sp_paird";
    /**
     * 关键字
     */
    public static final String SP_PAIRED_KEY = "address";
    /**
     * 固件升级URL
     */
    public static final String FIREWARE_FILE_HOST ="https://upgrade.robotpen.cn/fw/check";
    public static final int SUCCESS = 0;
    public static final int ERRORCODE = 1;
    public static final int FAILURE = 2;
    public static final int UPDATESUCCESS = 3;
    public static final int UPDATEFAILURE = 4;
    private RobotPenService robotPenService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("test","onCreate");
        setContentView(R.layout.activity_ble_connect);
        ButterKnife.bind(this);

        mPenAdapter = new PenAdapter(BleConnectActivity.this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //获取存储存储
        lastSp = this.getSharedPreferences(SP_LAST_PAIRED, MODE_PRIVATE);
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
                    if(getPenServiceBinder() != null) {
                        if (getPenServiceBinder().getConnectedDevice() == null) {
                            Log.e("test","开始连接设备： "+addr);
                            getPenServiceBinder().connectDevice(addr);
                        } else {
                            Toast.makeText(BleConnectActivity.this, "先断开当前设备", Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        Toast.makeText(BleConnectActivity.this, "服务未启动", Toast.LENGTH_SHORT).show();
                        robotPenService = new RobotPenServiceImpl(BleConnectActivity.this);
                        try {
                            robotPenService.startRobotPenService(BleConnectActivity.this, false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (RemoteException e) {
                    Toast.makeText(BleConnectActivity.this, "连接失败，请再次点击", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        mPenAdapter.release();
    }

    @Override
    public void onUpdateFirmwareFinished() {
        super.onUpdateFirmwareFinished();
        closeProgress();
        Log.e("test","update finish");
    }

    @Override
    public void onUpdateFirmwareProgress(int progress, int total, String info) {
        super.onUpdateFirmwareProgress(progress, total, info);
        Log.e("test","progress: "+progress+"total: "+total);
    }

    @OnClick({R.id.scanBut, R.id.disconnectBut, R.id.deviceSync, R.id.deviceUpdate})
    void OnClick(View view) {
        switch (view.getId()) {
            case R.id.scanBut:
                checkPermission();
                break;
            case R.id.disconnectBut:
                Toast.makeText(BleConnectActivity.this, "连接断开", Toast.LENGTH_SHORT).show();
                try {
                    getPenServiceBinder().disconnectDevice();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                scanBut.setVisibility(View.VISIBLE);
                disconnectBut.setVisibility(View.GONE);
                deviceSync.setVisibility(View.GONE);
                deviceUpdate.setVisibility(View.GONE);
                statusText.setText("未连接设备!");
                break;
            case R.id.deviceSync:
                showProgress("同步中");
                checkStorageNoteNum(mRobotDevice);//同步笔记
                break;
            case R.id.deviceUpdate:
                showProgress("升级中");
                updateDeviceNew();
                break;
        }
    }

    /**
     * 蓝牙未开启请求
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 0xb) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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
            startScan();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
    }

    /**
     * 服务连接成功
     */
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        Log.e("test","onServiceConnected");
        checkDevice();//检测设备如果连接过则自动连接
    }

    /**
     * 检测设备连接 如果本次连接的是P1则禁止使用如果本次连接的是蓝牙设备则不处理
     * 如果本次未连接但上次已连接蓝牙设备则直接连接
     * 只有在onServiceConnected之后robotService才可以正常使用
     **/
    private void checkDevice() {
        try {
            RobotDevice robotDevice = getPenServiceBinder().getConnectedDevice(); //获取目前连接的设备
            if (robotDevice != null) {//已连接设备
                statusText.setText("已连接设备: " + robotDevice.getName());
                if (robotDevice.getDeviceVersion() == DeviceType.P1.getValue()) { //已连接设备
                    Toast.makeText(this, "请先断开USB设备再进行蓝牙设备连接", Toast.LENGTH_SHORT).show();
                    scanBut.setVisibility(View.GONE);
                } else {
                    disconnectBut.setVisibility(View.VISIBLE);
                    scanBut.setVisibility(View.GONE);
                }
            } else {
                //获取上次连接设备
                /*if (!pairedSp.getString(SP_PAIRED_KEY, "").isEmpty()) {
                    //已经连接过蓝牙设备 从pairedSp中获取
                    String laseDeviceAddress = pairedSp.getString(SP_PAIRED_KEY, "");
                    getPenServiceBinder().connectDevice(laseDeviceAddress);
                    showProgress("正在检测上次连接的设备");
                }*/
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            // callbackType：确定这个回调是如何触发的
            // result：包括4.3版本的蓝牙信息，信号强度rssi，和广播数据scanRecord
            Log.e("testBLE","5.0+ result :"+result.getDevice().getAddress());
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            // 批量回调，一般不推荐使用，使用上面那个会更灵活
        }
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            // 扫描失败，并且失败原因
            Log.e("testBLE","5.0+ errorCode :"+errorCode);
        }
    };






    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DeviceEntity deviced = new DeviceEntity(device);
                    mPenAdapter.addItem(deviced);
                    mPenAdapter.notifyDataSetChanged();
                    //在这里可以把搜索到的设备保存起来
                    Log.e("testBLE","getName  : "+deviced.getName());
                    Log.e("testBLE","getAddress  : "+deviced.getAddress());
                }
            });
        }
    };

    /**
     * 当有扫描结果时的回调
     */
    RobotScanCallback robotScanCallback = new RobotScanCallback() {
        @Override
        public void onResult(BluetoothDevice bluetoothDevice, int i, boolean b) {
            DeviceEntity device = new DeviceEntity(bluetoothDevice);
            mPenAdapter.addItem(device);
            mPenAdapter.notifyDataSetChanged();
        }

        @Override
        public void onFailed(int i) {

        }
    };

    /**
     * 开始扫描Ble设备--带过滤
     */
    public void startScan() {
//        Object callback = robotScanCallback.getScanCallback();
//        if (callback == null) {
//            return;
//        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            // 5.0+版本
            BluetoothLeScanner scaner = mBluetoothAdapter.getBluetoothLeScanner();  // android5.0把扫描方法单独弄成一个对象了
            scaner.stopScan(mScanCallback);   // 停止扫描
            scaner.startScan(mScanCallback);  // 开始扫描

//            ScanSettings settings = new ScanSettings.Builder()
//                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                    .build();
//            List<ScanFilter> filters = new ArrayList<>();
//            ScanFilter filter = new ScanFilter.Builder()
//                    .setServiceUuid(new ParcelUuid(SERVICE_UUID))
//                    .build();
//            filters.add(filter);
//            mBluetoothAdapter.getBluetoothLeScanner()
//                    .startScan(filters, settings, (ScanCallback) callback);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            mBluetoothAdapter.startLeScan(
//                    null,//new UUID[]{SERVICE_UUID},
//                    (BluetoothAdapter.LeScanCallback) callback);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }

    /**
     * 停止扫描Ble设备
     */
    public void stopScan() {
        Object callback = robotScanCallback.getScanCallback();
        if (callback == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan((ScanCallback) callback);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback) callback);
        }
    }

    /**
     * 保存设备的连接信息
     * @param device
     * @param addr
     */
    private void saveConnectInfo(RobotDevice device, String addr) {
        SharedPreferences.Editor edit = lastSp.edit().clear();
        if (!TextUtils.isEmpty(addr)) {
            pairedSp.edit()
                    .putString(SP_PAIRED_KEY, addr)
                    .apply();
            edit.putString(String.valueOf(device.getDeviceVersion()), addr);
        }
        edit.apply();
    }
    /**--------------
     * 笔迹同步部分
     -----------------*/
    /**
     * 检查存储笔记数
     */
    private void checkStorageNoteNum(RobotDevice device) {
        int num = device.getOfflineNoteNum();
        if (num > 0) {
            deviceSync.setVisibility(View.VISIBLE);
            AlertDialog.Builder alert = new AlertDialog.Builder(BleConnectActivity.this);
            alert.setTitle("提示");
            alert.setMessage("共有" + num + "条数据可以同步！");
            alert.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        getPenServiceBinder().startSyncOffLineNote();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    dialog.dismiss();
                }
            });
            alert.setNegativeButton("取消", null);
            alert.show();
        }
    }

    @Override
    public void onOffLineNoteHeadReceived(String json) {
        super.onOffLineNoteHeadReceived(json);
    }

    @Override
    public void onSyncProgress(String key, int total, int progress) {
        super.onSyncProgress(key, total, progress);

    }

    @Override
    public void onOffLineNoteSyncFinished(String json, byte[] data) {
        if (data != null && data.length >= 5) {
            int num = 0, step = 1;
            List<DevicePoint> points = new ArrayList<>();
            DeviceType type = DeviceType.toDeviceType(mRobotDevice.getDeviceVersion());
            DevicePoint point;
            for (int i = 0; i <= data.length - 5; i += step) {
                try {
                    point = new DevicePoint(type, data, i);
                    step = 5;//5字节为一个点数据
                } catch (Exception e) {
                    step = 1;//查找下一个有效字节
                    e.printStackTrace();
                    continue;
                }
                if (point.isLeave()){
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
    }

    /**--------------
     * 设备升级部分
     -----------------*/
    /**
     * 固件升级的相关回调
     */
    String newBleFirmwareUrl="";
    String newMcuFirmwareUrl="";
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SUCCESS:
                    if (mRobotDevice != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(msg.obj.toString());
                            JSONObject jsonObject1 = jsonObject.getJSONObject("data");
                            newBleFirmwareVersion = jsonObject1.getString("ble_version");
                            newBleFirmwareVersion="0."+newBleFirmwareVersion;
                            newMcuFirmwareVersion = jsonObject1.getString("mcu_version");
                            newMcuFirmwareVersion="0."+newMcuFirmwareVersion;
                            newBleFirmwareUrl = jsonObject1.getString("ble_url");
                            newMcuFirmwareUrl = jsonObject1.getString("mcu_url");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if(!newMcuFirmwareVersion.equals("0.0")) {
                            String device_mcuwareVer = mRobotDevice.getMcuFirmwareVerStr();
                            String[] tmp = device_mcuwareVer.split("\\.");
                            device_mcuwareVer = "0." + tmp[tmp.length - 1];
                            String device_blewarever = mRobotDevice.getBleFirmwareVerStr();
                            String[] tmp2 = device_blewarever.split("\\.");
                            device_blewarever = "0." + tmp2[tmp2.length - 1];
                            if (!newBleFirmwareVersion.equals(device_blewarever) || !newMcuFirmwareVersion.equals(device_mcuwareVer)) {
                                deviceUpdate.setVisibility(View.GONE);
                            } else {
                                deviceUpdate.setVisibility(View.GONE);
                            }
                        }else {
                            String device_blewarever = mRobotDevice.getBleFirmwareVerStr();
                            String[] tmp2 = device_blewarever.split("\\.");
                            device_blewarever = "0." + tmp2[tmp2.length - 1];
                            if (!newBleFirmwareVersion.equals(device_blewarever)) {
                                deviceUpdate.setVisibility(View.GONE);
                            } else {
                                deviceUpdate.setVisibility(View.GONE);
                            }
                        }
                    }
                    break;
                case FAILURE:
                    Toast.makeText(BleConnectActivity.this, "获取数据失败", Toast.LENGTH_SHORT)
                            .show();
                    break;
                case ERRORCODE:
                    Toast.makeText(BleConnectActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                    break;
                case UPDATESUCCESS:
                    if (getPenServiceBinder() != null) {
                        byte[] newFirmwareVer = (byte[]) msg.obj;
                        try {
                            getPenServiceBinder().startUpdateFirmware(mNewVersion, newFirmwareVer);
                            //升级结果可以通过RemoteCallback 进行展示
                            //此时注意观察设备为紫灯常亮，直到设备升级完毕将自动进行重启
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case UPDATEFAILURE:
                    Toast.makeText(BleConnectActivity.this, "升级失败！", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }

        ;
    };

    /**
     * 检查设备固件版本
     */
    private void checkDeviceVersion() {
        new Thread() {
            public void run() {
                int code;
                try {
                    URL url = new URL(FIREWARE_FILE_HOST + "?"+"type="+mRobotDevice.getDeviceVersion());
                    HttpURLConnection conn = (HttpURLConnection) url
                            .openConnection();
                    conn.setRequestMethod("GET");//使用GET方法获取
                    conn.setConnectTimeout(5000);
                    code = conn.getResponseCode();
                    if (code == 200) {
                        InputStream is = conn.getInputStream();
                        String result = readMyInputStream(is);
                        Message msg = new Message();
                        msg.obj = result;
                        msg.what = SUCCESS;
                        mHandler.sendMessage(msg);
                    } else {
                        Message msg = new Message();
                        msg.what = ERRORCODE;
                        mHandler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.what = FAILURE;
                    mHandler.sendMessage(msg);
                }
            }
        }.start();
    }

    /**
     * 生成ble固件升级url
     * 例如：NEBULA_BLE_0.4.bin
     *
     * @param device
     * @return
     */
    private String newBleFirmwareVersion="";
    private String newMcuFirmwareVersion="";

   private void updateDeviceNew(){
        String bleUrl = newBleFirmwareUrl;
        String mcuUrl = newMcuFirmwareUrl;
        if(!TextUtils.isEmpty(bleUrl)){
            downloadFirmwareData(bleUrl, mcuUrl);
        }
    }



    public void downloadFirmwareData(String... urls) {
        UpdateFirmwareDownloadTask firmDownloadTask = new UpdateFirmwareDownloadTask() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(List<byte[]> bytes) {
                super.onPostExecute(bytes);
                onDownFirmwareFileFinished(bytes);

            }

            @Override
            protected void onProgressUpdate(Integer... values) {

            }
        };
        firmDownloadTask.execute(urls);
    }

    public void onDownFirmwareFileFinished(List<byte[]> data) {
        Log.e("test","下载完成，开始升级～～");
        try {
            getPenServiceBinder().startUpgradeDevice(
                    newBleFirmwareVersion,
                    data.get(0),
                    newMcuFirmwareVersion,
                    data.get(1));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Stream转String
     *
     * @param is
     * @return
     */
    public static String readMyInputStream(InputStream is) {
        byte[] result;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            is.close();
            baos.close();
            result = baos.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            String errorStr = "获取数据失败。";
            return errorStr;
        }
        return new String(result);
    }


    /**
     * 显示ProgressDialog
     **/
    private void showProgress(String flag) {
        mProgressDialog = ProgressDialog.show(this, "", flag + "……", true);
    }

    /**
     * 释放progressDialog
     **/
    private void closeProgress() {
        if (mProgressDialog != null) {
            if (mProgressDialog.isShowing())
                mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    public void onStateChanged(int i, String s) {
        switch (i) {
            case RemoteState.STATE_ERROR:
                Log.w("test","STATE_ERROR");
                break;
            case RemoteState.STATE_CONNECTED:
                Log.w("test","STATE_CONNECTED");
                break;
            case RemoteState.STATE_CONNECTING:
                break;
            case RemoteState.STATE_DISCONNECTED: //设备断开
                Log.w("test","STATE_DISCONNECTED");
                closeProgress();
                statusText.setText("未连接设备！");
                scanBut.setVisibility(View.VISIBLE);
                disconnectBut.setVisibility(View.GONE);
                deviceUpdate.setVisibility(View.GONE);
                deviceSync.setVisibility(View.GONE);
                break;
            case RemoteState.STATE_DEVICE_INFO: //设备连接成功状态
                Log.w("test","STATE_DEVICE_INFO");
                try {
                    RobotDevice robotDevice = getPenServiceBinder().getConnectedDevice();
                    if (null != robotDevice) {
                        closeProgress();
                        mRobotDevice = robotDevice;
                        if (robotDevice.getDeviceVersion() > 0) {//针对固件bug进行解决 STATE_DEVICE_INFO 返回两次首次无设备信息第二次会上报设备信息
                            if(robotDevice.getName()!=null)
                                Log.w("test",""+robotDevice.getName());
                            else
                                Log.w("test","名字是空");
                            statusText.setText("已连接设备: " + robotDevice.getName());
                            if (robotDevice.getDeviceVersion() == DeviceType.P1.getValue()) { //如果连接上的是usb设备
                                Toast.makeText(BleConnectActivity.this, "请先断开USB设备再进行蓝牙设备连接", Toast.LENGTH_SHORT).show();
                                scanBut.setVisibility(View.GONE);
                                disconnectBut.setVisibility(View.GONE);
                            } else {//如果连接的是蓝牙设备
                                saveConnectInfo(robotDevice, robotDevice.getAddress());
                                scanBut.setVisibility(View.GONE);
                                disconnectBut.setVisibility(View.VISIBLE);
                                //如果有离线笔记则同步离线笔记
                                if (robotDevice.getOfflineNoteNum() > 0) {
                                    deviceSync.setVisibility(View.VISIBLE);
                                } else
                                    deviceSync.setVisibility(View.GONE);
                                //进行版本升级
                                checkDeviceVersion();
                            }
                        }
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case RemoteState.STATE_ENTER_SYNC_MODE_SUCCESS://笔记同步成功
                deviceSync.setVisibility(View.GONE);
                Toast.makeText(BleConnectActivity.this, "笔记同步成功", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private String changeMac(String mac){
        int result = 0;
        try {
            byte[] tmpArray = getMacBytes(mac);
            byte [] tmpByte = new byte[4];
            System.arraycopy(tmpArray,3,tmpByte,0,1);
            System.arraycopy(tmpArray,2,tmpByte,1,1);
            System.arraycopy(tmpArray,1,tmpByte,2,1);
            System.arraycopy(tmpArray,0,tmpByte,3,1);
            int tmpInt = bytesToInteger(tmpByte);
//            byte [] tmpByte2 = new byte[]{0x00,(byte)0x80,0x00,0x00};
//            int tmpInt2 = bytesToInteger(tmpByte2);
            result=tmpInt;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result+"";
    }

    public byte [] getMacBytes(String mac){
        byte []macBytes = new byte[6];
        String [] strArr = mac.split(":");

        for(int i = 0;i < strArr.length; i++){
            int value = Integer.parseInt(strArr[i],16);
            macBytes[i] = (byte) value;
        }
        return macBytes;
    }

    public  byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * Convert char to byte
     * @param c char
     * @return byte
     */
    private byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public int bytesToInteger(byte... data) {
        int value = 0;
        for (int i = Math.max(data.length - 4, 0); i < data.length; i++) {
            int w = ((data.length - i - 1) * 8);
            value = value | ((data[i] & 0xFF) << w);
        }
        return value;
    }

    @Override
    public void onPenServiceError(String s) {
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
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


    /**
     * 以字节为单位读取文件，常用于读二进制文件，如图片、声音、影像等文件。
     */
    public  byte[] readFileByBytes() {
        InputStream in = null;
        byte[] dataArray=null;
        try {
            // 一次读多个字节
            in = getAssets().open("T7_BLE_0.0.0.44.bin");
            dataArray = new byte[in.available()];
            Log.e("test","in.available()："+in.available());
            showAvailableBytes(in);
            // 读入多个字节到字节数组中，byteread为一次读入的字节数
            while ((in.read(dataArray)) != -1) {
//                System.out.write(tempbytes, 0, byteread);
                Log.e("test","开始读取");
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                }
            }
        }

        return dataArray;
    }


    /**
     * 以字节为单位读取文件，常用于读二进制文件，如图片、声音、影像等文件。
     */
    public  byte[] readFileByBytes(String fileName) {
        InputStream in = null;
        byte[] dataArray=null;
        try {
            // 一次读多个字节
            in = new FileInputStream(fileName);
            dataArray = new byte[in.available()];
            showAvailableBytes(in);
            // 读入多个字节到字节数组中，byteread为一次读入的字节数
            while ((in.read(dataArray)) != -1) {
//                System.out.write(tempbytes, 0, byteread);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                }
            }
        }

        return dataArray;
    }

    /**
     * 显示输入流中还剩的字节数
     */
    private  void showAvailableBytes(InputStream in) {
        try {
            System.out.println("当前字节输入流中的字节数为:" + in.available());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
