package es.icp.pistola;

import android.app.Dialog;
import android.view.Menu;
import android.widget.ListView;

import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;

import es.icp.icp_commons.CustomNotification;

public class RfidGlobalVariables {

    public static boolean PUEDE_LEER_RFID    = true;
    public static boolean PUEDE_LEER_BARCODE = true;

    // VARIABLES EXCLUSIVAS PISTOLA
    public static ZebraManagerV2                          ZEBRA_MANAGER                  = null;
    public static CustomProgressDialog                    ZEBRA_MANAGER_PROGRESS_DIALOG  = null;
    public static ResponseHandlerInterface                RESPONSE_HANDLER               = null;
    public static ListView                                ZEBRA_MANAGER_PAIRED_LIST_VIEW = null;
    public static Menu                                    MENU                           = null;
    public static ReaderDevice                            CONNECTED_DEVICE               = null;
    public static RFIDReader                              CONNECTED_READER               = null;
    public static String                                  LAST_CONNECTED_READER          = "";
    public static int                                     LAST_CONNECTED_READER_POS      = 0;
    public static DeviceConnectTask                       deviceConnectTask              = null;
    public static CustomNotification                      universalNotification          = null;
    public static int                                     BATTERY_LEVEL                  = 0;
    public static boolean                                 BATTERY_CHARGING               = false;
    public static String                                  BATTERY_EVENT_CAUSE            = "";
    public static Dialog                                  BATTERY_DIALOG                 = null;

    public static ObserverConexionPistola      observerConexionPistola;
    public static ObserverPreparedZebraManager observerPreparedZebraManager;

    public static int     TAGS_LEER_RFID     = 100;

    public static boolean VIBRACION              = true;

    public static boolean MOSTRAR_ICONO_VIBRACION    = false;
    public static boolean MOSTRAR_ICONO_BATERIA      = true;
    public static boolean MOSTRAR_ICONO_PORCENTAJE   = false;
    public static boolean CONEXION_AUTOMATICA_INICIO = false;

    public static boolean CONEXION_AUTOMATICA = true;
}





