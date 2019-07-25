package cn.robotpenDemo.board;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.tbruyelle.rxpermissions.RxPermissions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.robotpenDemo.board.common.ResUtils;
import cn.robotpenDemo.board.connect.BleConnectActivity;
import cn.robotpenDemo.board.show.RecordBoardActivity;
import cn.robotpenDemo.board.show.WhiteBoardActivity;
import cn.robotpenDemo.board.show.WhiteBoardWithMethodActivity;
import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.list)
    ListView list;
    @BindView(R.id.ble_Button)
    Button bleButton;
    @BindView(R.id.activity_main)
    LinearLayout activityMain;

    List<String> itemList;
    private RxPermissions rxPermission;
    private Subscription permissionSub;

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
