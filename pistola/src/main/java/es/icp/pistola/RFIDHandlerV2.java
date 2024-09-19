package es.icp.pistola;

import static android.content.Context.RECEIVER_EXPORTED;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.Events;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RegionInfo;
import com.zebra.rfid.api3.RegulatoryConfig;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import es.icp.logs.core.MyLog;

public class RFIDHandlerV2 implements Readers.RFIDReaderEventHandler {

        static boolean reconexion = true;


    final static String TAG = "RFID_SAMPLE";
    // RFID Reader
    public static Readers readers;
    public static ArrayList<ReaderDevice> availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    public static RFIDReader reader = null;
    public  RFIDReader readerTmp = null;
    private EventHandler eventHandler;
    // UI and context
    TextView textView;
    private Context context;
    private ResponseHandlerInterface responseHandlerInterface;
    private ConnectedSonHandlerInterface connectedHandlerInterface;
    private int defaultMode;
    // general
    private int MAX_POWER = 270;
    // In case of RFD8500 change reader name with intended device below from list of paired RFD8500
    String readername = "RFD8500123";

    public RFIDHandlerV2(Context context, int defaultMode, ResponseHandlerInterface responseHandlerInterface,
                         ConnectedSonHandlerInterface connectedHandlerInterface, boolean init) {
        this.context = context;
        this.defaultMode = defaultMode;
        this.responseHandlerInterface = responseHandlerInterface;
        this.connectedHandlerInterface = connectedHandlerInterface;
        if (init) this.InitSDK();
        else this.ConnectSDK();
    }

//    public Context getContext() {
//        return context;
//    }
//
//    public void setContext(Context context) {
//        this.context = context;
//    }
//
//    public ResponseHandlerInterface getResponseHandlerInterface() {
//        return responseHandlerInterface;
//    }
//
//    public void setResponseHandlerInterface(ResponseHandlerInterface responseHandlerInterface) {
//        this.responseHandlerInterface = responseHandlerInterface;
//    }
//
//    public ZebraManagerV2.ConnectedSonHandlerInterface getConnectedHandlerInterface() {
//        return connectedHandlerInterface;
//    }
//
//    public void setConnectedHandlerInterface(ZebraManagerV2.ConnectedSonHandlerInterface connectedHandlerInterface) {
//        this.connectedHandlerInterface = connectedHandlerInterface;
//    }

