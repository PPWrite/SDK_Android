package cn.robotpenDemo.board;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.robotpen.pen.RobotPenServiceImpl;
import cn.robotpenDemo.board.common.ResUtils;
import cn.robotpenDemo.board.connect.BleConnectActivity;
import cn.robotpenDemo.board.show.RecordBoardActivity;
import cn.robotpenDemo.board.show.WhiteBoardActivity;
import cn.robotpenDemo.board.show.WhiteBoardWithMethodActivity;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.list)
    ListView list;
    @BindView(R.id.ble_Button)
    Button bleButton;
    @BindView(R.id.activity_main)
    LinearLayout activityMain;

    List<String> itemList;

    public RobotPenServiceImpl robotPenService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        //创建文件夹

        ResUtils.isDirectory(ResUtils.DIR_NAME_BUFFER);
        ResUtils.isDirectory(ResUtils.DIR_NAME_DATA);
        ResUtils.isDirectory(ResUtils.DIR_NAME_PHOTO);
        ResUtils.isDirectory(ResUtils.DIR_NAME_VIDEO);
        itemList = new ArrayList<String>();
        itemList.add("简单白板");
        itemList.add("简单白板+常用功能");
        itemList.add("录制白板");
        ListAdapter itemAdapter = new ArrayAdapter(this,R.layout.support_simple_spinner_dropdown_item,itemList);
        list.setAdapter(itemAdapter);
        list.setOnItemClickListener(itemClickListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkSDPermission();
    }

    private void checkSDPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("")
                        .setCancelable(false)
                        .setMessage("请授予SD卡读写权限")
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finish();
                            }
                        })
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent settingIntent = new Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", getPackageName(), null)
                                );
                                startActivityForResult(settingIntent, 0xF);
                            }
                        })
                        .create().show();
            }
        } else {
            robotPenService = new RobotPenServiceImpl(this.getBaseContext());
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //权限处理
                return;
            }
            robotPenService.startRobotPenService(this.getBaseContext(), true);//true为在通知栏显示通知 false将不在通知栏显示
        }
    }


    @OnClick(R.id.ble_Button)
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ble_Button:
                Intent intent = new Intent(MainActivity.this, BleConnectActivity.class);
                startActivity(intent);
                break;
        }
    }

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Intent intent;
            switch (i){
                case 0:
                    intent = new Intent(MainActivity.this, WhiteBoardActivity.class);
                    startActivity(intent);
                    break;
                case 1:
                    intent = new Intent(MainActivity.this, WhiteBoardWithMethodActivity.class);
                    startActivity(intent);
                    break;
                case 2:
                    intent = new Intent(MainActivity.this, RecordBoardActivity.class);
                    startActivity(intent);
                    break;
            }
        }
    };

}
