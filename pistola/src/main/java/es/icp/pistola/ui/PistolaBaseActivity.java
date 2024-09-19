package es.icp.pistola.ui;

import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.zebra.rfid.api3.TagData;

import es.icp.logs.core.MyLog;
import es.icp.pistola.Barcode;
import es.icp.pistola.ConnectedHandlerInterface;
import es.icp.pistola.ObserverPreparedZebraManager;
import es.icp.pistola.ResponseHandlerInterface;
import es.icp.pistola.RfidGlobalVariables;
import es.icp.pistola.ZebraManagerV2;

public abstract class PistolaBaseActivity extends AppCompatActivity implements ObserverPreparedZebraManager {

    // PROPIEDADES

    public ResponseHandlerInterface responseHandlerInterface;
    public ConnectedHandlerInterface connectedHandlerInterface;
    private static boolean preparedZebraManager = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RfidGlobalVariables.observerPreparedZebraManager = this;
        if (RfidGlobalVariables.ZEBRA_MANAGER == null) {
            this.inicializarZebraManager();
        } else {
            RfidGlobalVariables.ZEBRA_MANAGER.setConnectedHandler(defaultConnection);
        }
    }


    public Activity getActivity() {
        return this;
    }

    private ResponseHandlerInterface defaultResponse = new ResponseHandlerInterface() {

        @Override
        public void handleTagdata(final TagData[] tagDatas) {
            if (RfidGlobalVariables.RESPONSE_HANDLER != null) RfidGlobalVariables.RESPONSE_HANDLER.handleTagdata(tagDatas);
        }

        // EVENTO LECTURA BARCODE

        @Override
        public void handleBarcode(final Barcode barcode) {
            if (RfidGlobalVariables.RESPONSE_HANDLER != null) RfidGlobalVariables.RESPONSE_HANDLER.handleBarcode(barcode);
        }

        // EVENTO DE GATILLO PISTOLA

        @Override
        public void handleTriggerPress(boolean pressed) {
            if (RfidGlobalVariables.RESPONSE_HANDLER != null) RfidGlobalVariables.RESPONSE_HANDLER.handleTriggerPress(pressed);
        }
    };

    protected ConnectedHandlerInterface defaultConnection = new ConnectedHandlerInterface() {
        @Override
        public void handleConnection(final boolean connected) {
            if (connected) {
                PistolaBaseActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //showUniversalNotification(getString(R.string.conectado) + Constantes.space + RfidGlobalVariables.LAST_CONNECTED_READER);
                        if (RfidGlobalVariables.observerConexionPistola != null) {
                            RfidGlobalVariables.observerConexionPistola.status(connected);
                        }

                    }
                });
            } else {
                RfidGlobalVariables.CONNECTED_READER = null;
                RfidGlobalVariables.deviceConnectTask = null;
                RfidGlobalVariables.ZEBRA_MANAGER.disconnect();
                RfidGlobalVariables.CONNECTED_READER = null;
                RfidGlobalVariables.CONNECTED_DEVICE = null;
                PistolaBaseActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //showUniversalNotification(getString(R.string.error_conexion_intentelo_nuevamente));
                        if (RfidGlobalVariables.BATTERY_DIALOG != null) RfidGlobalVariables.BATTERY_DIALOG.dismiss();
                        if (RfidGlobalVariables.observerConexionPistola != null) {
                            RfidGlobalVariables.observerConexionPistola.status(connected);
                        }
                    }
                });
            }
            PistolaBaseActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() { // para actualizar los items / checkbox
                    try {
                        if (RfidGlobalVariables.ZEBRA_MANAGER != null && RfidGlobalVariables.ZEBRA_MANAGER.getAvailableDevices().size() > 0){
                            if (RfidGlobalVariables.observerConexionPistola != null) {
                                RfidGlobalVariables.observerConexionPistola.status(connected);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
        }

        @Override
        public void handleBattery(int level, boolean charging, String cause) {
            MyLog.d("HANDLE_BATTERY: level: " + level + ", charging: " + charging + ", cause: " + cause);
            RfidGlobalVariables.BATTERY_LEVEL = level;
            RfidGlobalVariables.BATTERY_CHARGING = charging;
            RfidGlobalVariables.BATTERY_EVENT_CAUSE = cause;
        }
    };

    private void inicializarZebraManager() {
        this.responseHandlerInterface = defaultResponse;
        this.connectedHandlerInterface = defaultConnection;
        RfidGlobalVariables.ZEBRA_MANAGER = new ZebraManagerV2(this,
                ZebraManagerV2.DefaultMode.RFID,
                ZebraManagerV2.RunnableMode.BOTH_MODES,
                this.responseHandlerInterface,
                this.connectedHandlerInterface);
    }

    // MÉTODOS CICLO DE VIDA

    @Override
    protected void onPostResume() {
        super.onPostResume();
//        RfidGlobalVariables.universalNotification = this.inicializarUniversalNotification();
//        RfidGlobalVariables.BATTERY_DIALOG = this.batteryDialog;
//        RfidGlobalVariables.universalNotification.setActivity(this);
    }
}