    public String Defaults() {
        // check reader connection
        if (!isReaderConnected())
            return "Not connected";
        try {
            // Power to 270
            Antennas.AntennaRfConfig config = null;

            config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(MAX_POWER);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);

            // singulation to S0
            Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
            s1_singulationControl.setSession(SESSION.SESSION_S0);
            s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
            s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
            reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
        }
         catch (NullPointerException e) {
            e.printStackTrace();
            //Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 NullPointerException  - Defaults", e, "");
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
        }
        catch (InvalidUsageException e) {
            e.printStackTrace();
            //Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 InvalidUsageException  - Defaults", e, "");
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
        } catch (OperationFailureException e) {
            e.printStackTrace();
            //Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 OperationFailureException  - Defaults", e, "");
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
            return e.getResults().toString() + " " + e.getVendorMessage();
        }
        return "Default settings applied";
    }

    public boolean isReaderConnected() {
        try{
            if (reader != null && reader.isConnected())
                return true;
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return false;
    }

    //
    //  Activity life cycle behavior
    //

    public String onResume() {
        return connect();
    }

    public void onPause() {
        disconnect();
    }

    public void onDestroy() {
        disconnect();
        stopTimer();
//        dispose();
    }

    //
    // RFID SDK
    //

    private void InitSDK() {
        Log.d(TAG, "InitSDK");
        if (readers == null) {
            new CreateInstanceTask().execute();
        }
    }

    public void ConnectSDK() {
        if (readers == null) {
            new CreateInstanceTask().execute();
        } else
            new ConnectionTask().execute();
    }

    // Enumerates SDK based on host device
    private class CreateInstanceTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "CreateInstanceTask");
            // Based on support available on host device choose the reader type
            InvalidUsageException invalidUsageException = null;
            try {
                context.registerReceiver(null, new IntentFilter(BluetoothDevice.ACTION_FOUND), RECEIVER_EXPORTED);
                readers = new Readers(context, ENUM_TRANSPORT.BLUETOOTH);
                RFIDHandlerV2.availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                RfidGlobalVariables.observerPreparedZebraManager.onPrepared(/*RFIDHandlerV2.availableRFIDReaderList*/);
                // todo
            } catch (InvalidUsageException e) {
                e.printStackTrace();
                //Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 InvalidUsageException  - CreateInstanceTask doInBackground", e, "");
                MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
                invalidUsageException = e;
            }
            catch (Exception e) {
                e.printStackTrace();
                //Dx.error(context, context.getString(R.string.error_conexion_bluetooth));
                MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
            }
            if (invalidUsageException != null) {
                readers.Dispose();
                readers = null;
                if (readers == null) {
                    readers = new Readers(context, ENUM_TRANSPORT.BLUETOOTH);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //new ConnectionTask().execute();
        }
    }

    public boolean startLocalizarTag(String tagID){

        try {

            reader.Actions.TagLocationing.Perform(tagID, null, null);
           /* Thread.sleep(5000);
            reader.Actions.TagLocationing.Stop();

            reader.Actions.Inventory.stop();*/
        }catch (Exception ex){
            MyLog.e(ex);
            //Helper.TratarExcepcion(context, ex.getMessage(), "RFIDHandlerV2 Exception  - startLocalizarTag", ex, "");
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + ex.getMessage());
        }

    /*    try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        reader.Actions.Inventory.stop();*/

        return true;
    }

    public boolean stopLocalizarTag(){

        try {


            reader.Actions.TagLocationing.Stop();

            reader.Actions.Inventory.stop();
        }catch (Exception ex){
            MyLog.e(ex);
            //Helper.TratarExcepcion(context, ex.getMessage(), "RFIDHandlerV2 Exception  - stopLocalizarTag", ex, "");
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + ex.getMessage());
        }

    /*    try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        reader.Actions.Inventory.stop();*/

        return true;
    }


    private class ConnectionTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            Log.d(TAG, "ConnectionTask");
            GetAvailableReader();
            if (reader != null)
                return connect();
            return "Failed to find or connect reader";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //textView.setText(result);
            if (!result.equals("Connected")) {
//                connectedHandlerInterface.handleConnection(ZebraManagerV2.DISCONNECTED, ZebraManagerV2.RFID_MODE);
            }
        }
    }

    public synchronized void GetAvailableReader() {
        Log.d(TAG, "GetAvailableReader");
        try {
            if (readers != null) {
                readers.attach(this);
                if (readers.GetAvailableRFIDReaderList() != null) {
                    RFIDHandlerV2.availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                    if (RFIDHandlerV2.availableRFIDReaderList.size() != 0) {
                        // if single reader is available then connect it
                        if (RFIDHandlerV2.availableRFIDReaderList.size() == 1) {
                            readerDevice = RFIDHandlerV2.availableRFIDReaderList.get(0);
                            reader = readerDevice.getRFIDReader();
                            RfidGlobalVariables.CONNECTED_DEVICE = readerDevice;
                            RfidGlobalVariables.CONNECTED_READER = reader;
                            MyLog.c("Conectado.... y teniendo READE y DEVICE");
                        } else {
                            // search reader specified by name
                            for (ReaderDevice device : RFIDHandlerV2.availableRFIDReaderList) {
                                if (device.getName().equals(RfidGlobalVariables.LAST_CONNECTED_READER)) {
                                    readerDevice = device;
                                    reader = readerDevice.getRFIDReader();
                                    RfidGlobalVariables.CONNECTED_DEVICE = readerDevice;
                                    RfidGlobalVariables.CONNECTED_READER = reader;
                                    MyLog.c("Conecado.... y teniendo READE y DEVICE");
                                }
                            }
                        }
                    }
                }
            }
        } catch (InvalidUsageException e) {
            e.printStackTrace();
            //Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 InvalidUsageException  - GetAvailableReader", e, "");
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
        }
    }

    // handler for receiving reader appearance events
    @Override
    public void RFIDReaderAppeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderAppeared " + readerDevice.getName());
        new ConnectionTask().execute();
    }

    @Override
    public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
        //Log.d(TAG, "RFIDReaderDisappeared " + readerDevice.getName());
        if (readerDevice != null && reader != null && readerDevice.getName().equals(reader.getHostName())){
            disconnect();
            this.connectedHandlerInterface.handleConnection(ZebraManagerV2.DISCONNECTED, ZebraManagerV2.RFID_MODE);
        }
    }


    private synchronized String connect() {
        if (reader != null) {
            Log.d(TAG, "connect " + reader.getHostName());
            try {
//                if (!reader.isConnected()) {
                    // Establish connection to the RFID Reader

                reader.connect();


                MyLog.c("Inicio");

                if (reader.Config == null) return "";
                RegulatoryConfig regulatoryConfig = reader.Config.getRegulatoryConfig();
                RegionInfo regionInfo = reader.ReaderCapabilities.SupportedRegions.getRegionInfo(18);
                regulatoryConfig.setRegion(regionInfo.getRegionCode());
                regulatoryConfig.setIsHoppingOn(regionInfo.isHoppingConfigurable());
                String str = "865700,866300,866900,867500";
                regulatoryConfig.setEnabledChannels( str.split(","));

                MyLog.c("FIN");




                    ConfigureReader();
                    connectedHandlerInterface.handleConnection(ZebraManagerV2.CONNECTED, ZebraManagerV2.RFID_MODE);

                    return "Connected";
//                } else {
//                    return "Ya conectado";
//                }
            }catch (InvalidUsageException e) {
                e.printStackTrace();
                MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
                MyLog.d( "InvalidUsageException -->" + e.getVendorMessage());
                //Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 InvalidUsageException  - connect", e, "");
                connectedHandlerInterface.handleConnection(ZebraManagerV2.DISCONNECTED, ZebraManagerV2.RFID_MODE);
                MyLog.d("FALLO EN LA CONEXIÓN RFID. INVALIDUSAGEEXCEPTION. INTENTANDO RECONECTAR...");
                this.disconnect();
                return "";
//                return this.connect();
            } catch (OperationFailureException e) {
                e.printStackTrace();
                //Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 OperationFailureException  - connect", e, "");
                MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());

                MyLog.d("OperationFailureException -->" + e.getVendorMessage());


                String des = e.getResults().toString();
                MyLog.c(e.getResults().toString());
                MyLog.c(e.getResults().ordinal + " -> " + e.getVendorMessage());

              //  if (e.getResults() == RFIDResults.RFID_READER_REGION_NOT_CONFIGURED) {
                if (e.getResults().ordinal == 107) {
                    MyLog.c("Demtrp");

                    try{
                        RegulatoryConfig regulatoryConfig = reader.Config.getRegulatoryConfig();
                        RegionInfo regionInfo = reader.ReaderCapabilities.SupportedRegions.getRegionInfo(18);
                        regulatoryConfig.setRegion(regionInfo.getRegionCode());
                        //regulatoryConfig.setIsHoppingOn(regionInfo.isHoppingConfigurable());


                        RegionInfo selectedRegionInfo = reader.Config.getRegionInfo(regionInfo);
                        MyLog.d("Selected Region Info: " + selectedRegionInfo.toString());


                        if (selectedRegionInfo.isHoppingConfigurable()){
                            regulatoryConfig.setIsHoppingOn(true);
                        }



                        String str = "865700,866300,866900,867500";
                        regulatoryConfig.setEnabledChannels( str.split(","));

                        MyLog.d("Region:" + regulatoryConfig.getRegion());
                        MyLog.d("Hopping:" + regulatoryConfig.isHoppingon());
                        MyLog.d("Enable Chanels:" + regulatoryConfig.getEnabledchannels());





                        reader.Config.setRegulatoryConfig(regulatoryConfig);
                        reader.connect();
                    }catch (Exception ex){
                      //  MyLog.c("Peta otra cosa..."  + ex.toString());
                        MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
                        this.disconnect();


                    }




                   /* if (reconexion){
                        reconexion = false;
                        connect();


                    }*/
                }else{
                    connectedHandlerInterface.handleConnection(ZebraManagerV2.DISCONNECTED, ZebraManagerV2.RFID_MODE);
                    MyLog.d("FALLO EN LA CONEXIÓN RFID. OPERATIONFAILUREEXCEPTION. INTENTANDO RECONECTAR... " + des);
                    this.disconnect();
//                return this.connect();
                    return "Connection failed" + e.getVendorMessage() + " " + des;
                }
                return "Connection failed" + e.getVendorMessage() + " " + des;

            }
        }else {
            return "Ya conectado";
        }
    }

    private void ConfigureReader() {
        Log.d(TAG, "ConfigureReader " + reader.getHostName());
        if (reader.isConnected()) {
            TriggerInfo triggerInfo = new TriggerInfo();
            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
            triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
            try {
                // receive events from reader
                if (eventHandler == null)
                    eventHandler = new EventHandler();
                reader.Events.addEventsListener(eventHandler);
                // HH event
                reader.Events.setHandheldEvent(true);
                // tag event with tag data
                reader.Events.setTagReadEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(false);
                reader.Events.setReaderDisconnectEvent(true);
                reader.Events.setBatteryEvent(true);
//                reader.Events.setTemperatureAlarmEvent(true);
                // set trigger mode as rfid so scanner beam will not come
                if (this.defaultMode == ZebraManagerV2.RFID_MODE) {
                    reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
                } else {
                    reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.BARCODE_MODE, true);
                }
                // set start and stop triggers
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);
                // power levels are index based so maximum power supported get the last one
                MAX_POWER = reader.ReaderCapabilities.getTransmitPowerLevelValues().length - 1;
                // set antenna configurations
                Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
                config.setTransmitPowerIndex(MAX_POWER);
                config.setrfModeTableIndex(0);
                config.setTari(0);
                reader.Config.Antennas.setAntennaRfConfig(1, config);
                // Set the singulation control
                Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
                s1_singulationControl.setSession(SESSION.SESSION_S0);
                s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
                s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
                reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
                // delete any prefilters
                reader.Actions.PreFilters.deleteAll();
                if (reader != null) reader.Config.getDeviceStatus(true, false, false); // obtención inicial de la configuración de la pistola
                resetTimer(); // temporizador (60seg) para la obtención del nivel de batería y de la temperatura de la pistola
                //




            } catch (InvalidUsageException | OperationFailureException | NullPointerException e) {
                e.printStackTrace();
                MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
                //Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 InvalidUsageException | OperationFailureException  - ConfigureReader", e, "");
            }
        }
    }

    public synchronized void SetTriggerMode(String mode)
    {
        try
        {
            if (reader != null && reader.Config != null){
                if (mode.equals("Barcode") && reader != null)
                {
                    reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.BARCODE_MODE, false);
                }
                else if (mode.equals("RFID") && reader != null)
                {
                    reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, false);
                }
            } else {
                //Dx.error(context, "Existe un fallo con la conexión de su pistola. Revisela y vuelva a conectarla.");
            }

