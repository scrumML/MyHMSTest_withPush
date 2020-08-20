/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.app.huawei;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.app.huawei.camera.camera.MainTransferActivity;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.InterstitialAd;
import com.huawei.hms.ads.banner.BannerView;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.common.ResolvableApiException;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationAvailability;
import com.huawei.hms.location.LocationCallback;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationResult;
import com.huawei.hms.location.LocationServices;
import com.huawei.hms.location.LocationSettingsRequest;
import com.huawei.hms.location.LocationSettingsResponse;
import com.huawei.hms.location.LocationSettingsStatusCodes;
import com.huawei.hms.location.SettingsClient;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
//
//import com.huawei.hms.ads.banner
import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hmssample2.HuaweiLocationActivity;

import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Define requestCode.
     */
    public static final int CAMERA_REQ_CODE = 111;
    public static final int DEFINED_CODE = 222;
    public static final int BITMAP_CODE = 333;
    public static final int MULTIPROCESSOR_SYN_CODE = 444;
    public static final int MULTIPROCESSOR_ASYN_CODE = 555;
    public static final int GENERATE_CODE = 666;
    public static final int DECODE = 1;
    public static final int GENERATE = 2;
    private static final int REQUEST_CODE_SCAN_ONE = 0X01;
    private static final int REQUEST_CODE_DEFINE = 0X0111;
    private static final int REQUEST_CODE_SCAN_MULTI = 0X011;
    public static final String DECODE_MODE = "decode_mode";
    public static final String RESULT = "SCAN_RESULT";
    //给插屏广告 提供 TAG
    private static final String TAG = MainActivity.class.getSimpleName();
    public InterstitialAd interstitialAd;

    // the callback of the request
    LocationCallback mLocationCallback;

    LocationRequest mLocationRequest;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private SettingsClient settingsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_mwcmain);
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        //Set noTitleBar.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }
        }

        /**
         * BannerView广告
         * */
        // 获取BannerView
        BannerView bannerView = findViewById(R.id.hw_banner_view);
        // 设置广告位ID和大小，"testw6vs28auh3"为专用的测试广告位ID
        bannerView.setAdId("testw6vs28auh3");
        bannerView.setBannerAdSize(BannerAdSize.BANNER_SIZE_360_57);
        // 创建广告请求，获取广告
        AdParam adParam = new AdParam.Builder().build();
        bannerView.loadAd(adParam);
        // 添加监听器
        bannerView.setAdListener(adListener);

//        //添加 location kit 监听
//        Button button_location = findViewById(R.id.button_location);
//        button_location.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, RequestLocationUpdatesWithCallbackActivity.class);
//                startActivity(intent);
//            }
//        });



        // create fusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // create settingsClient
        settingsClient = LocationServices.getSettingsClient(this);
        mLocationRequest = new LocationRequest();
        // Set the interval for location updates, in milliseconds.
        mLocationRequest.setInterval(10000);
        // set the priority of the request
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (null == mLocationCallback) {
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null) {
                        List<Location> locations = locationResult.getLocations();
                        if (!locations.isEmpty()) {
                            for (Location location : locations) {
                                Log.i(TAG,
                                        "onLocationResult location[Longitude,Latitude,Accuracy]:" + location.getLongitude()
                                                + "," + location.getLatitude() + "," + location.getAccuracy());
                            }
                        }
                    }
                }

                @Override
                public void onLocationAvailability(LocationAvailability locationAvailability) {
                    if (locationAvailability != null) {
                        boolean flag = locationAvailability.isLocationAvailable();
                        Log.i(TAG, "onLocationAvailability isLocationAvailable:" + flag);
                    }
                }
            };
        }

        // check location permisiion
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Log.i(TAG, "sdk < 28 Q");
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] strings =
                        {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
                ActivityCompat.requestPermissions(this, strings, 1);
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    "android.permission.ACCESS_BACKGROUND_LOCATION") != PackageManager.PERMISSION_GRANTED) {
                String[] strings = {android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        "android.permission.ACCESS_BACKGROUND_LOCATION"};
                ActivityCompat.requestPermissions(this, strings, 2);
            }
        }

    }

    /**
     * Ad listener.（Banner广告）
     */
    private AdListener adListener = new AdListener() {
        @Override
        public void onAdLoaded() {
            // Called when an ad is loaded successfully.
            showToast("Ad loaded.");
        }

        @Override
        public void onAdFailed(int errorCode) {
            // Called when an ad fails to be loaded.
            showToast(String.format(Locale.ROOT, "Ad failed to load with error code %d.", errorCode));
        }

        @Override
        public void onAdOpened() {
            // Called when an ad is opened.
            showToast(String.format("Ad opened "));
        }

        @Override
        public void onAdClicked() {
            // Called when a user taps an ad.
            showToast("Ad clicked");
        }

        @Override
        public void onAdLeave() {
            // Called when a user has left the app.
            showToast("Ad Leave");
        }

        @Override
        public void onAdClosed() {
            // Called when an ad is closed.
            showToast("Ad closed");
        }
    };
    //吐司
    private void showToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

