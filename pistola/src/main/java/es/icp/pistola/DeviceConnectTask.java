package es.icp.pistola;

// ASYNCTASK PARA CONECTAR CON EL DISPOSITIVO SELECCIONADO (ZEBRA-MANAGER)

import android.app.Activity;
import android.os.AsyncTask;

import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;

import es.icp.logs.core.MyLog;

public class DeviceConnectTask extends AsyncTask<Void, String, Boolean> {
    private final RFIDReader connectingDevice;
    private String prgressMsg;
    private OperationFailureException ex;
    private int pos;
    private Activity context;

    public DeviceConnectTask(Activity context, int pos, RFIDReader connectingDevice, String prgressMsg) {
        this.connectingDevice = connectingDevice;
        this.prgressMsg = prgressMsg;
        this.pos = pos;
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RfidGlobalVariables.ZEBRA_MANAGER_PROGRESS_DIALOG = new CustomProgressDialog(context, prgressMsg);
                RfidGlobalVariables.ZEBRA_MANAGER_PROGRESS_DIALOG.show();
            }
        });
    }

    @Override
    protected Boolean doInBackground(Void... a) {
        if (!connectingDevice.isConnected()) {
            // try {
            RFIDHandlerV2.reader = connectingDevice;
            RfidGlobalVariables.LAST_CONNECTED_READER = connectingDevice.getHostName();
            RfidGlobalVariables.LAST_CONNECTED_READER_POS = 1;
            MyLog.d("INTENTANDO CONTECTAR CON ZEBRAMANAGER : " + RFIDHandlerV2.reader.getHostName());
            RfidGlobalVariables.ZEBRA_MANAGER.connect(); // la clase BluetoothLEManager de la librería de ZEBRA no puede crear sus handlers dentro del AsyncTask...
            return true;
                /*}catch (Exception ex)
                {
                    Helper.TratarExcepcion(getApplicationContext(), ex.getMessage(), "BaseMenuActivity.doInBackground", ex, "");
                }
                return false;*/

        } else {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (RfidGlobalVariables.ZEBRA_MANAGER_PROGRESS_DIALOG != null) {
            RfidGlobalVariables.ZEBRA_MANAGER_PROGRESS_DIALOG.cancel();
        }
    }

    @Override
    protected void onCancelled() {
        RfidGlobalVariables.deviceConnectTask = null;
        if (RfidGlobalVariables.ZEBRA_MANAGER_PROGRESS_DIALOG != null) {
            RfidGlobalVariables.ZEBRA_MANAGER_PROGRESS_DIALOG.cancel();
        }
        super.onCancelled();
    }
}
