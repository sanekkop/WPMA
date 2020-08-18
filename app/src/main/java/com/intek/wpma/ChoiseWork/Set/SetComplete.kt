package com.intek.wpma.ChoiseWork.Set

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.R
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_set_complete.*
import kotlinx.android.synthetic.main.activity_set_complete.FExcStr
import kotlinx.android.synthetic.main.activity_set_complete.terminalView

class SetComplete : BarcodeDataReceiver() {

    private var docSet: String = ""
    var barcode: String = ""
    var codeId: String = ""             //показатель по которому можно различать типы штрих-кодов
    private var places: Int? = null

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
                        val toast = Toast.makeText(applicationContext, "Не удалось отсканировать штрихкод!", Toast.LENGTH_LONG)
                        toast.show()
                    }

                }
            }
        }
    }

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
        val textQuery =
            "SELECT " +
                    "journForBill.docno as DocNo, " +
                    "CONVERT(char(8), CAST(LEFT(journForBill.date_time_iddoc, 8) as datetime), 4) as DateDoc, " +
                    "journForBill.iddoc as Bill, " +
                    "Sector.descr as Sector, " +
                    "DocCCHead.SP3595 as Number, " +
                    "DocCCHead.SP2841 as SelfRemovel " +
                    "FROM " +
                    "DH2776 as DocCCHead (nolock) " +
                    "LEFT JOIN SC1141 as Sector (nolock) " +
                    "ON Sector.id = DocCCHead.SP2764 " +
                    "LEFT JOIN DH2763 as DocCB (nolock) " +
                    "ON DocCB.iddoc = DocCCHead.SP2771 " +
                    "LEFT JOIN DH196 as Bill (nolock) " +
                    "ON Bill.iddoc = DocCB.SP2759 " +
                    "LEFT JOIN _1sjourn as journForBill (nolock) " +
                    "ON journForBill.iddoc = Bill.iddoc " +
                    "WHERE DocCCHead.iddoc = '$docSet'"
        val dataTable = ss.executeWithRead(textQuery)
        previousAction.text = if (dataTable!![1][5].toInt() == 1) "(C) " else {
            ""
        } + dataTable[1][3].trim() + "-" +
                dataTable[1][4] + " Заявка " + dataTable[1][0] + " (" + dataTable[1][1] + ")"

        if (dataTable[1][5].toInt() == 1) DocView.text = "САМОВЫВОЗ" else DocView.text = "ДОСТАВКА"
        //тут этот код дублирую, чтобы поймать нажатие на enter после ввода колва с уже установленным принтером
        enterCountPlace.setOnKeyListener { v: View, keyCode: Int, event ->
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
                    countPlace.text = "Колво мест: $places"
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
                val scanAct = Intent(this@SetComplete, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","SetComplete")
                startActivity(scanAct)
            }
        }
    }

    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }

    private fun reactionBarcode(Barcode: String): Boolean {
        val idd: String = "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)


        if (ss.isSC(idd, "Принтеры")) {
            //получим путь принтера
            if(!ss.FPrinter.foundIDD(idd))
            {
                return false
            }
            printer.text = ss.FPrinter.path
            FExcStr.text = "Введите колво мест"
            enterCountPlace.visibility = View.VISIBLE

            return true
        } else if (!ss.isSC(idd, "Секции")) {
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
            "SELECT ID, SP3964, descr FROM SC1141 (nolock) WHERE SP1935= '$idd'"
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        reactionKey(keyCode, event)
        return super.onKeyDown(keyCode, event)
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?) {

        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4){

        }

        enterCountPlace.setOnKeyListener { v: View, keyCode: Int, event ->
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
                    countPlace.text = "Колво мест: $places"
                    countPlace.visibility = View.VISIBLE
                    FExcStr.text = "Ожидание команды"
                } catch (e: Exception) {

                }
            }
            false
        }

    }

    override fun onResume() {
        super.onResume()
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        Log.d("IntentApiSample: ", "onResume")
        if(scanRes != null){
            try {
                barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(barcode)
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
