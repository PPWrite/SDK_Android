package cn.robotpenDemo.board.show;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.robotpen.model.DevicePoint;
import cn.robotpen.model.entity.note.NoteEntity;
import cn.robotpen.model.symbol.DeviceType;
import cn.robotpen.pen.callback.OnUiCallback;
import cn.robotpen.pen.callback.RobotPenActivity;
import cn.robotpen.pen.model.RemoteState;
import cn.robotpen.pen.model.RobotDevice;
import cn.robotpen.utils.log.CLog;
import cn.robotpen.views.widget.WhiteBoardView;
import cn.robotpenDemo.board.MyApplication;
import cn.robotpenDemo.board.R;
import cn.robotpenDemo.board.common.ResUtils;

public class WhiteBoardWithMethodActivity extends RobotPenActivity
        implements WhiteBoardView.WhiteBoardInterface,OnUiCallback {

    DeviceType mDeDeviceType = DeviceType.P1;//默认连接设备为P1 当与连接设备有冲突时则需要进行切换
    float isRubber = 0;//是否是橡皮
    Handler mHandler;
    float mPenWeight = 2;//默认笔宽度是2像素
    int mPenColor = Color.BLUE;//默认为蓝色
    String mNoteKey = NoteEntity.KEY_NOTEKEY_TMP;//默认为临时笔记
    static final int SELECT_PICTURE = 1001;
    static final int SELECT_BG = 1002;
    Uri mInsertPhotoUri = null;
    Uri mBgUri = null;

    @BindView(R.id.whiteBoardView_m)
    WhiteBoardView whiteBoardView;
    @BindView(R.id.viewWindow)
    RelativeLayout viewWindow;
    @BindView(R.id.changePenBut)
    Button changePenBut;
    @BindView(R.id.changePenColorBut)
    Button changePenColorBut;
    @BindView(R.id.cleanLineBut)
    Button cleanLineBut;
    @BindView(R.id.cleanPhotoBut)
    Button cleanPhotoBut;
    @BindView(R.id.cleanScreenBut)
    Button cleanScreenBut;
    @BindView(R.id.innerPhotoBut)
    Button innerPhotoBut;
    @BindView(R.id.photoScaleTypeBut)
    Button photoScaleTypeBut;
    @BindView(R.id.removePhotoBut)
    Button removePhotoBut;
    @BindView(R.id.innerbgBut)
    Button innerbgBut;
    @BindView(R.id.bgScaleTypeBut)
    Button bgScaleTypeBut;
    @BindView(R.id.removeBgBut)
    Button removeBgBut;
    @BindView(R.id.delPageBut)
    Button delPageBut;
    @BindView(R.id.gotoProBut)
    Button gotoProBut;
    @BindView(R.id.gotoNextBut)
    Button gotoNextBut;
    @BindView(R.id.saveScreenBut)
    Button saveScreenBut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_white_board_with_method);
        ButterKnife.bind(this);
        mHandler = new Handler();
        whiteBoardView.setIsTouchWrite(true);//允许在屏幕上直接绘制
        whiteBoardView.setDaoSession(MyApplication.getInstance().getDaoSession());
        whiteBoardView.setLoadIgnorePhoto(false);
        whiteBoardView.setDataSaveDir(ResUtils.getSavePath(ResUtils.DIR_NAME_DATA));
        whiteBoardView.setSaveSnapshotDir(ResUtils.getSavePath(ResUtils.DIR_NAME_PHOTO));//设置截屏的目录
        whiteBoardView.setBgColor(R.color.colorPrimary);
    }

    @Override
    protected void onResume() {
        super.onResume();
        whiteBoardView.initDrawArea();
        checkIntentInsertPhoto();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (whiteBoardView != null) {
            whiteBoardView.dispose();
            whiteBoardView = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            mInsertPhotoUri = null;
            mBgUri = null;
            if (requestCode == SELECT_PICTURE && data != null) {
                mInsertPhotoUri = data.getData();
            }
            if (requestCode == SELECT_BG && data != null) {
                mBgUri = data.getData();
            }
        }
    }
    /**
     * 当服务服务连接成功后进行
     *
     * @param name
     * @param service
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        Log.e("test"," onServiceConnected ");
        checkDeviceConn();
    }

    public void checkDeviceConn() {
        if (getPenServiceBinder() != null) {
            try {
                RobotDevice device = getPenServiceBinder().getConnectedDevice();
                if (device != null) {
                    whiteBoardView.setIsTouchWrite(false);
                    DeviceType type = DeviceType.toDeviceType(device.getDeviceVersion());
                    //判断当前设备与笔记设备是否一致
                    if (whiteBoardView.getFrameSizeObject().getDeviceType() != type) {
                        mDeDeviceType = type;
                        mNoteKey = NoteEntity.KEY_NOTEKEY_TMP + "_" + mDeDeviceType.name();
                    }
                }else{
                    whiteBoardView.setIsTouchWrite(true);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }else {
            whiteBoardView.setIsTouchWrite(true);
        }
        //都需要刷新白板
        whiteBoardView.initDrawArea();
    }

    /**
     * 检查是否有Intent传入需要插入的图片
     */
    public void checkIntentInsertPhoto() {
        //检查是否有需要插入的图片uri
        if (null != mInsertPhotoUri) {
            whiteBoardView.insertPhoto(getRealFilePath(WhiteBoardWithMethodActivity.this,mInsertPhotoUri));
            mInsertPhotoUri = null;
        }
        if(null != mBgUri){
            whiteBoardView.setBgPhoto(mBgUri);
            mBgUri = null;
        }
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

    @OnClick({R.id.changePenBut, R.id.changePenColorBut
            , R.id.cleanLineBut, R.id.cleanPhotoBut, R.id.cleanScreenBut
            , R.id.innerPhotoBut, R.id.photoScaleTypeBut, R.id.removePhotoBut
            , R.id.saveScreenBut
            , R.id.innerbgBut, R.id.bgScaleTypeBut, R.id.removeBgBut
            , R.id.delPageBut, R.id.gotoProBut, R.id.gotoNextBut,R.id.exit_edit})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.changePenBut: //更改笔粗细
                final String[] penWeightItems = {"2个像素", "3个像素", "10个像素", "50个像素"};
                new AlertDialog.Builder(WhiteBoardWithMethodActivity.this).setTitle("修改笔粗细")
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
                new AlertDialog.Builder(WhiteBoardWithMethodActivity.this).setTitle("修改笔颜色")
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
                whiteBoardView.cleanTrail();
                break;
            case R.id.cleanPhotoBut:
                whiteBoardView.cleanPhoto();
                whiteBoardView.startPhotoEdit(false);// 退出图片编辑模式，否则此时点击图平铺会崩溃
                break;
            case R.id.cleanScreenBut:
                whiteBoardView.cleanScreen();
                whiteBoardView.startPhotoEdit(false);// 退出图片编辑模式，否则此时点击图平铺会崩溃
                break;
            case R.id.innerPhotoBut:
                whiteBoardView.setDataSaveDir(ResUtils.getSavePath(ResUtils.DIR_NAME_DATA));
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(intent, "选择图片"), SELECT_PICTURE);
                //支持多个图片的插入 所以插入图片成功后需要改变序号
                break;
            case R.id.photoScaleTypeBut:
                whiteBoardView.setPhotoScaleType(ImageView.ScaleType.CENTER_CROP);
                break;
            case R.id.removePhotoBut:
                whiteBoardView.delCurrEditPhoto();
                break;
            case R.id.innerbgBut:
                whiteBoardView.setDataSaveDir(ResUtils.getSavePath(ResUtils.DIR_NAME_DATA));
                Intent intent2 = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(intent2, "选择背景"), SELECT_BG);
                break;
            case R.id.bgScaleTypeBut:
                whiteBoardView.setBgScaleType(ImageView.ScaleType.CENTER_CROP);
                break;
            case R.id.removeBgBut:
                mBgUri = null;
                whiteBoardView.setBgPhoto(null);
                break;
            case R.id.saveScreenBut:
                whiteBoardView.setSaveSnapshotDir(ResUtils.getSavePath(ResUtils.DIR_NAME_PHOTO));//设置存储路径
                whiteBoardView.saveSnapshot();
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(ResUtils.getSavePath(ResUtils.DIR_NAME_PHOTO)))));
                break;
            case R.id.delPageBut:
                whiteBoardView.delCurrBlock();
                break;
            case R.id.gotoProBut:
                whiteBoardView.frontBlock();
                break;
            case R.id.gotoNextBut:
                whiteBoardView.nextBlock();
                break;
            case R.id.exit_edit:
                whiteBoardView.startPhotoEdit(false);
