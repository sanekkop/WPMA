package com.intek.wpma.ChoiseWork.Accept

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
import kotlinx.android.synthetic.main.activity_yap_item.FExcStr
import kotlinx.android.synthetic.main.activity_yap_item.table

class YapItem : Search() {


    override fun onCreate(savedInstanceState: Bundle?) {
        ss.CurrentMode = Global.Mode.AcceptanceAccepted
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yap_item)

        title = ss.title

/*
        if (currBtn.Name === "btnPrint" || currBtn.Name === "btnPrintCondition") {
            lblAction.Text = "Команда печати в обработке, подождите..."
            Refresh()
            SS.PrintLabels(if (currBtn.Name === "btnPrint") false else true)
            if (Screan === 1) {
                pnlCurrent.MoveControls(2 * pnlCurrent.Width, 0)
                Screan = -1
                pnlCurrent.GetControlByName("tbFind").Focus()
            }
            View()
            lblAction.Text = SS.ExcStr
        }
        break*/

        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x > oldx) {
                    ss.CurrentMode = Global.Mode.Waiting
                    val backHead = Intent(this, Search::class.java)
                    startActivity(backHead)
                    finish()
                }
            }
            return true
        })

        kolEtik.setOnKeyListener { v: View, keyCode: Int, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                if (ss.isMobile){  //спрячем клаву
                    val inputManager: InputMethodManager =  applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(this.currentFocus!!.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS)
                }
                // сохраняем текст, введенный до нажатия Enter в переменную
                try {
                    val count = kolEtik.text.toString().toInt()
                    //places = count
                    kolEtik.visibility = View.INVISIBLE
                } catch (e: Exception) {
                }
            }
            false
        }

        refreshActivity()
    }

    override fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        if (keyCode == 4){
            ss.CurrentMode = Global.Mode.Waiting
            val acBack = Intent(this, Search::class.java)
            startActivity(acBack)
            finish()
            return true
        }

        if (ss.helper.whatDirection(keyCode) == "Left") {
            ss.CurrentMode = Global.Mode.Waiting
            val backHead = Intent(this, Search::class.java)
            startActivity(backHead)
            finish()
            return true
        }
        return false
    }

    override fun refreshActivity() {

        super.refreshActivity()

        val linearLayout1 = LinearLayout(this)
        val rowTitle1 = TableRow(this)

        //добавим столбцы
        val num = TextView(this)
        num.text = "№"
        num.typeface = Typeface.SERIF
        num.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.05).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        num.gravity = Gravity.CENTER
        num.textSize = 18F
        num.setTextColor(-0x1000000)
        val doc = TextView(this)
        doc.text = "Накл."
        doc.typeface = Typeface.SERIF
        doc.gravity = Gravity.CENTER
        doc.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.15).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        doc.textSize = 18F
        doc.setTextColor(-0x1000000)
        val invkod = TextView(this)
        invkod.text = "Инв.Код"
        invkod.typeface = Typeface.SERIF
        invkod.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.15).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        invkod.gravity = Gravity.CENTER
        invkod.textSize = 18F
        invkod.setTextColor(-0x1000000)
        val nameItem = TextView(this)
        nameItem.text = "Наим."
        nameItem.typeface = Typeface.SERIF
        nameItem.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.25).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        nameItem.gravity = Gravity.CENTER
        nameItem.textSize = 18F
        nameItem.setTextColor(-0x1000000)
        val countItem = TextView(this)
        countItem.text = "Кол-во"
        countItem.typeface = Typeface.SERIF
        countItem.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.15).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        countItem.gravity = Gravity.CENTER
        countItem.textSize = 18F
        countItem.setTextColor(-0x1000000)
        val kof = TextView(this)
        kof.text = "Коэф."
        kof.typeface = Typeface.SERIF
        kof.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.1).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        kof.gravity = Gravity.CENTER
        kof.textSize = 18F
        kof.setTextColor(-0x1000000)
        val etik = TextView(this)
        etik.text = "Этик."
        etik.typeface = Typeface.SERIF
        etik.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.15).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        etik.gravity = Gravity.CENTER
        etik.textSize = 18F
        etik.setTextColor(-0x1000000)

        linearLayout1.addView(num)
        linearLayout1.addView(doc)
        linearLayout1.addView(invkod)
        linearLayout1.addView(nameItem)
        linearLayout1.addView(countItem)
        linearLayout1.addView(kof)
        linearLayout1.addView(etik)

        rowTitle1.addView(linearLayout1)
        rowTitle1.setBackgroundColor(Color.GRAY)
        table.addView(rowTitle1)

        if (acceptedItems.isNotEmpty()) {

            for (DR in acceptedItems) {

                val linearLayout2 = LinearLayout(this)
                val rowTitle2 = TableRow(this)

                //добавим столбцы
                val numBB = TextView(this)
                numBB.text = DR["Number"]
                numBB.typeface = Typeface.SERIF
                numBB.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.05).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                numBB.gravity = Gravity.CENTER
                numBB.textSize = 18F
                numBB.setTextColor(-0x1000000)
                val docUU = TextView(this)
                docUU.text = DR["DOCNO"]
                docUU.typeface = Typeface.SERIF
                docUU.gravity = Gravity.CENTER
                docUU.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.15).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                docUU.textSize = 18F
                docUU.setTextColor(-0x1000000)
                val invCode = TextView(this)
                invCode.text = DR["InvCode"]
                invCode.typeface = Typeface.SERIF
                invCode.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.15).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                invCode.gravity = Gravity.CENTER
                invCode.textSize = 18F
                invCode.setTextColor(-0x1000000)

                val itemName = TextView(this)
                itemName.text = DR["ItemName"].toString().substring(0,7)
                itemName.typeface = Typeface.SERIF
                itemName.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.25).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                itemName.gravity = Gravity.CENTER
                itemName.textSize = 18F
                itemName.setTextColor(-0x1000000)
                val countItem = TextView(this)
                countItem.text = DR["Count"]
                countItem.typeface = Typeface.SERIF
                countItem.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.15).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                countItem.gravity = Gravity.CENTER
                countItem.textSize = 18F
                countItem.setTextColor(-0x1000000)
                val koef = TextView(this)
                koef.text = ss.helper.byeTheNull(DR["Coef"].toString()) //обрежем нулики и точку
                koef.typeface = Typeface.SERIF
                koef.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.1).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                koef.gravity = Gravity.CENTER
                koef.textSize = 18F
                koef.setTextColor(-0x1000000)
                val etiks = TextView(this)
                etiks.text = DR["LabelCount"]
                etiks.typeface = Typeface.SERIF
                etiks.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.15).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                etiks.gravity = Gravity.CENTER
                etiks.textSize = 18F
                etiks.setTextColor(-0x1000000)

                linearLayout2.addView(numBB)
                linearLayout2.addView(docUU)
                linearLayout2.addView(invCode)
                linearLayout2.addView(itemName)
                linearLayout2.addView(countItem)
                linearLayout2.addView(koef)
                linearLayout2.addView(etiks)

                rowTitle2.addView(linearLayout2)
                rowTitle2.setBackgroundColor(Color.WHITE)
                table.addView(rowTitle2)
            }
        }
    }

}