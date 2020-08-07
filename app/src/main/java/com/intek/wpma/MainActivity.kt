package com.intek.wpma

import android.Manifest
import android.content.*
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
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

    var Barcode: String = ""
    var codeId:String = ""  //показатель по которому можно различать типы штрих-кодов

    //region шапка с необходимыми функциями для работы сканеров перехватчиков кнопок и т.д.
    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    try {
                        Barcode = intent.getStringExtra("data")!!
                        codeId = intent.getStringExtra("codeId")!!
                        reactionBarcode(Barcode)
                    }
                    catch (e: Exception){
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
        Log.d("IntentApiSample: ", "onResume")
        if(scanRes != null){
            try {
                Barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(Barcode)
            }
            catch (e: Exception){
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
        SS.CurrentMode = Global.Mode.Main
        SS.isMobile = checkCameraHardware(this)
        SS.FEmployer = RefEmployer()
        if(SS.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@MainActivity, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","MainActivity")
                startActivity(scanAct)
            }
        }
        SS.ANDROID_ID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        SS.widthDisplay = windowManager.defaultDisplay.width
        SS.heightDisplay = windowManager.defaultDisplay.height

        if (!UpdateProgram()) return

        //для начала запустим GetDataTime для обратной совместимости, ведь там он прописывает версию ТСД
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = SS.Vers
        if (!ExecCommandNoFeedback("GetDateTime", dataMapWrite)) {
            val toast = Toast.makeText(applicationContext, "Не удалось подключиться к базе!", Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        // получим номер терминала
        val textQuery = "SELECT code as code FROM \$Спр.Терминалы (nolock) WHERE descr = '${SS.ANDROID_ID}'"
        val dataTable = SS.ExecuteWithReadNew(textQuery) ?:return
        if (dataTable.isEmpty()){
            val toast = Toast.makeText(applicationContext, "Терминал не опознан!", Toast.LENGTH_SHORT)
            toast.show()
            return
        }
        SS.terminal = dataTable[0]["code"].toString()
        //Подтянем настройку обмена МОД
        SS.Const.Refresh()
        SS.title = SS.Vers + " " + SS.terminal.trim()
        title = SS.title
    }

    private fun reactionBarcode(Barcode: String) {
        //расшифруем IDD
        //ИДД = "99990" + СокрЛП(Сред(ШК,3,2)) + "00" + Сред(ШК,5,8);
        //99990010010982023
        //Это перелогин или первый логин
        val barcoderes =  SS.helper.DisassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        //если это не типовой справочник, то выходим
        if (typeBarcode != "113")   {
            actionLbl.text = "Нет действий с этим ШК в данном режиме"
            return
        }
        val idd = barcoderes["IDD"].toString()
        //если это не сотрудник выходим
        if (!SS.IsSC(idd, "Сотрудники")) {
            actionLbl.text = "Нет действий с этим ШК в данном режиме"
            return
        }
        //по идее тут всегда он должен попадать не логированный, так как при создании он создает новый объект
        //все, то что нужно, проверяем а логирован сотрудник или нет
        if (SS.FEmployer.Selected) {
            //а вот и не угадали, он логирован, разлогиним нашего сотрудника
            if (!Logout(SS.FEmployer.ID)) {
                resLbl.text = "Ошибка выхода из системы!"
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
           if (!SS.FEmployer.FoundIDD(idd)) {
                actionLbl.text = "Нет действий с ШК в данном режиме!"
                return
            }
            SS.title = SS.Vers + " " + SS.terminal.trim() + " " + SS.helper.GetShortFIO(SS.FEmployer.Name)
            //инициализация входа
            if (!Login(SS.FEmployer.ID)) {
                actionLbl.text = "Ошибка входа в систему!"
                return
            }
        }
        actionLbl.text = SS.FEmployer.Name
        scanRes = null
        //проверим может на нем уже что-то висит и нам надо срочно туда идти
        var newMode = Intent(this, Menu::class.java)
        when (CheckOrder()) {
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
    private fun UpdateProgram() :Boolean {
        val textQuery = "select vers as vers from RT_Settings where terminal_id = '${SS.ANDROID_ID}'"
        val  dataTable = SS.ExecuteWithReadNew(textQuery) ?:return false
        if (dataTable.isEmpty()){
            val toast = Toast.makeText(applicationContext, "Версия программы в базе не указана!", Toast.LENGTH_SHORT)
            toast.show()
            //не удлаось разобрать версию это не критично
            return true
        }
        val newVers = dataTable[0]["vers"].toString()
        if (SS.Vers == newVers)
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
