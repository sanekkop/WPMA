package com.intek.wpma

import android.Manifest
import android.content.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        tsdNumVers.text = tsdVers
        SS.isMobile = checkCameraHardware(this)

        if(SS.isMobile) {
            btnScanMainAct.visibility = View.VISIBLE
            btnScanMainAct!!.setOnClickListener {
                val scanAct = Intent(this@MainActivity, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","MainActivity")
                startActivity(scanAct)
            }
        }
        SS.ANDROID_ID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        UpdateProgram()
        //для начала запустим GetDataTime для обратной совместимости, ведь там он прописывает версию ТСД
        var dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = tsdVers
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
        if (EmployerIDD != "" && EmployerIDD == "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)) {
            if (!Logout(EmployerID)) {
                resLbl.text = "Ошибка выхода из системы!"
                return
            }
            scanRes = null      //если выходят с телефона, переприсвоим
            val main = Intent(this, MainActivity::class.java)
            startActivity(main)
            finish()
            return
        }
        if (EmployerIDD == "" || EmployerIDD != "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)) {
            if(EmployerIDD != "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12) && EmployerIDD != "") {
                if (!Logout(EmployerID)) {
                    resLbl.text = "Ошибка выхода из системы!"
                    return
                }
            }
            EmployerIDD = "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)

            val textQuery: String =
                "Select top 1 descr as Descr, SC838.SP4209 as Settings, ID as ID From SC838 (nolock) WHERE SP1933 = '" + EmployerIDD + "'"
            val dataTable = SS.ExecuteWithRead(textQuery)
            if (dataTable == null) {
                actionLbl.text = SS.ExcStr
                return
            } else if (dataTable.isEmpty()) {
                actionLbl.text = "Нет действий с ШК в данном режиме!"
                return
            }

            Employer = dataTable!!?.get(1)[0].trim()
            EmployerFlags = dataTable[1][1]
            EmployerID = dataTable[1][2]
            SS.FEmployer.FoundID(EmployerID)
            //инициализация входа
            if (!Login(EmployerID)) {
                actionLbl.text = "Ошибка входа в систему!"
                return
            }
            //конвертнем настройки сотрудника в 2ую сс
            var bigInteger: BigInteger = EmployerFlags.toBigInteger()
            EmployerFlags = bigInteger.toString(2)
        }

        if (Employer != "") {
            actionLbl.text = Employer
            scanRes = null
            val menu = Intent(this, Menu::class.java)
            menu.putExtra("Employer", Employer)
            menu.putExtra("EmployerIDD",EmployerIDD)
            menu.putExtra("EmployerFlags",EmployerFlags)
            menu.putExtra("EmployerID",EmployerID)
            menu.putExtra("ParentForm","MainActivity")
            startActivity(menu)
        } else
            actionLbl.text = "Нет действий с этим ШК в данном режиме"
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
        if (tsdVers == newVers)
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
