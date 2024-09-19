package es.icp.pistola;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.zebra.barcode.sdk.sms.ConfigurationUpdateEvent;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.FirmwareUpdateEvent;
import com.zebra.scannercontrol.IDcsSdkApiDelegate;
import com.zebra.scannercontrol.SDKHandler;

import java.util.ArrayList;
import java.util.List;

import es.icp.logs.core.MyLog;

public class BarcodeHandlerV2 implements ScannerAppEngine, IDcsSdkApiDelegate{

    public SDKHandler sdkHandler;

    public AvailableScanner availableScanner;

    public Context context;
    public ZebraManagerV2.ConnectedSonHandlerInterface connectedHandlerInterface;
    public ZebraManagerV2.ResponseHandlerInterface responseHandlerInterface;

    private boolean connected;

    public BarcodeHandlerV2(Context context, ZebraManagerV2.ResponseHandlerInterface responseHandlerInterface, ZebraManagerV2.ConnectedSonHandlerInterface connectedHandlerInterface){
        this.context = context;
        this.connectedHandlerInterface = connectedHandlerInterface;
        this.responseHandlerInterface = responseHandlerInterface;

        new Thread(){
            @Override
            public void run() {
                super.run();
                DCSSDKDefs.DCSSDK_RESULT result = inicializarSdkHandler();

                if (result != DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE && result != DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SCANNER_NOT_AVAILABLE) {
                    BarcodeHandlerV2.this.connected = true;
                    BarcodeHandlerV2.this.connectedHandlerInterface.handleConnection(ZebraManagerV2.CONNECTED, ZebraManagerV2.BARCODE_MODE);
                } else {
                    BarcodeHandlerV2.this.connectedHandlerInterface.handleConnection(ZebraManagerV2.DISCONNECTED, ZebraManagerV2.BARCODE_MODE);
                }
            }
        }.start();

    }

    public DCSSDKDefs.DCSSDK_RESULT inicializarSdkHandler(){
        //if (sdkHandler == null){
        sdkHandler = new SDKHandler(context);
        //}


        sdkHandler.dcssdkSetDelegate(this);

        sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);
        sdkHandler.dcssdkEnableAvailableScannersDetection(true);
        initializeDcsSdkWithAppSettings();
        DCSSDKDefs.DCSSDK_RESULT result = DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
//        int pos = 0;
//        do {
//            pos++;
//            RfidGlobalVariables.LAST_CONNECTED_READER_POS = pos;
//            MyLog.d("SE INTENTA CONECTAR AL READER " + RfidGlobalVariables.LAST_CONNECTED_READER_POS);
//            result = sdkHandler.dcssdkEstablishCommunicationSession(RfidGlobalVariables.LAST_CONNECTED_READER_POS);
//        } while (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE);


        ArrayList<DCSScannerInfo> scannerTreeList=new ArrayList<DCSScannerInfo>();
        sdkHandler.dcssdkGetAvailableScannersList(scannerTreeList);
        sdkHandler.dcssdkGetActiveScannersList(scannerTreeList);

        for (DCSScannerInfo s : scannerTreeList) {
            if (s.getScannerName().equals(RfidGlobalVariables.LAST_CONNECTED_READER)) {
                RfidGlobalVariables.LAST_CONNECTED_READER_POS = s.getScannerID();
                return sdkHandler.dcssdkEstablishCommunicationSession(s.getScannerID());
            }
        }
        return DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
    }

    private class MyAsyncTask extends AsyncTask<Void,AvailableScanner,Boolean> {
        private AvailableScanner scanner;
        public MyAsyncTask(AvailableScanner scn){
            this.scanner=scn;
        }
        @Override
        protected Boolean doInBackground(Void... voids) {
//            DCSSDKDefs.DCSSDK_RESULT result =
//                    DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
//            if (sdkHandler != null) {
//                result = sdkHandler.dcssdkEstablishCommunicationSession(scannerId);
//            }
//            if(result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS){
//                return true;
//            }
//            else if(result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE) {
//                return false;
//            }
//            return false;
            DCSSDKDefs.DCSSDK_RESULT result =connect(scanner.getScannerId());
            if(result== DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS){
//                curAvailableScanner = scanner;
//                curAvailableScanner.setConnected(true);
                connected = true;
                return true;
            }
            else {
//                curAvailableScanner=null;
                connected = false;
                return false;
            }
        }
    }

    @Override
    public void dcssdkEventScannerAppeared(DCSScannerInfo availableScanner) {
        Log.d("BARCODE_HANDLER", "Encontrado scanner... comenzando conexión.");
        this.availableScanner = new AvailableScanner(availableScanner);

        new MyAsyncTask(this.availableScanner).execute();

//        /* notify connections delegates */
//        for (IScannerAppEngineDevConnectionsDelegate delegate : mDevConnDelegates) {
//            if (delegate != null) {
//                /*result = */delegate.scannerHasAppeared(availableScanner.getScannerID());
////                if (result) {
////                            /*
////                             DevConnections delegates should NOT display any UI alerts,
////                             so from UI notification side the event is not processed
////                             */
////                    notificaton_processed = false;
////                }
//            }
//        }
    }

    @Override
    public void dcssdkEventConfigurationUpdate(ConfigurationUpdateEvent configurationUpdateEvent) {

    }
