package es.icp.pistola;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.TagData;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import es.icp.logs.core.MyLog;

public class ZebraManagerV2 {

    public RFIDHandlerV2 rfidHandler;
    public BarcodeHandlerV2 barcodeHandler;
    public int mode = -1;
    private DefaultMode defaultMode;
    private RunnableMode runnableMode;
    private Timer timer;
    public static final int RFID_MODE = 0;
    public static final int BARCODE_MODE = 1;
    protected static final int NO_TRIED = 0;
    protected static final int CONNECTED = 1;
    protected static final int DISCONNECTED = 2;
    private static final int TIEMPO_ESPERA_CONEXION = 25000; // tiempo de espera en milisegundos

    public enum DefaultMode {
        RFID,
        BARCODE
    }

    public enum RunnableMode {
        RFID_ONLY,
        BARCODE_ONLY,
        BOTH_MODES
    }

    private int rfidConnection;
    private int barcodeConnection;
    private ConnectedHandlerInterface connectedHandler;
    private ResponseHandlerInterface responseHandler;
    private Context context;
    private boolean inicializado = false;
    private Thread conexion;

    public ZebraManagerV2(Context context, ResponseHandlerInterface responseHandler, ConnectedHandlerInterface connectedHandler) {
        this(context, DefaultMode.RFID, RunnableMode.BOTH_MODES, responseHandler, connectedHandler);
    }

    public ZebraManagerV2(Context context, DefaultMode defaultMode, RunnableMode runnableMode, ResponseHandlerInterface responseHandler, ConnectedHandlerInterface connectedHandler) {
        this.connectedHandler = connectedHandler;
        this.responseHandler = responseHandler;
        this.context = context;
        this.mode = RFID_MODE;
        this.rfidConnection = NO_TRIED;
        this.barcodeConnection = NO_TRIED;
        this.defaultMode = defaultMode;
        this.runnableMode = runnableMode;
        this.inicializado = true;

//        this.connect();
        this.prepare();
    }

    // MÉTODOS DE PREPARACIÓN/INICIALIZACIÓN DE LOS HANDLER

    public void prepare() {
        this.rfidHandler = new RFIDHandlerV2(this.context, RFID_MODE, responseHandler, connectedSonHandler, true);
    }

    // MÉTODOS DE CONEXIÓN/DESCONEXIÓN

