package test.unfreeze;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import test.unfreeze.util.ChannelCallback;
import test.unfreeze.util.HalfDuplexChannel;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String currentActivityName = "MainActivity";

    private static final UUID uuidService = UUID.fromString("13147200-1000-9000-7000-301291E21220");

    private static final UUID uuidNotify = UUID.fromString("13147201-1000-9000-7000-301291E21220");

    private Button btnScan;

    private ImageView imgLoading;

    private TextView selectedTv;

    private Animation rotateAnimation;

    private DeviceAdapter deviceAdapter;

    private ProgressDialog progressDialog;

    private BleDevice bleDevice;

    private int soundLimit;

    private int speedLimit;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        initView();
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance().setReConnectCount(1, 5000).setConnectOverTime(10000L).setOperateTimeout(5000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id != R.id.btn_scan) {
            switch (id) {
                case R.id.test1:
                    if (this.bleDevice == null) {
                        Toast.makeText(this, "请先选择连接设备", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (!BleManager.getInstance().isConnected(this.bleDevice)) {
                        BleManager.getInstance().cancelScan();
                        // 恢复默认设置
                        this.soundLimit = 0;
                        this.speedLimit = 0;
                        unLockLimit(this.bleDevice);
                        return;
                    } else {
                        return;
                    }
                case R.id.test2:
                    if (this.bleDevice == null) {
                        Toast.makeText(this, "请先选择连接设备", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (!BleManager.getInstance().isConnected(this.bleDevice)) {
                        // 解除15公里超速报警
                        BleManager.getInstance().cancelScan();
                        this.soundLimit = 0;
                        this.speedLimit = 1;
                        unLockLimit(this.bleDevice);
                        return;
                    } else {
                        return;
                    }
                case R.id.test3:
                    if (this.bleDevice == null) {
                        Toast.makeText(this, "请先选择连接设备", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (!BleManager.getInstance().isConnected(this.bleDevice)) {
                        BleManager.getInstance().cancelScan();
                        // 解除25公里限速
                        this.soundLimit = 1;
                        this.speedLimit = 290;
                        unLockLimit(this.bleDevice);
                        return;
                    } else {
                        return;
                    }
                case R.id.test4:
                    if (this.bleDevice == null) {
                        Toast.makeText(this, "请先选择连接设备", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (!BleManager.getInstance().isConnected(this.bleDevice)) {
                        BleManager.getInstance().cancelScan();
                        // 解除35公里限速
                        this.soundLimit = 1;
                        this.speedLimit = 380;
                        unLockLimit(this.bleDevice);
                        return;
                    } else {
                        return;
                    }
                case R.id.test5:
                    if (this.bleDevice == null) {
                        Toast.makeText(this, "请先选择连接设备", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (!BleManager.getInstance().isConnected(this.bleDevice)) {
                        BleManager.getInstance().cancelScan();
                        // 解除40公里限速
                        this.soundLimit = 1;
                        this.speedLimit = 500;
                        unLockLimit(this.bleDevice);
                        return;
                    } else {
                        return;
                    }
                default:
                    return;
            }
        } else if (this.btnScan.getText().equals(getString(R.string.start_scan))) {
            bleManagerPermission();
        } else if (this.btnScan.getText().equals(getString(R.string.stop_scan))) {
            BleManager.getInstance().cancelScan();
        }
    }

    /* renamed from: m */
    private void initView() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        this.btnScan = (Button) findViewById(R.id.btn_scan);
        this.btnScan.setText(getString(R.string.start_scan));
        this.btnScan.setOnClickListener(this);
        this.selectedTv = (TextView) findViewById(R.id.selected);
        this.imgLoading = (ImageView) findViewById(R.id.img_loading);
        this.rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        this.rotateAnimation.setInterpolator(new LinearInterpolator());
        this.progressDialog = new ProgressDialog(this);
        this.deviceAdapter = new DeviceAdapter(this);
        this.deviceAdapter.addDeviceCompleteListen(new DeviceAdapter.DeviceAdapterBleDeviceComplete() {
            @Override
            public void clickComplete(BleDevice bleDevice) {
                MainActivity.this.bleDevice = bleDevice;
                StringBuilder sb = new StringBuilder();
                sb.append("选中：");
                sb.append(MainActivity.this.bleDevice.getName() == null ? MainActivity.this.bleDevice.getMac() : MainActivity.this.bleDevice.getName());
                MainActivity.this.selectedTv.setText(sb.toString());
            }
        });
        ((ListView) findViewById(R.id.list_device)).setAdapter((ListAdapter) this.deviceAdapter);
        findViewById(R.id.test1).setOnClickListener(this);
        findViewById(R.id.test2).setOnClickListener(this);
        findViewById(R.id.test3).setOnClickListener(this);
        findViewById(R.id.test4).setOnClickListener(this);
        findViewById(R.id.test5).setOnClickListener(this);
    }

    private void bleManagerInit() {
        BleManager.getInstance().initScanRule(new BleScanRuleConfig.Builder().setScanTimeOut(10000).build());
    }

    private void bleManagerScan() {
        BleManager.getInstance().scan(new BleScanCallback() {

            @Override
            public void onScanStarted(boolean z) {
                MainActivity.this.deviceAdapter.mo4169a();
                MainActivity.this.deviceAdapter.notifyDataSetChanged();
                MainActivity.this.imgLoading.startAnimation(MainActivity.this.rotateAnimation);
                MainActivity.this.imgLoading.setVisibility(View.VISIBLE);
                MainActivity.this.btnScan.setText(MainActivity.this.getString(R.string.stop_scan));
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                String bleDeviceName = bleDevice.getName();
                if (bleDeviceName != null && bleDeviceName.contains("MAIOS")) {
                    MainActivity.this.deviceAdapter.add(bleDevice);
                    MainActivity.this.deviceAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onScanFinished(List<BleDevice> list) {
                MainActivity.this.imgLoading.clearAnimation();
                MainActivity.this.imgLoading.setVisibility(View.INVISIBLE);
                MainActivity.this.btnScan.setText(MainActivity.this.getString(R.string.start_scan));
            }
        });
    }

    private void unLockLimit(BleDevice bleDevice2) {
        BleManager.getInstance().connect(bleDevice2, new BleGattCallback() {

            @Override
            public void onStartConnect() {
                MainActivity.this.progressDialog.show();
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException aVar) {
                MainActivity.this.imgLoading.clearAnimation();
                MainActivity.this.imgLoading.setVisibility(View.INVISIBLE);
                MainActivity.this.btnScan.setText(MainActivity.this.getString(R.string.start_scan));
                MainActivity.this.progressDialog.dismiss();
                Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.connect_fail), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt bluetoothGatt, int i) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        BleManager.getInstance().disconnectAllDevice();
                        MainActivity.this.progressDialog.dismiss();
                    }
                }, 500);
                final HalfDuplexChannel halfDuplexChannel = new HalfDuplexChannel(MainActivity.uuidService, MainActivity.uuidNotify, null, bleDevice);
                BleManager.getInstance().notify(bleDevice, MainActivity.uuidService.toString(), MainActivity.uuidNotify.toString(), new BleNotifyCallback() {
                    @Override
                    public void onNotifyFailure(BleException bleException) {
                    }
                    @Override
                    public void onNotifySuccess() {
                        // 定义容量为9的低字节序的数组
                        ByteBuffer byteBuffer = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN);
                        if (MainActivity.this.soundLimit == 0) {
                            byteBuffer.put((byte) 102);
                        } else {
                            byteBuffer.put((byte) 119);
                        }
                        byteBuffer.putInt(4);
                        byteBuffer.putInt(MainActivity.this.speedLimit);
                        halfDuplexChannel.writeData(byteBuffer.array(), 0, new ChannelCallback() {
                            @Override
                            public void channelCallback(int i) {
                            }
                        });
                    }
                    @Override
                    public void onCharacteristicChanged(byte[] bArr) {
                        halfDuplexChannel.dataChange(bArr);
                    }
                });
            }

            @Override
            public void onDisConnected(boolean z, BleDevice bleDevice, BluetoothGatt bluetoothGatt, int i) {
                MainActivity.this.progressDialog.dismiss();
            }
        });
    }

    @Override
    public final void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i == 2 && iArr.length > 0) {
            for (int i2 = 0; i2 < iArr.length; i2++) {
                if (iArr[i2] == 0) {
                    locationPermission(strArr[i2]);
                }
            }
        }
    }

    private void bleManagerPermission() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_SHORT).show();
            return;
        }
        String[] strArr = {"android.permission.ACCESS_FINE_LOCATION"};
        ArrayList arrayList = new ArrayList();
        for (String str : strArr) {
            if (ContextCompat.checkSelfPermission(this, str) == 0) {
                locationPermission(str);
            } else {
                arrayList.add(str);
            }
        }
        if (!arrayList.isEmpty()) {
            ActivityCompat.requestPermissions(this, (String[]) arrayList.toArray(new String[arrayList.size()]), 2);
        }
    }

    private void locationPermission(String str) {
        if (((str.hashCode() == -1888586689 && str.equals("android.permission.ACCESS_FINE_LOCATION")) ? (char) 0 : 65535) == 0) {
            if (Build.VERSION.SDK_INT < 23 || isGetLocation()) {
                bleManagerInit();
                bleManagerScan();
                return;
            }
            new AlertDialog.Builder(this).setTitle(R.string.notifyTitle).setMessage(R.string.gpsNotifyMsg).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.finish();
                }
            }).setPositiveButton(R.string.setting, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.startActivityForResult(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"), 1);
                }
            }).setCancelable(false).show();
        }
    }

    private boolean isGetLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        return locationManager.isProviderEnabled("gps");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == 1 && isGetLocation()) {
            bleManagerInit();
            bleManagerScan();
        }
    }
}