//    public ZebraManagerV2.ConnectedSonHandlerInterface getConnectedHandlerInterface() {
//        return connectedHandlerInterface;
//    }
//
//    public void setConnectedHandlerInterface(ZebraManagerV2.ConnectedSonHandlerInterface connectedHandlerInterface) {
//        this.connectedHandlerInterface = connectedHandlerInterface;
//    }
//
//    public ZebraManagerV2.ResponseHandlerInterface getResponseHandlerInterface() {
//        return responseHandlerInterface;
//    }
//
//    public void setResponseHandlerInterface(ZebraManagerV2.ResponseHandlerInterface responseHandlerInterface) {
//        this.responseHandlerInterface = responseHandlerInterface;
//    }

    public void onPause() {
        sdkHandler.dcssdkClose();
        disconnect(RfidGlobalVariables.LAST_CONNECTED_READER_POS);
    }

    public void onResume() {
//        sdkHandler.dcssdkEstablishCommunicationSession(RfidGlobalVariables.LAST_CONNECTED_READER_POS);
//        new MyAsyncTask(this.availableScanner).execute();
        DCSSDKDefs.DCSSDK_RESULT result = inicializarSdkHandler();
        connect(RfidGlobalVariables.LAST_CONNECTED_READER_POS);

        if (result != DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE && result != DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SCANNER_NOT_AVAILABLE) {
            this.connected = true;
            connectedHandlerInterface.handleConnection(ZebraManagerV2.CONNECTED, ZebraManagerV2.BARCODE_MODE);
        } else {
            connectedHandlerInterface.handleConnection(ZebraManagerV2.DISCONNECTED, ZebraManagerV2.BARCODE_MODE);
        }
    }

    public void onDestroy() {
//        sdkHandler.dcssdkEnableAvailableScannersDetection(false);
//        sdkHandler.dcssdkStopScanningDevices();
//        sdkHandler.dcssdkClose();
        disconnect(RfidGlobalVariables.LAST_CONNECTED_READER_POS);
    }


    @Override
    public void dcssdkEventScannerDisappeared(int i) {
        Log.d("BARCODE_HANDLER", "Desaparecido");
    }



    @Override
    public void dcssdkEventCommunicationSessionEstablished(DCSScannerInfo availableScanner) {
        Log.d("BARCODE_HANDLER", "SessionEstablished");
        Log.d("BARCODE_HANDLER", "Encontrado scanner... comenzando conexión.");
        MyLog.d("ESTABLECIENDO SESIÓN...");

//        if (!availableScanner.getScannerName().equals(RfidGlobalVariables.LAST_CONNECTED_READER)) {
//            RfidGlobalVariables.LAST_CONNECTED_READER_POS++;
//            sdkHandler.dcssdkTerminateCommunicationSession(availableScanner.getScannerID());
//            sdkHandler.dcssdkEstablishCommunicationSession(RfidGlobalVariables.LAST_CONNECTED_READER_POS);
//            MyLog.d("NO COINCIDIA...");
//        }

//        if (!this.connected) {
//            this.availableScanner = new AvailableScanner(availableScanner);
//
//            new MyAsyncTask(this.availableScanner).execute();
//        }
        this.connected = true;
        this.connectedHandlerInterface.handleConnection(ZebraManagerV2.CONNECTED, ZebraManagerV2.BARCODE_MODE);
    }

    @Override
    public void dcssdkEventCommunicationSessionTerminated(int i) {
        Log.d("BARCODE_HANDLER", "SessionTerminated");
//        this.connectedHandlerInterface.handleConnection(ZebraManagerV2.DISCONNECTED, ZebraManagerV2.BARCODE_MODE);
//        this.connected = false;
    }

    public boolean isReaderConnected(){
        return this.connected;
    }

    @Override
    public void dcssdkEventBarcode(byte[] barcodeData, int barcodeType, int fromScannerID) {
        Barcode barcode=new Barcode(barcodeData,barcodeType,fromScannerID);
        Log.d("BARCODE_HANDLER", barcode.toString());
        if (RfidGlobalVariables.PUEDE_LEER_BARCODE) {
            responseHandlerInterface.handleBarcode(barcode);
        }
    }

    @Override
    public void dcssdkEventImage(byte[] bytes, int i) {

    }

    @Override
    public void dcssdkEventVideo(byte[] bytes, int i) {

    }

    @Override
    public void dcssdkEventBinaryData(byte[] bytes, int i) {

    }

    @Override
    public void dcssdkEventFirmwareUpdate(FirmwareUpdateEvent firmwareUpdateEvent) {

    }

    @Override
    public void dcssdkEventAuxScannerAppeared(DCSScannerInfo dcsScannerInfo, DCSScannerInfo dcsScannerInfo1) {

    }

    @Override
    public void initializeDcsSdkWithAppSettings() {
        int notifications_mask = 0;
        // We would like to subscribe to all scanner available/not-available events
        notifications_mask |=
                DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value |
                        DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value;
        // We would like to subscribe to all scanner connection events
        notifications_mask |=
                DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value |
                        DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value;
        // We would like to subscribe to all barcode events
        notifications_mask |= DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value;
        // subscribe to events set in notification mask

//        sdkHandler.dcssdkEnableAvailableScannersDetection(true);

        sdkHandler.dcssdkSubsribeForEvents(notifications_mask);
    }

