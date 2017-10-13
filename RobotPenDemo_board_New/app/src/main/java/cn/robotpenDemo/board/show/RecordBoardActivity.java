package cn.robotpenDemo.board.show;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.robotpen.model.DevicePoint;
import cn.robotpen.model.entity.SettingEntity;
import cn.robotpen.model.entity.note.NoteEntity;
import cn.robotpen.model.entity.note.TrailsEntity;
import cn.robotpen.model.symbol.DeviceType;
import cn.robotpen.model.symbol.RecordState;
import cn.robotpen.pen.callback.RobotPenActivity;
import cn.robotpen.pen.model.RemoteState;
import cn.robotpen.pen.model.RobotDevice;
import cn.robotpen.record.widget.RecordBoardView;
import cn.robotpen.utils.log.CLog;
import cn.robotpen.views.module.NoteManageModule;
import cn.robotpen.views.widget.WhiteBoardView;
import cn.robotpenDemo.board.MyApplication;
import cn.robotpenDemo.board.R;
import cn.robotpenDemo.board.common.ResUtils;

public class RecordBoardActivity extends RobotPenActivity
        implements WhiteBoardView.WhiteBoardInterface,
        RecordBoardView.RecordBoardInterface {

    DeviceType mDeDeviceType = DeviceType.P1;//默认连接设备为P1 当与连接设备有冲突时则需要进行切换
    float isRubber = 0;//是否是橡皮
    ProgressDialog mProgressDialog;
    SettingEntity mSettingEntity;
    Handler mHandler;
    float mPenWeight = 2;//默认笔宽度是2像素
    int mPenColor = Color.BLACK;//默认为黑色
    String mNoteKey = NoteEntity.KEY_NOTEKEY_TMP;//默认为临时笔记
    static final int SELECT_PICTURE = 1001;
    static final int SELECT_BG = 1002;
    Uri mInsertPhotoUri = null;
    Uri mBgUri = null;
    int butFlag = 0;
    @BindView(R.id.recordBoardView)
    RecordBoardView recordBoardView;
    @BindView(R.id.viewWindow)
    RelativeLayout viewWindow;
    @BindView(R.id.cleanScreenBut)
    Button cleanScreenBut;
    @BindView(R.id.innerPhotoBut)
    Button innerPhotoBut;
    @BindView(R.id.removePhotoBut)
    Button removePhotoBut;
    @BindView(R.id.innerbgBut)
    Button innerbgBut;
    @BindView(R.id.removeBgBut)
    Button removeBgBut;
    @BindView(R.id.saveScreenBut)
    Button saveScreenBut;
    @BindView(R.id.recordBut)
    Button recordBut;
    @BindView(R.id.recordStopBut)
    Button recordStopBut;
    @BindView(R.id.recordCancelBut)
    Button recordCancelBut;

    @BindView(R.id.delPageBut)
    Button delPageBut;
    @BindView(R.id.gotoProBut)
    Button gotoProBut;
    @BindView(R.id.gotoNextBut)
    Button gotoNextBut;
    @BindView(R.id.isRubber)
    Button rubber;
    NoteManageModule mNoteManageModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_board);
        ButterKnife.bind(this);
        mHandler = new Handler();
        recordBoardView.setIsTouchWrite(true);
        recordBoardView.setDaoSession(MyApplication.getInstance().getDaoSession());
        mNoteManageModule = new NoteManageModule(this, MyApplication.getInstance().getDaoSession());
        recordBoardView.setLoadIgnorePhoto(false);
        recordBoardView.setDataSaveDir(ResUtils.getSavePath(ResUtils.DIR_NAME_DATA));
        recordBoardView.setIsTouchSmooth(true);
