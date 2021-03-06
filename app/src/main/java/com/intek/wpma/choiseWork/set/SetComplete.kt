package com.intek.wpma.choiseWork.set

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.choiseWork.Menu
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import com.intek.wpma.ref.RefEmployer
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_set_complete.*
import kotlinx.android.synthetic.main.activity_set_complete.FExcStr

class SetComplete : BarcodeDataReceiver() {

    private var docSet: String = ""
    private var places: Int? = null

    //region шапка с необходимыми функциями для работы сканеров перехватчиков кнопок и т.д.
    var barcode: String = ""
    var codeId: String = ""             //показатель по которому можно различать типы штрих-кодов

    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования
                    try {
                        barcode = intent.getStringExtra("data")!!
                        reactionBarcode(barcode)
                    }
                    catch(e: Exception) {
                        FExcStr.text = ("Не удалось отсканировать штрихкод!$e")
                        badVoice()
                   }

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
                FExcStr.text = e.toString()
                badVoice()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(barcodeDataReceiver)
        releaseScanner()
        Log.d("IntentApiSample: ", "onPause")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        reactionKey(keyCode, event)
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_complete)

        title = ss.title

        if (ss.FPrinter.selected) {
            printer.text = ss.FPrinter.path
            FExcStr.text = "Введите колво мест"
            enterCountPlace.visibility = View.VISIBLE
        }
        docSet = intent.extras!!.getString("iddoc")!!

        if (!isOnline(this)) {                                      //проверим интернет-соединение
            FExcStr.text = ("Ошибка доступа. Проблема интернет-соединения!")
            return
        }

        val textQuery =
            "SELECT " +
                    "journForBill.docno as DocNo, " +
                    "CONVERT(char(8), CAST(LEFT(journForBill.date_time_iddoc, 8) as datetime), 4) as DateDoc, " +
                    "journForBill.iddoc as Bill, " +
                    "Sector.descr as Sector, " +
                    "DocCCHead.\$КонтрольНабора.НомерЛиста as Number , " +
                    "DocCCHead.\$КонтрольНабора.ФлагСамовывоза as SelfRemovel " +
            "FROM " +
                "DH\$КонтрольНабора as DocCCHead (nolock) " +
                "LEFT JOIN \$Спр.Секции as Sector (nolock) " +
                    "ON Sector.id = DocCCHead.\$КонтрольНабора.Сектор " +
                "LEFT JOIN DH\$КонтрольРасходной as DocCB (nolock) " +
                    "ON DocCB.iddoc = DocCCHead.\$КонтрольНабора.ДокументОснование " +
                "LEFT JOIN DH\$Счет as Bill (nolock) " +
                    "ON Bill.iddoc = DocCB.\$КонтрольРасходной.ДокументОснование " +
                "LEFT JOIN _1sjourn as journForBill (nolock) " +
                    "ON journForBill.iddoc = Bill.iddoc " +
            "WHERE DocCCHead.iddoc = '$docSet'"
        val dataTable = ss.executeWithRead(textQuery)
        previousAction.text = if (dataTable!![1][5].toInt() == 1) "(C) " else  "" +
                dataTable[1][3].trim() + "-" +
                dataTable[1][4] + " Заявка " +
                dataTable[1][0] + " (" +
                dataTable[1][1] + ")"
        if (dataTable[1][5].toInt() == 1) DocView.text = "САМОВЫВОЗ" else DocView.text = "ДОСТАВКА"
        //тут этот код дублирую, чтобы поймать нажатие на enter после ввода колва с уже установленным принтером
        enterCountPlace.setOnKeyListener { _: View, keyCode: Int, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                if (ss.isMobile){  //спрячем клаву
                    val inputManager: InputMethodManager =  applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(this.currentFocus!!.windowToken,InputMethodManager.HIDE_NOT_ALWAYS)
                }
                // сохраняем текст, введенный до нажатия Enter в переменную
                try {
                    val count = enterCountPlace.text.toString().toInt()
                    places = count
                    enterCountPlace.visibility = View.INVISIBLE
                    countPlace.text = ("Колво мест: $places")
                    countPlace.visibility = View.VISIBLE
                    FExcStr.text = "Ожидание команды"
                } catch (e: Exception) {
               }
            }
            false
        }

        if (ss.isMobile){
            btnScanSetComplete.visibility = View.VISIBLE
            btnScanSetComplete!!.setOnClickListener {
                val scanAct = Intent(this, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","SetComplete")
                startActivity(scanAct)
            }
        }
    }

    private fun reactionBarcode(Barcode: String): Boolean {
        val idd: String = "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)

        if (!isOnline(this)) {                                      //проверим интернет-соединение
            FExcStr.text = ("Ошибка доступа. Проблема интернет-соединения!")
            return false
        }

        if (ss.isSC(idd, "Принтеры")) {
            //получим путь принтера
            if(!ss.FPrinter.foundIDD(idd)) {
                return false
            }
            printer.text = ss.FPrinter.path
            FExcStr.text = "Введите колво мест"
            enterCountPlace.visibility = View.VISIBLE

            return true
        } else if (ss.isSC(idd, "Сотрудники")) {
            lockoutDoc(docSet)      //разблокируем доки
            ss.FEmployer = RefEmployer()
            val mainInit = Intent(this, MainActivity::class.java)
            startActivity(mainInit)
            finish()
            return true
        }
        else if (!ss.isSC(idd, "Секции")) {
            FExcStr.text = "Нужен принтер и адрес предкомплектации, а не это!"
            return false
        }
        if (!ss.FPrinter.selected) {
            FExcStr.text = "Не выбран принтер!"
            return false
        }
        if (places == null){
            FExcStr.text = "Количество мест не указано!"
            return false
        }
        //подтянем адрес комплектации
        val textQuery =
            "SELECT ID, \$Спр.Секции.ТипСекции , descr FROM \$Спр.Секции (nolock) WHERE \$Спр.Секции.IDD = '$idd'"
        val dataTable = ss.executeWithRead(textQuery) ?: return false
        val addressType = dataTable[1][1]
        val addressID = dataTable[1][0]
        if (addressType == "12") {
            FExcStr.text = "Отсканируйте адрес предкопмплектации!"
            return false
        }
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] = ss.extendID(docSet, "КонтрольНабора")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(ss.FEmployer.id, "Спр.Сотрудники")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] = ss.extendID(addressID, "Спр.Секции")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = places!!
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"] = ss.FPrinter.path

        var dataMapRead: MutableMap<String, Any> = mutableMapOf()
        val fieldList: MutableList<String> = mutableListOf("Спр.СинхронизацияДанных.ДатаРез1")

        dataMapRead = execCommand("PicingComplete", dataMapWrite, fieldList, dataMapRead)

        if ((dataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() == -3) {
            FExcStr.text = dataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString()
            //сборочный уже закрыт, уйдем с формы завершения набора
            val setInitialization = Intent(this, SetInitialization::class.java)
            setInitialization.putExtra("ParentForm", "SetComplete")
            startActivity(setInitialization)
            finish()
            return false
        }
        if ((dataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() != 3) {
            FExcStr.text = "Не известный ответ робота... я озадачен..."
            return false
        }
        FExcStr.text = dataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString()

        lockoutDoc(docSet)      //разблокируем доки

        //вернемся обратно в SetInitialization
        val setInitialization = Intent(this, SetInitialization::class.java)
        setInitialization.putExtra("ParentForm", "SetComplete")
        startActivity(setInitialization)
        finish()

        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?) {

        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4){
            lockoutDoc(docSet)      //разблокируем доки
            val mainInit = Intent(this, Menu::class.java)
            startActivity(mainInit)
            finish()
            return
        }

        enterCountPlace.setOnKeyListener { _: View, keyCode: Int, event ->
            if (!isOnline(this)) {                                      //проверим интернет-соединение
                FExcStr.text = ("Ошибка доступа. Проблема интернет-соединения!")
            }
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                if (ss.isMobile){  //спрячем клаву
                    val inputManager: InputMethodManager =  applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(this.currentFocus!!.windowToken,InputMethodManager.HIDE_NOT_ALWAYS)
                }
                // сохраняем текст, введенный до нажатия Enter в переменную
                try {
                    val count = enterCountPlace.text.toString().toInt()
                    places = count
                    enterCountPlace.visibility = View.INVISIBLE
                    countPlace.text = ("Колво мест: $places")
                    countPlace.visibility = View.VISIBLE
                    FExcStr.text = "Ожидание команды"
                } catch (e: Exception) {

                }
            }
            false
        }
    }
}