//            SetAttribute setAttributeinfo = new SetAttribute();
//            setAttributeinfo.setAttvalue("2");
        }
        catch (InvalidUsageException ex)
        {
            ex.printStackTrace();
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + ex.getMessage());
            //Helper.TratarExcepcion(context, ex.getMessage(), "RFIDHandlerV2 InvalidUsageException  - SetTriggerMode", ex, "");
        }
        catch (OperationFailureException ex)
        {
            ex.printStackTrace();
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + ex.getMessage());
            //Helper.TratarExcepcion(context, ex.getMessage(), "RFIDHandlerV2 OperationFailureException  - SetTriggerMode", ex, "");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private synchronized void disconnect() {
        Log.d(TAG, "disconnect " + reader);
        try {
            if (reader != null) {
                reader.disconnect();
                if (reader.Events != null) reader.Events.removeEventsListener(eventHandler);
            }
        } catch (InvalidUsageException e) {
            e.printStackTrace();
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
//            Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 InvalidUsageException  - disconnect", e, "");
        } catch (OperationFailureException e) {
            e.printStackTrace();
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
//            Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 OperationFailureException  - disconnect", e, "");
        } catch (Exception e) {
            e.printStackTrace();
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
//            Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 Exception  - disconnect", e, "");
        }
    }

    private synchronized void dispose() {
        try {
            if (readers != null) {
                reader = null;
                readers.Dispose();
                readers = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
//            Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 Exception  - dispose", e, "");
        }
    }

    public synchronized void performInventory() {
        // check reader connection
        if (!isReaderConnected())
            return;
        try {
            reader.Actions.Inventory.perform();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
            //Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 InvalidUsageException  - performInventory", e, "");
        } catch (OperationFailureException e) {
            e.printStackTrace();
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
            //Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 OperationFailureException  - performInventory", e, "");
        }
    }

    public synchronized void stopInventory() {
        // check reader connection
        if (!isReaderConnected())
            return;
        try {
            reader.Actions.Inventory.stop();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
            //Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 InvalidUsageException  - stopInventory", e, "");
        } catch (OperationFailureException e) {
            e.printStackTrace();
            MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
            //Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 OperationFailureException  - stopInventory", e, "");
        }
    }



    // Read/Status Notify handler
    // Implement the RfidEventsLister class to receive event notifications
    public class EventHandler implements RfidEventsListener {


        // Read Event Notification
        public void eventReadNotify(RfidReadEvents e) {
            // Recommended to use new method getReadTagsEx for better performance in case of large tag population
            TagData[] myTags = reader.Actions.getReadTags(RfidGlobalVariables.TAGS_LEER_RFID);
            if (myTags != null) {
                for (int index = 0; index < myTags.length; index++) {
                    Log.d(TAG, "Tag ID " + myTags[index].getTagID());
                    if (myTags[index].getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ &&
                            myTags[index].getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
                        if (myTags[index].getMemoryBankData().length() > 0) {
                            Log.d(TAG, " Mem Bank Data " + myTags[index].getMemoryBankData());
                        }
                    }
                }
                // possibly if operation was invoked from async task and still busy
                // handle tag data responses on parallel thread thus THREAD_POOL_EXECUTOR
                new AsyncDataUpdate().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, myTags);
            }
        }

        // Status Event Notification
        public void eventStatusNotify(final RfidStatusEvents rfidStatusEvents) {
            Log.d(TAG, "Status Notification: " + rfidStatusEvents.StatusEventData.getStatusEventType());
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {

                    try{
                        responseHandlerInterface.handleTriggerPress(true);


                      /* new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {


                            responseHandlerInterface.handleTriggerPress(true);



                            return null;
                        }

                    }.execute();*/
                    }catch (Exception ex){
                        MyLog.e(ex.toString());
                        MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + ex.getMessage());
                    }
                }
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
                    try{
                        responseHandlerInterface.handleTriggerPress(false);

                   /* new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {

                                responseHandlerInterface.handleTriggerPress(false);

                            return null;
                        }
                    }.execute();*/


                    }catch (Exception ex){
                        MyLog.e(ex.toString());
                        MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + ex.getMessage());
                    }
                }
            }
            else if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.DISCONNECTION_EVENT) {
                connectedHandlerInterface.handleConnection(ZebraManagerV2.DISCONNECTED, ZebraManagerV2.RFID_MODE);
            }
            else if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.BATTERY_EVENT) {
                final Events.BatteryData batteryData = rfidStatusEvents.StatusEventData.BatteryData;
                resetTimer();
                connectedHandlerInterface.handleBattery(batteryData.getLevel(), batteryData.getCharging(), batteryData.getCause());
            }
