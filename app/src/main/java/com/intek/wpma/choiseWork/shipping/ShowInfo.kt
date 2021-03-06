package com.intek.wpma.choiseWork.shipping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.helpers.Helper
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import com.intek.wpma.ref.RefEmployer
import com.intek.wpma.ref.RefSection
import kotlinx.android.synthetic.main.activity_show_info.*

class ShowInfo : BarcodeDataReceiver() {

    var iddoc: String = ""
    private var iddocControl: String = ""
    var number: String = ""
    var dataTable: MutableList<MutableMap<String, String>> = mutableListOf()

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
                    } catch (e: Exception) {
                        val toast = Toast.makeText(
                            applicationContext,
                            "Не удалось отсканировать штрихкод!",
                            Toast.LENGTH_LONG
                        )
                        toast.show()
                    }

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        Log.d("IntentApiSample: ", "onResume")
        if (scanRes != null) {
            try {
                barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(barcode)
            } catch (e: Exception) {
                val toast = Toast.makeText(
                    applicationContext,
                    "Ошибка! Возможно отсутствует соединение с базой!",
                    Toast.LENGTH_LONG
                )
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        return if (reactionKey(keyCode, event)) true else super.onKeyDown(keyCode, event)
    }

    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_info)
        iddoc = intent.extras!!.getString("Doc")!!
        number = intent.extras!!.getString("Number")!!
        title = ss.FEmployer.name
        var oldx = 0F
        FExcStr.setOnTouchListener(fun(_: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx - 200) {
                    val load = Intent(this, Loading::class.java)
                    startActivity(load)
                    finish()
                }
            }
            return true
        })
        getControl()
        getShowInfo()

        //scroll.setOnTouchListener(@this)
    }

    private fun reactionBarcode(Barcode: String) {
        val helper = Helper()
        val barcoderes = helper.disassembleBarcode(Barcode)
        val idd = barcoderes["IDD"].toString()
        if (ss.isSC(idd, "Сотрудники")) {
            ss.FEmployer = RefEmployer()
            val mainInit = Intent(this, MainActivity::class.java)
            startActivity(mainInit)
            finish()
        } else {
            FExcStr.text = "Нет действий с данным ШК в данном режиме!"
            badVoice()
            return
        }
    }

    fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == 4 || ss.helper.whatDirection(keyCode) == "Left") { //нажали влево; вернемся к документу
            val loadingAct = Intent(this, Loading::class.java)
            startActivity(loadingAct)
            finish()
            return true
        }

        return false
    }

    private fun getControl() {
        val textQuery =
            "SELECT _1SJOURN.IDDOC, _1SJOURN.IDDOCDEF FROM _1SJOURN (NOLOCK INDEX=ACDATETIME), _1SCRDOC (NOLOCK INDEX=PARENT)" +
                    " WHERE _1SJOURN.DATE_TIME_IDDOC=_1SCRDOC.CHILD_DATE_TIME_IDDOC and _1SCRDOC.MDID=0 and _1SCRDOC.PARENTVAL='O1" +
                    ss.extendID(iddoc, "Счет") +
                    "' ORDER BY IDDOC"

        val dataTable = ss.executeWithReadNew(textQuery) ?: return

        if (dataTable.isNotEmpty()) {

            for (DR in dataTable) {
                if (ss.to1CName(DR["IDDOCDEF"].toString()) == "КонтрольРасходной") {

                    iddocControl = DR["IDDOC"].toString()
                }

            }
        }
    }

    private fun getShowInfo() {
        var textQuery = "SELECT IDDOC," +
                "\$КонтрольНабора.Сектор as Сектор , " +
                "\$КонтрольНабора.Дата1 as Дата1 , " +
                "\$КонтрольНабора.Дата2 as Дата2 , " +
                "\$КонтрольНабора.Дата3 as Дата3 , " +
                "\$КонтрольНабора.Время1 as Время1 , " +
                "\$КонтрольНабора.Время2 as Время2 , " +
                "\$КонтрольНабора.Время3 as Время3 ," +
                "\$КонтрольНабора.НомерЛиста as НомерЛиста , " +
                "\$КонтрольНабора.КолМест as КолМест , " +
                "\$КонтрольНабора.Наборщик as Наборщик ," +
                "\$КонтрольНабора.Комплектовщик as Комплектовщик ," +
                "\$КонтрольНабора.КолСтрок as КолСтрок ,  " +
                "ISNULL(Places.Count,0) as Мест  FROM DH\$КонтрольНабора  (nolock) " +
                "LEFT JOIN ( SELECT Count(*) as Count, \$Спр.МестаПогрузки.КонтрольНабора as DocCC " +
                "FROM \$Спр.МестаПогрузки  (nolock) " +
                "WHERE ismark = 0 and not  \$Спр.МестаПогрузки.Дата6 = :EmptyDate " +
                "GROUP BY  \$Спр.МестаПогрузки.КонтрольНабора " +
                ") as Places ON Places.DocCC = IDDOC " +
                "WHERE IDDOC in (SELECT _1SJOURN.IDDOC FROM _1SJOURN (NOLOCK INDEX=ACDATETIME), _1SCRDOC (NOLOCK INDEX=PARENT)" +
                "WHERE _1SJOURN.ISMARK = 0 and _1SJOURN.DATE_TIME_IDDOC=_1SCRDOC.CHILD_DATE_TIME_IDDOC and _1SCRDOC.MDID=0 and _1SCRDOC.PARENTVAL='O1" +
                ss.extendID(iddocControl, "КонтрольРасходной") +
                "') ORDER BY IDDOC "

        textQuery = ss.querySetParam(textQuery, "Number", number)
        textQuery = ss.querySetParam(textQuery, "iddoc", iddoc)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        dataTable = ss.executeWithReadNew(textQuery) ?: return

        refreshActivity()
    }

    fun refreshActivity() {
        table.removeAllViewsInLayout()
        var cvet = Color.rgb(192, 192, 192)

        if (dataTable.isNotEmpty()) {

            for (DR in dataTable) {
                val row1 = TableRow(this)
                val number = TextView(this)
                val linearLayout1 = LinearLayout(this)
                val sector = RefSection()
                sector.foundID(DR["Сектор"].toString())
                number.text = sector.name
                number.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.1).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                number.gravity = Gravity.CENTER_HORIZONTAL
                number.textSize = 16F
                number.setTextColor(-0x1000000)

                val nmest = TextView(this)
                nmest.text = DR["Мест"]
                nmest.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.05).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                nmest.gravity = Gravity.CENTER_HORIZONTAL
                nmest.textSize = 16F
                nmest.setTextColor(Color.RED)
                nmest.setBackgroundColor(Color.GREEN)

                val address = TextView(this)
                val employ = RefEmployer()
                employ.foundID(DR["Наборщик"].toString())
                address.text = (" " + ss.helper.getShortFIO(employ.name))
                address.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.4).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                address.gravity = Gravity.START
                address.textSize = 16F
                address.setTextColor(-0x1000000)

                val count = TextView(this)
                count.text = (" " + ss.helper.shortDate(DR["Дата1"].toString()) + " " +
                        ss.helper.timeToString(DR["Время1"].toString().toInt()) + " - " +
                        ss.helper.timeToString(DR["Время2"].toString().toInt()))
                count.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.45).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                count.gravity = Gravity.START
                count.textSize = 16F
                count.setTextColor(-0x1000000)

                linearLayout1.setPadding(3, 3, 3, 3)
                linearLayout1.addView(number)
                linearLayout1.addView(nmest)
                linearLayout1.addView(address)
                linearLayout1.addView(count)

                row1.setBackgroundColor(cvet)
                row1.addView(linearLayout1)

                val row2 = TableRow(this)
                val linearLayout2 = LinearLayout(this)

                val mest = TextView(this)
                mest.text = (" -" + DR["НомерЛиста"])
                mest.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.1).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                mest.gravity = Gravity.CENTER
                mest.textSize = 16F
                mest.setTextColor(-0x1000000)

                val kmest = TextView(this)
                kmest.text = DR["КолМест"]
                kmest.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.05).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                kmest.gravity = Gravity.CENTER_HORIZONTAL
                kmest.textSize = 16F
                kmest.setTextColor(Color.BLACK)
                kmest.setBackgroundColor(Color.GREEN)

                val code = TextView(this)
                employ.foundID(DR["Комплектовщик"].toString())
                code.text = (" " + ss.helper.getShortFIO(employ.name))
                code.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.4).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                code.gravity = Gravity.START
                code.textSize = 16F
                code.setTextColor(-0x1000000)

                val sum = TextView(this)
                sum.text =
                    (" " + ss.helper.shortDate(DR["Дата2"].toString()) + " " + ss.helper.timeToString(
                        DR["Время3"].toString().toInt()
                    ))
                sum.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.25).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                sum.gravity = Gravity.START
                sum.textSize = 16F
                sum.setTextColor(-0x1000000)

                val nstrok = TextView(this)
                nstrok.text = (DR["КолСтрок"] + " ")
                nstrok.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.2).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                nstrok.gravity = Gravity.CENTER_HORIZONTAL
                nstrok.textSize = 16F
                nstrok.setTextColor(Color.BLUE)

                linearLayout2.setPadding(3, 3, 3, 3)
                linearLayout2.addView(mest)
                linearLayout2.addView(kmest)
                linearLayout2.addView(code)
                linearLayout2.addView(sum)
                linearLayout2.addView(nstrok)

                row2.setBackgroundColor(cvet)
                row2.addView(linearLayout2)

                table.addView(row1)
                table.addView(row2)
                cvet =
                    if (cvet == Color.rgb(192, 192, 192)) Color.WHITE else Color.rgb(192, 192, 192)
            }
        }
    }

}
