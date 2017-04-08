package cn.robotpenDemo.point;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.robotpen.pen.RobotPenServiceImpl;

/**
 * Created by wang on 2017/3/3.
 */

public class SelectMainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    List<String> itemList;
    @BindView(R.id.mainactivity_listview)
    ListView mainActivityListview;

    private RobotPenServiceImpl robotPenService;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_main);
        ButterKnife.bind(this);
        itemList = new ArrayList<String>();
        itemList.add("集成SDK方式1：onStateChanged onPenServiceError onPenPositionChanged接口demo");
        itemList.add("集成SDK方式2：init connect disconnect onPenServiceStarted onReceiveDot等接口demo");
        ListAdapter itemAdapter = new ArrayAdapter(this,R.layout.support_simple_spinner_dropdown_item,itemList);
        mainActivityListview.setAdapter(itemAdapter);
        mainActivityListview.setOnItemClickListener(this);

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
           /* robotPenService = new RobotPenServiceImpl(this.getBaseContext());
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //权限处理
                return;
            }
            robotPenService.startRobotPenService(this.getBaseContext(), true);//true为在通知栏显示通知 false将不在通知栏显示*/
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent;
        switch (position){
            case 0:
                intent = new Intent(SelectMainActivity.this, MainActivity.class);
                startActivity(intent);
                break;
            case 1:
                intent = new Intent(SelectMainActivity.this, MainTwoActivity.class);
                startActivity(intent);
                break;
        }
    }
}
