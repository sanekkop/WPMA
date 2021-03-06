package com.intek.wpma

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import java.util.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.intek.wpma.choiseWork.Menu
import com.intek.wpma.choiseWork.shipping.Downing
import com.intek.wpma.choiseWork.shipping.FreeComplectation
import com.intek.wpma.choiseWork.shipping.NewComplectation
import com.intek.wpma.ref.RefEmployer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class MainActivity :  BarcodeDataReceiver() {

    var barcode: String = ""
    var codeId:String = ""  //показатель по которому можно различать типы штрих-кодов
    private val sdf = SimpleDateFormat("yyyyMMdd HH:mm:ss", Locale.US)
    val currentDate = sdf.format(Date()).substring(0, 8) + " 00:00:00.000"
    val currentTime = ss.timeStrToSeconds(sdf.format(Date()).substring(9, 17))

    //region шапка с необходимыми функциями для работы сканеров перехватчиков кнопок и т.д.
    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    try {
                        barcode = intent.getStringExtra("data")!!
                        codeId = intent.getStringExtra("codeId")!!
                        reactionBarcode(barcode)
                    }
                    catch (e: Exception){
                        badVoice()
                        val toast = Toast.makeText(applicationContext, "Отсутствует соединение с базой!", Toast.LENGTH_LONG)
                        toast.show()
                    }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        //        IntentFilter intentFilter = new IntentFilter("hsm.RECVRBI");
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        onWindowFocusChanged(true)
        Log.d("IntentApiSample: ", "onResume")
        if(scanRes != null){
            try {
                barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(barcode)
            }
            catch (e: Exception){
                badVoice()
                val toast = Toast.makeText(applicationContext, "Отсутствует соединение с базой!", Toast.LENGTH_LONG)
                toast.show()
            }
        }
    }
    override fun onPause() {
        super.onPause()
        unregisterReceiver(barcodeDataReceiver)
        releaseScanner()
        Log.d("IntentApiSample: ", "onPause")
    }
    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }
    //endregion

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //проверим разрешение на камеру
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA ) == -1)
            ||(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE ) == -1)
            ||(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE ) == -1))
           // ||(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == -1))
        {
            ActivityCompat.requestPermissions(
                this,arrayOf(Manifest.permission.CAMERA,Manifest.permission.INTERNET,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE),0)
        }
        ss.CurrentMode = Global.Mode.Main
        ss.isMobile = checkCameraHardware(this)
        ss.FEmployer = RefEmployer()
        ss.badVoice.load(this, R.raw.bad,1)
        ss.goodVoice.load(this, R.raw.good,1)
        ss.clickVoice.load(this, R.raw.click, 1)
        ss.tickVoice.load(this, R.raw.tick, 1)
        if(ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@MainActivity, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","MainActivity")
                startActivity(scanAct)
            }
        }
        ss.ANDROID_ID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        ss.widthDisplay = windowManager.defaultDisplay.width
        ss.heightDisplay = windowManager.defaultDisplay.height

        if (!updateProgram()) return

        initTSD()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == 4){
            val main = Intent(this, MainActivity::class.java)
            startActivity(main)
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun reactionBarcode(Barcode: String) {
        //проверка корректности времени при сканировании ШК
        if (!syncDateTime()) {
            badVoice()
            resLbl.text = ("Системное время сбито! \n Обратитесь к администратору!")
            return
        }

        if (!initTSD()) {
            return
        }
        //расшифруем IDD
        //ИДД = "99990" + СокрЛП(Сред(ШК,3,2)) + "00" + Сред(ШК,5,8);
        //99990010010982023
        //Это перелогин или первый логин
        val barcoderes =  ss.helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        //если это не типовой справочник, то выходим
        if (typeBarcode != "113")   {
            actionLbl.text = "Нет действий с этим ШК в данном режиме"
            badVoice()
            return
        }
        val idd = barcoderes["IDD"].toString()
        //если это не сотрудник выходим
        if (!ss.isSC(idd, "Сотрудники")) {
            actionLbl.text = "Нет действий с этим ШК в данном режиме"
            badVoice()
            return
        }
        //по идее тут всегда он должен попадать не логированный, так как при создании он создает новый объект
        //все, то что нужно, проверяем а логирован сотрудник или нет
        if (ss.FEmployer.selected) {
            //а вот и не угадали, он логирован, разлогиним нашего сотрудника
            if (!logout(ss.FEmployer.id)) {
                resLbl.text = "Ошибка выхода из системы!"
                badVoice()
                return
            }
            scanRes = null      //если выходят с телефона, переприсвоим
            ss.FEmployer = RefEmployer()
            val main = Intent(this, MainActivity::class.java)
            startActivity(main)
            finish()
            return
        }
        else {
            //не логирован
           if (!ss.FEmployer.foundIDD(idd)) {
               actionLbl.text = "Нет действий с ШК в данном режиме!"
               badVoice()
               return
            }
            ss.title = ss.fullVers + " " + ss.terminal.trim() + " " + ss.helper.getShortFIO(ss.FEmployer.name)
            //инициализация входа
            if (!login(ss.FEmployer.id)) {
                actionLbl.text = "Ошибка входа в систему!"
                ss.FEmployer = RefEmployer()
                badVoice()
                return
            }
        }
        actionLbl.text = ss.FEmployer.name
        scanRes = null
        //проверим может на нем уже что-то висит и нам надо срочно туда идти
        var newMode = Intent(this, Menu::class.java)

        when (checkOrder()) {
            "Menu" -> newMode = Intent(this, Menu::class.java)

            //мы уже с заданием, и в процедуре уже вызвали нужную активити
            "Down" -> newMode = Intent(this, Downing::class.java)

            "FreeDownComplete" -> newMode = Intent(this, FreeComplectation::class.java)

            "NewComplectation" -> newMode = Intent(this, NewComplectation::class.java)
        }
        goodVoice()
        startActivity(newMode)
        finish()
    }

    private fun updateProgram() :Boolean {
        val textQuery = "select vers as vers from RT_Settings where terminal_id = '${ss.ANDROID_ID}'"
        val  dataTable = ss.executeWithReadNew(textQuery) ?:return false
        if (dataTable.isEmpty()){
            val toast = Toast.makeText(applicationContext, "Версия программы в базе не указана!", Toast.LENGTH_SHORT)
            toast.show()
            //не удлаось разобрать версию это не критично
            return true
        }
        val newVers = dataTable[0]["vers"].toString()
        if (ss.vers == newVers) {
            //Все ок, не нуждается в обновлении
            return true
        }
        //Нуждается !!! Вызываем активити обновления
        val intentUpdate = Intent()
        intentUpdate.component = ComponentName("com.intek.updateapk", "com.intek.updateapk.MainActivity")
        intentUpdate.putExtra("tsdVers",newVers)
        startActivity(intentUpdate)
        //дальше на надо нам ничего делать пусть обновляется
        return false
    }

    private fun initTSD():Boolean{
        //для начала запустим GetDataTime для обратной совместимости, ведь там он прописывает версию ТСД
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = ss.vers
        if (!execCommandNoFeedback("GetDateTime", dataMapWrite)) {
            badVoice()
            //FExcStr.text = "Не удалось подключиться к базе!"
            return false
        }
        // получим номер терминала
        val textQuery = "SELECT code as code FROM \$Спр.Терминалы (nolock) WHERE descr = '${ss.ANDROID_ID}'"
        val dataTable = ss.executeWithReadNew(textQuery) ?: return false
        if (dataTable.isEmpty()){
           // FExcStr.text = "Терминал не опознан!"
            return false
        }
        ss.terminal = dataTable[0]["code"].toString()
        //Подтянем настройку обмена МОД
        ss.Const.refresh()
        ss.title = ss.fullVers + " " + ss.terminal.trim()
        title = ss.title

        GlobalScope.launch {
            updateInitialize(this@MainActivity)
       }
        return true

    }

    private fun syncDateTime() : Boolean {

        val inaccuracyTime = 3600   //погрешность времени между сервером и системным
        val needDate = currentDate.substring(6,8) + "." + currentDate.substring(4,6) + "." + currentDate.substring(2,4)

        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = ss.vers
        val dataMapRead: MutableMap<String, Any> = mutableMapOf()
        val fieldList : MutableList<String> = mutableListOf()
        fieldList.add("Спр.СинхронизацияДанных.ДатаВход2")
        fieldList.add("Спр.СинхронизацияДанных.ДатаРез1")
        fieldList.add("Спр.СинхронизацияДанных.ДатаРез2")
        if (execCommand("GetDateTime", dataMapWrite, fieldList, dataMapRead).isNotEmpty()) {

            if (dataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"].toString().toInt() == 3) {

                //первые два параметра пока, в принципе, не нужны, пусть висят, потом придумаю, если че вылезет
                //val deviceName = dataMapRead["Спр.СинхронизацияДанных.ДатаВход2"].toString()
                val strDate = dataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString().trim()
                val sec = dataMapRead["Спр.СинхронизацияДанных.ДатаРез2"].toString().trim().toInt()

                return !(currentTime - sec > inaccuracyTime || sec - currentTime > inaccuracyTime || strDate != needDate)

            }
            else if (dataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"].toString().toInt() == -3) {
                //в тексте исключения будет ответ 1С - например о том, что версия не подходит
                actionLbl.text = dataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString()
                return false
            }
        }
        return false
    }

}
