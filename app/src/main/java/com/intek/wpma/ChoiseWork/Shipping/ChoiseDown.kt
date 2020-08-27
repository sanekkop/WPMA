package com.intek.wpma.ChoiseWork.Shipping


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.Global
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefPrinter
import com.intek.wpma.Ref.RefSection
import com.intek.wpma.SQL.SQL1S.Const
import kotlinx.android.synthetic.main.activity_choise_down.*


class ChoiseDown : BarcodeDataReceiver() {

    private var rreviousAction = ""
    private var downSituation: MutableList<MutableMap<String, String>> = mutableListOf()
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
                    }
                    catch(e: Exception) {
                        val toast = Toast.makeText(applicationContext, "Не удалось отсканировать штрихкод!", Toast.LENGTH_LONG)
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
        if(scanRes != null){
            try {
                barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(barcode)
            }
            catch (e: Exception){
                val toast = Toast.makeText(applicationContext, "Ошибка! Возможно отсутствует соединение с базой!", Toast.LENGTH_LONG)
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
        setContentView(R.layout.activity_choise_down)

        title = ss.title
        btn9.isEnabled = false
        ss.CurrentMode = Global.Mode.ChoiseDown

        btnCancel.setOnClickListener {
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseWorkInit)
            finish()
        }
        btnRefresh.setOnClickListener {
            toModeChoiseDown()
        }
        btn1.setOnClickListener {
            if (!btn1.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 1)) badVoise() else goodVoise()
        }
        btn2.setOnClickListener {
            if (!btn2.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 2)) badVoise() else goodVoise()
        }
        btn3.setOnClickListener {
            if (!btn3.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 3)) badVoise() else goodVoise()
        }
        btn4.setOnClickListener {
            if (!btn4.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 4)) badVoise() else goodVoise()
        }
        btn5.setOnClickListener {
            if (!btn5.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 5)) badVoise() else goodVoise()
        }
        btn6.setOnClickListener {
            if (!btn6.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 6)) badVoise() else goodVoise()
        }
        btn7.setOnClickListener {
            if (!btn7.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 7)) badVoise() else goodVoise()
        }
        btn8.setOnClickListener {
            if (!btn8.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!newComplectationGetFirstOrder()) badVoise() else goodVoise()
        }
        btn9.setOnClickListener {
            //это резерв не работает
            /*
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseWorkInit)
            finish()

            */
        }
        toModeChoiseDown()
    }

    private fun toModeChoiseDown() {
        Const.refresh()
        ss.FEmployer.refresh()     // проверим не изменились ли галки на спуск/комплектацию
        //не может спускать и комплектовать выходим обратно
        if (!ss.FEmployer.canDown && !ss.FEmployer.canComplectation) {
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseWorkInit)
            finish()
        }
        if (!ss.FEmployer.canDown && ss.FEmployer.canComplectation) {
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseWorkInit)
            finish()
        }
        //Сам запрос
        var textQuery = "select * from WPM_fn_GetChoiseDown()"
        val down = ss.executeWithReadNew(textQuery)
        if (down == null){
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseWorkInit)
            finish()
            return
        }
        downSituation = down
        textQuery = "select * from dbo.WPM_fn_ComplectationInfo()"
        val dt = ss.executeWithReadNew(textQuery)
        rreviousAction = if (dt == null) {
            " < error > "
        } else {
            dt[0]["pallets"].toString() + " п, " + dt[0]["box"].toString() + " м, " + dt[0]["CountEmployers"].toString() + " с"
        }

        if (downSituation.isEmpty()) {
            FExcStr.text = "Нет заданий к спуску..."
        } else {
            FExcStr.text = "Выберите сектор спуска..."
        }
        refreshActivity()
        return
    }

    private fun refreshActivity() {
        ss.excStr = "Спуск выбор (" + (if (ss.Const.CarsCount == "0") "нет ограничений" else ss.Const.CarsCount + " авто") + ")"
        lblState.text = ss.excStr

        //сделаем все кнопки пока не кликабельным
        btn1.isEnabled = false
        btn2.isEnabled = false
        btn3.isEnabled = false
        btn4.isEnabled = false
        btn5.isEnabled = false
        btn6.isEnabled = false
        btn7.isEnabled = false
        btn8.isEnabled = false
        btn1.visibility = View.INVISIBLE
        btn2.visibility = View.INVISIBLE
        btn3.visibility = View.INVISIBLE
        btn4.visibility = View.INVISIBLE
        btn5.visibility = View.INVISIBLE
        btn6.visibility = View.INVISIBLE
        btn7.visibility = View.INVISIBLE
        for (i in 0..6) {
            if (downSituation.count() <= i) {
                break
            }
            val txt1: String = (i + 1).toString() + ". " + downSituation[i]["Sector"].toString()
                .trim() + " - " + downSituation[i]["CountBox"].toString()
            val txt2: String =
                " мест " + downSituation[i]["CountEmployers"].toString().trim() + " сотр."
            val allowed: Boolean = downSituation[i]["Allowed"].toString() == "1"
            //айдем нужную кнопку
            when (i) {
                0 -> {
                    btn1.text = "$txt1 $txt2"
                    btn1.isEnabled = allowed
                    btn1.visibility = View.VISIBLE

                }
                1 -> {
                    btn2.text = "$txt1 $txt2"
                    btn2.isEnabled = allowed
                    btn2.visibility = View.VISIBLE
                }
                2 -> {
                    btn3.text = "$txt1 $txt2"
                    btn3.isEnabled = allowed
                    btn3.visibility = View.VISIBLE
                }
                3 -> {
                    btn4.text = "$txt1 $txt2"
                    btn4.isEnabled = allowed
                    btn4.visibility = View.VISIBLE
                }
                4 -> {
                    btn5.text = "$txt1 $txt2"
                    btn5.isEnabled = allowed
                    btn5.visibility = View.VISIBLE
                }
                5 -> {
                    btn6.text = "$txt1 $txt2"
                    btn6.isEnabled = allowed
                    btn6.visibility = View.VISIBLE
                }
                6 -> {
                    btn7.text = "$txt1 $txt2"
                    btn7.isEnabled = allowed
                    btn7.visibility = View.VISIBLE
                }
            }

        }

        btn8.text = "8. КМ:$rreviousAction"
        btn8.isEnabled = ss.FEmployer.canComplectation
    }

    private fun choiseDownComplete(ChoiseLine: Int): Boolean {

        if (downSituation.count() < ChoiseLine) {
            ss.excStr = "$ChoiseLine - нет в списке!"
            FExcStr.text =  ss.excStr
            return false
        }
        if (downSituation[ChoiseLine - 1]["Allowed"] == "0") {
            ss.excStr = "Пока нельзя! Рано!"
            FExcStr.text =  ss.excStr
            return false
        }
        ss.FEmployer.refresh()
        val sectorPriory = RefSection()
        if (ss.FEmployer.getAttribute("ПосланныйСектор").toString() != ss.getVoidID()) {
            if (sectorPriory.foundID(ss.FEmployer.getAttribute("ПосланныйСектор").toString())) {
                if (downSituation[ChoiseLine - 1]["Sector"].toString()
                        .trim() != sectorPriory.name.trim()
                ) {
                    ss.excStr = "Нельзя! Можно только " + sectorPriory.name.trim() + " сектор!"
                    FExcStr.text = ss.excStr
                    return false
                }
            }
        }
        return choiseDownComplete(downSituation[ChoiseLine - 1]["Sector"].toString().trim())
    }

    private fun choiseDownComplete(CurrParent: String): Boolean {
        var textQuery =
            "declare @res int " +
                    "begin tran " +
                    "exec WPM_GetOrderDown :Employer, :NameParent, @res output " +
                    "if @res = 0 rollback tran else commit tran " +
                    ""
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        textQuery = ss.querySetParam(textQuery, "NameParent", CurrParent)

        if (!ss.executeWithoutRead(textQuery)) {
            return false
        }
        return toModeDown()
    }

    private fun toModeDown(): Boolean {
        val downingInit = Intent(this, Downing::class.java)
        downingInit.putExtra("ParentForm", "ChoiseDown")
        startActivity(downingInit)
        finish()
        return true
    }

    private fun newComplectationGetFirstOrder(): Boolean {
        val complectationInit = Intent(this, NewComplectation::class.java)
        complectationInit.putExtra("ParentForm", "ChoiseDown")
        startActivity(complectationInit)
        finish()
        return true
    }

    private fun reactionBarcode(Barcode: String): Boolean {

        val barcoderes = ss.helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "113") {
            //справочники типовые
            val idd = barcoderes["IDD"].toString()
            if (ss.isSC(idd, "Сотрудники")) {
                ss.FEmployer = RefEmployer()
                val mainInit = Intent(this, MainActivity::class.java)
                mainInit.putExtra("ParentForm", "ChoiseDown")
                startActivity(mainInit)
                finish()
            } else if (ss.isSC(idd, "Принтеры")) {
                if (ss.FPrinter.selected) {
                    ss.FPrinter = RefPrinter()
                } else ss.FPrinter.foundIDD(idd)
            } else {
                FExcStr.text = "Нет действий с данным ШК в данном режиме!"
                badVoise()
                return false
            }
        } else {
            FExcStr.text = "Нет действий с данным ШК в данном режиме!"
            badVoise()
            return false
        }

        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4) {
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        //Если Нажали DEL
        else if (keyCode == 67) {
            toModeChoiseDown()
            return true
        }
        val choise = ss.helper.whatInt(keyCode)
        if (choise in 1..7) {
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = choise)) badVoise()
            return true
        } else if (choise == 8 && ss.FEmployer.canComplectation) {
            FExcStr.text = "Получаю задание..."
            if (!newComplectationGetFirstOrder()) badVoise()
            return true

        } else if (choise == 0) {
            //0 - отмена
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        return false
    }

}