//    public Context getContext() {
//        return context;
//    }
//
//    public void setContext(Context context) {
//        this.context = context;
//    }

    @Override
    public void showMessageBox(String message) {

    }

    @Override
    public int showBackgroundNotification(String text) {
        return 0;
    }

    @Override
    public int dismissBackgroundNotifications() {
        return 0;
    }

    @Override
    public boolean isInBackgroundMode(Context context) {
        return false;
    }

    @Override
    public void addDevListDelegate(IScannerAppEngineDevListDelegate delegate) {

    }

    @Override
    public void addDevConnectionsDelegate(IScannerAppEngineDevConnectionsDelegate delegate) {

    }

    @Override
    public void addDevEventsDelegate(IScannerAppEngineDevEventsDelegate delegate) {

    }

    @Override
    public void removeDevListDelegate(IScannerAppEngineDevListDelegate delegate) {

    }

    @Override
    public void removeDevConnectiosDelegate(IScannerAppEngineDevConnectionsDelegate delegate) {

    }

    @Override
    public void removeDevEventsDelegate(IScannerAppEngineDevEventsDelegate delegate) {

    }

    @Override
    public List<DCSScannerInfo> getActualScannersList() {
//        return mScannerInfoList;
        return null;
    }

    @Override
    public DCSScannerInfo getScannerInfoByIdx(int dev_index) {
//        if (mScannerInfoList != null)
//            return mScannerInfoList.get(dev_index);
//        else
        return null;
    }

    @Override
    public DCSScannerInfo getScannerByID(int scannerId) {
//        if (mScannerInfoList != null) {
//            for (DCSScannerInfo scannerInfo : mScannerInfoList) {
//                if (scannerInfo != null && scannerInfo.getScannerID() == scannerId)
//                    return scannerInfo;
//            }
//        }
        return null;
    }

    @Override
    public void raiseDeviceNotificationsIfNeeded() {

    }

    @Override
    public void updateScannersList() {
//        if (sdkHandler != null) {
//            mScannerInfoList.clear();
//            //ArrayList<DCSScannerInfo> scannerTreeList=new ArrayList<DCSScannerInfo>();
//            sdkHandler.dcssdkGetAvailableScannersList(mScannerInfoList);
//            sdkHandler.dcssdkGetActiveScannersList(mScannerInfoList);
//            createFlatScannerList(mScannerInfoList);
//        }
    }

    private void createFlatScannerList(ArrayList<DCSScannerInfo> scannerTreeList) {
        for (DCSScannerInfo s :
                scannerTreeList) {
            addToScannerList(s);
        }
    }

    private void addToScannerList(DCSScannerInfo s) {
//        mScannerInfoList.add(s);
        if(s.getAuxiliaryScanners() !=null) {
            for (DCSScannerInfo aux :
                    s.getAuxiliaryScanners().values()) {
                addToScannerList(aux);
            }
        }
    }

    @Override
    public DCSSDKDefs.DCSSDK_RESULT connect(int scannerId) {
        if (sdkHandler != null) {
//            if(ScannersActivity.curAvailableScanner !=null){
//                Application.sdkHandler.dcssdkTerminateCommunicationSession(ScannersActivity.curAvailableScanner.getScannerId());
//            }
            this.connected = true;
            return sdkHandler.dcssdkEstablishCommunicationSession(scannerId);
        } else {
            return DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
        }
    }

    @Override
    public void disconnect(int scannerId) {
        if (sdkHandler != null) {


            DCSSDKDefs.DCSSDK_RESULT ret = sdkHandler.dcssdkTerminateCommunicationSession(scannerId);
            this.connected = false;
//            ScannersActivity.curAvailableScanner=null;
            updateScannersList();
        }
    }

    @Override
    public DCSSDKDefs.DCSSDK_RESULT setAutoReconnectOption(int scannerId, boolean enable) {
        DCSSDKDefs.DCSSDK_RESULT ret;
        if (sdkHandler != null) {
            ret =  sdkHandler.dcssdkEnableAutomaticSessionReestablishment(enable, scannerId);
            return ret;
        }
        return DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
    }

    @Override
    public void enableScannersDetection(boolean enable) {
        if (sdkHandler != null) {
            sdkHandler.dcssdkEnableAvailableScannersDetection(enable);
        }
    }

    @Override
    public void configureNotificationAvailable(boolean enable) {
        if (sdkHandler != null) {
            if (enable) {
                sdkHandler.dcssdkSubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value |
                        DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value);
            } else {
                sdkHandler.dcssdkUnsubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value |
                        DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value);
            }
        }
    }

    @Override
    public void configureNotificationActive(boolean enable) {
        if (sdkHandler != null) {
            if (enable) {
                sdkHandler.dcssdkSubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value |
                        DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value);
            } else {
                sdkHandler.dcssdkUnsubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value |
                        DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value);
            }
        }
    }

    @Override
    public void configureNotificationBarcode(boolean enable) {
        if (sdkHandler != null) {
            if (enable) {
                sdkHandler.dcssdkSubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value);
            } else {
                sdkHandler.dcssdkUnsubsribeForEvents(DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value);
            }
        }
    }

    @Override
    public void configureNotificationImage(boolean enable) {

    }

    @Override
    public void configureNotificationVideo(boolean enable) {

    }

    @Override
    public void configureOperationalMode(DCSSDKDefs.DCSSDK_MODE mode) {

    }

    @Override
    public boolean executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE opCode, String inXML, StringBuilder outXML, int scannerID) {
        if (sdkHandler != null)
        {
            if(outXML == null){
                outXML = new StringBuilder();
            }
            DCSSDKDefs.DCSSDK_RESULT result=sdkHandler.dcssdkExecuteCommandOpCodeInXMLForScanner(opCode,inXML,outXML,scannerID);
            if(result== DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS)
                return true;
            else if(result==DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE)
                return false;
        }
        return false;
    }

//    public interface ResponseHandlerInterface {
//        void handleBarcode(Barcode barcode);
//    }
//
//    public interface ConnectedHandlerInterface {
//        void handleConnection(boolean connected);
//    }
}
