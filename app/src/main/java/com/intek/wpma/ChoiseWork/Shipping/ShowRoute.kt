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

class ShowRoute: BarcodeDataReceiver() {

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
        setContentView(R.layout.activity_show_route)
        title = ss.title
        var oldx = 0F

        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                return true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
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

        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        ccrp = ss.executeWithReadNew(textQuery) ?: return

        refreshActivity()
    }

    private fun reactionBarcode(Barcode: String): Boolean {

                    refreshActivity()
                    return true
                }

    private fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем
        if (keyCode == 4|| ss.helper.whatDirection(keyCode) == "Left") {
            FExcStr.text = "Секунду..."
            val shoiseWorkInit = Intent(this, NewComplectation::class.java)
            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        return false

    }

    private fun refreshActivity() {

        var cvet = Color.rgb(192, 192, 192)
        Shapka.text =
            """Комплектация в ${if (ss.CurrentMode == Global.Mode.NewComplectation) "тележку" else "адрес"} (новая)"""
        var row = TableRow(this)
        var linearLayout = LinearLayout(this)

        var gate = TextView(this)
        gate.text = "Вр"
        gate.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.1).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        gate.gravity = Gravity.CENTER_HORIZONTAL
        gate.textSize = 16F

        var num = TextView(this)
        num.text = "Заявка"
        num.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.25).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        num.gravity = Gravity.CENTER_HORIZONTAL
        num.textSize = 16F

        var sector = TextView(this)
        sector.text = "Лист"
        sector.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.15).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        sector.gravity = Gravity.START
        sector.textSize = 16F

        var count = TextView(this)
        count.text = "М"
        count.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.1).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        count.gravity = Gravity.START
        count.textSize = 16F

        var address = TextView(this)
        address.text = "Адрес"
        address.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.4).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        address.gravity = Gravity.START
        address.textSize = 16F


        linearLayout.setPadding(3, 3, 3, 3)
        linearLayout.addView(gate)
        linearLayout.addView(num)
        linearLayout.addView(sector)
        linearLayout.addView(count)
        linearLayout.addView(address)

        row.setBackgroundColor(Color.LTGRAY)
        row.addView(linearLayout)
        table.addView(row)

        if (ccrp.isEmpty()) return

        for (dr in ccrp) {
            linearLayout = LinearLayout(this)
            row = TableRow(this)

            gate = TextView(this)
            gate.text = dr["Gate"].toString().trim()
            gate.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.1).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gate.gravity = Gravity.CENTER_HORIZONTAL
            gate.textSize = 16F

            num = TextView(this)
            num.text = dr["Bill"]
            num.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.25).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            num.gravity = Gravity.CENTER_HORIZONTAL
            num.textSize = 16F

            sector = TextView(this)
            sector.text = dr["CC"]
            sector.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.15).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            sector.gravity = Gravity.START
            sector.textSize = 16F

            count = TextView(this)
            count.text = dr["Boxes"]
            count.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.1).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            count.gravity = Gravity.START
            count.textSize = 16F

            address = TextView(this)
            address.text = dr["Adress"]
            address.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.4).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            address.gravity = Gravity.START
            address.textSize = 16F


            linearLayout.setPadding(3, 3, 3, 3)
            linearLayout.addView(gate)
            linearLayout.addView(num)
            linearLayout.addView(sector)
            linearLayout.addView(count)
            linearLayout.addView(address)

            row.addView(linearLayout)

            table.addView(row)

        }


    }

}