package com.intek.wpma.choiseWork.shipping

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.core.view.*
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.Global
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import com.intek.wpma.ref.RefEmployer
import com.intek.wpma.ref.RefPrinter
import com.intek.wpma.ref.RefSection
import com.intek.wpma.sql.SQL1S.Const
import kotlinx.android.synthetic.main.activity_choise_down.*

class ChoiseDown : BarcodeDataReceiver() {

    private var rreviousAction = ""
    private var downSituation: MutableList<MutableMap<String, String>> = mutableListOf()
    private var refresh = false

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
                    }
                    catch (e: Exception) {
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
        onWindowFocusChanged(true)
        Log.d("IntentApiSample: ", "onResume")
        if(scanRes != null){
            try {
                barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(barcode)
            }
            catch (e: Exception){
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
        setContentView(R.layout.activity_choise_down)

        title = ss.title
        btn9.isEnabled = false
        ss.CurrentMode = Global.Mode.ChoiseDown

        btnCancel.setOnClickListener {
            if (progressBar.isVisible) return@setOnClickListener
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            startActivity(shoiseWorkInit)
            finish()
        }
        btnRefresh.setOnClickListener {
            if (progressBar.isVisible) return@setOnClickListener
            progressBar.visibility = View.VISIBLE
            FExcStr.text = "Обновляю список..."
            toModeChoiseDown()
        }
        btn1.setOnClickListener {
            if (!btn1.isEnabled || progressBar.isVisible) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 1)) badVoice() else goodVoice()
        }
        btn2.setOnClickListener {
            if (!btn2.isEnabled || progressBar.isVisible) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 2)) badVoice() else goodVoice()
        }
        btn3.setOnClickListener {
            if (!btn3.isEnabled || progressBar.isVisible) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 3)) badVoice() else goodVoice()
        }
        btn4.setOnClickListener {
            if (!btn4.isEnabled || progressBar.isVisible) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 4)) badVoice() else goodVoice()
        }
        btn5.setOnClickListener {
            if (!btn5.isEnabled || progressBar.isVisible) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 5)) badVoice() else goodVoice()
        }
        btn6.setOnClickListener {
            if (!btn6.isEnabled || progressBar.isVisible) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 6)) badVoice() else goodVoice()
        }
        btn7.setOnClickListener {
            if (!btn7.isEnabled || progressBar.isVisible) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = 7)) badVoice() else goodVoice()
        }
        btn8.setOnClickListener {
            if (!btn8.isEnabled || progressBar.isVisible) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!newComplectationGetFirstOrder()) badVoice() else goodVoice()
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
            ss.excStr = "Сотрудник не может спускать и комплектовать!"
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            startActivity(shoiseWorkInit)
            finish()
            return
        }
        if (!ss.FEmployer.canDown && ss.FEmployer.canComplectation) {
            ss.excStr = "Сотрудник не может спускать!"
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            startActivity(shoiseWorkInit)
            finish()
            return
        }
        if (!isOnline(this)) {
            ss.excStr = "Ошибка сети!"
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            startActivity(shoiseWorkInit)
            finish()
            return
        }
        val runnable = Runnable {
            //Сам запрос
            ss.excStr = ""
            var textQuery = "select * from WPM_fn_GetChoiseDown()"
            val down = ss.executeWithReadNew(textQuery)
            if (down == null){
                ss.excStr += "Не удалось выполнить запрос задания!"
                val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
                startActivity(shoiseWorkInit)
                finish()
                return@Runnable
            }
            downSituation = down
            textQuery = "select * from dbo.WPM_fn_ComplectationInfo()"
            val dt = ss.executeWithReadNew(textQuery)
            rreviousAction = if (dt == null) {
                " < error > "
            } else {
                dt[0]["pallets"].toString() + " п, " + dt[0]["box"].toString() + " м, " + dt[0]["CountEmployers"].toString() + " с"
            }
            val msg = handler.obtainMessage()
            handler.sendMessage(msg)
        }
        val thread = Thread(runnable)
        thread.start()
        return
    }

    private var handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            refreshActivity()
        }
    }

    private fun refreshActivity() {
        if (downSituation.isEmpty()) {
            FExcStr.text = "Нет заданий к спуску..."
        } else {
            FExcStr.text = "Выберите сектор спуска..."
        }
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
                    btn1.text = ("$txt1 $txt2")
                    btn1.isEnabled = allowed
                    btn1.visibility = View.VISIBLE
                    btn1.setTextColor(if (allowed) Color.BLACK else Color.LTGRAY)
                }
                1 -> {
                    btn2.text = ("$txt1 $txt2")
                    btn2.isEnabled = allowed
                    btn2.visibility = View.VISIBLE
                    btn2.setTextColor(if (allowed) Color.BLACK else Color.LTGRAY)
                }
                2 -> {
                    btn3.text = ("$txt1 $txt2")
                    btn3.isEnabled = allowed
                    btn3.visibility = View.VISIBLE
                    btn3.setTextColor(if (allowed) Color.BLACK else Color.LTGRAY)
                }
                3 -> {
                    btn4.text = ("$txt1 $txt2")
                    btn4.isEnabled = allowed
                    btn4.visibility = View.VISIBLE
                    btn4.setTextColor(if (allowed) Color.BLACK else Color.LTGRAY)
                }
                4 -> {
                    btn5.text = ("$txt1 $txt2")
                    btn5.isEnabled = allowed
                    btn5.visibility = View.VISIBLE
                    btn5.setTextColor(if (allowed) Color.BLACK else Color.LTGRAY)
                }
                5 -> {
                    btn6.text = ("$txt1 $txt2")
                    btn6.isEnabled = allowed
                    btn6.visibility = View.VISIBLE
                    btn6.setTextColor(if (allowed) Color.BLACK else Color.LTGRAY)
                }
                6 -> {
                    btn7.text = ("$txt1 $txt2")
                    btn7.isEnabled = allowed
                    btn7.visibility = View.VISIBLE
                    btn7.setTextColor(if (allowed) Color.BLACK else Color.LTGRAY)
                }
            }

        }

        btn8.text = ("8. КМ:$rreviousAction")
        btn8.isEnabled = ss.FEmployer.canComplectation
        progressBar.visibility = View.INVISIBLE

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
        if (progressBar.isVisible) return true
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
                badVoice()
                return false
            }
        } else {
            FExcStr.text = "Нет действий с данным ШК в данном режиме!"
            badVoice()
            return false
        }

        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        if (progressBar.isVisible) return true
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
            progressBar.visibility = View.VISIBLE
            FExcStr.text = "Обновляю список..."
            toModeChoiseDown()
            return true
        }
        val choise = ss.helper.whatInt(keyCode)
        if (choise in 1..7) {
            FExcStr.text = "Получаю задание..."
            if (!choiseDownComplete(ChoiseLine = choise)) badVoice()
            return true
        } else if (choise == 8 && ss.FEmployer.canComplectation) {
            FExcStr.text = "Получаю задание..."
            if (!newComplectationGetFirstOrder()) badVoice()
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
