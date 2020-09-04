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
import kotlinx.android.synthetic.main.activity_show_info_new_comp.*
import kotlinx.android.synthetic.main.activity_show_info_new_comp.FExcStr
import kotlinx.android.synthetic.main.activity_show_info_new_comp.table

class ShowInfoNewComp: BarcodeDataReceiver() {

    var ccrp: MutableList<MutableMap<String, String>> = mutableListOf()
    var badDoc: MutableMap<String, String> = mutableMapOf()

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
                        barcode = intent.getStringExtra("data")
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
        setContentView(R.layout.activity_show_info_new_comp)
        title = ss.title
        badDoc["ID"] = intent.extras!!.getString("BadDocID")!!
        badDoc["View"] = intent.extras!!.getString("BadDocView")!!
        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            var oldx = 0F
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                return true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x > oldx) {
                    val shoiseWorkInit = Intent(this, NewComplectation::class.java)
                    startActivity(shoiseWorkInit)
                    finish()
                }
            }
            return true
        })
        refreshRoute()
    }

    private fun refreshRoute() {

        var textQuery = "select * from WPM_fn_ViewBillStatus(:iddoc)"
        textQuery = ss.querySetParam(textQuery, "iddoc", badDoc["ID"].toString())
        textQuery =  ss.querySetParam(textQuery, "View", badDoc["Veiw"].toString())
        ccrp = ss.executeWithReadNew(textQuery) ?: return
        refreshActivity()
    }

    private fun reactionBarcode(Barcode: String): Boolean {

        refreshActivity()
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем
        if (keyCode == 4 || ss.helper.whatDirection(keyCode) == "Right") {
            FExcStr.text = "Секунду..."
            var shoiseWorkInit = Intent(this, NewComplectation::class.java)
            if (ss.CurrentMode == Global.Mode.FreeDownComplete) {
                shoiseWorkInit = Intent(this, FreeComplectation::class.java)
            }
            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        return false

    }

    private fun refreshActivity() {

        Shapka.text =
            "Комплектация в " + (if (ss.CurrentMode == Global.Mode.NewComplectation) "тележку" else "адрес") + " (новая)" + "\n" + badDoc["View"].toString() //SS.QueryParser("lblDocInfo")
        if (ccrp.isNotEmpty()) {

            for (DR in ccrp) {

                val row1 = TableRow(this)
                val number = TextView(this)
                val linearLayout1 = LinearLayout(this)
                val colorRow = when(DR["cond"]) {
                    "H" -> Color.LTGRAY
                    "C" -> Color.WHITE
                    "K" -> Color.YELLOW
                    else -> Color.WHITE
                }
                number.text = DR["sector"]
                number.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.1).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                number.gravity = Gravity.CENTER_HORIZONTAL
                number.textSize = 16F

                val nmest = TextView(this)
                nmest.text = DR["cond"]
                nmest.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.05).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                nmest.gravity = Gravity.CENTER_HORIZONTAL
                nmest.textSize = 16F
                nmest.setBackgroundColor(Color.GREEN)

                val address = TextView(this)
                address.text = " " + ss.helper.getShortFIO(DR["employer"].toString())
                address.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.4).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                address.gravity = Gravity.START
                address.textSize = 16F

                val count = TextView(this)
                count.text = DR["adress"]
                if (DR["adress"] == "null") count.text = ""
                count.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.45).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                count.gravity = Gravity.START
                count.textSize = 16F

                linearLayout1.setPadding(3, 3, 3, 3)
                linearLayout1.addView(number)
                linearLayout1.addView(nmest)
                linearLayout1.addView(address)
                linearLayout1.addView(count)

                row1.setBackgroundColor(colorRow)
                row1.addView(linearLayout1)

                val row2 = TableRow(this)
                val linearLayout2 = LinearLayout(this)

                val mest = TextView(this)
                mest.text = " -" + DR["number"]
                mest.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.1).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                mest.gravity = Gravity.CENTER
                mest.textSize = 16F

                val kmest = TextView(this)
                kmest.text = " "
                kmest.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.05).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                kmest.gravity = Gravity.CENTER_HORIZONTAL
                kmest.textSize = 16F

                val code = TextView(this)
                code.text = " " +
                        if (ss.isVoidDate((DR["date2"].toString()))) ss.helper.shortDate(DR["date1"].toString())
                        else {
                            ss.helper.shortDate(DR["date2"].toString())
                        } +
                        " " + ss.helper.timeToString(DR["time1"].toString().toInt()) +
                        " - " + if (DR["time2"] != "0") ss.helper.timeToString(
                    DR["time2"].toString().toInt()
                ) else "..."
                code.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.4).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                code.gravity = Gravity.START
                code.textSize = 16F

                val sum = TextView(this)
                sum.text = " _ _ "
                sum.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.25).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                sum.gravity = Gravity.CENTER_HORIZONTAL
                sum.textSize = 16F

                val nstrok = TextView(this)
                nstrok.text = DR["boxes"] + " "
                nstrok.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.2).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                nstrok.gravity = Gravity.CENTER_HORIZONTAL
                nstrok.textSize = 16F

                if (DR["iddoc"] == badDoc["ID"]) {
                    number.setBackgroundColor(Color.rgb(128, 128, 128))
                    mest.setBackgroundColor(Color.rgb(128, 128, 128))
                }

                if (DR["time2"] == "0") {
                    number.setTextColor(Color.RED)
                    nmest.setTextColor(Color.RED)
                    address.setTextColor(Color.RED)
                    count.setTextColor(Color.RED)
                    mest.setTextColor(Color.RED)
                    kmest.setTextColor(Color.RED)
                    code.setTextColor(Color.RED)
                    sum.setTextColor(Color.RED)
                    nstrok.setTextColor(Color.RED)
                } else {
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

                linearLayout2.setPadding(3, 3, 3, 3)
                linearLayout2.addView(mest)
                linearLayout2.addView(kmest)
                linearLayout2.addView(code)
                linearLayout2.addView(sum)
                linearLayout2.addView(nstrok)

                row2.setBackgroundColor(colorRow)
                row2.addView(linearLayout2)

                table.addView(row1)
                table.addView(row2)
            }
        }
    }

}