//      recordBoardView.setPenIcon(R.mipmap.ic_launcher);  // 更改笔迹笔头图标
//      recordBoardView.setShowRecordDialog(true);// 录制笔记结束后是否弹出对话框 默认开启
    }

    @Override
    protected void onResume() {
        super.onResume();
        recordBoardView.initDrawArea();
        checkIntentInsertPhoto();
        if(Build.VERSION.SDK_INT >= 23 &&  ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != 0) {
            if(ActivityCompat.shouldShowRequestPermissionRationale((Activity)this, "android.permission.RECORD_AUDIO")) {
                Toast.makeText(this, cn.robotpen.record.R.string.robotpen_permission_request, Toast.LENGTH_SHORT).show();
            }

            ActivityCompat.requestPermissions((Activity)this, new String[]{"android.permission.RECORD_AUDIO"}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recordBoardView != null) {
            recordBoardView.dispose();
            recordBoardView = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            mInsertPhotoUri = null;
            mBgUri = null;
            if (requestCode == SELECT_PICTURE && data != null){
                mInsertPhotoUri = data.getData();
            }
            if (requestCode == SELECT_BG && data != null) {
                mBgUri = data.getData();
            }
        }
    }

    /**
     * 当服务服务连接成功后进行
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
                    recordBoardView.setIsTouchWrite(false);
                    DeviceType type = DeviceType.toDeviceType(device.getDeviceVersion());
                    //判断当前设备与笔记设备是否一致
                    if (recordBoardView.getFrameSizeObject().getDeviceType() != type) {
                        mDeDeviceType = type;
                        mNoteKey = NoteEntity.KEY_NOTEKEY_TMP ;
                    }
                }else {
                    recordBoardView.setIsTouchWrite(true);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }else {
            recordBoardView.setIsTouchWrite(true);
        }
        //都需要刷新白板
        recordBoardView.initDrawArea();

    }

    /**
     * 检查是否有Intent传入需要插入的图片
     */
    public void checkIntentInsertPhoto() {
        //检查是否有需要插入的图片uri
        if (null != mInsertPhotoUri) {
            recordBoardView.insertPhoto(getRealFilePath(RecordBoardActivity.this,mInsertPhotoUri));
            recordBoardView.startPhotoEdit(true); //插入图片后，设置图片可以编辑状态
            mInsertPhotoUri = null;
        }
        if (null != mBgUri) {
            recordBoardView.setBgPhoto(mBgUri);
            mBgUri = null;
        }
    }

    @OnClick({R.id.changePenBut, R.id.changePenColorBut
            , R.id.cleanLineBut,R.id.cleanScreenBut
            , R.id.innerPhotoBut, R.id.removePhotoBut
            , R.id.saveScreenBut, R.id.cleanPhotoBut
            , R.id.innerbgBut, R.id.removeBgBut,R.id.bgScaleTypeBut
            , R.id.delPageBut, R.id.gotoProBut, R.id.gotoNextBut
            , R.id.recordBut, R.id.recordStopBut,R.id.recordCancelBut,R.id.photoScaleTypeBut,R.id.isRubber,R.id.exit_edit,R.id.start_edit})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.changePenBut: //更改笔粗细
                final String[] penWeightItems = {"2个像素", "3个像素", "10个像素", "50个像素"};
                new AlertDialog.Builder(RecordBoardActivity.this).setTitle("修改笔粗细")
                        .setItems(penWeightItems, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                switch (which) {
                                    case 0:
                                        mPenWeight = 2;
                                        break;
                                    case 1:
                                        mPenWeight = 3;
                                        break;
                                    case 2:
                                        mPenWeight = 10;
                                        break;
                                    case 3:
                                        mPenWeight = 50;
                                        break;
                                }
                            }
                        }).show();
                break;
            case R.id.changePenColorBut:
                final String[] penColorItems = {"红色", "绿色", "蓝色", "黑色"};
                new AlertDialog.Builder(RecordBoardActivity.this).setTitle("修改笔颜色")
                        .setItems(penColorItems, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        mPenColor = Color.RED;
                                        break;
                                    case 1:
                                        mPenColor = Color.GREEN;
                                        break;
                                    case 2:
                                        mPenColor = Color.BLUE;
                                        break;
                                    case 3:
                                        mPenColor = Color.BLACK;
                                        break;
                                }
                            }
                        }).show();
                break;
            case R.id.cleanLineBut:
                recordBoardView.cleanTrail();
                recordBoardView.saveSnapshot();
                break;
            case R.id.cleanPhotoBut:
                recordBoardView.cleanPhoto();// 清图片
                recordBoardView.startPhotoEdit(false);// 退出图片编辑模式，否则此时点击图平铺会崩溃
                break;
            case R.id.cleanScreenBut:
                recordBoardView.cleanScreen();
                recordBoardView.startPhotoEdit(false);// 退出图片编辑模式，否则此时点击图平铺会崩溃
                break;
            case R.id.innerPhotoBut:
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(intent, "选择图片"), SELECT_PICTURE);
                //支持多个图片的插入 所以插入图片成功后需要改变序号
                break;
            case R.id.removePhotoBut:
                recordBoardView.setIsTouchWrite(!recordBoardView.isTouchWrite());
                break;
            case R.id.innerbgBut:
                Intent intent2 = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(intent2, "选择背景"), SELECT_BG);
                break;
            case R.id.photoScaleTypeBut:
                recordBoardView.setPhotoScaleType(ImageView.ScaleType.CENTER_CROP);
                break;
            case R.id.removeBgBut:
                mBgUri = null;
                recordBoardView.setBgPhoto(null);
                break;
            case R.id.saveScreenBut:
                recordBoardView.setSaveSnapshotDir(ResUtils.getSavePath(ResUtils.DIR_NAME_PHOTO));//设置存储路径
                recordBoardView.saveSnapshot();
                break;
            case R.id.delPageBut:
                recordBoardView.delCurrBlock();
                break;
            case R.id.gotoProBut:
                recordBoardView.frontBlock();
                break;
            case R.id.gotoNextBut:
                recordBoardView.nextBlock();
                break;
            case R.id.bgScaleTypeBut:
                recordBoardView.setBgScaleType(ImageView.ScaleType.CENTER_CROP);
                break;
            case R.id.recordBut:
                recordBoardView.setSaveVideoDir(ResUtils.getSavePath(ResUtils.DIR_NAME_VIDEO));//设置存储路径
                if (butFlag == 0) { // 点击开始录制按钮
                    butFlag = 1;// 可以暂停
                    ((Button) v).setText("暂停");
                    recordStopBut.setClickable(true);
                    recordStopBut.setBackgroundColor(Color.DKGRAY);
                    recordCancelBut.setClickable(true);
                    recordCancelBut.setBackgroundColor(Color.DKGRAY);
                    recordBoardView.startRecord();
                } else if (butFlag == 1) {// 点击暂停按钮
                    butFlag = 2;// 可以继续
                    ((Button) v).setText("继续");
                    recordBoardView.setIsPause(true);
                } else if (butFlag == 2) {// 点击继续按钮
                    butFlag = 1;// 可以暂停
                    ((Button) v).setText("暂停");
                    recordBoardView.setIsPause(false);
                }
                break;
            case R.id.recordStopBut:
                if (butFlag == 1 || butFlag == 2) {// 防止直接点击崩溃
                    butFlag = 0;// 可以暂停
                    recordBut.setText("录制");
                    v.setBackgroundColor(Color.GRAY);
                    v.setClickable(false);
                    recordCancelBut.setBackgroundColor(Color.GRAY);
                    recordCancelBut.setClickable(false);
                    recordBoardView.endRecord();
                }
                break;
            case R.id.recordCancelBut:
                if (butFlag == 1 || butFlag == 2) {// 防止直接点击崩溃
                    butFlag = 0;// 可以暂停
                    recordBut.setText("录制");
                    v.setBackgroundColor(Color.GRAY);
                    v.setClickable(false);
                    recordStopBut.setBackgroundColor(Color.GRAY);
                    recordStopBut.setClickable(false);
                }
                break;
            case R.id.isRubber:
                isRubber=50;
                break;
            case R.id.exit_edit:
                recordBoardView.startPhotoEdit(false);
                break;
            case R.id.start_edit:
                recordBoardView.startPhotoEdit(true);
                break;
        }
    }


    public boolean isScreenLanscape() {

        Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation ; //获取屏幕方向

        if(ori == mConfiguration.ORIENTATION_LANDSCAPE){
            return true;//横屏
        }else if(ori == mConfiguration.ORIENTATION_PORTRAIT){
            return false;//竖屏
        }
        return false;
    }

    /**
     * Try to return the absolute file path from the given Uri
     *
     * @param context
     * @param uri
     * @return the file path or null
     */
    public static String getRealFilePath(final Context context, final Uri uri ) {
        if ( null == uri ) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if ( scheme == null )
            data = uri.getPath();
        else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();
        } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
            Cursor cursor = context.getContentResolver().query( uri, new String[] { MediaStore.Images.ImageColumns.DATA }, null, null, null );
            if ( null != cursor ) {
                if ( cursor.moveToFirst() ) {
                    int index = cursor.getColumnIndex( MediaStore.Images.ImageColumns.DATA );
                    if ( index > -1 ) {
                        data = cursor.getString( index );
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    @Override
    public DeviceType getDeviceType() {
        return mDeDeviceType;
    }

    @Override
    public float getPenWeight() {
        return mPenWeight;
    }

    @Override
    public int getPenColor() {
        return mPenColor;
    }

    @Override
    public float getIsRubber() {
        return isRubber;
    } //非0时即为橡皮擦 具体数字代表橡皮擦宽度

    @Override
    public boolean getIsPressure() {
        return false;
    }

    @Override
    public boolean getIsHorizontal() {
        return isScreenLanscape();
    }

    @Override
    public long getCurrUserId() {
        return 0;
    }

    @Override
    public String getNoteKey() {
        return mNoteKey;
    }

    @Override
    public String getNewNoteName() {
        return null;
    }

    @Override
    public boolean onEvent(WhiteBoardView.BoardEvent boardEvent, Object o) {
        switch (boardEvent) {
            case TRAILS_COMPLETE:
                try {
                    getPenServiceBinder().setPageInfo(recordBoardView.getBlockIndex() + 1, recordBoardView.getBlockCount());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case BOARD_AREA_COMPLETE: //白板区域加载完成
                recordBoardView.beginBlock();

                break;
            case ERROR_DEVICE_TYPE: //检测到连接设备更换

                break;
            case ERROR_SCENE_TYPE: //横竖屏更换
                break;
            case ON_TRAILS:
                CLog.i(""+((TrailsEntity)o).getTrails());
                break;
        }
        return true;
    }

    @Override
    public boolean onMessage(String s, Object o) {
        return false;
    }

    @Override
    public void onPageInfoUpdated(String s) {

    }

    /*
    *录制时必须实现的方法
     */
    @Override
    public int getRecordLevel() {
        mSettingEntity = new SettingEntity(this);
        return mSettingEntity.getVideoQualityValue(); //录制质量
    }

    @Override
    public void onRecordButClick(int i) {
        switch (i) {
            case RecordBoardView.EVENT_CONFIRM_EXT_CLICK:
                break;
        }

    }

    @Override
    public void onRecordError(int i) {

    }

    /**
    *接收录制中的各种状态进行处理
     */
    @Override
    public boolean onRecordState(RecordState recordState, String s) {
        switch (recordState) {
            case START:
                break;
//            case CANCEL:
            case END:
                break;
            case PAUSE:
                break;
            case CONTINUE:
                break;
            case SAVING:
                break;
            case CODING:
                break;
            case COMPLETE:
                break;
            case ERROR:
                break;
            case RESISTANCE:
                recordBoardView.startRecord();
                break;
        }
        return true;
    }

    @Override
    public boolean onRecordTimeChange(Date date) {
    // 显示时间
        return true;
    }

    @Override
    public void getRecordVideoName(String s) {
        Log.e("test","getRecordVideoName :"+s);
    }


    @Override
    public void onStateChanged(int i, String s) {
        switch (i) {
            case RemoteState.STATE_CONNECTED:
                break;
            case RemoteState.STATE_DEVICE_INFO: //当出现设备切换时获取到新设备信息后执行的
                recordBoardView.setIsTouchWrite(false);
//                checkDeviceConn();
                break;
            case RemoteState.STATE_DISCONNECTED://设备断开
                recordBoardView.setIsTouchWrite(true);
                break;
        }
    }

    @Override
    public void onPenServiceError(String s) {

    }

    @Override
    public void onPenPositionChanged(int deviceType, int x, int y, int presure, byte state) {
        super.onPenPositionChanged(deviceType, x, y, presure, state);
        if(isRubber==0) {// isRubber==0  现在没用橡皮察，止选择橡皮擦的时候，不小心触碰笔，绘制笔迹。
//            DevicePoint p = DevicePoint.obtain(deviceType, x, y, presure, state);
//            recordBoardView.drawLine(p);
            DeviceType type = DeviceType.toDeviceType(deviceType);
            recordBoardView.drawDevicePoint(type,x,y,presure,state);
        }
    }

    private int currentPage = 0;
    @Override
    public void onPageInfo(int currentPage, int totalPage) {
        this.currentPage=currentPage;
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
    public void onRobotKeyEvent(int e) {
        super.onRobotKeyEvent(e);
        switch (e) {
            case 0x03:
                onEventFrontPage();
                break;
            case 0x04:
                onEventNextPage();
                break;
            case 0x05:
                recordBoardView .insertBlock(currentPage);
                break;
        }

    }


    /**
     * 用于响应设备按钮事件的翻页
     */
    private void onEventFrontPage() {
            if (recordBoardView.isFirstBlock()) {
                recordBoardView.lastBlock();
            } else {
                recordBoardView.frontBlock();
            }
    }

    /**
     * 用于响应设备按钮事件的翻页
     */
    private void onEventNextPage() {
            if (recordBoardView.isLastBlock()){
                recordBoardView.firstBlock();
            } else {
                recordBoardView.nextBlock();
            }
    }
}
