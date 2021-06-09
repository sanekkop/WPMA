package com.intek.wpma.choiseWork.accept.crossDoc

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import com.intek.wpma.Global
import com.intek.wpma.R
import com.intek.wpma.ref.RefItem
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_cross_non.*
import kotlinx.android.synthetic.main.activity_none_item.*
import kotlinx.android.synthetic.main.activity_none_item.FExcStr
import kotlinx.android.synthetic.main.activity_none_item.ItemName
import kotlinx.android.synthetic.main.activity_none_item.btnScan
import kotlinx.android.synthetic.main.activity_none_item.printPal
import kotlinx.android.synthetic.main.activity_none_item.scroll
import kotlinx.android.synthetic.main.activity_none_item.searchArt
import kotlinx.android.synthetic.main.activity_none_item.table

open class CrossNonItem : CrossDoc() {

    private var currentLine:Int = 1
    private var artSearch : String = ""   //а этот мы будем сравнивать
    private val itm = RefItem()
    private var idDocItm = ""
    private var noneAccItemLocal: MutableList<MutableMap<String, String>> = mutableListOf()
    private var flagBarcode = ""
    private var print = ""
    private var clientCheck = ""
    private var orderId = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        ss.CurrentMode = Global.Mode.AcceptanceNotAccepted
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cross_non)
        title = ss.title
        noneAccItemLocal.addAll(noneAccItem)

        parentIDD = intent.extras!!.getString("parentIDD")!!

        //фигня чтобы скрол не скролился
        table.setOnKeyListener { _, keyCode, event ->
            try {
                if (event.action == MotionEvent.ACTION_DOWN && ss.helper.whatDirection(keyCode) in listOf("Down", "Up")) reactionKeyLocal(keyCode)
                else if (event.action == MotionEvent.ACTION_DOWN) reactionKey(keyCode, event)
                else true
            } catch (e: Exception) {
                true
            }
        }
        FExcStr.setOnTouchListener(fun(_: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    ss.CurrentMode = Global.Mode.Waiting
                    val backHead = Intent(this, CrossDoc::class.java)
                    startActivity(backHead)
                    finish()
                }
            }
            return true
        })
        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@CrossNonItem, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "CrossNonItem")
                startActivity(scanAct)
            }
        }
        //позиционируемся на первом товаре
        if (noneAccItemLocal.isNotEmpty()) {
            if (itm.foundID(noneAccItemLocal[0]["id"].toString())) ItemName.text = itm.name
            else ItemName.text = noneAccItemLocal[0]["ItemName"].toString()
            idDocItm = noneAccItemLocal[0]["iddoc"].toString()
            clientCheck = noneAccItemLocal[0]["ClientName"].toString().trim()
            orderId = noneAccItemLocal[0]["OrderID"].toString()

        }
        refreshActivity()
    }

    //а вот и сама табличка
    @SuppressLint("ClickableViewAccessibility")
    override fun refreshActivity() {
        super.refreshActivity()
        searchArt.setTextColor(Color.BLACK)
        searchArt.textSize = 18F
        artSearch = searchArt.text.toString()

        print = printPal.text.toString()

        //шапочка
        val linearLayout = LinearLayout(this)
        var k = 0
        val widthArr : Array<Double> = arrayOf(0.05, 0.19, 0.24, 0.21, 0.21, 0.1)
        val stringArr : Array<String> = arrayOf("№", "Клиент", "Заказ", "Артикул", "Арт. на", "Кол.")
        val hatVal : MutableMap<String, TextView> = HashMap()
        for (i in 0..5) hatVal["hatVal$i"] = TextView(this)

        for ((i,_) in hatVal) {
            hatVal[i]?.text = stringArr[k]
            hatVal[i]?.typeface = Typeface.SERIF
            hatVal[i]?.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * widthArr[k]).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT
            )
            hatVal[i]?.gravity = Gravity.CENTER
            hatVal[i]?.textSize = 18F
            hatVal[i]?.setTextColor(-0x1000000)
            linearLayout.addView(hatVal[i])
            k++
        }

        linearLayout.setBackgroundColor(Color.rgb(192, 192, 192))
        table.addView(linearLayout)
        val countLocal = noneAccItemLocal.count()
        noneAccItemLocal.clear()
        if (noneAccItem.isNotEmpty()&&artSearch.isNotEmpty()) {
            for (DR in noneAccItem) {
                if (DR["ArticleFind"].toString().trim().indexOf(artSearch) == -1
                    && DR["ArticleOnPackFind"].toString().trim().indexOf(artSearch) == -1
                    && DR["ItemNameFind"].toString().trim().indexOf(artSearch) == -1) {
                    //если нет вхождений то надо удалить эту строку, если есть рисуем
                    continue
                }
                noneAccItemLocal.add(DR)            //есть контакт, скопируем строчку
            }
        }
        else noneAccItemLocal.addAll(noneAccItem)

        if (countLocal != noneAccItemLocal.count()&&noneAccItemLocal.isNotEmpty()) {
            //сменилось количество, обнулим текущую строку
            currentLine = 1
            //позиционируемся на первом товаре
            if (itm.foundID(noneAccItemLocal[0]["id"].toString())) ItemName.text = itm.name
            else ItemName.text = noneAccItemLocal[0]["ItemName"].toString()
            idDocItm = noneAccItemLocal[0]["iddoc"].toString()
        }

        var lineNom = 0
        //данные по товару
        if (noneAccItemLocal.isNotEmpty()) {

            for (DR in noneAccItemLocal) {
                lineNom ++

                val linearLayout1 = LinearLayout(this)
                linearLayout1.isClickable = true
                linearLayout1.setOnTouchListener{ _, _ ->  //выделение строки при таче
                    var i = 1
                    while (i < table.childCount) {
                        if (linearLayout1 != table.getChildAt(i)) {
                            if ((table.getChildAt(i).background as ColorDrawable).color == Color.LTGRAY) {
                                table.getChildAt(i).setBackgroundColor(Color.WHITE)
                            }
                        } else {
                            currentLine = i
                            linearLayout1.setBackgroundColor(Color.LTGRAY)
                            if (itm.foundID(DR["id"].toString())) ItemName.text = itm.name
                            else ItemName.text = DR["ItemName"]
                            clientCheck = DR["ClientName"].toString().trim()
                            idDocItm = DR["iddoc"].toString().trim()
                            orderId = DR["OrderID"].toString()

                        }
                        i++
                    }
                    true
                }
                //добавим столбцы
                var s = 0
                val strArr : Array<String> = arrayOf(
                    DR["Number"].toString(),
                    DR["ClientName"].toString().substring(0,7),
                    DR["OrderName"].toString().trim(),
                    DR["Article"].toString().trim(),
                    DR["ArticleOnPack"].toString().trim(),
                    DR["Count"].toString()
                )
                val bodyVal : MutableMap<String, TextView> = HashMap()
                for (i in 0..5) bodyVal["bodyVal$i"] = TextView(this)

                for ((i,_) in bodyVal) {
                    bodyVal[i]?.text = strArr[s]
                    bodyVal[i]?.typeface = Typeface.SERIF
                    bodyVal[i]?.layoutParams = LinearLayout.LayoutParams(
                        (ss.widthDisplay * widthArr[s]).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    bodyVal[i]?.gravity = Gravity.CENTER
                    bodyVal[i]?.textSize = 18F
                    bodyVal[i]?.setTextColor(-0x1000000)
                    linearLayout1.addView(bodyVal[i])
                    s++
                }
                var colorline =  Color.WHITE
                if (lineNom == currentLine) {
                    colorline = Color.LTGRAY
                }
                linearLayout1.setBackgroundColor(colorline)
                table.addView(linearLayout1)
            }
        }
    }

    override fun reactionBarcode(Barcode: String): Boolean {
        //если таковой имеется, то присваеваем айдишник и ищем в списке непринятого
        if (itm.foundBarcode(Barcode)) {
            var findItemInTable = false
            for (DR in noneAccItemLocal) {
                if (itm.id == DR["id"].toString()) {
                    flagBarcode = "1"
                    idDocItm = DR["iddoc"].toString()
                    findItemInTable = true
                    break
                }
            }
            if (findItemInTable) {
                //если товар есть в списке, переходим в карточку
                val gotoItem = Intent(this, CrossCard::class.java)
                gotoItem.putExtra("parentIDD", parentIDD)
                gotoItem.putExtra("itemID", itm.id)
                gotoItem.putExtra("flagBarcode", flagBarcode)
                gotoItem.putExtra("iddoc", idDocItm)
                gotoItem.putExtra("orderId", orderId)
                startActivity(gotoItem)
                finish()
                return true
            }
            else {
                //не нашли
                //если товар есть в списке, переходим в карточку
                val gotoItem = Intent(this, CrossCard::class.java)
                gotoItem.putExtra("parentIDD", parentIDD)
                gotoItem.putExtra("itemID", itm.id)
                gotoItem.putExtra("flagBarcode", flagBarcode)
                gotoItem.putExtra("iddoc", "")
                gotoItem.putExtra("orderId", orderId)
                startActivity(gotoItem)
                finish()
                return true
            }
        }
        else return super.reactionBarcode(Barcode)
    }

    override fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {
        if (ss.helper.whatDirection(keyCode) == "Right" || keyCode == 4) {
            clickVoice()
            ss.CurrentMode = Global.Mode.Waiting
            val acBack = Intent(this, CrossDoc::class.java)
            startActivity(acBack)
            finish()
        }
        if (ss.helper.whatDirection(keyCode) == "Left") {
            clickVoice()
            ss.CurrentMode = Global.Mode.Waiting
            val goInfo = Intent(this, CrossInfo::class.java)
            goInfo.putExtra("cliName", clientCheck)
            goInfo.putExtra("parentIDD", parentIDD)
            goInfo.putExtra("print", print)
            startActivity(goInfo)
            finish()
        }
        if (ss.helper.whatInt(keyCode) != -1) {             //артикуля, ля, ля, ля
            clickVoice()
            searchArt.text = (searchArt.text.toString().trim() + ss.helper.whatInt(keyCode).toString())
            refreshActivity()
        }
        if (keyCode == 66) {
                flagBarcode = "0"
                val gotoItem = Intent(this, CrossCard::class.java)
                gotoItem.putExtra("parentIDD", parentIDD)
                gotoItem.putExtra("itemID", itm.id)
                gotoItem.putExtra("flagBarcode", flagBarcode)
                gotoItem.putExtra("iddoc", idDocItm)
                gotoItem.putExtra("orderId", orderId)
                startActivity(gotoItem)
                finish()
            }
        if (keyCode == 67) {                                //чистит артикулы(введенное)
            if (searchArt.text.toString().isNotEmpty()) {
                searchArt.text = searchArt.text
                    .toString()
                    .substring(0, searchArt.text.toString().length - 1)
                refreshActivity()
            } else refreshActivity()
        }

        if (ss.helper.whatDirection(keyCode) in listOf("Down", "Up")) {
            //на случай кода дошли до конца экрана
            reactionKeyLocal(keyCode)
        }
        return true
    }

    private fun reactionKeyLocal(keyCode: Int):Boolean {
        tickVoice()
        var res = true
        if (noneAccItemLocal.isEmpty()) return false

        //тут частенько вылетает сделаем через попытку
        val oldCurrentLine = currentLine
        table.getChildAt(currentLine).setBackgroundColor(Color.WHITE)
        if (ss.helper.whatDirection(keyCode) == "Down") {
            if (currentLine < noneAccItemLocal.count()) currentLine++
            else currentLine = 1
        } else {
            if (currentLine > 1) currentLine--
            else currentLine = noneAccItemLocal.count()
        }

        if (noneAccItemLocal.count() >= 15) {
            if ((oldCurrentLine >= 15 && currentLine == 1) || currentLine < 8) {
                //переход в начало
                scroll.fullScroll(View.FOCUS_UP)
            } else if ((oldCurrentLine == 1 && currentLine >= 15) || currentLine > (noneAccItemLocal.count() - 8)) {
                //переход в конец
                scroll.fullScroll(View.FOCUS_DOWN)
            } else if (currentLine % 8 == 0) res = false
        }

        if (itm.foundID(noneAccItemLocal[currentLine - 1]["id"].toString())) ItemName.text = itm.name
        else ItemName.text = noneAccItemLocal[currentLine - 1]["ItemName"].toString()

        idDocItm = noneAccItemLocal[currentLine - 1]["iddoc"].toString()
        clientCheck = noneAccItemLocal[currentLine - 1]["ClientName"].toString().trim()
        orderId = noneAccItemLocal[currentLine - 1]["OrderID"].toString()
        //теперь подкрасим строку серым
        table.getChildAt(currentLine).setBackgroundColor(Color.LTGRAY)
        return res
    }
}