package com.example.jk.bluetoothapp;


import android.Manifest;
import android.app.Activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.model.LatLng;

import java.util.Timer;
import java.util.TimerTask;

import bluetooth.BluetoothService;
import bluetooth.DeviceListActivity;


public abstract class AMainActivity extends Activity {


    private static final String SEND_BLUETOOTH_STATUS = "com.lzk.bluetooth.status";
    private static final String GET_BLUETOOTH_DATA = "com.lzk.bluetooth.data";

    private String username, usernumber,locationmsg;
    public String usermessage;
    private int timecount = 0, receivecount = 0;
    public boolean isSendMessage=false;

    //蓝牙相关
    protected BluetoothService bluetoothService;
    private BluetoothServiceReceiver bluetoothServiceReceiver;

    // 定位相关声明
    public LocationClient locationClient = null;
    private SDKReceiver mReceiver;

    //定时器
    private Timer timer;
    private TimerTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 注册自定义动态广播消息
        AppcheckPermission();

        Intent intent2 = new Intent(AMainActivity.this,
                BluetoothService.class);
        bindService(intent2, bluetoothServiceConnection, Context.BIND_AUTO_CREATE);

        bluetoothServiceReceiver = new BluetoothServiceReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SEND_BLUETOOTH_STATUS);
        intentFilter.addAction(GET_BLUETOOTH_DATA);
        registerReceiver(bluetoothServiceReceiver, intentFilter);
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
        mReceiver = new SDKReceiver();
        registerReceiver(mReceiver, iFilter);
        locationClient = new LocationClient(getApplicationContext()); // 实例化LocationClient类
        locationClient.registerLocationListener(myListener); // 注册监听函数
        setLocationOption();    //设置定位参数
        locationClient.start(); // 开始定位

        task = new MyTimerTask();
        timer = new Timer();
        /* 表示0毫秒之後，每隔2000毫秒執行一次 */
        timer.schedule(task, 0, 2000);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("lzk", "destroy");
        timer.cancel();
        unregisterReceiver(bluetoothServiceReceiver);
        bluetoothService.closeBluetooth();
        unbindService(bluetoothServiceConnection);
        //退出时销毁定位
        unregisterReceiver(mReceiver);
        locationClient.stop();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            ContentResolver reContentResolverol = getContentResolver();
            Uri contactData = data.getData();
            @SuppressWarnings("deprecation")
            Cursor cursor = managedQuery(contactData, null, null, null, null);
            cursor.moveToFirst();
            username = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            Cursor phone = reContentResolverol.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                    null,
                    null);
            while (phone.moveToNext()) {
                usernumber = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            }
        } else {
            showToast("not select");
        }
        showContactor(username, usernumber);
    }

    /**
     * 设置定位参数
     */
    private void setLocationOption() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        int span = 1000;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        locationClient.setLocOption(option);
    }

    private ServiceConnection bluetoothServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.ServiceBinder mybinder = (BluetoothService.ServiceBinder) service;
            bluetoothService = mybinder.getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            bluetoothService = null;
        }
    };


    //选择蓝牙模块
    public void SelectBluetooth() {
        if (!bluetoothService.isConnect()) {
            Intent serverIntent = new Intent(AMainActivity.this, DeviceListActivity.class); //跳转程序设置
            serverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(serverIntent);
        }
    }

    public void AutoDialog() {

        if (usernumber != null && !usernumber.equals("")) {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + usernumber));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            AMainActivity.this.startActivity(intent);
        }
        else {
            showToast("Please select contactor");
        }
    }


    public void sendTextMsg() {
        String SENT = "sms_sent";
        String DELIVERED = "sms_delivered";

        if(usernumber==null || usernumber.equals("") ||
                usermessage==null || usermessage.equals("") ||
                locationmsg==null || locationmsg.equals("")){
            showToast("号码或者信息不正确");
            Log.i("lzk", "send ssm msg");
            return;
        }

        Log.i("lzk","send ssm msg");

        PendingIntent sentPI = PendingIntent.getActivity(this, 0, new Intent(SENT), 0);
        PendingIntent deliveredPI = PendingIntent.getActivity(this, 0, new Intent(DELIVERED), 0);

        registerReceiver(new BroadcastReceiver(){

            @Override
            public void onReceive(Context context, Intent intent) {
                switch(getResultCode())
                {
                    case Activity.RESULT_OK:
                        Log.i("lzk", "Activity.RESULT_OK");
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Log.i("lzk", "RESULT_ERROR_GENERIC_FAILURE");
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Log.i("lzk", "RESULT_ERROR_NO_SERVICE");
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Log.i("lzk", "RESULT_ERROR_NULL_PDU");
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Log.i("lzk", "RESULT_ERROR_RADIO_OFF");
                        break;
                }
            }
        }, new IntentFilter(SENT));

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Log.i("====>", "RESULT_OK");
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i("=====>", "RESULT_CANCELED");
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager smsm = SmsManager.getDefault();
        smsm.sendTextMessage(usernumber, null, usermessage, sentPI, deliveredPI);
    }


	public class BluetoothServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {


			if(intent.getAction().equals(SEND_BLUETOOTH_STATUS)){
				int status=intent.getIntExtra("status",-1);
				if(status==0){
					showToast("连接成功");
                    updateBluetoothStatus(true);
				}
				else if(status==1){
					showToast("连接失败");
                    updateBluetoothStatus(false);
				}
			}
			if(intent.getAction().equals(GET_BLUETOOTH_DATA)){
				int[] readdate=intent.getIntArrayExtra("readdata");
                receivecount=receivecount+readdate.length;
                updateProperty(readdate);
				Log.i("lzk", "receive msg:" + readdate.length);
			}
		}

	}

    public class SDKReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR))
            {
                Log.i("lzk","fail----");
                // key 验证失败，相应处理
            }else {
                Log.i("lzk","success----");
            }

        }
    }



    public BDLocationListener myListener = new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            //Receive Location
            int type=location.getLocType();
            Log.i("lzk", "type" + type);
            if(type==BDLocation.TypeGpsLocation || type==BDLocation.TypeNetWorkLocation ||
                    type==BDLocation.TypeOffLineLocation){
                locationmsg="http://api.map.baidu.com/marker?location=" +
                        location.getLatitude() + "," + location.getLongitude() +
                        "&title=我的位置&content=百度奎科大厦" +
                        "&output=html&src=yourComponyName|yourAppName";

                updateLocation("Address:"+location.getAddrStr());
            }
            else {
                locationmsg=null;
                updateLocation("not located");
            }



        }
    };
    private void AppcheckPermission() {
//        PackageManager pm = getPackageManager();
//        try {
//            PackageInfo pack = pm.getPackageInfo("packageName",PackageManager.GET_PERMISSIONS);
//            String[] permissionStrings = pack.requestedPermissions;
//
//            for(String s:permissionStrings)
//                showLog(s);
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
//        boolean permission = (PackageManager.PERMISSION_GRANTED ==
//                pm.checkPermission("android.permission.CALL_PHONE", "packageName"));
//        showLog(permission+" ");


    }


    private class MyTimerTask extends TimerTask    {
        @Override
        public void run()
        {
            if(receivecount>0){
                timecount++;
                //4-6s的计数
                if(timecount>2){
                    Log.i("lzk","receivecount:"+receivecount);
                    if(receivecount==1){
                        Log.i("lzk","11111");
                        showToast("one touch");

                    }
                    else if(receivecount==2){
                        Log.i("lzk","2222");
                        showToast("two touch");
                        //send msg
                        sendTextMsg();

                    }else {
                        Log.i("lzk","many");
                        showToast("a lot touch");
                        //diag number
                        if(isSendMessage)
                            sendTextMsg();
                        AutoDialog();
                    }
                    receivecount=0;
                }
            }
            if(bluetoothService!=null)
                updateBluetoothStatus(bluetoothService.isConnect());
        }
    };

    private void showLog(String s) {
        Log.i("lzk",s);
    }


    protected abstract void updateProperty(int[] readdate);
    protected abstract void updateBluetoothStatus(boolean status);
    protected abstract void updateLocation(String address);
    protected abstract void showContactor(String username,String usernumber);
    protected abstract void showToast(String text);

}
	

