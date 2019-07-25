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


import com.tbruyelle.rxpermissions.Permission;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.robotpen.pen.RobotPenServiceImpl;
import cn.robotpen.utils.LogUtil;
import cn.robotpen.utils.screen.LogUtils;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by wang on 2017/3/3.
 */

public class SelectMainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    List<String> itemList;
    @BindView(R.id.mainactivity_listview)
    ListView mainActivityListview;
    private RxPermissions rxPermission;
    private Subscription permissionSub;

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
        rxPermission = new RxPermissions(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        checkSDPermission();
    }

    private void checkSDPermission() {
        unsubscribe(permissionSub);
        permissionSub = rxPermission.request(Manifest.permission.WRITE_EXTERNAL_STORAGE
                , Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (!aBoolean) {
                            finish();
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unsubscribe(permissionSub);
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
    /**
     * 解除注册
     *
     * @param subs Subscriptions
     */
    protected void unsubscribe(Subscription... subs) {
        for (Subscription sub : subs) {
            if (isUnsubscribed(sub)) {
                sub.unsubscribe();
            }
        }
    }
    protected boolean isUnsubscribed(Subscription sub) {
        return sub != null && !sub.isUnsubscribed();
    }
}
