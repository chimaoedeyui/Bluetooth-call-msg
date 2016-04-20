package bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;


public class BluetoothService extends Service {

    private static final String SEND_BLUETOOTH_STATUS = "com.lzk.bluetooth.status";
    private static final String GET_BLUETOOTH_DATA = "com.lzk.bluetooth.data";

    private static final String Set_Address = "com.lzk.address";
    private ConnectBluetoothReceiver connectBluetoothReceiver;

    private InputStream is;    //输入流，用来接收蓝牙数据
    private boolean Readquit=false;

    //跟蓝牙有关的对象
    private BluetoothSocket socket = null;      //蓝牙通信socket

    private BluetoothAdapter bluetooth ;    //获取本地蓝牙适配器，即蓝牙设备
    private BluetoothManager bluetoothmanager;

    private ServiceBinder myBinder = new  ServiceBinder();

    private Handler servicehandler;
    private Thread readthread;

    private TimerTask task;


    @Override
    public IBinder onBind(Intent intent) {
        return  myBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
       // Log.i("lzk", "onUnbind(Intent intent)");
        return  super.onUnbind(intent);
    }

    public void onCreate() {
        super.onCreate();

        bluetoothmanager=BluetoothManager.getInstance();
        bluetooth=bluetoothmanager.getBluetoothAdapter();

        servicehandler = new ServiceHandler();

        // 注册自定义动态广播消息
        connectBluetoothReceiver = new ConnectBluetoothReceiver();
        registerReceiver(connectBluetoothReceiver, new IntentFilter(Set_Address));
        // 设置设备可以被搜索
        new Thread(){
            public void run(){
                while(!bluetooth.isEnabled()){
                    bluetooth.enable();
                }
            }
        }.start();
        readthread=new ReadDataLooper();//在连接成功后开启读取数据子线程，通过readThreadHandler与子线程通信
        Log.i("lzk", "service starting");
        task = new TimerTask(){
            public void run() {
                Message message = new Message();
                message.what = 2;
                servicehandler.sendMessage(message);
            }
        };
    }

    public void sendmsg(int number) {
        byte[] cmd=new byte[1];
        cmd[0]= (byte) (0xff & number);

        bluetoothmanager.SendCmd(cmd);

    }


    //拿到service中处理数据
    private class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int[] readdata;
            switch (msg.what) {
                case 1:
                    readdata = FileUtils.Byte2Int((byte[]) msg.obj);
                    Intent intent = new Intent();
                    intent.setAction(GET_BLUETOOTH_DATA);
                    intent.putExtra("readdata", readdata);
                    Log.i("lzk", "ServiceHandler readdata  " + readdata.length);
                    sendBroadcast(intent);
                    break;

                case 2:
                    if(!bluetoothmanager.isConnect()){
                        task.cancel();
                        Intent intent2 = new Intent();
                        intent2.setAction(SEND_BLUETOOTH_STATUS);
                        intent2.putExtra("status", 1);
                        sendBroadcast(intent2);
                        Log.i("lzk","not connect");
                    }
                    break;


                default:
                    Log.i("lzk","??ServiceHandler??");
                    break;

            }
        }
    }



    //取得读取数据子线程
    public class ReadDataLooper extends Thread {
        @Override
        public void run() {
            Timer timer = new Timer(true);
            timer.schedule(task, 1000, 1000); //延时1000ms后执行，1000ms执行一次

            while (true) {
                try {
                    int count = 0;
                    while (count == 0) {
                        if (Readquit) break;
                        count = is.available();
                    }
                    DisplayToast("READ3-------------");
                    if (Readquit) break;
                    byte[] temp = new byte[count];
                    int readCount = 0; // 已经成功读取的字节的个数
                    while (readCount < count) {
                        readCount += is.read(temp, readCount, count - readCount);
                    }

                    Message message = Message.obtain();
                    message.what = 1;//requestcode
                    message.obj = temp;
                    servicehandler.sendMessage(message);
                    DisplayToast("b byte[]: " + temp.length);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }



    public void DisplayToast(String str)
    {
        Log.d("lzk", str);
    }


    public void startConnect() {
        int status=1;
        if(bluetoothmanager.isSetAddress() && socket==null) {
            socket = bluetoothmanager.connectBluetooth();
            if (socket != null) {
                status = 0;
                is = bluetoothmanager.getInputStream();

                readthread.start();

            }
        }
        Intent intent = new Intent();
        intent.setAction(SEND_BLUETOOTH_STATUS);
        intent.putExtra("status", status);
        sendBroadcast(intent);
     }


    public void onDestroy() {

        super.onDestroy();
        Log.i("lzk", "bluetooth service destroy");
        closeBluetooth();
       // readThreadHandler.getLooper().quit();
        Readquit=true;

        unregisterReceiver(connectBluetoothReceiver);

    }
    public void closeBluetooth(){
        bluetoothmanager.destroyBluetooth();
    }

    //此方法是为了可以在Acitity中获得服务的实例
    public class ServiceBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    public class ConnectBluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(bluetooth.isEnabled()){
                String address=intent.getStringExtra("address");
                bluetoothmanager.setAddress(address);
                //-------------------------thread为单例-------------------
                new Thread(){
                    public void run(){
                        startConnect();
                    }
                }.start();

            }


        }
    }
    public boolean isConnect(){
        if(bluetooth.isEnabled())
            return bluetoothmanager.isConnect();
        else
            return false;
    }




}
