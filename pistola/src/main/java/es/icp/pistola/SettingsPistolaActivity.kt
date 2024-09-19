package es.icp.gpv.rfid

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Switch
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.zebra.rfid.api3.*
import es.icp.gpv.R
import es.icp.gpv.helpers.Constantes
import es.icp.gpv.ui.Comunes.BaseMenuActivity
import es.icp.gpv.ui.test_lectura.TestLecturaActivity
import es.icp.logs.core.MyException
import es.icp.logs.core.MyLog

class SettingsPistolaActivity : BaseMenuActivity(), ObserverConexionPistola {
    private var ctx: Context? = null
    private var seekSonido: SeekBar? = null
    private var txtNombreRFID: TextView? = null
    private var txtValorSonido: TextView? = null
    private var seekPotencia: SeekBar? = null
    private var txtValorPotencia: TextView? = null
    private var seekLecturasSimultaneas: SeekBar? = null
    private var txtLecturasSimultaneas: TextView? = null
    private var swLeerRFID: Switch? = null
    private var swLeerBarcode: Switch? = null
    private var swMostrarVibracion: Switch? = null
    private var swMostrarIndicadorBateria: Switch? = null
    private var swPorcentajeBateria: Switch? = null
    private var swAutoConnect: Switch? = null
    private val btnTest: Button? = null
    private var btnGuardar: Button? = null
    private var btnPorDefecto: Button? = null
    private var btnTestLectura: Button? = null
    private var btnBeep: Button? = null
    private var cardBotones: CardView? = null
    private var cardPistolaNoConectada: CardView? = null

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_pistola)
        ctx = this@SettingsPistolaActivity
        RfidGlobalVariables.observerConexionPistola = this
        findByIds()
        inicializarBaseMenu()
        setEvents()
    }

    protected override fun onPostResume() {
        super.onPostResume()
        try {
            status(RfidGlobalVariables.ZEBRA_MANAGER.isConnected())
        }catch (e:Exception){
            e.printStackTrace()
        }

    }

    private fun cargarValoresConfigurados() {
        try {
            txtNombreRFID!!.text = RFIDHandlerV2.reader.hostName
            val leerRFID = PreferenciasHelper.get(ctx, Constantes.LEER_RFID, true) as Boolean
            val leerBarcode = PreferenciasHelper.get(ctx, Constantes.LEER_BARCODE, true) as Boolean
            val volume: BEEPER_VOLUME =
                RfidGlobalVariables.CONNECTED_READER.Config.getBeeperVolume()
            var v = 1
            if (volume === BEEPER_VOLUME.QUIET_BEEP) {
                v = 1
            }
            if (volume === BEEPER_VOLUME.LOW_BEEP) {
                v = 2
            }
            if (volume === BEEPER_VOLUME.MEDIUM_BEEP) {
                v = 3
            }
            if (volume === BEEPER_VOLUME.HIGH_BEEP) {
                v = 4
            }
            seekSonido!!.progress = v
            pintarIndicadorSonido(v)
            val config = RFIDHandlerV2.reader.Config.Antennas.getAntennaRfConfig(1)
            val power = config.transmitPowerIndex
            seekPotencia!!.progress = power
            pintarIndicadorPotencia(power)
            RfidGlobalVariables.MOSTRAR_ICONO_BATERIA =
                PreferenciasHelper.get(ctx, Constantes.INDICADOR_BATERIA, true) as Boolean
            RfidGlobalVariables.MOSTRAR_ICONO_VIBRACION =
                PreferenciasHelper.get(ctx, Constantes.INDICADOR_VIBRACION, true) as Boolean
            RfidGlobalVariables.MOSTRAR_ICONO_PORCENTAJE =
                PreferenciasHelper.get(ctx, Constantes.PORCENTAJE_BATERIA, true) as Boolean
            RfidGlobalVariables.CONEXION_AUTOMATICA_INICIO =
                PreferenciasHelper.get(ctx, Constantes.CONEXION_AUTOMATICA_INICIO, true) as Boolean
            swLeerBarcode!!.isChecked = leerBarcode
            swLeerRFID!!.isChecked = leerRFID
            swMostrarIndicadorBateria!!.isChecked = RfidGlobalVariables.MOSTRAR_ICONO_BATERIA
            swPorcentajeBateria!!.isChecked = RfidGlobalVariables.MOSTRAR_ICONO_PORCENTAJE
            swMostrarVibracion!!.isChecked = RfidGlobalVariables.MOSTRAR_ICONO_VIBRACION
            swAutoConnect!!.isChecked = RfidGlobalVariables.CONEXION_AUTOMATICA_INICIO
        } catch (ee: Exception) {
            MyException.e(ee)
        }
    }

    private fun guardarSettings() {
        try {
            PreferenciasHelper.put(ctx, Constantes.LEER_RFID, swLeerRFID!!.isChecked)
            PreferenciasHelper.put(ctx, Constantes.LEER_BARCODE, swLeerBarcode!!.isChecked)
            PreferenciasHelper.put(
                ctx,
                Constantes.INDICADOR_BATERIA,
                swMostrarIndicadorBateria!!.isChecked
            )
            PreferenciasHelper.put(
                ctx,
                Constantes.INDICADOR_VIBRACION,
                swMostrarVibracion!!.isChecked
            )
            PreferenciasHelper.put(
                ctx,
                Constantes.PORCENTAJE_BATERIA,
                swPorcentajeBateria!!.isChecked
            )
            PreferenciasHelper.put(
                ctx,
                Constantes.CONEXION_AUTOMATICA_INICIO,
                swAutoConnect!!.isChecked
            )
            RfidGlobalVariables.PUEDE_LEER_RFID = swLeerRFID!!.isChecked
            RfidGlobalVariables.PUEDE_LEER_BARCODE = swLeerBarcode!!.isChecked
            RfidGlobalVariables.MOSTRAR_ICONO_BATERIA = swMostrarIndicadorBateria!!.isChecked
            RfidGlobalVariables.MOSTRAR_ICONO_VIBRACION = swMostrarVibracion!!.isChecked
            RfidGlobalVariables.MOSTRAR_ICONO_PORCENTAJE = swPorcentajeBateria!!.isChecked
            RfidGlobalVariables.CONEXION_AUTOMATICA_INICIO = swAutoConnect!!.isChecked
            var volume = BEEPER_VOLUME.MEDIUM_BEEP
            when (seekSonido!!.progress) {
                1 -> volume = BEEPER_VOLUME.QUIET_BEEP
                2 -> volume = BEEPER_VOLUME.LOW_BEEP
                3 -> volume = BEEPER_VOLUME.MEDIUM_BEEP
                4 -> volume = BEEPER_VOLUME.HIGH_BEEP
            }
            RfidGlobalVariables.CONNECTED_READER.Config.setBeeperVolume(volume)
            val config = RFIDHandlerV2.reader.Config.Antennas.getAntennaRfConfig(1)
            config.transmitPowerIndex = seekPotencia!!.progress
            RFIDHandlerV2.reader.Config.Antennas.setAntennaRfConfig(1, config)
            RfidGlobalVariables.CONNECTED_READER.Config.saveConfig()
        } catch (e: Exception) {
        }
    }

    override fun status(estado: Boolean) {
        if (estado) {
            cargarValoresConfigurados()
            cardBotones!!.visibility = View.VISIBLE
            cardPistolaNoConectada!!.visibility = View.GONE
        } else {
            cardBotones!!.visibility = View.GONE
            cardPistolaNoConectada!!.visibility = View.VISIBLE
        }
    }

    private fun inicializarBaseMenu() {
        RfidGlobalVariables.RESPONSE_HANDLER = this.responseHandlerInterface
        if (RfidGlobalVariables.ZEBRA_MANAGER != null && RfidGlobalVariables.ZEBRA_MANAGER.isConnected()) {
            cargarValoresConfigurados()
        }
    }

    private fun findByIds() {
        cardBotones = findViewById(R.id.cardBotones)
        cardPistolaNoConectada = findViewById(R.id.cardPistolaNoConectada)
        txtNombreRFID = findViewById(R.id.txtNombreRFID) as TextView?
        swLeerRFID = findViewById(R.id.swLeerRFID) as Switch?
        swLeerBarcode = findViewById(R.id.swLeerBarcode) as Switch?
        seekLecturasSimultaneas = findViewById(R.id.seekLecturasSimultaneas) as SeekBar?
        txtLecturasSimultaneas = findViewById(R.id.txtLecturasSimultaneas) as TextView?
        seekSonido = findViewById(R.id.seekSonido) as SeekBar?
        txtValorSonido = findViewById(R.id.txtValorSonido) as TextView?
        swMostrarVibracion = findViewById(R.id.swMostrarVibracion) as Switch?
        swMostrarIndicadorBateria = findViewById(R.id.swMostrarIndicadorBateria) as Switch?
        swPorcentajeBateria = findViewById(R.id.swPorcentajeBateria) as Switch?
        txtValorSonido = findViewById(R.id.txtValorSonido) as TextView?
        seekPotencia = findViewById(R.id.seekPotencia) as SeekBar?
        txtValorPotencia = findViewById(R.id.txtValorPotencia) as TextView?
        btnGuardar = findViewById(R.id.btnGuardar) as Button?
        btnPorDefecto = findViewById(R.id.btnPorDefecto) as Button?
        btnTestLectura = findViewById(R.id.btnTestLectura) as Button?
        btnBeep = findViewById(R.id.btnBeep) as Button?
        swAutoConnect = findViewById(R.id.swAutoConnect) as Switch?
    }

    private fun setEvents() {
        btnBeep!!.setOnClickListener { }
        btnPorDefecto!!.setOnClickListener { settingsPorDefecto() }
        btnGuardar!!.setOnClickListener {
            guardarSettings()
            //reloadActivity();
            //onBackPressed();
            this@SettingsPistolaActivity.finish()
        }
        btnTestLectura!!.setOnClickListener {
            startActivity(
                Intent(
                    this@SettingsPistolaActivity,
                    TestLecturaActivity::class.java
                )
            )
        }
        seekLecturasSimultaneas!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                txtLecturasSimultaneas!!.text = progress.toString()
                RfidGlobalVariables.TAGS_LEER_RFID = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        seekSonido!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                pintarIndicadorSonido(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        seekPotencia!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                pintarIndicadorPotencia(seekBar.progress)
            }
        })
    }

    private fun pintarIndicadorSonido(progress: Int) {
        try {
            MyLog.d("Pre:" + RfidGlobalVariables.CONNECTED_READER.Config.getBeeperVolume())
            var volume = BEEPER_VOLUME.MEDIUM_BEEP
            when (progress) {
                1 -> volume = BEEPER_VOLUME.QUIET_BEEP
                2 -> volume = BEEPER_VOLUME.LOW_BEEP
                3 -> volume = BEEPER_VOLUME.MEDIUM_BEEP
                4 -> volume = BEEPER_VOLUME.HIGH_BEEP
            }
            RfidGlobalVariables.CONNECTED_READER.Config.setBeeperVolume(volume)
            txtValorSonido!!.text = progress.toString()
            MyLog.d("Post:" + RfidGlobalVariables.CONNECTED_READER.Config.getBeeperVolume())
        } catch (ex: Exception) {
            MyLog.e(ex.toString())
//            Helper.TratarExcepcion(ctx, ex.message, "setOnSeekBarChangeListener", ex, null)
        }
    }

    private fun pintarIndicadorPotencia(progress: Int) {
        try {
            val c = 100 * progress / seekPotencia!!.max
            MyLog.d("valor: $progress c:$c")
            val config = RFIDHandlerV2.reader.Config.Antennas.getAntennaRfConfig(1)
            config.transmitPowerIndex = progress
            RFIDHandlerV2.reader.Config.Antennas.setAntennaRfConfig(1, config)
            //txtValorPotencia.setText(String.valueOf((valor / 10f)) + " db");
            txtValorPotencia!!.text = "$c %"
        } catch (ex: InvalidUsageException) {
            ex.printStackTrace()
            MyLog.e(ex.info)
            MyLog.e(ex.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            MyLog.e(e)
//            Helper.TratarExcepcion(ctx, e.message, "pintarIndicadorPotencia", e, "")
        }
    }

    private fun settingsPorDefecto() {
        if (RFIDHandlerV2.reader.isConnected) {
            val triggerInfo = TriggerInfo()
            triggerInfo.StartTrigger.triggerType = START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE
            triggerInfo.StopTrigger.triggerType = STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE
            try {

                //configuracion por defecto de la pistola
                RFIDHandlerV2.reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true)
                RFIDHandlerV2.reader.Config.startTrigger = triggerInfo.StartTrigger
                RFIDHandlerV2.reader.Config.stopTrigger = triggerInfo.StopTrigger
                val MAX_POWER: Int =
                    RFIDHandlerV2.reader.ReaderCapabilities.transmitPowerLevelValues.size - 1
                val config = RFIDHandlerV2.reader.Config.Antennas.getAntennaRfConfig(1)
                config.transmitPowerIndex = MAX_POWER
                config.setrfModeTableIndex(0)
                config.tari = 0
                RFIDHandlerV2.reader.Config.Antennas.setAntennaRfConfig(1, config)
                val s1_singulationControl =
                    RFIDHandlerV2.reader.Config.Antennas.getSingulationControl(1)
                s1_singulationControl.session = SESSION.SESSION_S0
                s1_singulationControl.Action.inventoryState = INVENTORY_STATE.INVENTORY_STATE_A
                s1_singulationControl.Action.slFlag = SL_FLAG.SL_ALL
                RFIDHandlerV2.reader.Config.Antennas.setSingulationControl(1, s1_singulationControl)
                RFIDHandlerV2.reader.Actions.PreFilters.deleteAll()
                val regulatoryConfig = RFIDHandlerV2.reader.Config.regulatoryConfig
                val regionInfo =
                    RFIDHandlerV2.reader.ReaderCapabilities.SupportedRegions.getRegionInfo(18) //18: esp |22. fra
                regulatoryConfig.region = regionInfo.regionCode
                regulatoryConfig.setIsHoppingOn(regionInfo.isHoppingConfigurable)
                val str = "865700,866300,866900,867500"
                regulatoryConfig.setEnabledChannels(str.split(",".toRegex()).toTypedArray())


                //configuracion de los valores que se pueden modificar desde la activity
                swLeerRFID!!.isChecked = true
                swLeerBarcode!!.isChecked = true
                seekSonido!!.progress = 4
                seekPotencia!!.progress = seekPotencia!!.max
                swMostrarVibracion!!.isChecked = true
                swMostrarIndicadorBateria!!.isChecked = true
                swPorcentajeBateria!!.isChecked = true
                swAutoConnect!!.isChecked = true
                btnGuardar!!.callOnClick()
            } catch (e: InvalidUsageException) {
                e.printStackTrace()
            } catch (e: OperationFailureException) {
                e.printStackTrace()
            }
        }
        MyLog.c("Fin config por defecto")
    }

    private fun reloadActivity() {
//        CustomDialog.dialogAdvertencia(ctx, "Valores establecidos", object : ListenerAccion() {
//            fun accion(code: Int, `object`: Any?) {
//                //reseteamos la activity
//                val intent: Intent = getIntent()
//                finish()
//                startActivity(intent)
//            }
//        })
    }

    override fun onBackPressed() {
        MyLog.c("bsack pac")
        RfidGlobalVariables.RESPONSE_HANDLER = null
        RfidGlobalVariables.observerConexionPistola = null
        super.onBackPressed()
        MyLog.c("2")
    }
}


