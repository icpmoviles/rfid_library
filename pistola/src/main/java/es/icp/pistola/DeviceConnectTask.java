package es.icp.pistola;

import android.os.AsyncTask;

import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;

import es.icp.logs.core.MyLog;

public class DeviceConnectTask extends AsyncTask<Void, String, Boolean> {
    private final RFIDReader connectingDevice;
    private String prgressMsg;
    private OperationFailureException ex;
    private int pos;

    public DeviceConnectTask(int pos, RFIDReader connectingDevice, String prgressMsg) {
        this.connectingDevice = connectingDevice;
        this.prgressMsg = prgressMsg;
        this.pos = pos;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... a) {
        if (!connectingDevice.isConnected()) {
            RFIDHandlerV2.reader = connectingDevice;
//                mConnectedReader = RFIDHandlerV2.reader;
//                StoreConnectedReader(pos); // todo: revisar si es posible eliminar este método antiguo
            MyLog.d("INTENTANDO CONTECTAR CON ZEBRAMANAGER : " + RFIDHandlerV2.reader.getHostName());
            RfidGlobalVariables.ZEBRA_MANAGER.connect();
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
    }

    @Override
    protected void onCancelled() {
        RfidGlobalVariables.deviceConnectTask = null;
        super.onCancelled();
    }
}