    public void connect() {
        if (this.timer != null) timer.cancel();
        if (!this.inicializado) return; // en caso de no haber inicializado el objeto
        if (!configuradorBluetooth())
            return; // en caso de error de bluetooth, no se procede a la conexión
        //if (this.rfidHandler != null || this.barcodeHandler != null) return ; // en caso de haber una conexión en proceso

        if (this.conexion != null && this.conexion.isAlive()) {
            this.conexion.interrupt();
            //return ;
        }
        if (isRfidConnected()) this.rfidHandler.onDestroy();
        if (isBarcodeConnected()) this.barcodeHandler.onDestroy();

        this.conexion = new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    if (runnableMode == RunnableMode.RFID_ONLY || runnableMode == RunnableMode.BOTH_MODES) {
                        if (defaultMode == DefaultMode.RFID) {
                            rfidHandler = new RFIDHandlerV2(context, RFID_MODE, responseHandler, connectedSonHandler, false);
                            MyLog.d("INTENTANDO CONECTAR CON RFID...");
                        } else {
                            MyLog.d("INTENTANDO CONECTAR CON RFID...");
                            rfidHandler = new RFIDHandlerV2(context, BARCODE_MODE, responseHandler, connectedSonHandler, false);
                        }
                    }
                    if (runnableMode == RunnableMode.BARCODE_ONLY) {
                        Log.d("ZEBRA_MANAGER", "Conectando BARCODE");
                        MyLog.d("INTENTANDO CONECTAR CON BARCODE...");
                        barcodeHandler = new BarcodeHandlerV2(context, responseHandler, connectedSonHandler);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
//                    Helper.TratarExcepcion(context, e.getMessage(), "ZebraManagerV2 Exception  - connect", e, "");
                    connectedHandler.handleConnection(false);
                }
            }
        };
        this.conexion.start();

        this.timer = new Timer();
        timer.schedule(obtenerTimerTask(), TIEMPO_ESPERA_CONEXION);
    }

    public void disconnect() {
        if (this.rfidHandler != null || isRfidConnected()) {
            this.rfidHandler.onDestroy();
        }
        if (this.barcodeHandler != null || isBarcodeConnected()) {
            this.barcodeHandler.onDestroy();
        }
        this.rfidConnection = NO_TRIED;
        this.barcodeConnection = NO_TRIED;
    }

    // LISTENER PARA LA CONEXIÓN DE LOS HANDLER

    protected ConnectedSonHandlerInterface connectedSonHandler = new ConnectedSonHandlerInterface() {
        @Override
        public synchronized void handleConnection(int connection, int mode) {
            MyLog.d("HANDLE CONNECTION ---> CONNECTION : " + connection + " - MODE : " + mode);
            switch (mode) {
                case RFID_MODE:
                    rfidConnection = connection;
                    break;
                case BARCODE_MODE:
                    barcodeConnection = connection;
                    break;
            }

            if (runnableMode == RunnableMode.RFID_ONLY || runnableMode == RunnableMode.BARCODE_ONLY) {
                if (rfidConnection == CONNECTED || barcodeConnection == CONNECTED) {
                    connectedHandler.handleConnection(true);
                } else if (rfidConnection == DISCONNECTED || barcodeConnection == DISCONNECTED) {
                    connectedHandler.handleConnection(false);
                    disconnect(); // en caso de que alguno de los dos se haya conseguido conectar
                }
            } else if (runnableMode == RunnableMode.BOTH_MODES) {
                if (rfidConnection == CONNECTED && barcodeConnection == CONNECTED) {
                    connectedHandler.handleConnection(true);
                } else if (rfidConnection == DISCONNECTED || barcodeConnection == DISCONNECTED) {
                    connectedHandler.handleConnection(false);
                    disconnect(); // en caso de que alguno de los dos se haya conseguido conectar
                } else if (rfidConnection == CONNECTED && barcodeConnection == NO_TRIED && runnableMode == RunnableMode.BOTH_MODES) {
                    if (conexion.isAlive()) conexion.interrupt();
                    conexion = new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            MyLog.d("INTENTANDO CONECTAR CON BARCODE...");
                            barcodeHandler = new BarcodeHandlerV2(context, responseHandler, connectedSonHandler);
                            Log.d("ZEBRA_MANAGER", "Conectando BARCODE");
                        }
                    };
                    conexion.start();
                }
            }
        }

        @Override
        public void handleBattery(int level, boolean charging, String cause) {
            connectedHandler.handleBattery(level, charging, cause);
        }

