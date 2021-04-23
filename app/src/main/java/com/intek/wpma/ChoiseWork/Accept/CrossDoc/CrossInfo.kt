package com.intek.wpma.ChoiseWork.Accept.CrossDoc

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import com.intek.wpma.R
import kotlinx.android.synthetic.main.activity_cross_info.*

class CrossInfo : CrossDoc() {
    private var currentLine : Int = 1
    private var idDocItm = ""
    private val widArr : Array<Double> = arrayOf(0.1, 0.7, 0.2)
    private val strArr : Array<String> = arrayOf("№", "Наименования", "Кол.")
    private var noneAccItemLocal1: MutableList<MutableMap<String, String>> = mutableListOf()
    private var print = ""
    private var clientCheck = ""

    //region шапка с необходимыми функциями для работы сканеров перехватчиков кнопок и т.д.
    override fun onResume() {
        super.onResume()
        onWindowFocusChanged(true)
        Log.d("IntentApiSample: ", "onResume")
    }
    override fun onPause() {
        super.onPause()
        Log.d("IntentApiSample: ", "onPause")
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (reactionKey(keyCode, event)) true else super.onKeyDown(keyCode, event)
    }
    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
        var iddoc: String = ""
        var parentIDD : String = ""
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cross_info)
        clientCheck = intent.extras?.getString("cliName", clientCheck)!!
        print = intent.extras?.getString("print", print)!!
        parentIDD = intent.extras!!.getString("parentIDD")!!
        printPall.text = print.trim()

        //вытащим данные нужные нам
        noneAccItemLocal1.addAll(noneAccItem)

        //фигня чтобы скрол не скролился
        tableCross.setOnKeyListener { _, keyCode, event ->
            try {
                if (event.action == MotionEvent.ACTION_DOWN && ss.helper.whatDirection(keyCode) in listOf("Down", "Up")) reactionKeyLocal(keyCode)
                else if (event.action == MotionEvent.ACTION_DOWN) reactionKey(keyCode, event)
                else true
            } catch (e: Exception) {
                true
            }
        }

        //позиционируемся на первом товаре
        if (noneAccItemLocal1.isNotEmpty()) {
            idDocItm = noneAccItemLocal1[0]["iddoc"].toString()
        }
        tableCross()
    }

    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables")
    private fun tableCross() {
        var lineNom = 0

        val hatRow = TableRow(this)
        val hatLinearLayout = LinearLayout(this)
        val hatVal : MutableMap<String, TextView> = HashMap()
        for (i in 0..2) hatVal["hatVal$i"] = TextView(this)
        var k = 0

        for((i,_) in hatVal) {
            hatVal[i]?.apply {
                text = (" " + strArr[k])
                layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * widArr[k]).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.START
               // background = getDrawable(R.drawable.cell_border)
                textSize = 18F
                setTextColor(-0x1000000)
            }
            hatLinearLayout.addView(hatVal[i])
            k++
        }
        hatLinearLayout.setBackgroundColor(Color.rgb(192, 192, 192))
        hatRow.addView(hatLinearLayout)
        tableCross.addView(hatRow)

        val countLocal = noneAccItemLocal1.count()
        if (countLocal != noneAccItemLocal1.count() && noneAccItemLocal1.isNotEmpty()) {
            //сменилось количество, обнулим текущую строку
            currentLine = 1
            idDocItm = noneAccItemLocal1[0]["iddoc"].toString()
        }

        if (noneAccItemLocal1.isNotEmpty()) {

            for (DR in noneAccItemLocal1) {
                lineNom++

                if (DR["ClientName"].toString().trim() != clientCheck) continue

                val bodyRow = TableRow(this)
                val linearLayout1 = LinearLayout(this)
                bodyRow.isClickable = true
                bodyRow.setOnTouchListener{ _, _ ->  //выделение строки при таче
                    var i = 1
                    while (i < tableCross.childCount) {
                        if (bodyRow != tableCross.getChildAt(i)) {
                            if ((tableCross.getChildAt(i).background as ColorDrawable).color == Color.LTGRAY) {
                                tableCross.getChildAt(i).setBackgroundColor(Color.WHITE)
                            }
                        } else {
                            currentLine = i
                            idDocItm = DR["iddoc"].toString().trim()
                            bodyRow.setBackgroundColor(Color.LTGRAY)
                        }
                        i++
                    }
                    true
                }
                //добавим столбцы
                val stringArr : Array<String> = arrayOf(
                    DR["Number"].toString(),
                    DR["ItemName"].toString().trim(),
                    DR["Count"].toString()
                )
                headClient.text = clientCheck //DR["ClientName"].toString().trim()
                val bodyVal : MutableMap<String, TextView> = HashMap()
                for (i in 0..2) bodyVal["bodyVal$i"] = TextView(this)
                var s = 0

                for ((p,_) in bodyVal) {
                    bodyVal[p]?.apply {
                        text = (" " + stringArr[s])
                        typeface = Typeface.SERIF
                        layoutParams = LinearLayout.LayoutParams(
                            (ss.widthDisplay * widArr[s]).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        if (stringArr[s] == DR["ItemName"].toString().trim()) background = getDrawable(R.drawable.cell_border)
                        gravity = Gravity.START
                        textSize = 18F
                        setTextColor(-0x1000000)
                    }
                    linearLayout1.background = getDrawable(R.drawable.cell_border)
                    linearLayout1.addView(bodyVal[p])
                    s++
                }

                var colorline =  Color.WHITE
                if (lineNom == currentLine) {
                    colorline = Color.LTGRAY
                }
                bodyRow.setBackgroundColor(colorline)
                bodyRow.addView(linearLayout1)
                tableCross.addView(bodyRow)
            }
        }
    }

    override fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {
        if (ss.helper.whatDirection(keyCode) == "Right" || keyCode == 4) {
            clickVoise()
            val backAcc = Intent(this, CrossNonItem::class.java)
            backAcc.putExtra("parentIDD", parentIDD)
            startActivity(backAcc)
            finish()
            return true
        }

        if (ss.helper.whatDirection(keyCode) in listOf("Down", "Up")) {
            //на случай кода дошли до конца экрана
            reactionKeyLocal(keyCode)
        }
        return false
    }

    private fun reactionKeyLocal(keyCode: Int):Boolean {
        tickVoise()
        var res = true
        if (noneAccItemLocal1.isEmpty()) return false

        //тут частенько вылетает сделаем через попытку
        val oldCurrentLine = currentLine
        tableCross.getChildAt(currentLine).setBackgroundColor(Color.WHITE)
        if (ss.helper.whatDirection(keyCode) == "Down") {
            if (currentLine < noneAccItemLocal1.count()) currentLine++
            else currentLine = 1
        } else {
            if (currentLine > 1) currentLine--
            else currentLine = noneAccItemLocal1.count()
        }

        if (noneAccItemLocal1.count() >= 15) {
            if ((oldCurrentLine >= 15 && currentLine == 1) || currentLine < 8) {
                //переход в начало
                scroll.fullScroll(View.FOCUS_UP)
            } else if ((oldCurrentLine == 1 && currentLine >= 15) || currentLine > (noneAccItemLocal1.count() - 8)) {
                //переход в конец
                scroll.fullScroll(View.FOCUS_DOWN)
            } else if (currentLine % 8 == 0) res = false
        }


        idDocItm = noneAccItemLocal1[currentLine - 1]["iddoc"].toString()
        //теперь подкрасим строку серым
        tableCross.getChildAt(currentLine).setBackgroundColor(Color.LTGRAY)
        return res
    }
}