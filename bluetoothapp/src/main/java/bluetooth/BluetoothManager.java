package bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothManager {
    private static BluetoothManager bluetoothManager = null;
    private BluetoothAdapter bluetooth = null;
    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP服务UUID号
    private BluetoothDevice device = null;     //蓝牙设备
    private BluetoothSocket socket = null;      //蓝牙通信socket
    private String address=null;

    private OutputStream os;   //蓝牙连接输出流
    private InputStream is;    //输入流，用来接收蓝牙数据

    public BluetoothSocket getSocket() {
        return socket;
    }

    private BluetoothManager() {
        bluetooth = BluetoothAdapter.getDefaultAdapter();
    }

    public static BluetoothManager getInstance() {

        if (bluetoothManager == null) {
            bluetoothManager = new BluetoothManager();
        }
        return bluetoothManager;
    }



    public BluetoothAdapter getBluetoothAdapter(){

        return bluetooth;

    }


    public void setAddress(String address) {
        this.address=address;
    }

    public String getAddress() {
        return address;
    }

    public Boolean isSetAddress() {
        if(address==null || address.equals(""))
            return false;
        else
            return true;
    }

    public Boolean isConnect() {
        if(socket==null || !socket.isConnected())
            return false;
        else
            return true;
    }

    //蓝牙配对连接
    public BluetoothSocket connectBluetooth(){
        // 得到蓝牙设备句柄
        device = bluetooth.getRemoteDevice(address);

        // 用服务号得到socket
        try{
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
        }catch(IOException e){
            Log.i("lzk","连接失败1！");
        }
        //连接socket
        try{
            socket.connect();
            if(socket!=null){
                os = socket.getOutputStream();   //蓝牙连接输出流
                is = socket.getInputStream();   //得到蓝牙数据输入流
            }
            Log.i("lzk","连接ok！");
        }catch(IOException e){
            try{
                Log.i("lzk", "连接失败3！");
                if (is!=null)
                        is.close();
                if (os!=null)
                    os.close();
                socket.close();
                socket = null;
            }catch(IOException ee){
                Log.i("lzk","连接失败4！");
            }
        }
        return socket;
    }


    public void closeSocket() {
        if(os!=null)
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if(socket!=null)  //关闭连接socket
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    //关闭蓝牙
    public void destroyBluetooth() {
        closeSocket();
        bluetooth.disable();  //关闭蓝牙服务

    }


    //蓝牙发送数据
    public void SendCmd(byte[] cmd)//串口发送数据
    {
        if(socket!=null){
            try {
                os.write(cmd, 0, cmd.length);
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public InputStream getInputStream() {
        return is;
    }




}





