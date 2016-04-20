package com.example.jk.bluetoothapp;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.TimerTask;


public class MainActivity extends AMainActivity {

    private Button connectbtn,sendbtn,addContactor,saveMeaasge;

    private int property=0x00;


    private EditText messageEdit;
    private TextView connecttv,propertytv,contactortv,locationtv;
    private CheckBox CheckMessage;



    private Handler updatehandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectbtn=(Button)findViewById(R.id.connectbtn);
        connectbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectBluetooth();
            }
        });
        connecttv=(TextView)findViewById(R.id.bluetoothstatus);
        propertytv=(TextView)findViewById(R.id.property);
        propertytv.setText(" " + property + " ");
        sendbtn= (Button) findViewById(R.id.sendblebtn);
        sendbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (property == 0x01) {
                    property = 0x00;
                } else {
                    property = 0x01;
                }
                propertytv.setText(" " + property + " ");
                bluetoothService.sendmsg(property);
            }
        });

        contactortv=(TextView)findViewById(R.id.contactor);
        contactortv.setOnClickListener(contactorlistener);
        addContactor = (Button) findViewById(R.id.namebtn);
        addContactor.setOnClickListener(contactorlistener);
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

        messageEdit=(EditText)findViewById(R.id.messageinfo);
        messageEdit.setFocusable(false);
        messageEdit.setFocusableInTouchMode(false);
        usermessage=messageEdit.getText().toString();
        saveMeaasge = (Button) findViewById(R.id.savemeaasge);
        saveMeaasge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (saveMeaasge.getText().toString().equals("保存")) {
                    saveMeaasge.setText("编辑");
                    usermessage = messageEdit.getText().toString();
                    messageEdit.setFocusable(false);
                    messageEdit.setFocusableInTouchMode(false);
                } else {
                    messageEdit.setFocusableInTouchMode(true);
                    messageEdit.setFocusable(true);
                    messageEdit.requestFocus();
                    saveMeaasge.setText("保存");
                }
            }
        });
        locationtv=(TextView)findViewById(R.id.location);
        locationtv.setText("not located");

        updatehandler = new myHandler();

    }

    private View.OnClickListener contactorlistener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivityForResult(new Intent(
                    Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), 0);
        }
    };

    @Override
    protected void updateProperty(int[] readdate) {
        Message message = new Message();
        message.what = 1;
        message.obj=readdate;
        updatehandler.sendMessage(message);

    }

    @Override
    protected void updateBluetoothStatus(boolean status) {
        Message message = new Message();
        message.what = 2;
        message.obj=status;
        updatehandler.sendMessage(message);
    }

    @Override
    protected void updateLocation(String address) {
//        Message message = new Message();
//        message.what = 3;
//        message.obj=address;
//        updatehandler.sendMessage(message);
        locationtv.setText(address);

    }

    @Override
    protected void showContactor(String username, String usernumber) {
        if(usernumber!=null && !usernumber.equals("") &&
                username!=null && !username.equals(""))
            contactortv.setText(usernumber + " (" + username + ")");
    }

    @Override
    protected void showToast(String text) {
        Message message = new Message();
        message.what = 5;
        message.obj=text;
        updatehandler.sendMessage(message);

    }


    private class myHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 1:

                    int[] readdata = (int[]) msg.obj;
                    // readdata[0]为发送字母的ascii码
                    property=readdata[0];
                    propertytv.setText(" " + property+" ");
                    break;
                case 2:
                    boolean status=(boolean)msg.obj;
                    if(status){
                        connecttv.setText("Connected");
                    }
                    else {
                        connecttv.setText("Not Connected");
                    }
                    break;
                case 3:
                    String address=(String)msg.obj;
                    locationtv.setText(address);
                    break;

                case 5:
                    String text=(String)msg.obj;
                    Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                    break;


                default:
                    break;
            }
            super.handleMessage(msg);
        }

    }

}
