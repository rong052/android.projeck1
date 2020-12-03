package com.sin.android.camhxa1;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.sin.android.usb.pl2303hxa.PL2303Driver;
import com.sin.android.usb.pl2303hxa.PL2303Exception;
//import com.sin.android.sinlibs.utils.InjectUtils;

public class MainActivity extends Activity {

    CameraPreview cameraView;
    ImageView mImg;
    private TextView tv_log;
    private String mStr;
    int a=0;

    // PL2303驱动
    private PL2303Driver curDriver = null;

    private static final String TEXT_CHARSET = "UTF-8"; // 字符编码方式

    //private static final String[] BAUDRATES = { "9600", "14400", "19200", "38400", "56000", "57600", "115200" };

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (PL2303Driver.ACTION_PL2303_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        //open();
                        AddLog("授权成功");
                    } else {
                        curDriver = null;
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 注册USB广播接收器，用于监听授权
        IntentFilter filter = new IntentFilter(PL2303Driver.ACTION_PL2303_PERMISSION);
        filter.addAction(PL2303Driver.ACTION_PL2303_PERMISSION);
        this.registerReceiver(mUsbReceiver, filter);
        mImg = (ImageView) findViewById(R.id.imageView);
        tv_log = (TextView) findViewById(R.id.textView);

        //InjectUtils.injectViews(this, R.id.imageView);
        preOpen();

        int delay = 0; // delay for 0 sec.
        int period = 500; // repeat every 1S.
        Timer timer = new Timer();

        cameraView = (CameraPreview) findViewById(R.id.cameraView);

        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {

                runOnUiThread(new Runnable() {        //可以使用此方法臨時交給UI做顯示
                    public void run(){
                        doGreyscale();
                    }
                });
            }
        }, delay, period);
    }

    private void AddLog(String mstr)
    {
        mStr=mstr;
        runOnUiThread(new Runnable() {        //可以使用此方法臨時交給UI做顯示
            public void run(){
                tv_log.append(mStr);
                tv_log.append("\n");
            }
        });
    }


    // 招到PL2303并请求打开
    private void preOpen() {
        // 试用第1个PL2303设备
        List<UsbDevice> devices = PL2303Driver.getAllSupportedDevices(this);
        if (devices.size() == 0) {
            AddLog("请先插入PL2303HXA设备");
        } else {
            AddLog("当前PL2303HXA设备:");
            for (UsbDevice d : devices) {
                AddLog(" " + d.getDeviceId());
            }

            // 使用找到的第1个PL2303HXA设备
            PL2303Driver dev = new PL2303Driver(this, devices.get(0));
            openPL2302Device(dev);
        }

        // 打开选择对话框选择设备
        /*
         * PL2303Selector.createSelectDialog(this, 0, false, new
         * PL2303Selector.Callback() {
         *
         * @Override public boolean whenPL2303Selected(PL2303Driver driver) {
         * openPL2302Device(driver); return true; } }).show();
         */
    }

    private void openPL2302Device(PL2303Driver dev) {
        if (dev != null) {
            curDriver = dev;
            if (curDriver.checkPermission())
            {
                // 如果已经授权就直接打开
                open();
            }
        }
    }

    // 打开串口
    private void open() {
        if (curDriver == null)
            return;
        try {
            if (curDriver.isOpened()) {
                curDriver.cleanRead();
                curDriver.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            curDriver.setBaudRate(38400);
            curDriver.open();
            AddLog("打开串口");
        } catch (PL2303Exception e) {
            AddLog("打开失败");
        }
    }

    private void close() {
        AddLog("关闭串口");
        synchronized (curDriver) {
            curDriver.cleanRead();
            curDriver.close();
        }
        curDriver = null;
        AddLog("关闭成功");
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.onResume(this);
    }

    @Override
    protected void onPause() {
        //cameraView.onPause();
        super.onPause();
    }

    public void takePic(View view) {
        cameraView.takePicture();
    }


    public void doGreyscale() {
        // create output bitmap
        Bitmap bmOut = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);

        while (curDriver != null) {
            PL2303Driver tDriver = curDriver;
            byte dat = 0;
            synchronized (tDriver) {
                dat = tDriver.read();
            }
            if (tDriver.isReadSuccess())
                //recs.add(dat);
                a++;
            else
                break;
        }

        if (a > 200)
            a -= 200;

        // get image size
        int width = bmOut.getWidth();
        int height = bmOut.getHeight();

        // scan through every single pixel
        for(int x = 0; x < width; x+=2) {
            for(int y = 0; y < height; y+=2) {
                // set new pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(255, 100, a, 0));
            }
        }

        //converting bitmap object to show in imageview
        mImg.setImageBitmap(bmOut);
    }
}