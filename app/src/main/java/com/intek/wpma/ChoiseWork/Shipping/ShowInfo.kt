package com.intek.wpma.ChoiseWork.Shipping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.widget.TableRow
import android.widget.TextView
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.ParentForm
import com.intek.wpma.R
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefSection
import com.intek.wpma.SQL.SQL1S
import kotlinx.android.synthetic.main.activity_show_info.*


class ShowInfo : BarcodeDataReceiver() {

    var iddoc: String = ""
    var iddocControl : String = ""
    var number: String = ""
    var Barcode: String = ""
    var codeId:String = ""  //показатель по которому можно различать типы штрих-кодов
    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования

                    Barcode = intent.getStringExtra("data")
                    codeId = intent.getStringExtra("codeId")
                    reactionBarcode(Barcode)

                }
            }
        }
    }

    private fun reactionBarcode(Barcode: String) {
        val toast = Toast.makeText(applicationContext, "ШК не работают на данном экране!", Toast.LENGTH_SHORT)
        toast.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_info)
        iddoc = intent.extras!!.getString("Doc")!!
        number = intent.extras!!.getString("Number")!!
        ParentForm = intent.extras!!.getString("ParentForm")!!
        terminalView.text = SS.terminal
        title = SS.FEmployer.Name
        getControl()
        getShowInfo()

        //scroll.setOnTouchListener(@this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == 21){ //нажали влево; вернемся к документу
            val loadingAct = Intent(this, Loading::class.java)
            loadingAct.putExtra("Doc",iddoc)
            loadingAct.putExtra("Number",number)
            loadingAct.putExtra("ParentForm","ShowInfo")
            startActivity(loadingAct)
            finish()
        }

//        else if (keyCode == 20){    //вниз
//
//        }
        return super.onKeyDown(keyCode, event)
    }

    private fun getControl() {
        var textQuery ="SELECT _1SJOURN.IDDOC, _1SJOURN.IDDOCDEF FROM _1SJOURN (NOLOCK INDEX=ACDATETIME), _1SCRDOC (NOLOCK INDEX=PARENT)" +
                " WHERE _1SJOURN.DATE_TIME_IDDOC=_1SCRDOC.CHILD_DATE_TIME_IDDOC and _1SCRDOC.MDID=0 and _1SCRDOC.PARENTVAL='O1" +
                SS.ExtendID(iddoc, "Счет") +
                "' ORDER BY IDDOC"

val dataTable = SS.ExecuteWithReadNew(textQuery) ?: return

        if(dataTable.isNotEmpty()){

            for (DR in dataTable){
                if (SS.To1CName(DR["IDDOCDEF"].toString()) == "КонтрольРасходной") {

                    iddocControl = DR["IDDOC"].toString()
                }

            }
        }
    }

    private fun getShowInfo(){
        var textQuery ="SELECT IDDOC," +
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
                SS.ExtendID(iddocControl, "КонтрольРасходной") +
                "') ORDER BY IDDOC "

        textQuery = SS.QuerySetParam(textQuery, "Number", number)
        textQuery = SS.QuerySetParam(textQuery, "iddoc", iddoc)
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate())
        val dataTable = SS.ExecuteWithReadNew(textQuery) ?: return

        var cvet = Color.rgb(192,192,192)

        if(dataTable.isNotEmpty()){

            for (DR in dataTable){
                val row1 = TableRow(this)
                val number = TextView(this)

                val linearLayout = LinearLayout(this)
                val sector = RefSection()
                sector.FoundID(DR["Сектор"].toString())
                number.text = sector.Name + "-" + DR["НомерЛиста"]
                number.layoutParams = LinearLayout.LayoutParams(80,ViewGroup.LayoutParams.WRAP_CONTENT)
                number.gravity = Gravity.CENTER_HORIZONTAL
                number.textSize = 16F
                number.setTextColor(-0x1000000)

                val nmest = TextView(this)
                nmest.text = DR["Мест"]
                nmest.layoutParams = LinearLayout.LayoutParams(25, ViewGroup.LayoutParams.WRAP_CONTENT)
                nmest.gravity = Gravity.CENTER_HORIZONTAL
                nmest.textSize = 16F
                nmest.setTextColor(Color.RED)
                nmest.setBackgroundColor(Color.GREEN)

                val address = TextView(this)
                var employ = RefEmployer()
                employ.FoundID(DR["Наборщик"].toString())
                address.text = " " + SS.helper.GetShortFIO(employ.Name)
                address.layoutParams = LinearLayout.LayoutParams(165,ViewGroup.LayoutParams.WRAP_CONTENT)
                address.gravity = Gravity.LEFT
                address.textSize = 16F
                address.setTextColor(-0x1000000)

                val count = TextView(this)
                count.text = " " + SS.helper.ShortDate(DR["Дата1"].toString()) + " " +
                        SS.helper.timeToString(DR["Время1"].toString().toInt()) + " - " +
                        SS.helper.timeToString(DR["Время2"].toString().toInt())
                count.layoutParams = LinearLayout.LayoutParams(180,ViewGroup.LayoutParams.WRAP_CONTENT)
                count.gravity = Gravity.LEFT

                count.textSize = 16F
                count.setTextColor(-0x1000000)

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
                mest.text = " -" + DR["НомерЛиста"]
                mest.layoutParams = LinearLayout.LayoutParams(80,ViewGroup.LayoutParams.WRAP_CONTENT)
                mest.gravity = Gravity.CENTER
                mest.textSize = 16F
                mest.setTextColor(-0x1000000)

                val kmest = TextView(this)
                kmest.text = DR["КолМест"]
                kmest.layoutParams = LinearLayout.LayoutParams(25, ViewGroup.LayoutParams.WRAP_CONTENT)
                kmest.gravity = Gravity.CENTER_HORIZONTAL
                kmest.textSize = 16F
                kmest.setTextColor(Color.BLACK)
                kmest.setBackgroundColor(Color.GREEN)

                val code = TextView(this)
                employ.FoundID(DR["Комплектовщик"].toString())
                code.text = " " + SS.helper.GetShortFIO(employ.Name)
                code.layoutParams = LinearLayout.LayoutParams(165,ViewGroup.LayoutParams.WRAP_CONTENT)
                code.gravity = Gravity.LEFT
                code.textSize = 16F
                code.setTextColor(-0x1000000)

                val sum = TextView(this)
                sum.text = " " + SS.helper.ShortDate(DR["Дата2"].toString()) + " " + SS.helper.timeToString(DR["Время3"].toString().toInt())
                sum.layoutParams = LinearLayout.LayoutParams(150,ViewGroup.LayoutParams.WRAP_CONTENT)
                sum.gravity = Gravity.LEFT
                sum.textSize = 16F
                sum.setTextColor(-0x1000000)

                val nstrok = TextView(this)
                nstrok.text = DR["КолСтрок"] + " "
                nstrok.layoutParams = LinearLayout.LayoutParams(30,ViewGroup.LayoutParams.WRAP_CONTENT)
                nstrok.gravity = Gravity.RIGHT
                nstrok.textSize = 16F
                nstrok.setTextColor(Color.BLUE)

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

    override fun onResume() {
        super.onResume()
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        Log.d("IntentApiSample: ", "onResume")
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(barcodeDataReceiver)
        releaseScanner()
        Log.d("IntentApiSample: ", "onPause")
    }
}
