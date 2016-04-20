package com.example.jk.phonecall;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.model.LatLng;

import bluetooth.BluetoothService;
import bluetooth.DeviceListActivity;

;

public class MainActivity extends Activity {

    private static final String SEND_BLUETOOTH_STATUS = "com.lzk.bluetooth.status";
    private static final String GET_BLUETOOTH_DATA = "com.lzk.bluetooth.data";

    private Button addContactor,saveMeaasge,connectbtn,sendbtn;
    private String username,usernumber,usermessage,reiceivedmsg,sendmsg;
    private EditText namedisplay;
    private EditText messageEdit,sendEdit;
    private TextView reiceivedview;
    private CheckBox CheckMessage;
    private boolean isSendMessage;


    protected BluetoothService bluetoothService;
    private BluetoothServiceReceiver bluetoothServiceReceiver;

    // 定位相关声明
    public LocationClient locationClient = null;
    private LatLng ll=null;
    private SDKReceiver mReceiver;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
        mReceiver = new SDKReceiver();
        registerReceiver(mReceiver, iFilter);

        locationClient = new LocationClient(getApplicationContext()); // 实例化LocationClient类
        locationClient.registerLocationListener(myListener); // 注册监听函数
        this.setLocationOption();	//设置定位参数
        locationClient.start(); // 开始定位

        Intent intent2 = new Intent(MainActivity.this,
                BluetoothService.class);
        bindService(intent2, bluetoothServiceConnection, Context.BIND_AUTO_CREATE);


        connectbtn=(Button)findViewById(R.id.connectbtn);
        connectbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectBluetooth();
            }
        });
        namedisplay=(EditText)findViewById(R.id.namedisplay);
        addContactor = (Button) findViewById(R.id.namebtn);
        addContactor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(
                        Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), 0);
            }
        });
        messageEdit=(EditText)findViewById(R.id.messageinfo);
        usermessage=messageEdit.getText().toString();
        sendEdit=(EditText)findViewById(R.id.sendmsg);


        saveMeaasge = (Button) findViewById(R.id.savemeaasge);
        if(usermessage==null || usermessage.equals(""))
            saveMeaasge.setText("保存");
        else
            saveMeaasge.setText("编辑");
        saveMeaasge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(saveMeaasge.getText().toString().equals("保存")){
                    saveMeaasge.setText("编辑");
                    usermessage=messageEdit.getText().toString();
                }
                else{
                    saveMeaasge.setText("保存");
                }
            }
        });

        sendbtn= (Button) findViewById(R.id.sendbtn);
        sendbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendmsg=sendEdit.getText().toString();
                bluetoothService.sendmsg(0x06, 0x02, 0x02, 0x02, 0x02);
                Log.i("lzk", "msg:" + sendmsg);
              /*  PackageManager pm = getPackageManager();
                boolean permission = (PackageManager.PERMISSION_GRANTED ==
                        pm.checkPermission("android.permission.RECORD_AUDIO", "packageName"));
                if (permission) {
                    showToast("有这个权限");
                    if(usernumber!=null && !usernumber.equals("")){
                        Intent intent = new Intent(Intent.ACTION_CALL , Uri.parse("tel:" + usernumber));
                        //相应事件
                        MainActivity.this.startActivity(intent);
                    }

                }else {
                    showToast("木有这个权限");
                }





                if(usernumber==null || usernumber.equals("") ||
                        usermessage==null || usermessage.equals("")){
                    showToast("号码或者信息不正确");
                }
                else{
                    sendTextMsg(usernumber,usermessage);
                    Log.i("lzk","send ssmmsg");
                }*/


                if(ll!=null && !ll.equals("")) {
                    Uri uri = Uri.parse("http://api.map.baidu.com/marker?location=" +
                            ll.latitude + "," + ll.longitude + "&title=我的位置&content=百度奎科大厦" +
                            "&output=html&src=yourComponyName|yourAppName");

                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                    startActivity(intent);
                }


            }
        });



        CheckMessage = (CheckBox) findViewById(R.id.checkMessage);
        CheckMessage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch (buttonView.getId()) {

                    case R.id.checkMessage:
                        if (isChecked) {
                            isSendMessage = true;
                            Toast.makeText(getBaseContext(), "CheckBox 01 check ", Toast.LENGTH_SHORT).show();
                        } else {
                            isSendMessage = false;
                            Toast.makeText(getBaseContext(), "CheckBox 01 ucheck ", Toast.LENGTH_SHORT).show();
                        }

                }
            }
        });


        reiceivedview=(TextView)findViewById(R.id.receivedmsg);

        // 注册自定义动态广播消息
        bluetoothServiceReceiver = new BluetoothServiceReceiver();
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(SEND_BLUETOOTH_STATUS);
        intentFilter.addAction(GET_BLUETOOTH_DATA);
        registerReceiver(bluetoothServiceReceiver, intentFilter);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.i("lzk", "destroy");
        unregisterReceiver(bluetoothServiceReceiver);
        bluetoothService.closeBluetooth();
        unbindService(bluetoothServiceConnection);
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
                namedisplay.setText(usernumber + " (" + username + ")");
            }

        }
    }

    /**
     * 设置定位参数
     */
    private void setLocationOption() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        int span=1000;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        locationClient.setLocOption(option);
    }


    public class BluetoothServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {


            if(intent.getAction().equals(SEND_BLUETOOTH_STATUS)){
                int status=intent.getIntExtra("status",-1);
                if(status==0){
                    showToast("连接成功");
                }
                else if(status==1){
                    showToast("连接失败");
                }
            }
            if(intent.getAction().equals(GET_BLUETOOTH_DATA)){
                int[] readdate=intent.getIntArrayExtra("readdata");
                Message message = new Message();
                message.what = 1;
                message.obj=readdate;
                updatehandler.sendMessage(message);
                Log.i("lzk", "receive msg:" + readdate[0]);
            }
        }

    }

    private Handler updatehandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            int[] readdata;
            switch (msg.what) {
                case 1:
                    readdata = (int[]) msg.obj;
                   // readdata[0]为发送字母的ascii码
                    reiceivedview.setText("lzk:"+readdata[0]);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    //选择蓝牙模块
    public void SelectBluetooth() {
        if(!bluetoothService.isConnect()){
            Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class); //跳转程序设置
            //      serverIntent.putExtra("UserId", user.getId());
            serverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(serverIntent);
        }

    }


    private void sendTextMsg(String number, String message){
        String SENT = "sms_sent";
        String DELIVERED = "sms_delivered";

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

        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent){
                switch(getResultCode())
                {
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
        smsm.sendTextMessage(number, null, message, sentPI, deliveredPI);
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
            if(type==61 || type==65|| type==66|| type==161){
                ll=new LatLng(location.getLatitude(),location.getLongitude());
                reiceivedview.setText("http://api.map.baidu.com/marker?location=" +
                        ll.latitude + "," + ll.longitude + "&title=我的位置&content=百度奎科大厦" +
                        "&output=html&src=yourComponyName|yourAppName");
            }
            else {
                ll=null;
                reiceivedview.setText("wrong");
            }



        }
    };


    private ServiceConnection bluetoothServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.ServiceBinder mybinder=(BluetoothService.ServiceBinder) service;
            bluetoothService = mybinder.getService();
        }
        public void onServiceDisconnected(ComponentName name) {
            bluetoothService = null;
        }
    };


    public void showToast(String text) {
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }
}
