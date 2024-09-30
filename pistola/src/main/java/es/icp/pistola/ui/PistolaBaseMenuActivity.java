package es.icp.pistola.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;

import es.icp.icp_commons.CustomNotification;
import es.icp.icp_commons.PreferenciasHelper;
import es.icp.logs.core.MyLog;
import es.icp.pistola.Barcode;
import es.icp.pistola.ConnectedHandlerInterface;
import es.icp.pistola.Constantes;
import es.icp.pistola.DeviceConnectTask;
import es.icp.pistola.ObserverPreparedZebraManager;
import es.icp.pistola.R;
import es.icp.pistola.TagData;
import es.icp.pistola.ui.adapters.ReaderListAdapter;
import es.icp.pistola.ResponseHandlerInterface;
import es.icp.pistola.RfidGlobalVariables;
import es.icp.pistola.ZebraManagerV2;

public abstract class PistolaBaseMenuActivity extends AppCompatActivity implements ObserverPreparedZebraManager, ResponseHandlerInterface, ConnectedHandlerInterface {

    // PROPIEDADES

    public ResponseHandlerInterface responseHandlerInterface;
    public ConnectedHandlerInterface connectedHandlerInterface;
    private ReaderListAdapter readerListAdapter;
    public Menu menu;
    private Vibrator vibrator;
    private static final long TIEMPO_VIBRACION = 200;
    protected CustomNotification universalNotification = null;
    private Dialog batteryDialog = null;
    private static boolean preparedZebraManager = false;

