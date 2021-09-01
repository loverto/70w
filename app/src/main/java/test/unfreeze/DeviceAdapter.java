package test.unfreeze;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;
import java.util.ArrayList;
import java.util.List;


public class DeviceAdapter extends BaseAdapter {

    private Context context;

    private List<BleDevice> bleDevices = new ArrayList();

    private DeviceAdapterBleDeviceComplete deviceAdapterBleDeviceComplete;


    public interface DeviceAdapterBleDeviceComplete {
        void clickComplete(BleDevice bleDevice);
    }

    public long getItemId(int i) {
        return 0;
    }

    public DeviceAdapter(Context context) {
        this.context = context;
    }

    public void add(BleDevice bleDevice) {
        addObtain(bleDevice);
        this.bleDevices.add(bleDevice);
    }

    public void addObtain(BleDevice bleDevice) {
        for (int i = 0; i < this.bleDevices.size(); i++) {
            if (bleDevice.getKey().equals(this.bleDevices.get(i).getKey())) {
                this.bleDevices.remove(i);
            }
        }
    }

    public void removeObtain() {
        for (int i = 0; i < this.bleDevices.size(); i++) {
            if (!BleManager.getInstance().isConnected(this.bleDevices.get(i))) {
                this.bleDevices.remove(i);
            }
        }
    }

    public int getCount() {
        return this.bleDevices.size();
    }

    public BleDevice getItem(int i) {
        if (i > this.bleDevices.size()) {
            return null;
        }
        return this.bleDevices.get(i);
    }

    public View getView(int i, View view, ViewGroup viewGroup) {
        Hoodler bVar;
        if (view != null) {
            bVar = (Hoodler) view.getTag();
        } else {
            view = View.inflate(this.context, R.layout.adapter_device, null);
            bVar = new Hoodler();
            view.setTag(bVar);
            bVar.nameTv = view.findViewById(R.id.txt_name);
            bVar.macTv = view.findViewById(R.id.txt_mac);
            bVar.selectTv = view.findViewById(R.id.select);
        }
        final BleDevice bleDevice = getItem(i);
        if (bleDevice != null) {
            String name = bleDevice.getName();
            String ma = bleDevice.getMac();
            bVar.nameTv.setText(name);
            bVar.macTv.setText(ma);
        }
        bVar.selectTv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (DeviceAdapter.this.deviceAdapterBleDeviceComplete != null) {
                    DeviceAdapter.this.deviceAdapterBleDeviceComplete.clickComplete(bleDevice);
                }
            }
        });
        return view;
    }

    class Hoodler {

        TextView nameTv;

        TextView macTv;

        Button selectTv;

        Hoodler() {
        }
    }

    public void addDeviceCompleteListen(DeviceAdapterBleDeviceComplete anDeviceAdapterBleDeviceComplete) {
        this.deviceAdapterBleDeviceComplete = anDeviceAdapterBleDeviceComplete;
    }
}
