package com.intek.wpma.choiseWork.accept.crossDoc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import com.intek.wpma.Global
import com.intek.wpma.R
import kotlinx.android.synthetic.main.activity_yap_item.*
import kotlin.math.min

class CrossYepItem: CrossDoc() {

    private var currentLine:Int = 1
    private var isMoveButton = true

    override fun onCreate(savedInstanceState: Bundle?) {
        ss.CurrentMode = Global.Mode.AcceptanceAccepted
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cross_yep)

        title = ss.title

        etikPol.setOnClickListener {
            //обработчик события при нажатии на кнопку принятия товара
            if (printLabels(false)) {
                noneItem()
                yapItem()
                updateTableInfo()
                val backH = Intent(this, CrossNonItem::class.java)
                backH.putExtra("parentIDD", parentIDD)
                startActivity(backH)
                finish()
            }
        }
        etik.setOnClickListener {
            //обработчик события при нажатии на кнопку принятия товара
            if (printLabels(true)) {
                noneItem()
                yapItem()
                updateTableInfo()
                val backH = Intent(this, CrossNonItem::class.java)
                backH.putExtra("parentIDD", parentIDD)
                startActivity(backH)
                finish()
            }
        }
        FExcStr.setOnTouchListener(fun(_: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x > oldx) {
                    ss.CurrentMode = Global.Mode.Waiting
                    val backHead = Intent(this, CrossDoc::class.java)
                    startActivity(backHead)
                    finish()
                }
            }
            return true
        })
        kolEtik.setOnKeyListener { _: View, keyCode: Int, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                if (ss.isMobile){  //спрячем клаву
                    val inputManager: InputMethodManager =  applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(
                        this.currentFocus!!.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS
                    )
                }
                // сохраняем текст, введенный до нажатия Enter в переменную
                try {
                    acceptedItems[currentLine-1]["LabelCount"] = kolEtik.text.toString()
                } catch (e: Exception) { }
            }
            false
        }
        refreshActivity()
    }

    override fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        if (keyCode == 4) {
            clickVoice()
            ss.CurrentMode = Global.Mode.Waiting
            val acBack = Intent(this, CrossDoc::class.java)
            startActivity(acBack)
            finish()
            return true
        }
        if (ss.helper.whatDirection(keyCode) == "Left") {
            clickVoice()
            ss.CurrentMode = Global.Mode.Waiting
            val backHead = Intent(this, CrossDoc::class.java)
            startActivity(backHead)
            finish()
            return true
        }
        //tab
        if (keyCode == 61) {
            clickVoice()
            if (printLabels(true)) {
                noneItem()
                yapItem()
                updateTableInfo()
                val backH = Intent(this, CrossNonItem::class.java)
                backH.putExtra("parentIDD", parentIDD)
                startActivity(backH)
                finish()
            }
            return true
        }
        if (keyCode == 56) {
            clickVoice()
            if (printLabels(false)) {
                noneItem()
                yapItem()
                updateTableInfo()
                val backH = Intent(this, CrossNonItem::class.java)
                backH.putExtra("parentIDD", parentIDD)
                startActivity(backH)
                finish()
            }
            return true
        }
        if (ss.helper.whatDirection(keyCode) in listOf("Down", "Up")) {
            tickVoice()
            isMoveButton = true
            table.getChildAt(currentLine).isFocusable = false
            table.getChildAt(currentLine).setBackgroundColor(Color.WHITE)
            if (ss.helper.whatDirection(keyCode) == "Down") {
                if (currentLine < acceptedItems.count()) currentLine++
                else currentLine = 1
            } else {
                if (currentLine > 1) currentLine--
                else currentLine = acceptedItems.count()
            }
            when {
                currentLine < 10 -> scroll.fullScroll(View.FOCUS_UP)
                currentLine > acceptedItems.count() - 10 -> scroll.fullScroll(View.FOCUS_DOWN)
                currentLine % 10 == 0 -> scroll.scrollTo(0, 30 * currentLine - 1)
            }
            //теперь подкрасим строку серым
            table.getChildAt(currentLine).setBackgroundColor(Color.LTGRAY)
            table.getChildAt(currentLine).isActivated = false
            kolEtik.text = acceptedItems[currentLine - 1]["BoxCount"].toString()
            return true
        }

        if (ss.helper.whatInt(keyCode) >= 0) {
            var thisInt = ss.helper.whatInt(keyCode).toString()
            thisInt = if (isMoveButton) thisInt else acceptedItems[currentLine - 1]["BoxCount"].toString() + thisInt
            if (changeLabelCount(thisInt)) refreshActivity()
            isMoveButton = false
            return true
        }

        if (keyCode == 67) {
            //это делете, оотменим приемку если до этого двигались
            return if (isMoveButton) {
                val invCode = acceptedItems[currentLine - 1]["InvCode"].toString()
                if (!deleteRowAcceptedItems(acceptedItems[currentLine - 1])) FExcStr.text = ss.excStr
                else FExcStr.text = (invCode.trim() + " - приемка отменена!")
                refreshActivity()
                true
            } else {
                var textForEdit = acceptedItems[currentLine - 1]["BoxCount"].toString()
                textForEdit = if (textForEdit.count() == 1) "1" else textForEdit.substring(0, textForEdit.count() - 1)
                if (changeLabelCount(textForEdit)) refreshActivity()
                true
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun refreshActivity() {

        super.refreshActivity()

        val linearLayout1 = LinearLayout(this)
        val rowTitle1 = TableRow(this)

        var k = 0
        //добавим столбцы
        val widArr : Array<Double> = arrayOf(0.05, 0.33, 0.17, 0.15, 0.1, 0.1, 0.1)
        val striArr : Array<String> = arrayOf("№", "Клиент", "Заказ", "Пал", "Прин", "Всего", "ЗКР")
        val hatVal : MutableMap<String, TextView> = HashMap()
        for (i in 0..6) hatVal["hatVal$i"] = TextView(this)

        for ((i,_) in hatVal) {
            hatVal[i]?.text = striArr[k]
            hatVal[i]?.typeface = Typeface.SERIF
            hatVal[i]?.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * widArr[k]).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT
            )
            hatVal[i]?.gravity = Gravity.CENTER
            hatVal[i]?.textSize = 16F
            hatVal[i]?.setTextColor(-0x1000000)
            linearLayout1.addView(hatVal[i])
            k++
        }

        rowTitle1.addView(linearLayout1)
        rowTitle1.setBackgroundColor(Color.GRAY)
        table.addView(rowTitle1)
        var lineNom = 0

        if (acceptedItems.isNotEmpty()) {

            for (DR in acceptedItems) {

                lineNom ++
                val linearLayout2 = LinearLayout(this)
                val rowTitle2 = TableRow(this)
                rowTitle2.isClickable = true
                rowTitle2.setOnTouchListener{ _, _ ->  //выделение строки при таче
                    var i = 1
                    while (i < table.childCount) {
                        if (rowTitle2 != table.getChildAt(i)) {
                            table.getChildAt(i).setBackgroundColor(Color.WHITE)
                        } else {
                            currentLine = i
                            rowTitle2.setBackgroundColor(Color.LTGRAY)
                            kolEtik.text = acceptedItems[currentLine - 1]["BoxCount"].toString()
                        }
                        i++
                    }
                    true
                }

                var s = 0
                //добавим столбцы
                val widtArr : Array<Double> = arrayOf(0.05, 0.33, 0.17, 0.15, 0.1, 0.1, 0.1)
                val strinArr : Array<String> = arrayOf(
                    DR["Number"].toString(), DR["ClientName"].toString().substring(0, min(DR["ClientName"].toString().length,10)), DR["OrderName"].toString(),
                    DR["PalletName"].toString(), DR["Count"].toString(), DR["CountAll"].toString(),
                    DR["CloseOrder"].toString())
                val bodVal : MutableMap<String, TextView> = HashMap()
                for (i in 0..6) bodVal["bodVal$i"] = TextView(this)

                for ((i,_) in bodVal) {
                    bodVal[i]?.text = strinArr[s]
                    bodVal[i]?.typeface = Typeface.SERIF
                    bodVal[i]?.layoutParams = LinearLayout.LayoutParams(
                        (ss.widthDisplay * widtArr[s]).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    bodVal[i]?.gravity = Gravity.CENTER
                    bodVal[i]?.textSize = 16F
                    bodVal[i]?.setTextColor(-0x1000000)
                    linearLayout2.addView(bodVal[i])
                    s++
                }

                rowTitle2.addView(linearLayout2)
                var colorline =  Color.WHITE
                if (lineNom == currentLine) {
                    colorline = Color.LTGRAY
                    kolEtik.text = acceptedItems[currentLine - 1]["BoxCount"].toString()
                }
                rowTitle2.setBackgroundColor(colorline)
                table.addView(rowTitle2)

            }
        }
    }

    private fun deleteRowAcceptedItems(currRow:MutableMap<String,String>):Boolean {
        //Нуно создать новую строку
        var textQuery =
        "SELECT max(DT\$АдресПоступление .lineno_) + 1 as NewLineNo_ " +
                "FROM DT\$АдресПоступление WHERE  DT\$АдресПоступление .iddoc = :Doc";
        textQuery = ss.querySetParam(textQuery, "Doc", currRow["iddoc"].toString())
        var dt = ss.executeWithReadNew(textQuery) ?: return false
        if (dt.isEmpty()) {
            return false
        }
        val newLineNo_ = dt[0]["NewLineNo_"].toString()

        textQuery =
            "BEGIN TRAN; " +
                    "IF EXISTS(SELECT LineNo_ FROM DT\$АдресПоступление as ACDT " +
                    "WHERE ACDT.IDDOC = :ACID " +
                    "and ACDT.\$АдресПоступление.Товар = :ItemID " +
                    "and ACDT.\$АдресПоступление.Состояние0 = 0) " +
                    "BEGIN " +
                    //увеличиваем количество не принятого
                    "UPDATE DT\$АдресПоступление " +
                    "SET \$АдресПоступление.Количество = \$АдресПоступление.Количество + :Count " +
                    "WHERE DT\$АдресПоступление .iddoc = :ACID " +
                    "and DT\$АдресПоступление .\$АдресПоступление.Товар = :ItemID " +
                    "and DT\$АдресПоступление .\$АдресПоступление.Состояние0 = 0; " +
                    //уменьшаем количество принятого
                    "UPDATE DT\$АдресПоступление " +
                    "SET \$АдресПоступление.Количество = \$АдресПоступление.Количество - :Count " +
                    "WHERE DT\$АдресПоступление .iddoc = :ACID " +
                    "and DT\$АдресПоступление .lineno_ = :lineno_ " +
                    //Удаляем пустые строки
                    "DELETE FROM DT\$АдресПоступление " +
                    "WHERE DT\$АдресПоступление .iddoc = :ACID " +
                    "and \$АдресПоступление.Количество = 0 " +
                    "END ELSE BEGIN " +
                    //сначала делаем строку принятого товара внизу
                    "INSERT INTO DT\$АдресПоступление " +
                    "SELECT DocAP.IDDOC, :NewLineNo_, :Number, " +
                    ":ItemID, DocAP.\$АдресПоступление.Количество - :Count, " +
                    ":EmptyID, DocAP.\$АдресПоступление.Коэффициент , 1, " +
                    "DocAP.\$АдресПоступление.Сотрудник0 , DocAP.\$АдресПоступление.Адрес0 , DocAP.\$АдресПоступление.Дата0 , " +
                    "DocAP.\$АдресПоступление.Время0 , DocAP.\$АдресПоступление.ФлагПечати , DocAP.\$АдресПоступление.КоличествоЭтикеток , " +
                    "DocAP.\$АдресПоступление.ЕдиницаШК , 0, 0, DocAP.\$АдресПоступление.Паллета " +
                    "FROM DT\$АдресПоступление as DocAP " +
                    "WHERE DocAP.IDDOC = :ACID " +
                    "and DocAP.lineno_ = :lineno_ " +
                    //Делаем строку не принятого товара
                    "UPDATE DT\$АдресПоступление " +
                    "SET \$АдресПоступление.Количество = :Count, " +
                    "\$АдресПоступление.Сотрудник0 = :EmptyID, "+
                    "\$АдресПоступление.Состояние0 = 0, " +
                    "\$АдресПоступление.Адрес0 = :EmptyID, " +
                    "\$АдресПоступление.Дата0 = :EmptyDate, " +
                    "\$АдресПоступление.Время0 = 0, " +
                    "\$АдресПоступление.ФлагПечати = 0, " +
                    "\$АдресПоступление.КоличествоЭтикеток = 0, " +
                    "\$АдресПоступление.Паллета = :EmptyID " +
                    "WHERE DT\$АдресПоступление .iddoc = :ACID " +
                    "and DT\$АдресПоступление .lineno_ = :lineno_ " +
                    //Удаляем пустые строки
                    "DELETE FROM DT\$АдресПоступление " +
                    "WHERE DT\$АдресПоступление .iddoc = :ACID " +
                    "and \$АдресПоступление.Количество = 0 " +

                    "END; " +
                    //обновляем заказ
                    "UPDATE DT\$ЗаказНаКлиента " +
                    "SET " +
                    "\$ЗаказНаКлиента.Принято = \$ЗаказНаКлиента.Принято - :Count " +
                    "WHERE " +
                    "DT\$ЗаказНаКлиента .iddoc = :OrderID " +
                    "and \$ЗаказНаКлиента.Товар = :ItemID " +
                    "COMMIT TRAN;";
         textQuery = ss.querySetParam(textQuery, "OrderID", currRow["OrderID"].toString())
        textQuery = ss.querySetParam(textQuery, "NewLineNo_", newLineNo_)
        textQuery = ss.querySetParam(textQuery, "Number", currRow["Number"].toString())
        textQuery = ss.querySetParam(textQuery, "ACID", currRow["iddoc"].toString())
        textQuery = ss.querySetParam(textQuery, "Count", currRow["Count"].toString())
        textQuery = ss.querySetParam(textQuery, "ItemID", currRow["id"].toString())
        textQuery = ss.querySetParam(textQuery, "lineno_", currRow["LineNO_"].toString())
        textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        if (!ss.executeWithoutRead(textQuery)) {
            return false
        }
        noneItem()
        yapItem()
        updateTableInfo()
        return true
    } // DeleteRowAcceptedItems()

    private fun changeLabelCount(labelCount:String):Boolean {

        var textQuery =
            "UPDATE DH\$ЗаказНаКлиента " +
                    "SET \$ЗаказНаКлиента.КолМест = :LabelCount " +
                    "WHERE DH\$ЗаказНаКлиента .iddoc = :Doc"
        textQuery = ss.querySetParam(textQuery, "LabelCount", labelCount)
        textQuery = ss.querySetParam(textQuery, "Doc", acceptedItems[currentLine - 1]["OrderID"].toString())
        if (!ss.executeWithoutRead(textQuery))  return false
        for (DR in acceptedItems) {
           if (DR["OrderID"] == acceptedItems[currentLine - 1]["OrderID"]) {
               DR["BoxCount"] = labelCount
           }
        }
        return true
    }

    private fun printLabels(condition: Boolean):Boolean {

        //FExcStr = null;
       // PalletAcceptedItem = null;
        if (consignmen.isEmpty()) {
            FExcStr.text = "Не выбраны накладные для приемки!"
            return false
        }
        if (acceptedItems.isEmpty()) {
            FExcStr.text= "Нет принятых товаров в текущей сессии!"
            return false
        }

        if (!ss.FPrinter.selected)
        {
            FExcStr.text= "Принтер не выбран!";
            return false;
        }

        //Формируем строку с ид-шниками АдресовПоступления
        var strACID = ""
        for (dr in consignmen) {
            strACID += dr["ACID"].toString().trim() + ","
        }
        strACID = strACID.substring(0, strACID.length - 1)

        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"]       = strACID
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"]       = condition.toString()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"]    = ss.extendID(ss.FPrinter.id, "Спр.Принтеры")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"]    = ss.extendID(ss.FEmployer.id, "Спр.Сотрудники")
        var dataMapRead: MutableMap<String, Any> = mutableMapOf()
        val fieldList: MutableList<String> = mutableListOf("Спр.СинхронизацияДанных.ДатаРез1")
        try {
            dataMapRead = execCommand("CompleteAcceptanceCross", dataMapWrite, fieldList, dataMapRead)
        } catch (e: Exception) {
            badVoice()
            FExcStr.text= "Не удалось напечатать этикетки"
            return false
        }
        if ((dataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() == -3) {
            badVoice()
            FExcStr.text = dataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString()
            return false
        }
        FExcStr.text = dataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString()
        return true

    }
}