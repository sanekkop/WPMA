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
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.Ref.RefEmployer
import kotlinx.android.synthetic.main.activity_main.*
import java.math.BigInteger


class MainActivity :  BarcodeDataReceiver() {

    var EmployerID: String  = ""
    var Employer: String = ""
    var EmployerFlags: String = ""
    var EmployerIDD: String = ""
    var Barcode: String = ""
    var codeId:String = ""  //показатель по которому можно различать типы штрих-кодов
    private var textView: TextView? = null

    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    try {
                        Barcode = intent.getStringExtra("data")
                        codeId = intent.getStringExtra("codeId")
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
        tsdNumVers.text = SS.Vers
        SS.isMobile = checkCameraHardware(this)
        SS.FEmployer = RefEmployer()
        if(SS.isMobile) {
            btnScanMainAct.visibility = View.VISIBLE
            btnScanMainAct!!.setOnClickListener {
                val scanAct = Intent(this@MainActivity, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","MainActivity")
                startActivity(scanAct)
            }
        }
        SS.ANDROID_ID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        SS.widthDisplay = windowManager.defaultDisplay.width
        SS.heightDisplay = windowManager.defaultDisplay.height

        UpdateProgram()
        //для начала запустим GetDataTime для обратной совместимости, ведь там он прописывает версию ТСД
        var dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = SS.Vers
        if (!ExecCommandNoFeedback("GetDateTime", dataMapWrite)) {
            val toast = Toast.makeText(applicationContext, "Не удалось подключиться к базе!", Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        // получим номер терминала
        var textQuery =
            "SELECT " +
                    "SC5096.code " +
                    "FROM " +
                    "SC5096 " +
                    "WHERE " +
                    "descr = '${SS.ANDROID_ID}'"
        var dataTable: Array<Array<String>>
        try {
            dataTable = SS.ExecuteWithRead(textQuery)!!
        }
        catch (e: Exception) {
            val toast = Toast.makeText(applicationContext, "Не удалось подключиться к базе!", Toast.LENGTH_SHORT)
            toast.show()
            return
        }
        if (dataTable!!.isEmpty()){
            val toast = Toast.makeText(applicationContext, "Терминал не опознан!", Toast.LENGTH_SHORT)
            toast.show()
            return
        }
        SS.terminal = dataTable!![1][0]
        terminalView.text = SS.terminal
        //Подтянем настройку обмена МОД
        SS.Const.Refresh()
        SS.title = SS.Vers + " " + SS.terminal.trim()

    }

    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }

    private fun setText(text: String) {
        if (textView != null) {
            runOnUiThread { textView!!.text = text }
        }
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
        }
        val idd = barcoderes["IDD"].toString()
        //если это не сотрудник выходим
        if (!SS.IsSC(idd, "Сотрудники")) {
            actionLbl.text = "Нет действий с этим ШК в данном режиме"
        }
        //все, то что нужно, проверяем а логирован сотрудник или нет
        if (SS.FEmployer.Selected && SS.FEmployer.IDD == idd){
            //логинился
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
        if (!SS.FEmployer.Selected ){
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
            else if (SS.FEmployer.IDD != idd) {
            if (!Logout(SS.FEmployer.ID)) {
                resLbl.text = "Ошибка выхода из системы!"
                return
            }
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
        if (SS.FEmployer.Selected) {
            actionLbl.text = SS.FEmployer.Name
            scanRes = null
            val menu = Intent(this, Menu::class.java)
            menu.putExtra("ParentForm","MainActivity")
            startActivity(menu)

        }
    }
    private fun UpdateProgram()
    {
        val textQuery = "select vers as vers from RT_Settings where terminal_id = '${SS.ANDROID_ID}'";
        var dataTable: Array<Array<String>>
        try {
            dataTable = SS.ExecuteWithRead(textQuery)!!
        }
        catch (e: Exception) {
            val toast = Toast.makeText(applicationContext, "Не удалось подключиться к базе!", Toast.LENGTH_SHORT)
            toast.show()
            return
        }
        if (dataTable!!.isEmpty()){
            val toast = Toast.makeText(applicationContext, "Терминал не опознан!", Toast.LENGTH_SHORT)
            toast.show()
            return
        }
        val newVers = dataTable!![1][0]
        if (SS.Vers == newVers)
       {
            //Все ок, не нуждается в обновлении
            return
        }
        //Нуждается !!! Вызываем активити обновления
        val intentUpdate = Intent()
        intentUpdate.component = ComponentName("com.intek.updateapk", "com.intek.updateapk.MainActivity")
        intentUpdate.putExtra("tsdVers",newVers)
        startActivity(intentUpdate)
        return
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
}
