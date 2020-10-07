package com.intek.wpma.ChoiseWork.Accept

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import com.intek.wpma.Global
import com.intek.wpma.R
import com.intek.wpma.Ref.RefItem
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_none_item.*


class NoneItem : Search() {

    private var currentLine:Int = 1
    var artSearch : String = ""   //а этот мы будем сравнивать
    private val itm = RefItem()
    var flagBarcode = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        ss.CurrentMode = Global.Mode.AcceptanceNotAccepted
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_none_item)
        title = ss.title
/*
            scroll.setOnScrollChangeListener(fun(v:View,scrollX:Int,scrollY:Int,oldScrollX:Int,oldScrollY:Int) {
            if (oldScrollY - scrollY == 10 )
            {
                return
            }
            else {
                return
            }
        })

 */
        //фигня чтобы скрол не скролился
        table.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                reactionKeyLocal(keyCode, event)
            } else true
        })


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


        if (ss.isMobile){
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@NoneItem, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "NoneItem")
                startActivity(scanAct)
            }
        }

        refreshActivity()
    }

    //а вот и сама табличка
    override fun refreshActivity() {

        super.refreshActivity()

        searchArt.setTextColor(Color.BLACK)
        searchArt.textSize = 18F
        artSearch = searchArt.text.toString()
        var lineNom = 0

        //шапочка
        val linearLayout = LinearLayout(this)
        val rowTitle = TableRow(this)

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

        rowTitle.addView(linearLayout)
        rowTitle.setBackgroundColor(Color.rgb(192, 192, 192))
        table.addView(rowTitle)

        //данные по товару
        if (noneAccItem.isNotEmpty()) {

            for (DR in noneAccItem) {
                lineNom ++
                if (DR["ArticleFind"].toString().trim().indexOf(artSearch) == -1 && DR["ArticleOnPackFind"].toString().trim().indexOf(
                        artSearch
                    ) == -1 && DR["ItemNameFind"].toString().trim().indexOf(artSearch) == -1) continue //пока нет вхождений пропускаем, если есть рисуем

                val linearLayout1 = LinearLayout(this)
                val rowTitle1 = TableRow(this)

                rowTitle1.isClickable = true
                rowTitle1.setOnTouchListener{ v, event ->  //выделение строки при таче
                    var i = 0
                    while (i < table.childCount) {
                        if (rowTitle1 != table.getChildAt(i)) {
                            table.getChildAt(i).setBackgroundColor(Color.WHITE)
                        } else {
                            currentLine = i
                            rowTitle1.setBackgroundColor(Color.GRAY)
                        }
                        i++
                        if (itm.foundID(DR["id"].toString()))
                        {
                            ItemName.text = itm.name
                        }
                        else {
                            ItemName.text = DR["ItemName"]
                        }

                    }
                    true
                }

                var colorline =  Color.WHITE
                if (lineNom == currentLine) {
                    colorline = Color.GRAY
                }
                rowTitle1.setBackgroundColor(colorline)

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
                val addRess = TextView(this)
                addRess.text = DR["Article"].toString().trim()
                addRess.typeface = Typeface.SERIF
                addRess.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.24).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                addRess.gravity = Gravity.START
                addRess.textSize = 18F
                addRess.setTextColor(-0x1000000)
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
                linearLayout1.addView(addRess)
                linearLayout1.addView(boxES)
                linearLayout1.addView(boxesFact)
                linearLayout1.addView(koEf)

                rowTitle1.addView(linearLayout1)
                rowTitle1.setBackgroundColor(Color.WHITE)
                table.addView(rowTitle1)
            }
        }
    }

    override fun reactionBarcode(Barcode: String): Boolean {
        //если таковой имеется, то присваеваем айдишник и ищем в списке непринятого
        if (itm.foundBarcode(Barcode) == true) {
            for (DR in noneAccItem) {
                if (itm.id == DR["id"].toString().trim()) {
                    flagBarcode = "1"
                    break
                }
            }
            //если товар есть в списке, переходим в карточку
            val gotoItem = Intent(this, ItemCard::class.java)
            gotoItem.putExtra("itemID", itm.id)
            gotoItem.putExtra("flagBarcode", flagBarcode)
            startActivity(gotoItem)
            finish()
            return true
        }
        else {
           return super.reactionBarcode(Barcode)
        }
    }

    private fun reactionKeyLocal(keyCode: Int, event: KeyEvent?):Boolean {

        if (keyCode == 4){
            ss.CurrentMode = Global.Mode.Waiting
            val acBack = Intent(this, Search::class.java)
            startActivity(acBack)
            finish()
            return true
        }

        if (ss.helper.whatDirection(keyCode) == "Right") {
            ss.CurrentMode = Global.Mode.Waiting
            val backHead = Intent(this, Search::class.java)
            startActivity(backHead)
            finish()
            return true
        }

        if (ss.helper.whatDirection(keyCode) in listOf("Down", "Up")) {
            table.getChildAt(currentLine).isFocusable = false
            table.getChildAt(currentLine).setBackgroundColor(Color.WHITE)
            if (ss.helper.whatDirection(keyCode) == "Down") {
                if (currentLine < noneAccItem.count()) {
                    currentLine++
                }else {
                    currentLine = 1
                }
            } else {
                if (currentLine > 1) {
                    currentLine--
                } else {
                    currentLine = noneAccItem.count()
                }

            }
            if (currentLine < 10) {
                scroll.fullScroll(View.FOCUS_UP)
            }
            else if (currentLine > noneAccItem.count() - 10) {
                scroll.fullScroll(View.FOCUS_DOWN)
            }
            else if (currentLine%10 == 0)
            {
                scroll.scrollTo(0, 30*currentLine-1)
            }

            if (itm.foundID(noneAccItem[currentLine - 1]["id"].toString())) {
                ItemName.text = itm.name
            } else {
                ItemName.text = noneAccItem[currentLine - 1]["ItemName"].toString()
            }
            //теперь подкрасим строку серым
            table.getChildAt(currentLine).setBackgroundColor(Color.GRAY)
            table.getChildAt(currentLine).isActivated = false
            return true
        }

        if (ss.helper.whatInt(keyCode) != -1) {             //артикуля, ля, ля, ля
            searchArt.text = (searchArt.text.toString().trim() + ss.helper.whatInt(keyCode).toString())
            refreshActivity()
        }

        if (keyCode == 66 && !ss.FPallet.selected) {    //нет паллеты, забудь про карточку
            FExcStr.text = "Не выбрана паллета!"
            badVoise()
            return false
        } else if (keyCode == 66 && ss.FPallet.selected) {
            flagBarcode = "0"
            val gotoItem = Intent(this, ItemCard::class.java)
            gotoItem.putExtra("itemID", itm.id)
            gotoItem.putExtra("flagBarcode", flagBarcode)
            startActivity(gotoItem)
            finish()
            return true
        }

        if (keyCode == 67) {                                //чистит артикулы(введенное)
            if (searchArt.text.toString().isNotEmpty()) {
                searchArt.text = searchArt.text
                    .toString()
                    .substring(0, searchArt.text.toString().length - 1)
                refreshActivity()
            } else refreshActivity()
        }
        return false
    }
}