package com.intek.wpma.choiseWork.accept.transfer

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import com.intek.wpma.Global
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import com.intek.wpma.helpers.Helper
import com.intek.wpma.ref.RefEmployer
import kotlinx.android.synthetic.main.activity_transfer_yepitem.*
import kotlinx.android.synthetic.main.activity_transfer_yepitem.FExcStr

class TransferYepItem : TransferMode() {

    private var curLine : Int = 0
    private var helper = Helper()
    private var printID = ""
    private val widArr : Array<Double> = arrayOf(0.25, 0.22, 0.13, 0.13, 0.27)
    private val strArr : Array<String> = arrayOf("Инв. код", "Артикул", "К.во", "Кф", "Адрес")
    private var itemOnShelfLocal : MutableList<MutableMap<String, String>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_yepitem)
        ss.CurrentMode = Global.Mode.TransferYep
        title = ss.title
        headShelf.text = (" Разнос (ПОЛКА) "+ outputZone[0]["GateName"].toString().trim() + " " + fZone.name.trim())
        headShelf.setTextColor(Color.BLACK)

        addressOut.text = (" " + getWareHouse(outputWarehouse, "name"))
        addressPut.text = (" " + getWareHouse(inputWarehouse, "name"))

        itemOnShelfLocal.addAll(itemOnShelf)

        complete.setOnClickListener {
            transferComplete()
        }
        customTable(this, 4, strArr, widArr, headTabShelf, "head")

        refreshActivity()
    }

    @SuppressLint("UseCompatLoadingForDrawables", "ClickableViewAccessibility")
    private fun refreshActivity() {
        var lineNom = 0

        val countLocal = itemOnShelfLocal.count()
        if (countLocal != itemOnShelfLocal.count() && itemOnShelfLocal.isNotEmpty()) {
            //сменилось количество, обнулим текущую строку
            curLine = 0
        }

        if (itemOnShelfLocal.isNotEmpty()) {

            for (DR in itemOnShelfLocal) {
                lineNom++

                val bodyRow = TableRow(this)
                val linearLayout1 = LinearLayout(this)
                bodyRow.isClickable = true
                bodyRow.setOnTouchListener{ _, _ ->  //выделение строки при таче
                    var i = 0
                    while (i < itemYepOn.childCount) {
                        if (bodyRow != itemYepOn.getChildAt(i))  itemYepOn.getChildAt(i).setBackgroundColor(Color.WHITE)
                        else {
                            curLine = i
                            bodyRow.setBackgroundColor(Color.LTGRAY)
                        }
                        i++
                    }
                    true
                }
                //добавим столбцы
                val stringArr : Array<String> = arrayOf(
                    DR["InvCode"].toString().trim(),
                    DR["Article"].toString(),
                    DR["CountPackage"].toString().trim(),
                    helper.byeTheNull(DR["Coef"].toString()),
                    DR["AdressName"].toString()
                )
                val bodyVal : MutableMap<String, TextView> = HashMap()
                for (i in 0..4) bodyVal["bodyVal$i"] = TextView(this)
                var s = 0

                for ((p,_) in bodyVal) {
                    bodyVal[p]?.apply {
                        text = (" " + stringArr[s])
                        typeface = Typeface.SERIF
                        layoutParams = LinearLayout.LayoutParams(
                            (ss.widthDisplay * widArr[s]).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        background = getDrawable(R.drawable.cell_border)
                        gravity = Gravity.START
                        textSize = 18F
                        setTextColor(-0x1000000)
                    }
                    linearLayout1.background = getDrawable(R.drawable.cell_border)
                    linearLayout1.addView(bodyVal[p])
                    s++
                }

                var colorLine = Color.WHITE
                if (lineNom == curLine) {
                    colorLine = Color.LTGRAY
                }
                bodyRow.setBackgroundColor(colorLine)
                bodyRow.addView(linearLayout1)
                itemYepOn.addView(bodyRow)
            }
        }
    }

    private fun transferComplete() : Boolean {

        if (itemOnShelf.count() == 0 && itemOnPallet.count() == 0) {
            //Все пусто! это отмена разноса!
            var textQuery = "BEGIN TRAN; " +
                        "UPDATE DH\$АдресПеремещение " +
                        "SET " +
                        "\$АдресПеремещение.Склад = :EmptyID, " +
                        "\$АдресПеремещение.СкладПолучатель = :EmptyID, " +
                        "\$АдресПеремещение.ТипДокумента = 0 " +
                        "WHERE " +
                        "DH\$АдресПеремещение .iddoc = :IDDoc; " +
                        "UPDATE _1sjourn " +
                        "SET _1sjourn.\$Автор = :EmptyID WHERE _1sjourn.iddoc = :IDDoc; " +
                        "COMMIT TRAN;"
            textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "IDDoc", iddoc)
            if (!ss.executeWithoutRead(textQuery)) return false

            val backStart = Intent(this, TransferInitialize::class.java)
            startActivity(backStart)
            finish()
        }
        if (itemOnShelfLocal.count() == 0) {
            FExcStr.text = "Нет разнесенных позиций!"
            return false
        }
        if (itemOnPallet.count() != 0) {
            FExcStr.text = "Выкладка не завершена!"
            return false
        }

        //Проверим, возможно такая команда уже была послана
        var textQuery = "SELECT ID as ID " +
                "FROM \$Спр.СинхронизацияДанных (nolock INDEX=VI" + ss.getSync("Спр.СинхронизацияДанных.ДокументВход").substring(2) + ") " +
                "WHERE " +
                "\$Спр.СинхронизацияДанных.ФлагРезультата in (1,2,3) " +
                "and \$Спр.СинхронизацияДанных.ДокументВход = :IDDoc " +
                "and descr = 'AdressDistribution'"
        textQuery = ss.querySetParam(textQuery, "IDDoc", ss.extendID(iddoc, "АдресПеремещение"))
        val dataMap = ss.executeWithReadNew(textQuery) ?: return false

        //Дожидаемся ответа команды
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] = ss.extendID(iddoc, "АдресПеремещение")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(printID, "Спр.Принтеры")
        val dataMapRead: MutableMap<String, Any> = mutableMapOf()
        val fieldList : MutableList<String> = mutableListOf("Спр.СинхронизацияДанных.ДатаРез1")
        //Либо создаем новую, либо цепляемся к уже посланной
        if (dataMap.count() > 0) {
            if (dataMap[0]["ID"]?.let {
                    execCommand("AdressDistribution", dataMapWrite, fieldList, dataMapRead, it).isEmpty()
            } == true) return false
        }
        else if (execCommand("AdressDistribution", dataMapWrite, fieldList, dataMapRead).isEmpty()) return false

        //анализ результата
        if (dataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] == -3) {
            FExcStr.text = dataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString()
            return false
        }
        if (dataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] != 3) {
            FExcStr.text = "Неизвестный ответ робота... я озадачен..."
            return false
        }
        if (!completeTransferInitialize(outputWarehouse, inputWarehouse)) return false

        quitModeTransfer()
        return true
    }

    override fun reactionBarcode(Barcode: String):Boolean {
        val helper = Helper()
        val barcodeRes = helper.disassembleBarcode(Barcode)
        val typeBarcode = barcodeRes["Type"].toString()
        if (typeBarcode == "113") {
            val idd = barcodeRes["IDD"].toString()

            if (ss.isSC(idd, "Принтеры")) {
                if (!ss.FPrinter.foundIDD(idd)) {
                    return false
                }
                if (ss.FPrinter.selected) {
                    printID = ss.FPrinter.path
                    printer.text = ss.FPrinter.path
                }
                goodVoice()
                return true
            }
            if (ss.isSC(idd, "Сотрудники")) {
                ss.FEmployer = RefEmployer()
                val mainInit = Intent(this, MainActivity::class.java)
                startActivity(mainInit)
                finish()
            }
        }
        return false
    }

    override fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == 4) {
            clickVoice()
            val backAcc = Intent(this, TransferMode::class.java)
            startActivity(backAcc)
            return true
        }
        if (ss.helper.whatDirection(keyCode) == "Left") {
            clickVoice()
            val backAcc = Intent(this, TransferMode::class.java)
            startActivity(backAcc)
            return true
        }
        if (ss.helper.whatDirection(keyCode) in listOf("Down", "Up")) {
            if (itemOnShelfLocal.isNotEmpty()) reactionKeyLocal(keyCode)
            else return false
        }
        return false
    }

    private fun reactionKeyLocal(keyCode: Int) : Boolean {
        tickVoice()
        itemYepOn.getChildAt(curLine).isFocusable = false
        itemYepOn.getChildAt(curLine).setBackgroundColor(Color.WHITE)
        if (ss.helper.whatDirection(keyCode) == "Down") {
            if (curLine < itemOnShelfLocal.count()) curLine++ else curLine = 0
            if (curLine == itemOnShelfLocal.count()) curLine = 0
        } else {
            if (curLine == 0) curLine = itemOnShelfLocal.count()
            if (curLine > 0 || curLine == itemOnShelfLocal.count()) curLine--
            else curLine = itemOnShelfLocal.count()
        }
        when {
            curLine < 10 -> scrollTab.fullScroll(View.FOCUS_UP)

            curLine > itemOnShelfLocal.count() - 10 -> scrollTab.fullScroll(View.FOCUS_DOWN)

            curLine % 10 == 0 -> scrollTab.scrollTo(0, 30 * curLine - 1)
        }

        //теперь подкрасим строку серым
        itemYepOn.getChildAt(curLine).setBackgroundColor(Color.LTGRAY)
        itemYepOn.getChildAt(curLine).isActivated = false
        return true
    }
}