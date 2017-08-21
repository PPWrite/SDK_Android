package cn.robotpenDemo.point;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

import cn.robotpen.pen.adapter.OnPenConnectListener;
import cn.robotpen.pen.adapter.RobotPenAdapter;
import cn.robotpenDemo.point.connect.BleConnectTwoActivity;
import cn.robotpenDemo.point.connect.BytesHelper;

/**
 * Created by wang on 2017/3/3.
 */

public class BaseTwoActivity extends AppCompatActivity implements OnPenConnectListener<String> {

    public RobotPenAdapter<BaseTwoActivity, String> adapter;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressDialog=new ProgressDialog(BaseTwoActivity.this);
        progressDialog.setMessage("正在初始化");
        progressDialog.show();
        try {
            adapter = new RobotPenAdapter<BaseTwoActivity, String>(this, this) {
                @Override
                public void onPageNumberAndCategory(int pageNumber, int category) {

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
                public void onCheckModuleUpdateFinish(byte[] data){

                }

                @Override
                protected String convert(byte[] bytes) {
                        return new BytesHelper().bytes2Str(bytes);
                }
            };
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        boolean result = adapter.init(null);
        if(!result){
            Toast.makeText(BaseTwoActivity.this,"初始化失败",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(progressDialog!=null){
            progressDialog.dismiss();
            progressDialog=null;
        }
        adapter.release();
    }


    @Override
    public void onPenServiceStarted() {
        if(progressDialog!=null){
            progressDialog.dismiss();
            progressDialog=null;
        }
    }

    @Override
    public void onConnected(int i) {

    }

    @Override
    public void onConnectFailed(int i) {

    }

    @Override
    public void onReceiveDot(long l, int i, int i1, int i2, int i3) {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onMemoryFillLevel(int i) {

    }

    @Override
    public void onRemainBattery(int i) {
        Log.e("test","电池电量："+i);
    }

    @Override
    public void onOfflineDataReceived(String s, boolean b) {

    }


    @Override
    public void onOfflineSyncStart(String head) {

    }

    @Override
    public void onOfflienSyncProgress(String key, int total, int progress) {

    }

    @Override
    public void onOffLineNoteSyncFinished(String json, byte[] data) {

    }


}
