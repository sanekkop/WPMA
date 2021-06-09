package com.intek.wpma.ChoiseWork.Accept

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
import com.intek.wpma.Ref.RefItem
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_accept.*
import kotlinx.android.synthetic.main.activity_none_item.*
import kotlinx.android.synthetic.main.activity_none_item.FExcStr
import kotlinx.android.synthetic.main.activity_none_item.btnScan
import kotlinx.android.synthetic.main.activity_none_item.scroll
import kotlinx.android.synthetic.main.activity_none_item.table
import kotlinx.android.synthetic.main.activity_yap_item.*

class NoneItem : Search() {

    private var currentLine:Int = 1
    private var artSearch : String = ""   //а этот мы будем сравнивать
    private val itm = RefItem()
    private var idDocItm = ""
    private var noneAccItemLocal: MutableList<MutableMap<String, String>> = mutableListOf()
    private var flagBarcode = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        ss.CurrentMode = Global.Mode.AcceptanceNotAccepted
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_none_item)
        title = ss.title
        //при переходе напишем ошибку если она была
        if (ss.excStr != "" && ss.excStr != "null") {
            FExcStr.text = ss.excStr
            //обнулим ошибку
            ss.excStr = ""
        }

        noneAccItemLocal.addAll(noneAccItem)

        //фигня чтобы скрол не скролился
        table.setOnKeyListener { v, keyCode, event ->
            try {
                if (event.action == MotionEvent.ACTION_DOWN && ss.helper.whatDirection(keyCode) in listOf("Down", "Up")) {
                    reactionKeyLocal(keyCode)
                } else if (event.action == MotionEvent.ACTION_DOWN) {
                    reactionKey(keyCode, event)
                } else true
            } catch (e: Exception) {
                true
            }
        }


        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    ss.CurrentMode = Global.Mode.Waiting
                    val backHead = Intent(this, Search::class.java)
                    startActivity(backHead)
                    finish()
                }
            }
            return true
        })


        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@NoneItem, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "NoneItem")
                startActivity(scanAct)
            }
        }
        //позиционируемся на первом товаре
        if (noneAccItemLocal.isNotEmpty()) {
            if (itm.foundID(noneAccItemLocal[0]["id"].toString())) {
                ItemName.text = itm.name
            } else {
                ItemName.text = noneAccItemLocal[0]["ItemName"].toString()
            }
            idDocItm = noneAccItemLocal[0]["iddoc"].toString()
        }
        refreshActivity()
    }

    //а вот и сама табличка
    override fun refreshActivity() {

        super.refreshActivity()

        searchArt.setTextColor(Color.BLACK)
        searchArt.textSize = 18F
        artSearch = searchArt.text.toString()

        //шапочка
        val linearLayout = LinearLayout(this)

        val number = TextView(this)
        number.text = "№"
        number.typeface = Typeface.SERIF
        number.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.05).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        number.gravity = Gravity.CENTER
        number.textSize = 18F
        number.setTextColor(-0x1000000)
        number.setBackgroundResource(R.drawable.bg)
        number.setBackgroundColor(Color.GRAY)
        val docum = TextView(this)
        docum.text = "Накл."
        docum.typeface = Typeface.SERIF
        docum.gravity = Gravity.CENTER
        docum.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.21).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        docum.textSize = 18F
        docum.setTextColor(-0x1000000)
        docum.setBackgroundResource(R.drawable.bg)
        docum.setBackgroundColor(Color.GRAY)
        val address = TextView(this)
        address.text = "Артикул"
        address.typeface = Typeface.SERIF
        address.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.24).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        address.gravity = Gravity.CENTER
        address.textSize = 18F
        address.setTextColor(-0x1000000)
        address.setBackgroundResource(R.drawable.bg)
        address.setBackgroundColor(Color.GRAY)
        val boxes = TextView(this)
        boxes.text = "Арт. на"
        boxes.typeface = Typeface.SERIF
        boxes.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.24).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        boxes.gravity = Gravity.CENTER
        boxes.textSize = 18F
        boxes.setTextColor(-0x1000000)
        boxes.setBackgroundResource(R.drawable.bg)
        boxes.setBackgroundColor(Color.GRAY)
        val boxesfact = TextView(this)
        boxesfact.text = "Кол."
        boxesfact.typeface = Typeface.SERIF
        boxesfact.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.13).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        boxesfact.gravity = Gravity.CENTER
        boxesfact.textSize = 18F
        boxesfact.setTextColor(-0x1000000)
        boxesfact.setBackgroundResource(R.drawable.bg)
        boxesfact.setBackgroundColor(Color.GRAY)
        val kof = TextView(this)
        kof.text = "Коэф."
        kof.typeface = Typeface.SERIF
        kof.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.13).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        kof.gravity = Gravity.CENTER
        kof.textSize = 18F
        kof.setTextColor(-0x1000000)
        kof.setBackgroundResource(R.drawable.bg)
        kof.setBackgroundColor(Color.GRAY)

        linearLayout.addView(number)
        linearLayout.addView(docum)
        linearLayout.addView(address)
        linearLayout.addView(boxes)
        linearLayout.addView(boxesfact)
        linearLayout.addView(kof)

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
                //есть контакт, скопируем строчку
                noneAccItemLocal.add(DR)
            }
        }
        else {
            noneAccItemLocal.addAll(noneAccItem)
        }
        if (countLocal != noneAccItemLocal.count()&&noneAccItemLocal.isNotEmpty()) {
            //сменилось количество, обнулим текущую строку
            currentLine = 1
            //позиционируемся на первом товаре
            if (itm.foundID(noneAccItemLocal[0]["id"].toString())) {
                ItemName.text = itm.name
            } else {
                ItemName.text = noneAccItemLocal[0]["ItemName"].toString()
            }
            idDocItm = noneAccItemLocal[0]["iddoc"].toString()
        }

        var lineNom = 0
        //данные по товару
        if (noneAccItemLocal.isNotEmpty()) {

            for (DR in noneAccItemLocal) {
                lineNom ++

                val linearLayout1 = LinearLayout(this)
                linearLayout1.isClickable = true
                linearLayout1.setOnTouchListener{ v, event ->  //выделение строки при таче
                    var i = 1
                    while (i < table.childCount) {
                        if (linearLayout1 != table.getChildAt(i)) {
                            if ((table.getChildAt(i).background as ColorDrawable).color == Color.LTGRAY) {
                                table.getChildAt(i).setBackgroundColor(Color.WHITE)
                            }
                        } else {
                            currentLine = i
                            linearLayout1.setBackgroundColor(Color.LTGRAY)
                            if (itm.foundID(DR["id"].toString()))
                            {
                                ItemName.text = itm.name
                            }
                            else {
                                ItemName.text = DR["ItemName"]
                            }
                            idDocItm = DR["iddoc"].toString().trim()
                        }
                        i++
                    }
                    true
                }
                //добавим столбцы
                val numBer = TextView(this)
                numBer.text = DR["Number"]
                numBer.typeface = Typeface.SERIF
                numBer.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.05).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                numBer.gravity = Gravity.CENTER
                numBer.textSize = 18F
                numBer.setTextColor(-0x1000000)
                val dcNum = TextView(this)
                dcNum.text = DR["DOCNO"]
                dcNum.typeface = Typeface.SERIF
                dcNum.gravity = Gravity.CENTER
                dcNum.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.21).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                dcNum.textSize = 18F
                dcNum.setTextColor(-0x1000000)
                val address = TextView(this)
                address.text = DR["Article"].toString().trim()
                address.typeface = Typeface.SERIF
                address.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.24).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                address.gravity = Gravity.START
                address.textSize = 18F
                address.setTextColor(-0x1000000)
                val boxES = TextView(this)
                boxES.text = DR["ArticleOnPack"].toString().trim()
                boxES.typeface = Typeface.SERIF
                boxES.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.24).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                boxES.gravity = Gravity.START
                boxES.textSize = 18F
                boxES.setTextColor(-0x1000000)
                val boxesFact = TextView(this)
                boxesFact.text = DR["Count"]
                boxesFact.typeface = Typeface.SERIF
                boxesFact.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.13).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                boxesFact.gravity = Gravity.CENTER
                boxesFact.textSize = 18F
                boxesFact.setTextColor(-0x1000000)
                val koEf = TextView(this)
                koEf.text = ss.helper.byeTheNull(DR["CoefView"].toString()) //обрежем нулики и точку
                koEf.typeface = Typeface.SERIF
                koEf.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.13).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                koEf.gravity = Gravity.CENTER
                koEf.textSize = 18F
                koEf.setTextColor(-0x1000000)

                linearLayout1.addView(numBer)
                linearLayout1.addView(dcNum)
                linearLayout1.addView(address)
                linearLayout1.addView(boxES)
                linearLayout1.addView(boxesFact)
                linearLayout1.addView(koEf)

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
                val gotoItem = Intent(this, ItemCard::class.java)
                gotoItem.putExtra("itemID", itm.id)
                gotoItem.putExtra("flagBarcode", flagBarcode)
                gotoItem.putExtra("iddoc", idDocItm)
                startActivity(gotoItem)
                finish()
                return true
            }
            else {
                //не нашли
                //если товар есть в списке, переходим в карточку
                val gotoItem = Intent(this, ItemCard::class.java)
                gotoItem.putExtra("itemID", itm.id)
                gotoItem.putExtra("flagBarcode", flagBarcode)
                gotoItem.putExtra("iddoc", "")
                startActivity(gotoItem)
                finish()
                return true
            }

        }
        else {
           return super.reactionBarcode(Barcode)
        }
    }

    override fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {
        if (keyCode == 4) {
            clickVoise()
            ss.CurrentMode = Global.Mode.Waiting
            val acBack = Intent(this, Search::class.java)
            startActivity(acBack)
            finish()
        }

        if (ss.helper.whatDirection(keyCode) == "Right") {
            clickVoise()
            ss.CurrentMode = Global.Mode.Waiting
            val backHead = Intent(this, Search::class.java)
            startActivity(backHead)
            finish()
        }
        if (ss.helper.whatDirection(keyCode) == "Left") {
           //говорят при нажатии в лево вылетает программа
            //првоерим
        }

        if (ss.helper.whatInt(keyCode) != -1) {             //артикуля, ля, ля, ля
            searchArt.text = (searchArt.text.toString().trim() + ss.helper.whatInt(keyCode).toString())
            clickVoise()
            refreshActivity()
        }

        if (keyCode == 66 && !ss.FPallet.selected) {    //нет паллеты, забудь про карточку
            FExcStr.text = "Не выбрана паллета!"
            badVoise()
        } else
            if (keyCode == 66 && ss.FPallet.selected) {
                flagBarcode = "0"
                val gotoItem = Intent(this, ItemCard::class.java)
                gotoItem.putExtra("itemID", itm.id)
                gotoItem.putExtra("flagBarcode", flagBarcode)
                gotoItem.putExtra("iddoc", idDocItm)
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
        tickVoise()
        var res = true
        if (noneAccItemLocal.isEmpty()) {
            return false
        }
        //тут частенько вылетает сделаем через попытку
        val oldCurrentLine = currentLine
        table.getChildAt(currentLine).setBackgroundColor(Color.WHITE)
        if (ss.helper.whatDirection(keyCode) == "Down") {
            if (currentLine < noneAccItemLocal.count()) {
                currentLine++
            } else {
                currentLine = 1
            }
        } else {
            if (currentLine > 1) {
                currentLine--
            } else {
                currentLine = noneAccItemLocal.count()
            }

        }

        if (noneAccItemLocal.count() >= 15) {
            if ((oldCurrentLine >= 15 && currentLine == 1) || currentLine < 8) {
                //переход в начало
                scroll.fullScroll(View.FOCUS_UP)
            } else if ((oldCurrentLine == 1 && currentLine >= 15) || currentLine > (noneAccItemLocal.count() - 8)) {
                //переход в конец
                scroll.fullScroll(View.FOCUS_DOWN)
            } else if (currentLine % 8 == 0) {
                res = false
            }
        }

        if (itm.foundID(noneAccItemLocal[currentLine - 1]["id"].toString())) {
            ItemName.text = itm.name
        } else {
            ItemName.text = noneAccItemLocal[currentLine - 1]["ItemName"].toString()
        }
        idDocItm = noneAccItemLocal[currentLine - 1]["iddoc"].toString()
        //теперь подкрасим строку серым
        table.getChildAt(currentLine).setBackgroundColor(Color.LTGRAY)
        return res
    }
}