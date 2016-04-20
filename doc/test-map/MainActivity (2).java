package com.example.jk.test;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.share.LocationShareURLOption;
import com.baidu.mapapi.search.share.OnGetShareUrlResultListener;
import com.baidu.mapapi.search.share.ShareUrlResult;
import com.baidu.mapapi.search.share.ShareUrlSearch;


public class MainActivity extends Activity {
    // 定位相关声明
    public LocationClient locationClient = null;
    private ShareUrlSearch mShareUrlSearch = null;
    private LatLng ll=null;
    private LatLng ll2=null;
    private String address=null;
    private LatLng mPoint = new LatLng(40.056878, 116.308141);
    private GeoCoder mGeoCoder = null;
    private SDKReceiver mReceiver;

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

    public OnGetGeoCoderResultListener myGeoResultListener=new OnGetGeoCoderResultListener(){

        @Override
        public void onGetGeoCodeResult(GeoCodeResult result) {

        }

        @Override
        public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
            if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(MainActivity.this, "抱歉，未找到结果",
                        Toast.LENGTH_LONG).show();
                return;
            }
            ll2=result.getLocation();
            address=result.getAddress();
            mShareUrlSearch
                    .requestLocationShareUrl(new LocationShareURLOption()
                            .location(ll2).snippet("测试分享点")
                            .name(address));
        }
    };

    public OnGetShareUrlResultListener myShareUrlResultListener=new OnGetShareUrlResultListener(){

        @Override
        public void onGetPoiDetailShareUrlResult(ShareUrlResult shareUrlResult) {
            if (shareUrlResult.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(MainActivity.this, "抱歉，未搜索到短串",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "poi详情分享url：" + shareUrlResult.getUrl(),
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onGetLocationShareUrlResult(ShareUrlResult shareUrlResult) {
            if (shareUrlResult.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(MainActivity.this, "抱歉，未搜索到短串",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this,  "请求位置信息分享url：" + shareUrlResult.getUrl(),
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onGetRouteShareUrlResult(ShareUrlResult shareUrlResult) {
            if (shareUrlResult.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(MainActivity.this, "抱歉，未搜索到短串",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this,  "请求位置信息分享url：" + shareUrlResult.getUrl(),
                        Toast.LENGTH_LONG).show();
            }
        }
    };



    public BDLocationListener myListener = new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            //Receive Location
            int type=location.getLocType();
            Log.i("lzk","type"+type);

            ll=new LatLng(location.getLatitude(),location.getLongitude());

            StringBuffer sb = new StringBuffer(256);
            sb.append("time : ");
            sb.append(location.getTime());
            sb.append("\nerror code : ");
            sb.append(location.getLocType());
            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());
            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());
            sb.append("\nradius : ");
            sb.append(location.getRadius());
            if (location.getLocType() == BDLocation.TypeGpsLocation){// GPS定位结果
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());// 单位：公里每小时
                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());
                sb.append("\nheight : ");
                sb.append(location.getAltitude());// 单位：米
                sb.append("\ndirection : ");
                sb.append(location.getDirection());// 单位度
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append("\ndescribe : ");
                sb.append("gps定位成功");

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation){// 网络定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                //运营商信息
                sb.append("\noperationers : ");
                sb.append(location.getOperators());
                sb.append("\ndescribe : ");
                sb.append("网络定位成功");
            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                sb.append("\ndescribe : ");
                sb.append("离线定位成功，离线定位结果也是有效的");
            } else if (location.getLocType() == BDLocation.TypeServerError) {
                sb.append("\ndescribe : ");
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                sb.append("\ndescribe : ");
                sb.append("网络不同导致定位失败，请检查网络是否通畅");
            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                sb.append("\ndescribe : ");
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
            }

            Log.i("BaiduLocationApiDem", sb.toString());
        }
    };

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

        mShareUrlSearch = ShareUrlSearch.newInstance();
        mShareUrlSearch.setOnGetShareUrlResultListener(myShareUrlResultListener);

        mGeoCoder = GeoCoder.newInstance();
        mGeoCoder.setOnGetGeoCodeResultListener(myGeoResultListener);

        Button btn=(Button)findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // locationClient.start();
                locationClient.stop();
          /*      if(ll==null || ll.equals("")){
                    Log.i("lzk","not located");
                }
                else{
                    Log.i("lzk", "located");
                    LocationShareURLOption locationoption=new LocationShareURLOption();
                    locationoption.location(ll);
                    locationoption.name("my position");
                    locationoption.snippet("我的位置");
                    mShareUrlSearch.requestLocationShareUrl(locationoption);
                }*/

                // 发起反地理编码请求
                /*
                mGeoCoder.reverseGeoCode(new ReverseGeoCodeOption().location(mPoint));
                Toast.makeText(
                        MainActivity.this,
                        String.format("搜索位置： %f，%f", mPoint.latitude, mPoint.longitude),
                        Toast.LENGTH_SHORT).show();*/
                mShareUrlSearch
                        .requestLocationShareUrl(new LocationShareURLOption()
                                .location(mPoint).snippet("测试分享点")
                                .name("sb-"));


            }
        });


        locationClient = new LocationClient(getApplicationContext()); // 实例化LocationClient类
        locationClient.registerLocationListener(myListener); // 注册监听函数
        this.setLocationOption();	//设置定位参数
        locationClient.start(); // 开始定位

    }

    // 三个状态实现地图生命周期管理
    @Override
    protected void onDestroy() {
        //退出时销毁定位
        unregisterReceiver(mReceiver);
        mShareUrlSearch.destroy();
        locationClient.stop();
        super.onDestroy();

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
}
