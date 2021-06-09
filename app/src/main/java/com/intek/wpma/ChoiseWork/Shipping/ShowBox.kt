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
import android.view.*
import android.widget.LinearLayout
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefSection
import kotlinx.android.synthetic.main.activity_show_box.*
import kotlinx.android.synthetic.main.activity_show_box.FExcStr
import kotlinx.android.synthetic.main.activity_show_box.table
import kotlinx.android.synthetic.main.activity_show_info_new_comp.*


class ShowBox : BarcodeDataReceiver() {

    var iddoc = ""
    private var adressCompl = ""
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
        setContentView(R.layout.activity_show_box)
        iddoc = intent.extras!!.getString("Doc")!!
        adressCompl = intent.extras!!.getString("AdressCompl")!!
        title = ss.FEmployer.name
        var oldx = 0F
        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
           if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x > oldx + 200) {
                    val shoiseWorkInit = Intent(this, Loading::class.java)
                    startActivity(shoiseWorkInit)
                    finish()
                }
            }
            return true
        })
        showInfoBox()
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
            badVoise()
            return
        }
    }

    fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == 4 || ss.helper.whatDirection(keyCode) == "Right") { //нажали влево; вернемся к документу
            val loadingAct = Intent(this, Loading::class.java)
            startActivity(loadingAct)
            finish()
            return true
        }

        return false
    }

    private fun showInfoBox() {
        var textQuery =
            "Select " +
                    "\$Спр.МестаПогрузки.НомерМеста as BoxeNum, " +
                    "\$Спр.МестаПогрузки.Адрес9 as AdressCompl, " +
                    "isnull(Sector.descr, 'Пу') as Sector, " +
                    "DocCC.\$КонтрольНабора.НомерЛиста as Number, " +
                    "DocCC.\$КонтрольНабора.КолМест as BoxAll " +
                    "from \$Спр.МестаПогрузки (nolock) " +
                    "INNER JOIN \$Спр.Секции as Sections (nolock) " +
                    "ON Sections.ID =  \$Спр.МестаПогрузки.Адрес9 " +
                    "LEFT JOIN DH\$КонтрольНабора as DocCC (nolock) " +
                    "ON DocCC.IDDOC =  \$Спр.МестаПогрузки.КонтрольНабора " +
                    "LEFT JOIN \$Спр.Секции as Sector (nolock) " +
                    "ON Sector.ID =  DocCC.\$КонтрольНабора.Сектор " +
                    "where " +
                    "\$Спр.МестаПогрузки.Дата6 = :EmptyDate " +
                    "and right(\$Спр.МестаПогрузки.Док , 9) = :iddoc" +
                    "and Sections.descr = :adress"
        textQuery = ss.querySetParam(textQuery, "iddoc", iddoc)
        textQuery = ss.querySetParam(textQuery, "adress", adressCompl)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        dataTable = ss.executeWithReadNew(textQuery) ?: return

        refreshActivity()
    }

    fun refreshActivity() {

        table.removeAllViewsInLayout()
        var row = TableRow(this)
        var linearLayout = LinearLayout(this)
        var number = TextView(this)
        var adress = TextView(this)
        var numCC = TextView(this)
        var allbox = TextView(this)
        numCC.text = "Сб.Л."
        numCC.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.2).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        numCC.gravity = Gravity.CENTER_HORIZONTAL
        numCC.textSize = 20F
        numCC.setTextColor(-0x1000000)

        adress.text = "Адрес"
        adress.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.4).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        adress.gravity = Gravity.CENTER_HORIZONTAL
        adress.textSize = 20F
        adress.setTextColor(-0x1000000)

        number.text = "Место"
        number.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.2).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        number.gravity = Gravity.CENTER_HORIZONTAL
        number.textSize = 20F
        number.setTextColor(-0x1000000)

        allbox.text = "ИЗ"
        allbox.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.2).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        allbox.gravity = Gravity.CENTER_HORIZONTAL
        allbox.textSize = 20F
        allbox.setTextColor(-0x1000000)

        linearLayout.setPadding(3, 3, 3, 3)
        linearLayout.addView(numCC)
        linearLayout.addView(adress)
        linearLayout.addView(number)
        linearLayout.addView(allbox)
        row.addView(linearLayout)
        table.addView(row)

        if (dataTable.isNotEmpty()) {

            for (dr in dataTable) {
                row = TableRow(this)
                linearLayout = LinearLayout(this)
                number = TextView(this)
                adress = TextView(this)
                numCC = TextView(this)
                allbox = TextView(this)
                val sector = RefSection()
                sector.foundID(dr["AdressCompl"].toString())

                numCC.text = dr["Sector"].toString().trim() + "-" + dr["Number"].toString().trim()
                numCC.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.2).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                numCC.gravity = Gravity.CENTER_HORIZONTAL
                numCC.textSize = 20F
                numCC.setTextColor(-0x1000000)

                adress.text = sector.name
                adress.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.4).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adress.gravity = Gravity.CENTER_HORIZONTAL
                adress.textSize = 20F
                adress.setTextColor(-0x1000000)

                number.text = dr["BoxeNum"].toString()
                number.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.2).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                number.gravity = Gravity.CENTER_HORIZONTAL
                number.textSize = 20F
                number.setTextColor(-0x1000000)

                allbox.text =  dr["BoxAll"].toString()
                allbox.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.2).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                allbox.gravity = Gravity.CENTER_HORIZONTAL
                allbox.textSize = 20F
                allbox.setTextColor(-0x1000000)

                linearLayout.setPadding(3, 3, 3, 3)
                linearLayout.addView(numCC)
                linearLayout.addView(adress)
                linearLayout.addView(number)
                linearLayout.addView(allbox)
                row.addView(linearLayout)
                table.addView(row)
            }
        }
    }
}