    // MÉTODOS PARA PINTAR/INICIALIZAR EL MENÚ

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        RfidGlobalVariables.observerPreparedZebraManager = this;
        if (RfidGlobalVariables.ZEBRA_MANAGER == null) {
            this.inicializarZebraManager();
        } else {
            RfidGlobalVariables.ZEBRA_MANAGER.setConnectedHandler(this);
        }
        this.menu = menu;
        this.inflateMenu();
        return super.onCreateOptionsMenu(this.menu);
    }

    public Activity getActivity() {
        return this;
    }

    private void inflateMenu() {
        if (!RfidGlobalVariables.ZEBRA_MANAGER.isConnected()) {
            addNormalIcon();
        }
        else {
            addBatteryIcon();
        }
        this.inicializarMenu();
        this.inicializarVibracion();
    }

    private void addBatteryIcon() {
        if (this.menu != null) {
            this.menu.clear();
            this.getMenuInflater().inflate(R.menu.menu_pistola_vib_bat, this.menu);
            this.menu.getItem(2).getIcon().setLevel(RfidGlobalVariables.BATTERY_LEVEL);
        }
    }

    private void addNormalIcon() {
        if (this.menu != null) {
            this.menu.clear();
            this.getMenuInflater().inflate(R.menu.menu_pistola_vib, this.menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final int id = item.getItemId();

        if (id == R.id.ic_action_pistola) { // icono pistola
            try {
                if (RfidGlobalVariables.ZEBRA_MANAGER.getAvailableDevices().size() == 1 && RfidGlobalVariables.CONEXION_AUTOMATICA) {
                    this.conexionAutomatica(RfidGlobalVariables.ZEBRA_MANAGER.getAvailableDevices().get(0).getRFIDReader(), getApplicationContext());
                } else {
                    this.mostrarReadersListFragment();
                }
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            }

        } else if (id == R.id.ic_action_vibrate) { // icono vibración

            if (RfidGlobalVariables.VIBRACION) {
                menu.getItem(0).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_vibrar_off));
                RfidGlobalVariables.VIBRACION = false;
                showUniversalNotification(getString(R.string.vibracion_off));
            } else {
                menu.getItem(0).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_vibrar_on));
                RfidGlobalVariables.VIBRACION = true;
                this.vibrator.vibrate(TIEMPO_VIBRACION);
                showUniversalNotification(getString(R.string.vibracion_on));
            }
            es.icp.icp_commons.PreferenciasHelper.put(getApplicationContext(), "VIBRACION",  RfidGlobalVariables.VIBRACION);

        } else if (id == R.id.ic_action_battery) { // icono batería

            this.mostrarBatteryFragment();

        }

        return super.onOptionsItemSelected(item);
    }

    // MÉTODOS PRIVADOS

    public void conexionAutomatica(final RFIDReader readerDevice, Context context) {
        if (RfidGlobalVariables.CONNECTED_READER == null) {
            if (RfidGlobalVariables.deviceConnectTask == null || RfidGlobalVariables.deviceConnectTask.isCancelled()) {
                RfidGlobalVariables.deviceConnectTask = new DeviceConnectTask(this,0, readerDevice, getString(R.string.conectado_con) + Constantes.space+  readerDevice.getHostName());
                RfidGlobalVariables.deviceConnectTask.execute();
                PistolaBaseMenuActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        menu.getItem(1).setIcon(ContextCompat.getDrawable(context, R.drawable.ic_connecting_white));
                        showUniversalNotification(getString(R.string.conectado_con) + Constantes.space + readerDevice.getHostName());
                    }
                });
            }
        } else {
            RfidGlobalVariables.deviceConnectTask = null;
            RfidGlobalVariables.ZEBRA_MANAGER.disconnect();
            RfidGlobalVariables.CONNECTED_READER = null;
            RfidGlobalVariables.CONNECTED_DEVICE = null;
            Activity activity = this;

            this.runOnUiThread(new Runnable() {
                @Override
                public void run() { // para actualizar los items / checkbox
                    invalidateOptionsMenu();
                    showUniversalNotification(getString(R.string.desconectado) + Constantes.space + RfidGlobalVariables.LAST_CONNECTED_READER);
                    menu.getItem(1).setIcon(ContextCompat.getDrawable(context, R.drawable.ic_disconnected_white));
                    if (!readerDevice.getHostName().equals(RfidGlobalVariables.LAST_CONNECTED_READER) && (RfidGlobalVariables.deviceConnectTask == null || RfidGlobalVariables.deviceConnectTask.isCancelled())) {
                        RfidGlobalVariables.deviceConnectTask = new DeviceConnectTask(activity,0, readerDevice, getString(R.string.conectado_con) + Constantes.space+  readerDevice.getHostName());
                        RfidGlobalVariables.deviceConnectTask.execute();
                        showUniversalNotification(getString(R.string.conectado_con) + Constantes.space + readerDevice.getHostName());
                        menu.getItem(1).setIcon(ContextCompat.getDrawable(context, R.drawable.ic_connecting_white));
                    }
                }
            });
        }
    }

    private void showUniversalNotification(String text) {
        RfidGlobalVariables.universalNotification.showText(text);
    }

    public CustomNotification inicializarUniversalNotification() {
        return new CustomNotification.Builder(PistolaBaseMenuActivity.this)
                .setSimpleMode()
                .setDuration(CustomNotification.LENGTH_SHORT)
                .build();
    }

    public void inicializarVibracion() {
        try {
            if (this.menu != null) {

                this.vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                RfidGlobalVariables.VIBRACION = (Boolean) PreferenciasHelper.get(getApplicationContext(), "VIBRACION", true);

                if (RfidGlobalVariables.VIBRACION) {
                    this.menu.getItem(0).setIcon(ContextCompat.getDrawable(PistolaBaseMenuActivity.this, R.drawable.ic_vibrar_on));
                } else {
                    this.menu.getItem(0).setIcon(ContextCompat.getDrawable(PistolaBaseMenuActivity.this, R.drawable.ic_vibrar_off));
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            //Helper.TratarExcepcion(getActivity(), e.getMessage(), "BaseMenuActivity NullPointerException  - inicializarVibracion", e, "");
        }
    }

    //ResponseHandlerInterface Implementation

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


    //ConnectedHandlerInterface Implementation

    @Override
    public void handleConnection(final boolean connected) {
        invalidateOptionsMenu();
        if (connected) {
            String defaults = RfidGlobalVariables.ZEBRA_MANAGER.rfidDefaultConfig();
            guardarReaderSharedPreferences();
            PistolaBaseMenuActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                        RepoApp_OLD.InsertarLetcor(getApplicationContext(), RfidGlobalVariables.LAST_CONNECTED_READER, RfidGlobalVariables.SFID, new VolleyCallBack() {
//                            @Override
//                            public void onSuccess(Object result) {
//                                //No hacemos nada
//                            }
//
//                            @Override
//                            public void onError(String error) {
//                                //No hacemos nada
//                            }
//
//                            @Override
//                            public void onOffline() {
//                                //No hacemos nada
//                            }
//                        });
                    showUniversalNotification(getString(R.string.conectado) + Constantes.space + RfidGlobalVariables.LAST_CONNECTED_READER);
                    menu.getItem(1).setIcon(ContextCompat.getDrawable(PistolaBaseMenuActivity.this, R.drawable.ic_connected));
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
            PistolaBaseMenuActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showUniversalNotification(getString(R.string.error_conexion_intentelo_nuevamente));
                    menu.getItem(1).setIcon(ContextCompat.getDrawable(PistolaBaseMenuActivity.this, R.drawable.ic_disconnected_white));
                    if (RfidGlobalVariables.BATTERY_DIALOG != null) RfidGlobalVariables.BATTERY_DIALOG.dismiss();
                    if (RfidGlobalVariables.observerConexionPistola != null) {
                        RfidGlobalVariables.observerConexionPistola.status(connected);
                    }
                }
            });
        }
        PistolaBaseMenuActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() { // para actualizar los items / checkbox
                try {
                    if (RfidGlobalVariables.ZEBRA_MANAGER != null && RfidGlobalVariables.ZEBRA_MANAGER.getAvailableDevices().size() > 0){
                        PistolaBaseMenuActivity.this.readerListAdapter = new ReaderListAdapter(PistolaBaseMenuActivity.this, R.layout.readers_list_item, RfidGlobalVariables.ZEBRA_MANAGER.getAvailableDevices());
                        if (RfidGlobalVariables.ZEBRA_MANAGER_PAIRED_LIST_VIEW != null) RfidGlobalVariables.ZEBRA_MANAGER_PAIRED_LIST_VIEW.setAdapter(PistolaBaseMenuActivity.this.readerListAdapter);
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
        PistolaBaseMenuActivity.this.rellenarDatosBatteryDialog();
        batteryEventCauseHandler();

        if (menu != null && menu.size() > 2) menu.getItem(2).getIcon().setLevel(RfidGlobalVariables.BATTERY_LEVEL);
        if (RfidGlobalVariables.MENU != null && RfidGlobalVariables.MENU.size() > 2) RfidGlobalVariables.MENU.getItem(2).getIcon().setLevel(RfidGlobalVariables.BATTERY_LEVEL);
    }

    private void guardarReaderSharedPreferences() {
        if (RfidGlobalVariables.CONNECTED_READER != null ){
            PreferenciasHelper.put(getApplicationContext(), Constantes.ULTIMA_PISTOLA_CONECTADA,  RfidGlobalVariables.CONNECTED_READER.getHostName());
        }
    }

    private RFIDReader recuperarReaderSharedPreferences() {
        RFIDReader readerDevice = null;
        String name = (String) PreferenciasHelper.get(getApplicationContext(), Constantes.ULTIMA_PISTOLA_CONECTADA, "");
        if (!name.equals("")) {
//            readerDevice = new Gson().fromJson(json, RFIDReader.class);
            try {
                for (ReaderDevice rd : RfidGlobalVariables.ZEBRA_MANAGER.getAvailableDevices()) {
                    if (rd.getRFIDReader().getHostName().equals(name)) {
                        readerDevice = rd.getRFIDReader();
                    }
                }
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            }
        }
        return readerDevice;
    }

    protected void conexionAutomaticaUltimaPistolaConectada(Context context) {
        RFIDReader readerDevice = recuperarReaderSharedPreferences();
        if (readerDevice != null && RfidGlobalVariables.CONEXION_AUTOMATICA) {
            conexionAutomatica(readerDevice, context);
        }
    }

    private void batteryEventCauseHandler() {
        RfidGlobalVariables.BATTERY_EVENT_CAUSE = RfidGlobalVariables.BATTERY_EVENT_CAUSE.toLowerCase();
        if (!RfidGlobalVariables.BATTERY_EVENT_CAUSE.equals(Constantes.BATTERY_REQUESTED)) {
            switch (RfidGlobalVariables.BATTERY_EVENT_CAUSE) {
                case Constantes.BATTERY_CHARGING:
                    showUniversalNotification(getString(R.string.charger_connected));
                    break;
                case Constantes.BATTERY_DISCHARGING:
                    showUniversalNotification(getString(R.string.charger_disconnected));
                    break;
                case Constantes.BATTERY_CRITICAL:
                    showUniversalNotification(getString(R.string.battery_critical));
                    break;
                case Constantes.BATTERY_LOW:
                    showUniversalNotification(getString(R.string.battery_low));
                    break;
            }
        }
    }

    private void rellenarDatosBatteryDialog() {
        if (RfidGlobalVariables.BATTERY_DIALOG != null) {
            MyLog.d("rellenando batterydialog");
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (RfidGlobalVariables.BATTERY_DIALOG != null){
                            ((TextView) RfidGlobalVariables.BATTERY_DIALOG.findViewById(R.id.batteryLevelText)).setText(RfidGlobalVariables.BATTERY_LEVEL + "%");
                            ((TextView) RfidGlobalVariables.BATTERY_DIALOG.findViewById(R.id.batteryStatusText)).setText((RfidGlobalVariables.BATTERY_CHARGING) ? "Estado: cargando" : "Estado: descargando");
                            ((ImageView) RfidGlobalVariables.BATTERY_DIALOG.findViewById(R.id.batteryLevelImage)).setImageLevel((int) (RfidGlobalVariables.BATTERY_LEVEL / 10));
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                        //Helper.TratarExcepcion(getActivity(), e.getMessage(), "BaseMenuActivity NullPointerException  - rellenarDatosBatteryDialog", e, "");
                    }
                }
            });
        }
    }

    private void inicializarMenu() {
        if (RfidGlobalVariables.ZEBRA_MANAGER != null && RfidGlobalVariables.ZEBRA_MANAGER.isConnected()) {
            this.menu.getItem(1).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_connected));
        } else {
            this.menu.getItem(1).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_disconnected_white));
        }


        ocultarMostrarIconosMenu();
    }

    private void inicializarZebraManager() {
        this.responseHandlerInterface = this;
        this.connectedHandlerInterface = this;
        RfidGlobalVariables.ZEBRA_MANAGER = new ZebraManagerV2(this,
                ZebraManagerV2.DefaultMode.RFID,
                ZebraManagerV2.RunnableMode.BOTH_MODES,
                this.responseHandlerInterface,
                this.connectedHandlerInterface);
    }

    private void ocultarMostrarIconosMenu(){

        if (this.menu != null && this.menu.size() > 0) this.menu.getItem(0).setVisible(RfidGlobalVariables.MOSTRAR_ICONO_VIBRACION);
        //if (this.menu != null && this.menu.size() > 1) this.menu.getItem(1).setVisible(RfidGlobalVariables.MOSTRAR_ICONO_BATERIA);
        if (this.menu != null && this.menu.size() > 2) this.menu.getItem(2).setVisible(RfidGlobalVariables.MOSTRAR_ICONO_BATERIA);




    }
    private void mostrarBatteryFragment() {
        this.batteryDialog = new Dialog(this, android.R.style.Theme_Material_Light_Dialog);
        this.batteryDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(255, 255, 255)));
        this.batteryDialog.setContentView(R.layout.fragment_battery_info);
        this.batteryDialog.setCancelable(true);
        this.batteryDialog.setTitle(getActivity().getString(R.string.informaci_n_de_la_bater_a_rfid));
        this.batteryDialog.show();

        RfidGlobalVariables.BATTERY_DIALOG = this.batteryDialog;

        rellenarDatosBatteryDialog();

    }

    public void mostrarReadersListFragment() {
//        RfidGlobalVariables.ZEBRA_MANAGER.prepare();
        MyLog.d("MOSTRANDO LISTA DE DISPOSITIVOS EMPAREJADOS");
        final Dialog rlDialogue = new Dialog(this, android.R.style.Theme_Material_Light_Dialog);
        rlDialogue.getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(255, 255, 255)));
        rlDialogue.setContentView(R.layout.fragment_readers_list);
        rlDialogue.setCancelable(true);
        rlDialogue.setTitle(getActivity().getString(R.string.fragment_readers_list_bondedReadersTitle_text));
        rlDialogue.show();

        RfidGlobalVariables.ZEBRA_MANAGER_PAIRED_LIST_VIEW = rlDialogue.findViewById(R.id.bondedReadersList);
        final TextView tv_emptyView = rlDialogue.findViewById(R.id.empty);
        try {
            PistolaBaseMenuActivity.this.readerListAdapter = new ReaderListAdapter(this, R.layout.readers_list_item, RfidGlobalVariables.ZEBRA_MANAGER.getAvailableDevices());

            if (PistolaBaseMenuActivity.this.readerListAdapter.getCount() == 0) {
                RfidGlobalVariables.ZEBRA_MANAGER_PAIRED_LIST_VIEW.setEmptyView(tv_emptyView);
            } else
                RfidGlobalVariables.ZEBRA_MANAGER_PAIRED_LIST_VIEW.setAdapter(PistolaBaseMenuActivity.this.readerListAdapter);

            RfidGlobalVariables.ZEBRA_MANAGER_PAIRED_LIST_VIEW.setOnItemClickListener(mDeviceClickListener);
            RfidGlobalVariables.ZEBRA_MANAGER_PAIRED_LIST_VIEW.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, final int pos, long arg3) {
            final ReaderDevice readerDevice = PistolaBaseMenuActivity.this.readerListAdapter.getItem(pos);
            if (RfidGlobalVariables.CONNECTED_READER == null) {
                if (RfidGlobalVariables.deviceConnectTask == null || RfidGlobalVariables.deviceConnectTask.isCancelled()) {
                    RfidGlobalVariables.deviceConnectTask = new DeviceConnectTask((Activity) v.getContext(), pos, readerDevice.getRFIDReader(), getString(R.string.conectado_con) + Constantes.space+  readerDevice.getName());
                    RfidGlobalVariables.deviceConnectTask.execute();
                    menu.getItem(1).setIcon(ContextCompat.getDrawable(PistolaBaseMenuActivity.this, R.drawable.ic_connecting_white));
                    showUniversalNotification(getString(R.string.conectado_con) + Constantes.space + readerDevice.getName());
                }
            } else {
                RfidGlobalVariables.deviceConnectTask = null;
                RfidGlobalVariables.ZEBRA_MANAGER.disconnect();
                RfidGlobalVariables.CONNECTED_READER = null;
                RfidGlobalVariables.CONNECTED_DEVICE = null;
                PistolaBaseMenuActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() { // para actualizar los items / checkbox
                        invalidateOptionsMenu();
                        try {
                            PistolaBaseMenuActivity.this.readerListAdapter = new ReaderListAdapter(PistolaBaseMenuActivity.this, R.layout.readers_list_item, RfidGlobalVariables.ZEBRA_MANAGER.getAvailableDevices());
                            RfidGlobalVariables.ZEBRA_MANAGER_PAIRED_LIST_VIEW.setAdapter(PistolaBaseMenuActivity.this.readerListAdapter);
                        } catch (InvalidUsageException e) {
                            MyLog.e( "ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                        showUniversalNotification(getString(R.string.desconectado) + Constantes.space + RfidGlobalVariables.LAST_CONNECTED_READER);
                        menu.getItem(1).setIcon(ContextCompat.getDrawable(PistolaBaseMenuActivity.this, R.drawable.ic_disconnected_white));
                        if (!readerDevice.getRFIDReader().getHostName().equals(RfidGlobalVariables.LAST_CONNECTED_READER) && (RfidGlobalVariables.deviceConnectTask == null || RfidGlobalVariables.deviceConnectTask.isCancelled())) {
                            RfidGlobalVariables.deviceConnectTask = new DeviceConnectTask((Activity) v.getContext(), pos, readerDevice.getRFIDReader(), getString(R.string.conectado_con) + Constantes.space+  readerDevice.getName());
                            RfidGlobalVariables.deviceConnectTask.execute();
                            showUniversalNotification(getString(R.string.conectado_con) + Constantes.space + readerDevice.getName());
                            menu.getItem(1).setIcon(ContextCompat.getDrawable(PistolaBaseMenuActivity.this, R.drawable.ic_connecting_white));
                        }
                    }
                });
            }
        }
    };

    // MÉTODOS CICLO DE VIDA

    @Override
    protected void onPostResume() {
        super.onPostResume();
        RfidGlobalVariables.universalNotification = this.inicializarUniversalNotification();
        RfidGlobalVariables.BATTERY_DIALOG = this.batteryDialog;
        RfidGlobalVariables.universalNotification.setActivity(this);
        try{
            if (RfidGlobalVariables.ZEBRA_MANAGER != null) RfidGlobalVariables.ZEBRA_MANAGER.prepare();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (RfidGlobalVariables.ZEBRA_MANAGER != null && RfidGlobalVariables.ZEBRA_MANAGER_PAIRED_LIST_VIEW != null) {
            try {
                PistolaBaseMenuActivity.this.readerListAdapter = new ReaderListAdapter(this, R.layout.readers_list_item, RfidGlobalVariables.ZEBRA_MANAGER.getAvailableDevices());
                RfidGlobalVariables.ZEBRA_MANAGER_PAIRED_LIST_VIEW.setAdapter(PistolaBaseMenuActivity.this.readerListAdapter);
            } catch (InvalidUsageException e) {
                e.printStackTrace();
                MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
            }
            RfidGlobalVariables.ZEBRA_MANAGER.setConnectedHandler(this);
        }

        if (RfidGlobalVariables.ZEBRA_MANAGER != null && this.menu != null) {
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onPrepared() {
        if (!preparedZebraManager) {
            preparedZebraManager = true;
            if (RfidGlobalVariables.CONEXION_AUTOMATICA_INICIO) {
                conexionAutomaticaUltimaPistolaConectada(getApplicationContext());
            }
        }

        if (RfidGlobalVariables.ZEBRA_MANAGER != null && RfidGlobalVariables.ZEBRA_MANAGER_PAIRED_LIST_VIEW != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        PistolaBaseMenuActivity.this.readerListAdapter = new ReaderListAdapter(PistolaBaseMenuActivity.this, R.layout.readers_list_item, RfidGlobalVariables.ZEBRA_MANAGER.getAvailableDevices());
                        RfidGlobalVariables.ZEBRA_MANAGER_PAIRED_LIST_VIEW.setAdapter(PistolaBaseMenuActivity.this.readerListAdapter);
                    } catch (InvalidUsageException e) {
                        MyLog.e("ERROR "+ new Object(){}.getClass().getEnclosingMethod().getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

        }
    }
}