//        @Override
//        public void handleTemperatureAlarm(ALARM_LEVEL level, int current, int ambient, String cause) {
//            connectedHandler.handleTemperatureAlarm(level, current, ambient, cause);
//        }
    };

    // TIMERTASK DE TIEMPO DE ESPERA DE CONEXIÓN

    private TimerTask obtenerTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                if (!isConnected()) {
                    disconnect(); // al acabar el tiempo de espera, se desconecta por si acaso algún lector llegó a conectarse y para mandar notificación a través del ConnectionHandler
//                    ZebraManagerV2.this.connectedHandler.handleConnection(false);
                }
            }
        };
    }

    // MÉTODOS PÚBLICOS

    public ArrayList<ReaderDevice> getAvailableDevices() throws InvalidUsageException {
//        return RFIDHandlerV2.availableRFIDReaderList;
        return RFIDHandlerV2.readers.GetAvailableRFIDReaderList();
//        rfidHandler.GetAvailableReader();
//        return RFIDHandlerV2.availableRFIDReaderList;
    }

    public void setConnectedHandler(ConnectedHandlerInterface connectedHandler) {
        this.connectedHandler = connectedHandler;
    }

    public void setModoBarcode() {
        if (isConnected()) {
            rfidHandler.SetTriggerMode("Barcode");
            mode = BARCODE_MODE;
        }
    }

    public void cambiarModo() {
        if (isConnected()) {

            if (this.mode == RFID_MODE) {
                if (RfidGlobalVariables.PUEDE_LEER_BARCODE) {
                    rfidHandler.SetTriggerMode("Barcode");
                    mode = BARCODE_MODE;
                }
            } else {
                if (RfidGlobalVariables.PUEDE_LEER_RFID) {
                    rfidHandler.SetTriggerMode("RFID");
                    mode = RFID_MODE;
                }
            }
        }
    }

    public void cambiarModo(int tipo) {
        if (isConnected()) {
            switch (tipo) {
                case BARCODE_MODE:
                    rfidHandler.SetTriggerMode("Barcode");
                    mode = BARCODE_MODE;
                    break;
                case RFID_MODE:
                    rfidHandler.SetTriggerMode("RFID");
                    mode = RFID_MODE;
                    break;
            }
        }
    }

    public String rfidDefaultConfig() {
        if (rfidHandler != null)
            return rfidHandler.Defaults();
        else
            return null;
    }

    public void performInventory() {
        this.rfidHandler.performInventory();
    }

    public void stopInventory() {
        this.rfidHandler.stopInventory();
    }

    public void startLocalizarTag(String tag) {
        this.rfidHandler.startLocalizarTag(tag);
    }

    public void stopLocalizarTag() {
        this.rfidHandler.stopLocalizarTag();
    }

    // MÉTODOS PRIVADOS

    public boolean isConnected() {
        switch (this.runnableMode) {
            case RFID_ONLY:
                return isRfidConnected();
            case BARCODE_ONLY:
                return isBarcodeConnected();
            default:
                return isRfidConnected() && isBarcodeConnected();
        }
    }

    private boolean isRfidConnected() {
        return (this.rfidHandler != null && this.rfidHandler.isReaderConnected());
    }

    private boolean isBarcodeConnected() {
        return (this.barcodeHandler != null && this.barcodeHandler.isReaderConnected());
    }

    private boolean configuradorBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) { // el dispositivo no dispone de bluetooh
            return false;
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.enable();
                }
            }
            return true;
        }
    }

    // MÉTODOS PARA EL CICLO DE VIDA DE LA ACTIVITY QUE USE EL ZEBRA-MANAGER

    public void onPause() {
        if (this.conexion.isAlive()) this.conexion.interrupt();// en caso de error de conexión
        if (this.rfidHandler != null) this.rfidHandler.onPause();
        if (this.barcodeHandler != null) this.barcodeHandler.onPause();
    }

    public void onPostResume() {
        new Thread(){
            @Override
            public void run() {
                super.run();
                if (rfidHandler != null) rfidHandler.onResume();
                if (barcodeHandler != null) barcodeHandler.onResume();
//                if (rfidHandler != null || barcodeHandler != null) connect();
            }
        }.start();
    }

    public void onDestroy() {
        if (this.conexion != null && this.conexion.isAlive()) this.conexion.interrupt(); // en caso de error
        if (this.rfidHandler != null && isRfidConnected()) this.rfidHandler.onDestroy();
        if (this.barcodeHandler != null && isBarcodeConnected()) this.barcodeHandler.onDestroy();
    }

    // INTERFACES PÚBLICAS

    public interface ResponseHandlerInterface {
        void handleTagdata(TagData[] tagData);

        void handleBarcode(Barcode barcode);

        void handleTriggerPress(boolean pressed);
    }

    public interface ConnectedHandlerInterface {
        void handleConnection(boolean connected);

        void handleBattery(int level, boolean charging, String cause);

//        void handleTemperatureAlarm(ALARM_LEVEL level, int current, int ambient, String cause);
    }

    // INTERFACES INTERNAS

    protected interface ConnectedSonHandlerInterface {
        void handleConnection(int connection, int mode);

        void handleBattery(int level, boolean charging, String cause);

//        void handleTemperatureAlarm(ALARM_LEVEL level, int current, int ambient, String cause);
    }
}