//    /**
//     * Call RequestLocationUpdatesWithCallbackActivity.定位 Kit
//     */
//    public void openLocationBtnClick(View view) {
//        startActivity(new Intent(MainActivity.this, RequestLocationUpdatesWithCallbackActivity.class));
//    }

    /**
     * Call BankCardRecognitionActivity.
     */
    public void loadBankCardRecognitionBtnClick(View view) {
        startActivity(new Intent(MainActivity.this, BankCardRecognitionActivity.class));
    }
    public void loadIdCardRecognitionBtnClick(View view){
        startActivity(new Intent(MainActivity.this, IDCardRecognitionActivity.class));
    }
    public void loadTextRecognitionBtnClick(View view){
        // Text recognition
        startActivity(new Intent(MainActivity.this, TextRecognitionActivity.class));
    }
    public void loadTextRecognitionPicBtnClick(View view){
        //todo 图片识别翻译入口
        startActivity(new Intent(MainActivity.this, MainTransferActivity.class));
    }
    /**
     * Call the default view.
     */
    public void loadScanKitBtnClick(View view) {
       requestPermission(CAMERA_REQ_CODE, DECODE);
    }

    /**
     * Call the customized view.
     */
    public void newViewBtnClick(View view) {
        requestPermission(DEFINED_CODE, DECODE);
    }

    /**
     * Call the bitmap.
     */
    public void bitmapBtnClick(View view) {
        requestPermission(BITMAP_CODE, DECODE);
    }

    /**
     * Call the MultiProcessor API in synchronous mode.
     */
    public void multiProcessorSynBtnClick(View view) {
        requestPermission(MULTIPROCESSOR_SYN_CODE, DECODE);
    }

    /**
     * Call the MultiProcessor API in asynchronous mode.
     */
    public void multiProcessorAsynBtnClick(View view) {
        requestPermission(MULTIPROCESSOR_ASYN_CODE, DECODE);
    }
public void openLocationBtnClick(View view){
    startActivity(new Intent(MainActivity.this, RequestLocationUpdatesWithCallbackActivity.class));

}
    /**
     * Start generating the barcode. 生成 QR Code, 并 启动插屏广告
     */
    public void generateQRCodeBtnClick(View view) {
        requestPermission(GENERATE_CODE, GENERATE);

//        //添加 插屏 广告
//        InterstitialAd interstitialAd = new InterstitialAd(this);
//        interstitialAd.setAdId("teste9ih9j0rc3"); // "testb4znbuh3n2"为专用的测试广告位ID
//        interstitialAd.setAdListener(adListener2);
//        //获取插屏广告。
//        AdParam adParam = new AdParam.Builder().build();
//        interstitialAd.loadAd(adParam);
    }

//    Button button_interstitial = findViewById(R.id.button_interstitial);
//    button_interstitial.setOnClickLisene