//                String  path = whiteBoardView.saveSnapshot();
//                if(null!=path){
//                    Toast.makeText(this, "截图成功", Toast.LENGTH_LONG).show();
//                }else {
//                    Toast.makeText(this, "截图失败", Toast.LENGTH_LONG).show();
//                }
                //更新图库
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

    // 设置设备类型
    @Override
    public DeviceType getDeviceType() {
        return mDeDeviceType;
    }

    // 设置笔迹宽度
    @Override
    public float getPenWeight() {
        return mPenWeight;
    }

    // 设置笔迹颜色
    @Override
    public int getPenColor() {
        return mPenColor;
    }

    // 设置是否是橡皮插模式 非0时即为橡皮擦模式
    @Override
    public float getIsRubber() {
        return isRubber;
    }

    //设置是否打开压感
    @Override
    public boolean getIsPressure() {
        return true;
    }

    // 设置是否横屏显示
    @Override
    public boolean getIsHorizontal() {
        return isScreenLanscape();
    }

    // 设置当前用户ID（备用接口，暂无意义）
    @Override
    public long getCurrUserId() {
        return 0;
    }

    // 设置当前笔记的notekey
    @Override
    public String getNoteKey() {
        return mNoteKey;
    }

    // 设置笔记名字,如果要新建的话.
    @Override
    public String getNewNoteName() {
        return null;
    }

    /**
     * 返回当前白板发生的事件
     *
     * @param boardEvent
     * @param o   当前白板tag
     * @return 返回false，表示忽略
     */
    @Override
    public boolean onEvent(WhiteBoardView.BoardEvent boardEvent, Object o) {
        switch (boardEvent) {
            case BOARD_AREA_COMPLETE: //白板区域加载完成
                whiteBoardView.beginBlock();
                break;
            case ERROR_DEVICE_TYPE: //检测到连接设备更换
                break;
            case ERROR_SCENE_TYPE: //横竖屏更换
                break;
            case TRAILS_COMPLETE:
                break;
        }
        return true;
    }

    /**
     * 返回当前正在处理的消息，设置为true拦截，白板不显示
     *
     * @param s 消息
     * @param o 当前白板tag
     * @return 返回true，白板将不自动显示消息
     */
    @Override
    public boolean onMessage(String s, Object o) {
        return false;
    }

    @Override
    public void onPageInfoUpdated(String s) {

    }

    @Override
    public void onStateChanged(int i, String s) {
        switch (i) {
            case RemoteState.STATE_CONNECTED:
                break;
            case RemoteState.STATE_DEVICE_INFO:
                //当出现设备切换时获取到新设备信息后执行的
//                checkDeviceConn();
                whiteBoardView.setIsTouchWrite(false);
                break;
            case RemoteState.STATE_DISCONNECTED://设备断开
                whiteBoardView.setIsTouchWrite(true);
                break;
        }
    }

    // 笔服务错误返回信息
    @Override
    public void onPenServiceError(String s) {

    }

    @Override
    public void onPenPositionChanged(int deviceType, int x, int y, int presure, byte state) {
        super.onPenPositionChanged(deviceType, x, y, presure, state);
        if(isRubber==0) {// isRubber==0  现在没用橡皮察,  防止选择橡皮擦的时候，不小心触碰笔，绘制笔迹。
            DeviceType type = DeviceType.toDeviceType(deviceType);
            whiteBoardView.drawDevicePoint(type,x,y,presure,state);
        }
    }



    private int currentPage =0;
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
                whiteBoardView.insertBlock(currentPage);
                break;
        }

    }


    /**
     * 用于响应设备按钮事件的翻页 上一页
     */
    private void onEventFrontPage() {
        if (whiteBoardView.isFirstBlock()) {
            whiteBoardView.lastBlock();
        } else {
            whiteBoardView.frontBlock();
        }
    }

    /**
     * 用于响应设备按钮事件的翻页 下一页
     */
    private void onEventNextPage() {
        if (whiteBoardView.isLastBlock()) {
            whiteBoardView.firstBlock();
        } else {
            whiteBoardView.nextBlock();
        }
    }

}
