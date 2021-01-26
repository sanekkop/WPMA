package com.intek.wpma

import android.Manifest
import android.content.*
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.intek.wpma.ChoiseWork.Menu
import com.intek.wpma.ChoiseWork.Shipping.Downing
import com.intek.wpma.ChoiseWork.Shipping.FreeComplectation
import com.intek.wpma.ChoiseWork.Shipping.NewComplectation
import com.intek.wpma.Ref.RefEmployer
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity :  BarcodeDataReceiver() {

    var barcode: String = ""
    var codeId:String = ""  //показатель по которому можно различать типы штрих-кодов

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
                        badVoise()
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
                badVoise()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //проверим разрешение на камеру
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA ) == -1)
            ||(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE ) == -1)
            ||(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE ) == -1))
        {
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.CAMERA,Manifest.permission.INTERNET,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE),0)
        }
        ss.CurrentMode = Global.Mode.Main
        ss.isMobile = checkCameraHardware(this)
        ss.FEmployer = RefEmployer()
        ss.badvoise.load(this, R.raw.bad,1)
        ss.goodvoise.load(this, R.raw.good,1)
        ss.clickvoise.load(this, R.raw.click, 1)
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

        //для начала запустим GetDataTime для обратной совместимости, ведь там он прописывает версию ТСД
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = ss.vers
        if (!execCommandNoFeedback("GetDateTime", dataMapWrite)) {
            badVoise()
            val toast = Toast.makeText(applicationContext, "Не удалось подключиться к базе!", Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        // получим номер терминала
        val textQuery = "SELECT code as code FROM \$Спр.Терминалы (nolock) WHERE descr = '${ss.ANDROID_ID}'"
        val dataTable = ss.executeWithReadNew(textQuery) ?:return
        if (dataTable.isEmpty()){
            val toast = Toast.makeText(applicationContext, "Терминал не опознан!", Toast.LENGTH_SHORT)
            toast.show()
            return
        }
        ss.terminal = dataTable[0]["code"].toString()
        //Подтянем настройку обмена МОД
        ss.Const.refresh()
        ss.title = ss.fullVers + " " + ss.terminal.trim()
        title = ss.title
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
        //расшифруем IDD
        //ИДД = "99990" + СокрЛП(Сред(ШК,3,2)) + "00" + Сред(ШК,5,8);
        //99990010010982023
        //Это перелогин или первый логин
        val barcoderes =  ss.helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        //если это не типовой справочник, то выходим
        if (typeBarcode != "113")   {
            actionLbl.text = "Нет действий с этим ШК в данном режиме"
            badVoise()
            return
        }
        val idd = barcoderes["IDD"].toString()
        //если это не сотрудник выходим
        if (!ss.isSC(idd, "Сотрудники")) {
            actionLbl.text = "Нет действий с этим ШК в данном режиме"
            badVoise()
            return
        }
        //по идее тут всегда он должен попадать не логированный, так как при создании он создает новый объект
        //все, то что нужно, проверяем а логирован сотрудник или нет
        if (ss.FEmployer.selected) {
            //а вот и не угадали, он логирован, разлогиним нашего сотрудника
            if (!logout(ss.FEmployer.id)) {
                resLbl.text = "Ошибка выхода из системы!"
                badVoise()
                return
            }
            scanRes = null      //если выходят с телефона, переприсвоим
            val main = Intent(this, MainActivity::class.java)
            startActivity(main)
            finish()
            return
        }
        else {
            //не логирован
           if (!ss.FEmployer.foundIDD(idd)) {
                actionLbl.text = "Нет действий с ШК в данном режиме!"
               badVoise()
               return
            }
            ss.title = ss.fullVers + " " + ss.terminal.trim() + " " + ss.helper.getShortFIO(ss.FEmployer.name)
            //инициализация входа
            if (!login(ss.FEmployer.id)) {
                actionLbl.text = "Ошибка входа в систему!"
                badVoise()
                return
            }
        }
        actionLbl.text = ss.FEmployer.name
        scanRes = null
        //проверим может на нем уже что-то висит и нам надо срочно туда идти
        var newMode = Intent(this, Menu::class.java)
        when (checkOrder()) {
            "Menu" -> {
                newMode = Intent(this, Menu::class.java)
            }
            //мы уже с заданием, и в процедуре уже вызвали нужную активити
            "Down" -> {
                newMode = Intent(this, Downing::class.java)
            }
            "FreeDownComplete" -> {
                newMode = Intent(this, FreeComplectation::class.java)
            }
            "NewComplectation" -> {
                newMode = Intent(this, NewComplectation::class.java)
             }
        }
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
        if (ss.vers == newVers)
       {
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

}
