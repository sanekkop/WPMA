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
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefPrinter
import com.intek.wpma.Ref.RefSection
import com.intek.wpma.SQL.SQL1S.Const
import kotlinx.android.synthetic.main.activity_choise_down.*
import kotlinx.android.synthetic.main.activity_choise_down.FExcStr
import kotlinx.android.synthetic.main.activity_choise_down.terminalView
import kotlinx.android.synthetic.main.activity_unloading.*


class ChoiseDown : BarcodeDataReceiver() {

    var PreviousAction = ""
    var DownSituation: MutableList<MutableMap<String, String>> = mutableListOf()
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
                Barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(Barcode)
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

        return if (ReactionKey(keyCode, event)) true else super.onKeyDown(keyCode, event)
    }
    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choise_down)

        terminalView.text = SS.terminal
        title = SS.title
        btn9.isEnabled = false

        btnCancel.setOnClickListener {
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseWorkInit)
            finish()
        }
        btnRefresh.setOnClickListener {
            ToModeChoiseDown()
        }
        btn1.setOnClickListener {
            if (!btn1.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!ChoiseDownComplete(ChoiseLine = 1)) BadVoise() else GoodVoise()
        }
        btn2.setOnClickListener {
            if (!btn2.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!ChoiseDownComplete(ChoiseLine = 2)) BadVoise() else GoodVoise()
        }
        btn3.setOnClickListener {
            if (!btn3.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!ChoiseDownComplete(ChoiseLine = 3)) BadVoise() else GoodVoise()
        }
        btn4.setOnClickListener {
            if (!btn4.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!ChoiseDownComplete(ChoiseLine = 4)) BadVoise() else GoodVoise()
        }
        btn5.setOnClickListener {
            if (!btn5.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!ChoiseDownComplete(ChoiseLine = 5)) BadVoise() else GoodVoise()
        }
        btn6.setOnClickListener {
            if (!btn6.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!ChoiseDownComplete(ChoiseLine = 6)) BadVoise() else GoodVoise()
        }
        btn7.setOnClickListener {
            if (!btn7.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!ChoiseDownComplete(ChoiseLine = 7)) BadVoise() else GoodVoise()
        }
        btn8.setOnClickListener {
            if (!btn8.isEnabled) return@setOnClickListener
            FExcStr.text = "Получаю задание..."
            if (!NewComplectationGetFirstOrder()) BadVoise() else GoodDone()
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
        ToModeChoiseDown()
    }

    fun ToModeChoiseDown() {
        Const.Refresh()
        SS.FEmployer.Refresh()     // проверим не изменились ли галки на спуск/комплектацию
        //не может спускать и комплектовать выходим обратно
        if (!SS.FEmployer.CanDown && !SS.FEmployer.CanComplectation) {
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseWorkInit)
            finish()
        }
        if (!SS.FEmployer.CanDown && SS.FEmployer.CanComplectation) {
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseWorkInit)
            finish()
        }
        //Сам запрос
        var textQuery = "select * from WPM_fn_GetChoiseDown()"
        DownSituation = SS.ExecuteWithReadNew(textQuery) ?: return

        if (DownSituation.isEmpty()) {
            return
        }

        textQuery = "select * from dbo.WPM_fn_ComplectationInfo()";
        var DT = SS.ExecuteWithReadNew(textQuery)
        if (DT == null) {
            PreviousAction = " < error > "
        } else {
            PreviousAction =
                DT[0]["pallets"].toString() + " п, " + DT[0]["box"].toString() + " м, " + DT[0]["CountEmployers"].toString() + " с";
        }

        if (DownSituation.count() == 0) {
            FExcStr.text = "Нет заданий к спуску...";
        } else {
            FExcStr.text = "Выберите сектор спуска...";
        }
        RefreshActivity()
        return
    }

    fun RefreshActivity() {
        lblState.text =
            "Спуск выбор (" + (if (SS.Const.CarsCount == "0") "нет ограничений" else SS.Const.CarsCount + " авто") + ")"
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
            if (DownSituation.count() <= i) {
                break
            }
            val txt1: String = (i + 1).toString() + ". " + DownSituation[i]["Sector"].toString()
                .trim() + " - " + DownSituation[i]["CountBox"].toString()
            val txt2: String =
                " мест " + DownSituation[i]["CountEmployers"].toString().trim() + " сотр."
            val allowed: Boolean = DownSituation[i]["Allowed"].toString() == "1"
            //айдем нужную кнопку
            when (i) {
                0 -> {
                    btn1.text = txt1 + " " + txt2
                    btn1.isEnabled = allowed
                    btn1.visibility = View.VISIBLE

                }
                1 -> {
                    btn2.text = txt1 + " " + txt2
                    btn2.isEnabled = allowed
                    btn2.visibility = View.VISIBLE
                }
                2 -> {
                    btn3.text = txt1 + " " + txt2
                    btn3.isEnabled = allowed
                    btn3.visibility = View.VISIBLE
                }
                3 -> {
                    btn4.text = txt1 + " " + txt2
                    btn4.isEnabled = allowed
                    btn4.visibility = View.VISIBLE
                }
                4 -> {
                    btn5.text = txt1 + " " + txt2
                    btn5.isEnabled = allowed
                    btn5.visibility = View.VISIBLE
                }
                5 -> {
                    btn6.text = txt1 + " " + txt2
                    btn6.isEnabled = allowed
                    btn6.visibility = View.VISIBLE
                }
                6 -> {
                    btn7.text = txt1 + " " + txt2
                    btn7.isEnabled = allowed
                    btn7.visibility = View.VISIBLE
                }
            }

        }

        btn8.text = "8. КМ: " + PreviousAction
        btn8.isEnabled = if (SS.FEmployer.CanComplectation) true else false
    }

    fun ChoiseDownComplete(ChoiseLine: Int): Boolean {

        if (DownSituation.count() < ChoiseLine) {
            FExcStr.text = ChoiseLine.toString() + " - нет в списке!"
            return false
        }
        if (DownSituation[ChoiseLine - 1]["Allowed"] == "0") {
            FExcStr.text = "Пока нельзя! Рано!"
            return false
        }
        SS.FEmployer.Refresh()
        val SectorPriory = RefSection()
        if (SectorPriory.FoundID(SS.FEmployer.GetAttribute("ПосланныйСектор").toString())) {
            if (DownSituation[ChoiseLine - 1]["Sector"].toString()
                    .trim() != SectorPriory.Name.trim()
            ) {
                FExcStr.text = "Нельзя! Можно только " + SectorPriory.Name.trim() + " сектор!"
                return false
            }
        }
        return ChoiseDownComplete(DownSituation[ChoiseLine - 1]["Sector"].toString().trim())
    }

    fun ChoiseDownComplete(CurrParent: String): Boolean {
        var textQuery =
            "declare @res int " +
                    "begin tran " +
                    "exec WPM_GetOrderDown :Employer, :NameParent, @res output " +
                    "if @res = 0 rollback tran else commit tran " +
                    ""
        textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID);
        textQuery = SS.QuerySetParam(textQuery, "NameParent", CurrParent);

        if (!SS.ExecuteWithoutRead(textQuery)) {
            return false
        }
        return ToModeDown()
    }

    fun ToModeDown(): Boolean {
        val downingInit = Intent(this, Downing::class.java)
        downingInit.putExtra("ParentForm", "ChoiseDown")
        startActivity(downingInit)
        finish()
        return true
    }

    fun NewComplectationGetFirstOrder(): Boolean {
        val ComplectationInit = Intent(this, NewComplectation::class.java)
        ComplectationInit.putExtra("ParentForm", "ChoiseDown")
        startActivity(ComplectationInit)
        finish()
        return true
    }

    private fun reactionBarcode(Barcode: String): Boolean {

        val barcoderes = SS.helper.DisassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "113") {
            //справочники типовые
            val idd = barcoderes["IDD"].toString()
            if (SS.IsSC(idd, "Сотрудники")) {
                SS.FEmployer = RefEmployer()
                val mainInit = Intent(this, MainActivity::class.java)
                mainInit.putExtra("ParentForm", "ChoiseDown")
                startActivity(mainInit)
                finish()
            } else if (SS.IsSC(idd, "Принтеры")) {
                if (SS.FPrinter.Selected) {
                    SS.FPrinter = RefPrinter()
                } else SS.FPrinter.FoundIDD(idd)
            } else {
                FExcStr.text = "Нет действий с данным ШК в данном режиме!"
                BadVoise()
                return false
            }
        } else {
            FExcStr.text = "Нет действий с данным ШК в данном режиме!"
            BadVoise()
            return false
        }

        return true
    }

    private fun ReactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4) {
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        var choise = SS.helper.WhatInt(keyCode)
        if (choise in 1..7) {
            FExcStr.text = "Получаю задание..."
            if (!ChoiseDownComplete(ChoiseLine = choise)) BadVoise()
            return true
        } else if (choise == 8 && SS.FEmployer.CanComplectation) {
            FExcStr.text = "Получаю задание..."
            if (!NewComplectationGetFirstOrder()) BadVoise()
            return true

        } else if (choise == 9 && SS.FEmployer.CanComplectation) {
            //отгрузка пока не работает
            return true
        }
        return false
    }

}