//    // 为插屏广告， 添加 监听器2
//    private AdListener adListener2 = new AdListener() {
//        @Override
//        public void onAdLoaded() {
//            super.onAdLoaded();
//            Toast.makeText(MainActivity.this, "Ad loaded", Toast.LENGTH_SHORT).show();
//            // Display an interstitial ad.
//            showInterstitial();
//        }
//
//        @Override
//        public void onAdFailed(int errorCode) {
//            Toast.makeText(MainActivity.this, "Ad load failed with error code: " + errorCode,
//                    Toast.LENGTH_SHORT).show();
//            Log.d(TAG, "Ad load failed with error code: " + errorCode);
//        }
//
//        @Override
//        public void onAdClosed() {
//            super.onAdClosed();
//            Toast.makeText(MainActivity.this, "Ad closed", Toast.LENGTH_SHORT).show();
//            Log.d(TAG, "onAdClosed");
//        }
//
//        @Override
//        public void onAdClicked() {
//            Log.d(TAG, "onAdClicked");
//            super.onAdClicked();
//        }
//
//        @Override
//        public void onAdOpened() {
//            Log.d(TAG, "onAdOpened");
//            super.onAdOpened();
//        }
//    };
//
//    private void showInterstitial() {
//        // Display an interstitial ad.
//        if (interstitialAd != null && interstitialAd.isLoaded()) {
//            interstitialAd.show();
//        } else {
//            Toast.makeText(this, "Ad did not load", Toast.LENGTH_SHORT).show();
//        }
//    }
//
// 第一次改动？

    /**
     * function：Requests location updates with a callback on the specified Looper thread.
     * first：use SettingsClient object to call checkLocationSettings(LocationSettingsRequest locationSettingsRequest) method to check device settings.
     * second： use  FusedLocationProviderClient object to call requestLocationUpdates (LocationRequest request, LocationCallback callback, Looper looper) method.
     */
    private void requestLocationUpdatesWithCallback() {
        try {
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
            builder.addLocationRequest(mLocationRequest);
            LocationSettingsRequest locationSettingsRequest = builder.build();
            // check devices settings before request location updates.
            settingsClient.checkLocationSettings(locationSettingsRequest)
                    .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                            Log.i(TAG, "check location settings success");
                            // request location updates
                            fusedLocationProviderClient
                                    .requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.i(TAG, "requestLocationUpdatesWithCallback onSuccess");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(Exception e) {
                                            Log.e(TAG,
                                                    "requestLocationUpdatesWithCallback onFailure:" + e.getMessage());
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "checkLocationSetting onFailure:" + e.getMessage());
                            int statusCode = ((ApiException) e).getStatusCode();
                            switch (statusCode) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    try {
                                        ResolvableApiException rae = (ResolvableApiException) e;
                                        rae.startResolutionForResult(MainActivity.this, 0);
                                    } catch (IntentSender.SendIntentException sie) {
                                        Log.e(TAG, "PendingIntent unable to execute request.");
                                    }
                                    break;
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "requestLocationUpdatesWithCallback exception:" + e.getMessage());
        }
    }

    /**
     * Apply for permissions.
     */
    private void requestPermission(int requestCode, int mode) {
        if (mode == DECODE) {
            decodePermission(requestCode);
        } else if (mode == GENERATE) {
            generatePermission(requestCode);
        }
    }

    /**
     * Apply for permissions.
     */
    private void decodePermission(int requestCode) {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                requestCode);
    }

    /**
     * Apply for permissions.
     */
    private void generatePermission(int requestCode) {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                requestCode);
    }

    /**
     * Call back the permission application result. If the permission application is successful, the barcode scanning view will be displayed.
     * @param requestCode Permission application code.
     * @param permissions Permission array.
     * @param grantResults: Permission application result array.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (permissions == null || grantResults == null) {
            return;
        }

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == GENERATE_CODE) {
            Intent intent = new Intent(this, GenerateCodeActivity.class);
            this.startActivity(intent);
        }

        if (grantResults.length < 2 || grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //Default View Mode
        if (requestCode == CAMERA_REQ_CODE) {
            ScanUtil.startScan(this, REQUEST_CODE_SCAN_ONE, new HmsScanAnalyzerOptions.Creator().create());
        }
        //Customized View Mode
        if (requestCode == DEFINED_CODE) {
            Intent intent = new Intent(this, DefinedActivity.class);
            this.startActivityForResult(intent, REQUEST_CODE_DEFINE);
        }
        //Bitmap Mode
        if (requestCode == BITMAP_CODE) {
            Intent intent = new Intent(this, CommonActivity.class);
            intent.putExtra(DECODE_MODE, BITMAP_CODE);
            this.startActivityForResult(intent, REQUEST_CODE_SCAN_MULTI);
        }
        //Multiprocessor Synchronous Mode
        if (requestCode == MULTIPROCESSOR_SYN_CODE) {
            Intent intent = new Intent(this, CommonActivity.class);
            intent.putExtra(DECODE_MODE, MULTIPROCESSOR_SYN_CODE);
            this.startActivityForResult(intent, REQUEST_CODE_SCAN_MULTI);
        }
        //Multiprocessor Asynchronous Mode
        if (requestCode == MULTIPROCESSOR_ASYN_CODE) {
            Intent intent = new Intent(this, CommonActivity.class);
            intent.putExtra(DECODE_MODE, MULTIPROCESSOR_ASYN_CODE);
            this.startActivityForResult(intent, REQUEST_CODE_SCAN_MULTI);
        }
    }

    /**
     * Event for receiving the activity result.
     *
     * @param requestCode Request code.
     * @param resultCode Result code.
     * @param data        Result.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        //Default View
        if (requestCode == REQUEST_CODE_SCAN_ONE) {
            HmsScan obj = data.getParcelableExtra(ScanUtil.RESULT);
            if (obj != null) {
                Intent intent = new Intent(this, DisPlayActivity.class);
                intent.putExtra(RESULT, obj);
                startActivity(intent);
            }
            //MultiProcessor & Bitmap
        } else if (requestCode == REQUEST_CODE_SCAN_MULTI) {
            Parcelable[] obj = data.getParcelableArrayExtra(CommonActivity.SCAN_RESULT);
                if (obj != null && obj.length > 0) {
                    //Get one result.
                    if (obj.length == 1) {
                        if (obj[0] != null && !TextUtils.isEmpty(((HmsScan) obj[0]).getOriginalValue())) {
                            Intent intent = new Intent(this, DisPlayActivity.class);
                            intent.putExtra(RESULT, obj[0]);
                            startActivity(intent);
                        }
                    } else {
                        Intent intent = new Intent(this, DisPlayMulActivity.class);
                        intent.putExtra(RESULT, obj);
                        startActivity(intent);
                    }
                }
            //Customized View
        } else if (requestCode == REQUEST_CODE_DEFINE) {
            HmsScan obj = data.getParcelableExtra(DefinedActivity.SCAN_RESULT);
            if (obj != null) {
                Intent intent = new Intent(this, DisPlayActivity.class);
                intent.putExtra(RESULT, obj);
                startActivity(intent);
            }
        }
    }


    public void openLocationKitBtnClick(View view) {
        startActivity(new Intent(MainActivity.this, HuaweiLocationActivity.class));
    }

    public void openPushKitBtnClick(View view) {
        startActivity(new Intent(MainActivity.this, com.huawei.codelabpush.MainActivity.class));
    }
}
