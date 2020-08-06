package com.intek.wpma.ChoiseWork.Shipping

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
import com.intek.wpma.*
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefSection
import kotlinx.android.synthetic.main.activity_new_complectation.*
import kotlinx.android.synthetic.main.activity_show_info.*
import kotlinx.android.synthetic.main.activity_show_info_new_comp.*
import kotlinx.android.synthetic.main.activity_show_info_new_comp.FExcStr
import kotlinx.android.synthetic.main.activity_show_info_new_comp.table

class ShowInfoNewComp: BarcodeDataReceiver() {

    var CCRP: MutableList<MutableMap<String, String>> = mutableListOf()
    var BadDoc: MutableMap<String, String> = mutableMapOf()

    //region шапка с необходимыми функциями для работы сканеров перехватчиков кнопок и т.д.
    var Barcode: String = ""
    var codeId: String = ""             //показатель по которому можно различать типы штрих-кодов
    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования
                    try {
                        Barcode = intent.getStringExtra("data")
                        reactionBarcode(Barcode)
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
                Barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(Barcode)
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

        return if (ReactionKey(keyCode, event)) true else super.onKeyDown(keyCode, event)
    }

    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_info_new_comp)
        title = SS.title
        BadDoc.put("ID",intent.extras!!.getString("BadDocID")!!)
        BadDoc.put("View",intent.extras!!.getString("BadDocView")!!)

        RefreshRoute()


    }

    fun RefreshRoute() {
        var textQuery =
            "select " +
                    "right(min(journForBill.docno), 5) as Bill, " +
                    "rtrim(min(isnull(Sections.descr, 'Пу'))) + '-' + cast(min(DocCC.\$КонтрольНабора.НомерЛиста ) as char) as CC, " +
                    "max(AllTab.CountAllBox) as Boxes, " +
                    "rtrim(max(RefAdress9.descr)) as Adress, " +
                    "max(Gate.descr) as Gate " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                    "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                    "left join \$Спр.Секции as Sections (nolock) " +
                    "on Sections.id = DocCC.\$КонтрольНабора.Сектор " +
                    "inner join DH\$КонтрольРасходной as DocCB (nolock) " +
                    "on DocCB .iddoc = DocCC.\$КонтрольНабора.ДокументОснование " +
                    "inner JOIN DH\$Счет as Bill (nolock) " +
                    "on Bill.iddoc = DocCB.\$КонтрольРасходной.ДокументОснование " +
                    "inner join _1sjourn as journForBill (nolock) " +
                    "on journForBill.iddoc = Bill.iddoc " +
                    "left join \$Спр.Секции as RefAdress9 (nolock) " +
                    "on RefAdress9.id = dbo.WMP_fn_GetAdressComplete(Ref.id) " +
                    "left join \$Спр.Ворота as Gate (nolock) " +
                    "on Gate.id = DocCB.\$КонтрольРасходной.Ворота " +
                    "inner join ( " +
                    "select " +
                    "DocCC.iddoc as iddoc, " +
                    "count(*) as CountAllBox " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                    "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                    "where " +
                    "Ref.ismark = 0 " +
                    "and Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                    "and Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate " +
                    "group by DocCC.iddoc ) as AllTab " +
                    "on AllTab.iddoc = DocCC.iddoc " +
                    "where " +
                    "Ref.ismark = 0 " +
                    "and Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate " +
                    "and Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate " +
                    "group by DocCC.iddoc"

        textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID);
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate());
        textQuery = "select * from WPM_fn_ViewBillStatus(:iddoc)"
        textQuery = SS.QuerySetParam(textQuery, "iddoc", BadDoc["ID"].toString())
        textQuery =  SS.QuerySetParam(textQuery, "View", BadDoc["Veiw"].toString())
        CCRP = SS.ExecuteWithReadNew(textQuery) ?: return

        var oldx : Float = 0F
        var cvet = Color.rgb(192,192,192)

        Shapka.text = "Комплектация в тележку (новая)" + "\n" + BadDoc["View"].toString() //SS.QueryParser("lblDocInfo")

        FExcStr.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x > oldx) {
                    val shoiseWorkInit = Intent(this, NewComplectation::class.java)
                    shoiseWorkInit.putExtra("ParentForm", "ShowInfoNewComp")
                    startActivity(shoiseWorkInit)
                    finish()
                }
            }
            true
        }

        if(CCRP.isNotEmpty()){

            for (DR in CCRP){

                val row1 = TableRow(this)
                val number = TextView(this)
                val linearLayout1 = LinearLayout(this)

                number.text = DR["sector"]
                number.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.1).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                number.gravity = Gravity.CENTER_HORIZONTAL
                number.textSize = 16F

                val nmest = TextView(this)
                nmest.text = DR["cond"]
                nmest.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.05).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                nmest.gravity = Gravity.CENTER_HORIZONTAL
                nmest.textSize = 16F
                nmest.setBackgroundColor(Color.GREEN)

                val address = TextView(this)
                address.text = " " + SS.helper.GetShortFIO(DR["employer"].toString())
                address.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.4).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                address.gravity = Gravity.LEFT
                address.textSize = 16F

                val count = TextView(this)
                count.text = DR["adress"]
                if (DR["adress"] == "null") count.text = ""
                count.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.45).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                count.gravity = Gravity.LEFT
                count.textSize = 16F

                linearLayout1.setPadding(3,3,3,3)
                linearLayout1.addView(number)
                linearLayout1.addView(nmest)
                linearLayout1.addView(address)
                linearLayout1.addView(count)

                row1.setBackgroundColor(cvet)
                row1.addView(linearLayout1)

                val row2 = TableRow(this)
                val linearLayout2 = LinearLayout(this)

                val mest = TextView(this)
                mest.text = " -" + DR["number"]
                mest.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.1).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                mest.gravity = Gravity.CENTER
                mest.textSize = 16F

                val kmest = TextView(this)
                kmest.text = " "
                kmest.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.05).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                kmest.gravity = Gravity.CENTER_HORIZONTAL
                kmest.textSize = 16F

                val code = TextView(this)
                code.text =  " " +
                        if (SS.IsVoidDate((DR["date2"].toString())) == true) SS.helper.ShortDate(DR["date1"].toString())
                        else {SS.helper.ShortDate(DR["date2"].toString())} +
                        " " + SS.helper.timeToString(DR["time1"].toString().toInt()) +
                        " - " + if (DR["time2"] != "0") SS.helper.timeToString(DR["time2"].toString().toInt()) else "..."
                code.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.4).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                code.gravity = Gravity.LEFT
                code.textSize = 16F

                val sum = TextView(this)
                sum.text = " _ _ "
                sum.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.25).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                sum.gravity = Gravity.CENTER_HORIZONTAL
                sum.textSize = 16F

                val nstrok = TextView(this)
                nstrok.text =  DR["boxes"] + " "
                nstrok.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.2).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                nstrok.gravity = Gravity.CENTER_HORIZONTAL
                nstrok.textSize = 16F

                if (DR["iddoc"] == BadDoc["ID"]) {
                    number.setBackgroundColor(Color.rgb(128,128,128))
                    mest.setBackgroundColor(Color.rgb(128,128,128))
                }

                if (DR["time2"] == "0")  {
                    number.setTextColor(Color.RED)
                    nmest.setTextColor(Color.RED)
                    address.setTextColor(Color.RED)
                    count.setTextColor(Color.RED)
                    mest.setTextColor(Color.RED)
                    kmest.setTextColor(Color.RED)
                    code.setTextColor(Color.RED)
                    sum.setTextColor(Color.RED)
                    nstrok.setTextColor(Color.RED)
                }
                else {
                    number.setTextColor(Color.BLACK)
                    nmest.setTextColor(Color.BLACK)
                    address.setTextColor(Color.BLACK)
                    count.setTextColor(Color.BLACK)
                    mest.setTextColor(Color.BLACK)
                    kmest.setTextColor(Color.BLACK)
                    code.setTextColor(Color.BLACK)
                    sum.setTextColor(Color.BLACK)
                    nstrok.setTextColor(Color.BLUE)
                }

                linearLayout2.setPadding(3,3,3,3)
                linearLayout2.addView(mest)
                linearLayout2.addView(kmest)
                linearLayout2.addView(code)
                linearLayout2.addView(sum)
                linearLayout2.addView(nstrok)

                row2.setBackgroundColor(cvet)
                row2.addView(linearLayout2)

                table.addView(row1)
                table.addView(row2)

                if (cvet == Color.rgb(192,192,192)) cvet = Color.WHITE else cvet = Color.rgb(192,192,192)
            }
        }
        return
    }

    private fun reactionBarcode(Barcode: String): Boolean {

                    RefreshActivity()
                    return true
                }

    private fun ReactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем
        if (keyCode == 4|| SS.helper.WhatDirection(keyCode) == "Right") {
            val shoiseWorkInit = Intent(this, NewComplectation::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ShowInfoNewComp")
            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        return false

    }

    fun RefreshActivity() {

    }

}