//            else if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.TEMPERATURE_ALARM_EVENT) {
//                final Events.TemperatureAlarmData temperatureAlarmData = rfidStatusEvents.StatusEventData.TemperatureAlarmData;
//                connectedHandlerInterface.handleTemperatureAlarm(temperatureAlarmData.getAlarmLevel(), temperatureAlarmData.getCurrentTemperature(), temperatureAlarmData.getAmbientTemp(), temperatureAlarmData.getCause());
//            }
        }
    }

    private static ScheduledExecutorService scheduler;
    private static ScheduledFuture<?> taskHandle;

    public void resetTimer() {
        stopTimer();
        startTimer();
    }

    public void startTimer() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1);
            final Runnable task = new Runnable() {
                public void run() {
                    try {
                        if (reader != null && reader.Config != null)
                            reader.Config.getDeviceStatus(true, false, false);
                        else
                            stopTimer();
                    } catch (InvalidUsageException | OperationFailureException e) {
                        e.printStackTrace();
                        MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
//                        Helper.TratarExcepcion(context, e.getMessage(), "RFIDHandlerV2 InvalidUsageException | OperationFailureException  - startTimer", e, "");
                    }
                }
            };
            taskHandle = scheduler.scheduleAtFixedRate(task, 5, 5, SECONDS);
        }
    }

    public static void stopTimer() {
        if (taskHandle != null) {
            taskHandle.cancel(true);
            scheduler.shutdown();
        }
        taskHandle = null;
        scheduler = null;
    }

    private class AsyncDataUpdate extends AsyncTask<TagData[], Void, Void> {
        @Override
        protected Void doInBackground(TagData[]... params) {
            if (RfidGlobalVariables.PUEDE_LEER_RFID) {
                responseHandlerInterface.handleTagdata(params[0]);
            }

            return null;
        }
    }

//    public interface ResponseHandlerInterface {
//        void handleTagdata(TagData[] tagData);
//
//        void handleTriggerPress(boolean pressed);
//    }
//
//    public interface ConnectedHandlerInterface {
//        void handleConnection(boolean connected);
//    }

}
