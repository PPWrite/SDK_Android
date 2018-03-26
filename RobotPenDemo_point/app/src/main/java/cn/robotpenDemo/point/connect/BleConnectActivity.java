package cn.robotpenDemo.point.connect;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import java.lang.ref.WeakReference;
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
import cn.robotpen.pen.adapter.RobotPenAdapter;
import cn.robotpen.pen.callback.RobotPenActivity;
import cn.robotpen.pen.model.RemoteState;
import cn.robotpen.pen.model.RobotDevice;
import cn.robotpen.pen.scan.RobotScanCallback;
import cn.robotpen.pen.scan.RobotScannerCompat;
import cn.robotpen.utils.log.CLog;
import cn.robotpenDemo.point.BaseTwoActivity;
import cn.robotpenDemo.point.R;


public class BleConnectActivity extends RobotPenActivity{

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
    SharedPreferences pairedSp;
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
    /**
     * 当有扫描结果时的回调
     */
    RobotScannerCompat robotScannerCompat;
    RobotScanCallback scanCallback;
    private RobotPenService robotPenService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_connect);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   //应用运行时，保持屏幕高亮，不锁屏
        ButterKnife.bind(this);
        mPenAdapter = new PenAdapter();
        //获取存储存储
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
                    if (getPenServiceBinder() != null) {
                        if (getPenServiceBinder().getConnectedDevice() == null) {
                            Log.e("test","开始链接:"+addr);
                            getPenServiceBinder().connectDevice(addr);//通过监听获取连接状态
                        } else {
                            Toast.makeText(BleConnectActivity.this, "先断开当前设备", Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        Toast.makeText(BleConnectActivity.this, "服务未启动", Toast.LENGTH_SHORT).show();
                    }
                } catch(RemoteException e){
                    e.printStackTrace();
                }
            }
        });
        scanCallback = new MyScanCallback(this);
        robotScannerCompat = new RobotScannerCompat(scanCallback);
    }

    public void addRobotDevice2list(BluetoothDevice bluetoothDevice) {
        DeviceEntity device = new DeviceEntity(bluetoothDevice);
        mPenAdapter.addItem(device);
        mPenAdapter.notifyDataSetChanged();
    }

    /**
     * @param progress
     * @param total
     * @param info
     */
    @Override
    public void onUpdateFirmwareProgress(int progress, int total, String info) {
        super.onUpdateFirmwareProgress(progress, total, info);
        Log.e("test",""+progress + " "+total);
//        deviceSync.setText(("升级进度"+progress/total)+"");

    }

    @Override
    protected void onDestroy() {
        stopScan();
        robotScannerCompat = null;
        super.onDestroy();
    }

    @OnClick({R.id.scanBut, R.id.disconnectBut, R.id.deviceSync, R.id.deviceUpdate})
    void OnClick(View view) {
        switch (view.getId()) {
            case R.id.scanBut:
                checkPermission();
                break;
            case R.id.disconnectBut:
                try {
                    getPenServiceBinder().disconnectDevice();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                scanBut.setVisibility(View.VISIBLE);
                disconnectBut.setVisibility(View.GONE);
                statusText.setText("未连接设备!");
                break;
            case R.id.deviceSync:
                showProgress("同步中");
                checkStorageNoteNum(mRobotDevice);//同步笔记
                break;
            case R.id.deviceUpdate:
                showProgress("升级中");
//                updateDeviceNew();
                byte[] bletmp =  readBLEFileByBytes();
                byte[] mcutmp =  readMCUFileByBytes();
                try {
                    getPenServiceBinder().startUpgradeDevice("0.29", bletmp,"0.29",mcutmp);//newBleFirmwareVersion
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    /**
     * 以字节为单位读取文件，常用于读二进制文件，如图片、声音、影像等文件。
     */
    public  byte[] readMCUFileByBytes() {
        InputStream in = null;
        byte[] dataArray=null;
        try {
            // 一次读多个字节
            in = getAssets().open("X8E-A5_MCU_0.29.bin");
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
    public  byte[] readBLEFileByBytes() {
        InputStream in = null;
        byte[] dataArray=null;
        try {
            // 一次读多个字节
            in = getAssets().open("X8E-A5_BLE_0.29.bin");
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

    /**
     * 蓝牙未开启请求
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 0xb) {
        }
    }

    @Override
    public void onUpdateFirmwareFinished() {
        deviceUpdate.setVisibility(View.GONE);
        Toast.makeText(BleConnectActivity.this, "固件升级完毕", Toast.LENGTH_SHORT).show();

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
            startScan();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
    }

    /**
     * 服务连接回调
     */
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        Log.e("test"," onServiceConnected ");
        checkDevice();//检测设备如果连接过则自动连接
    }

    /**
     * 检测设备连接 如果本次连接的是P1则禁止使用如果本次连接的是蓝牙设备则不处理
     * 如果本次未连接但上次已连接蓝牙设备则直接连接
     * 只有在onServiceConnected之后robotService才可以正常使用
     **/
    private void checkDevice() {
        try {
            if(getPenServiceBinder()!=null) {
                RobotDevice robotDevice = getPenServiceBinder().getConnectedDevice(); //获取目前连接的设备
                if (robotDevice != null) {//已连接设备
                    if(robotDevice.getName()!=null)
                        Log.w("test",""+robotDevice.getName());
                    else
                        Log.w("test","名字是空");
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
//                if (!pairedSp.getString(SP_PAIRED_KEY, "").isEmpty()) {
//                    //已经连接过蓝牙设备 从pairedSp中获取
//                    String laseDeviceAddress = pairedSp.getString(SP_PAIRED_KEY, "");
//                    getPenServiceBinder().connectDevice(laseDeviceAddress);
//                    showProgress("正在检测上次连接的设备");
//                }
                }
            }else {
                Toast.makeText(BleConnectActivity.this, "服务未启动", Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * 开始扫描Ble设备--带过滤
     */
    public void startScan() {
        robotScannerCompat.startScan();
    }

    /**
     * 停止扫描Ble设备
     */
    public void stopScan() {
        robotScannerCompat.stopScan();
    }

    /**
     * 保存设备的连接信息
     *
     * @param addr
     */
    private void saveConnectInfo( String addr) {
        if (!TextUtils.isEmpty(addr)) {
            pairedSp.edit()
                    .putString(SP_PAIRED_KEY, addr)
                    .apply();
        }
    }
    /**--------------
     * 笔迹同步部分
     -----------------*/
    /**
     * 检查存储笔记数
     */
    private void checkStorageNoteNum(RobotDevice device) {
        if(device!=null) {
            int num = device.getOfflineNoteNum();
            if (num > 0) {
                deviceSync.setVisibility(View.VISIBLE);
                AlertDialog.Builder alert = new AlertDialog.Builder(BleConnectActivity.this);
                alert.setTitle("提示");
                alert.setMessage("共有" + num + "条数据可以同步！");
                alert.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try{
                            getPenServiceBinder().startSyncOffLineNote();
                        }catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                    }
                });
                alert.setNegativeButton("取消", null);
                alert.show();
            }
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
    }

    /**--------------
     * 设备升级部分
     -----------------*/
    /**
     * 固件升级的相关回调
     */
    private String newBleFirmwareVersion;
    private String newMcuFirmwareVersion;
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
                                deviceUpdate.setVisibility(View.VISIBLE);
                            } else {
                                deviceUpdate.setVisibility(View.VISIBLE);
                            }
                        }else {
                            String device_blewarever = mRobotDevice.getBleFirmwareVerStr();
                            Log.e("test","device_blewarever :"+device_blewarever);
                            String[] tmp2 = device_blewarever.split("\\.");
                            device_blewarever = "0." + tmp2[tmp2.length - 1];
                            if (!newBleFirmwareVersion.equals(device_blewarever)) {
                                deviceUpdate.setVisibility(View.VISIBLE);
                            } else {
                                deviceUpdate.setVisibility(View.VISIBLE);
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
        try {
            if (data.size() == 2) {
                getPenServiceBinder().startUpgradeDevice(
                        newBleFirmwareVersion,
                        data.get(0),
                        newMcuFirmwareVersion,
                        data.get(1));
            } else {
                getPenServiceBinder().startUpdateFirmware(newBleFirmwareVersion, data.get(0));
            }

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
        mProgressDialog.setCanceledOnTouchOutside(true);
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
                Log.i("test","STATE_ERROR");
                break;
            case RemoteState.STATE_CONNECTED:
                Log.i("test","STATE_CONNECTED");
                break;
            case RemoteState.STATE_CONNECTING:
                break;
            case RemoteState.STATE_DISCONNECTED: //设备断开
                Log.i("test","STATE_DISCONNECTED");
                closeProgress();
                statusText.setText("未连接设备！");
                scanBut.setVisibility(View.VISIBLE);
                disconnectBut.setVisibility(View.GONE);
                break;
            case RemoteState.STATE_DEVICE_INFO: //设备连接成功状态
                try {
                    Log.i("test","STATE_DEVICE_INFO");
                    mPenAdapter.clearItems();
                    mPenAdapter.notifyDataSetChanged();
                    RobotDevice robotDevice = getPenServiceBinder().getConnectedDevice();

                    String device_blewarever = robotDevice.getBleFirmwareVerStr();
                    Log.e("test","device_blewarever :"+device_blewarever);
                    if (null != robotDevice) {
                        closeProgress();
                        mRobotDevice = robotDevice;
                        if (robotDevice.getDeviceVersion() > 0) {//针对固件bug进行解决 STATE_DEVICE_INFO 返回两次首次无设备信息第二次会上报设备信息
                            statusText.setText("已连接设备: " + robotDevice.getName());
                            if (robotDevice.getDeviceVersion() == DeviceType.P1.getValue()) { //如果连接上的是usb设备
                                Toast.makeText(BleConnectActivity.this, "请先断开USB设备再进行蓝牙设备连接", Toast.LENGTH_SHORT).show();
                                scanBut.setVisibility(View.GONE);
                                disconnectBut.setVisibility(View.GONE);
                            } else {//如果连接的是蓝牙设备
                                saveConnectInfo(robotDevice.getAddress());
                                scanBut.setVisibility(View.GONE);
                                disconnectBut.setVisibility(View.VISIBLE);
                                //如果有离线笔记则同步离线笔记
                                //checkStorageNoteNum(robotDevice);
                                if (robotDevice.getOfflineNoteNum() > 0) {
                                    deviceSync.setVisibility(View.VISIBLE);
                                } else
                                    deviceSync.setVisibility(View.VISIBLE);
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

    @Override
    public void onPenServiceError(String s) {
        Toast.makeText(BleConnectActivity.this,s,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPageInfo(int i, int i1) {

    }

    @Override
    public void onPageNumberAndCategory(int pageNumber, int category) {
        CLog.d("插入页码："+pageNumber+" 插入的页码类别："+category);
    }


    static class MyScanCallback extends RobotScanCallback {
        WeakReference<BleConnectActivity> act;

        public MyScanCallback(BleConnectActivity a) {
            act = new WeakReference<BleConnectActivity>(a);
        }

        @Override
        public void onResult(BluetoothDevice bluetoothDevice, int i, boolean b) {
            BleConnectActivity myact=act.get();
            if(myact!=null) {
                myact.addRobotDevice2list(bluetoothDevice);
            }
        }

        @Override
        public void onFailed(int i) {

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeProgress();
    }
}
