package com.intek.wpma.choiseWork.set

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.R
import kotlinx.android.synthetic.main.activity_watch_table_part.*

class WatchTablePart : BarcodeDataReceiver() {

    var iddoc: String = ""
    private var addressID: String = ""
    private var invCode: String = ""
    //при принятии маркировок, чтобы не сбились уже отсканированные QR-коды
    private var countFact: Int = 0

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
                    barcode = intent.getStringExtra("data")!!
                    codeId = intent.getStringExtra("codeId")!!
                    reactionBarcode(barcode)
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
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(barcodeDataReceiver)
        releaseScanner()
        Log.d("IntentApiSample: ", "onPause")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        return if (reactionKey(keyCode, event)) true else super.onKeyDown(keyCode, event)
    }
    //endregion

    private fun reactionBarcode(Barcode: String) {
        FExcStr.text = "ШК не работают на данном экране!"
        badVoice()
     }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_table_part)

        iddoc = intent.extras!!.getString("iddoc")!!
        addressID = intent.extras!!.getString("addressID")!!
        invCode = intent.extras!!.getString("ItemCode")!!
        PreviousAction.text = intent.extras!!.getString("DocView")!!
        countFact = intent.extras!!.getString("CountFact")!!.toInt()
        title = ss.title

        var oldx = 0F                      //для свайпа, чтобы посмотреть накладную
        FExcStr.setOnTouchListener(fun(_: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x > oldx) {
                    FExcStr.text = "Подгружаю список..."

                    if (!isOnline(this)) {                                      //проверим интернет-соединение
                        FExcStr.text = ("Ошибка доступа. Проблема интернет-соединения!")
                        return false
                    }

                    //перейдем на форму просмотра
                    val setInitialization = Intent(this, SetInitialization::class.java)
                    setInitialization.putExtra("DocSetID", iddoc)
                    setInitialization.putExtra("AddressID", addressID)
                    setInitialization.putExtra("PreviousAction", PreviousAction.text.toString())
                    setInitialization.putExtra("CountFact", countFact.toString())
                    setInitialization.putExtra("ParentForm", "WatchTablePart")
                    startActivity(setInitialization)
                    finish()
                }
            }
            return true
        })

        //строка с шапкой
        val rowTitle = TableRow(this)
        val linearLayout = LinearLayout(this)

        //добавим столбцы
        val number = TextView(this)
        number.text = "№"
        number.typeface = Typeface.SERIF
        number.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.09).toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
        number.gravity = Gravity.CENTER
        number.textSize = 16F
        number.setTextColor(-0x1000000)
        val address = TextView(this)
        address.text = "Адрес"
        address.typeface = Typeface.SERIF
        address.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.27).toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
        address.textSize = 16F
        address.setTextColor(-0x1000000)
        val code = TextView(this)
        code.text = "Инв.код"
        code.typeface = Typeface.SERIF
        code.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.29).toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
        code.gravity = Gravity.CENTER
        code.textSize = 16F
        code.setTextColor(-0x1000000)
        val count = TextView(this)
        count.text = "Кол."
        count.typeface = Typeface.SERIF
        count.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.1).toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
        count.gravity = Gravity.CENTER
        count.textSize = 16F
        count.setTextColor(-0x1000000)
        val sum = TextView(this)
        sum.text = "Сумма"
        sum.typeface = Typeface.SERIF
        sum.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.25).toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
        sum.gravity = Gravity.CENTER
        sum.textSize = 16F
        sum.setTextColor(-0x1000000)

        linearLayout.addView(number)
        linearLayout.addView(address)
        linearLayout.addView(code)
        linearLayout.addView(count)
        linearLayout.addView(sum)

        rowTitle.addView(linearLayout)
        table.addView(rowTitle)
        getTablePart(iddoc)
    }

    fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == 21 || keyCode == 4){ //нажали влево; вернемся к документу
            val setInitialization = Intent(this, SetInitialization::class.java)
            setInitialization.putExtra("DocSetID",iddoc)
            setInitialization.putExtra("AddressID",addressID)
            setInitialization.putExtra("PreviousAction",PreviousAction.text.toString())
            setInitialization.putExtra("CountFact",countFact.toString())
            setInitialization.putExtra("ParentForm","WatchTablePart")
            startActivity(setInitialization)
            finish()
            return true
        }
        return false
    }

    private fun getTablePart(iddoc: String): Boolean{
        var textQuery =
            "select " +
                    "DocCC.lineno_ as Number, " +
                    "Sections.descr as Adress, " +
                    "Goods.SP1036 as InvCode, " +
                    "DocCC.SP3110 as Count, " +
                    "DocCC.SP3114 as Sum, " +
                    "DocCCHead.SP3114 as totalSum " +
            "from " +
                    "DT2776 as DocCC (nolock) " +
                    "LEFT JOIN DH2776 as DocCCHead (nolock) ON DocCCHead.iddoc = DocCC.iddoc " +
                    "LEFT JOIN SC33 as Goods (nolock) ON Goods.id = DocCC.SP3109 " +
                    "LEFT JOIN SC1141 as Sections (nolock) ON Sections.id = DocCC.SP5508 " +
            "where " +
                    "DocCC.iddoc = :iddoc " +
                    "and DocCC.SP5986 = :EmptyDate " +
                    "and DocCC.SP3116 = 0 " +
                    "and DocCC.SP3110 > 0 "+
            "order by " +
                    "DocCCHead.SP2764 , Sections.SP5103 , Number"
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "iddoc", iddoc)
        textQuery = ss.querySetParam(textQuery, "addressID", addressID)
        val dataTable = ss.executeWithRead(textQuery) ?: return false

        if(dataTable.isNotEmpty()){

            for (i in 1 until dataTable.size){
                val row = TableRow(this)
                val number = TextView(this)
                val linearLayout = LinearLayout(this)
                number.text = dataTable[i][0]
                number.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.09).toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
                number.gravity = Gravity.CENTER
                number.textSize = 16F
                number.setTextColor(-0x1000000)
                val address = TextView(this)
                address.text = dataTable[i][1]
                address.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.27).toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
                address.textSize = 16F
                address.setTextColor(-0x1000000)
                val code = TextView(this)
                code.text = dataTable[i][2]
                code.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.29).toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
                code.gravity = Gravity.CENTER
                code.textSize = 16F
                code.setTextColor(-0x1000000)
                val count = TextView(this)
                count.text = dataTable[i][3]
                count.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.1).toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
                count.gravity = Gravity.CENTER
                count.textSize = 16F
                count.setTextColor(-0x1000000)
                val sum = TextView(this)
                sum.text = dataTable[i][4]
                sum.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.25).toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
                sum.gravity = Gravity.CENTER
                sum.textSize = 16F
                sum.setTextColor(-0x1000000)

                Price.text = ("Сумма : " + dataTable[1][5])

                linearLayout.addView(number)
                linearLayout.addView(address)
                linearLayout.addView(code)
                linearLayout.addView(count)
                linearLayout.addView(sum)

                row.addView(linearLayout)
                if (dataTable[i][2] == invCode){
                    row.setBackgroundColor(Color.YELLOW)
                }
                table.addView(row)
            }
        }
        return true
    }
}
