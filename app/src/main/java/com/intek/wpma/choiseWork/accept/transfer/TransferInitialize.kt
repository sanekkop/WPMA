package com.intek.wpma.choiseWork.accept.transfer

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color.*
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setPadding
import com.intek.wpma.Global
import com.intek.wpma.R
import com.intek.wpma.choiseWork.Menu
import kotlinx.android.synthetic.main.activity_transfer_init.*
import kotlinx.android.synthetic.main.activity_transfer_init.FExcStr
import kotlinx.android.synthetic.main.activity_transfer_init.addressGet

open class TransferInitialize : TransferMode() {

    private var currentLine0 : Int = 0
    private var currentLine1 : Int = 0
    private var selectedTab : Int = 0
    private var receivingWare :  MutableList<MutableMap<String, String>> = mutableListOf()
    private var putWare :  MutableList<MutableMap<String, String>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_init)
        title = ss.title

        FExcStr.text = ("Нажмите DEL для выбора склада!")

        btnAgree.setOnClickListener {
            if (selectedTab == 2) {
                if (outputWarehouse == "" || inputWarehouse == "") {
                    badVoice()
                    FExcStr.text = "Выберите склады!"
                    return@setOnClickListener
                }
                val nextStep = Intent(this, TransferMode::class.java)
                ss.CurrentMode = Global.Mode.TransferMode
                startActivity(nextStep)
                finish()
            }
        }

        //фигня чтобы скрол не скролился
        addressGet.setOnKeyListener { _, keyCode, event ->
            try {
                if (event.action == MotionEvent.ACTION_DOWN && ss.helper.whatDirection(keyCode) in listOf("Down", "Up")) reactionKeyLocal(keyCode)
                else if (event.action == MotionEvent.ACTION_DOWN) reactionKey(keyCode, event) else true
            } catch (e: Exception) {
                true
            }
        }
        selectWarehouseGet()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun selectWarehouseGet() {
        var lineNom0 = 0

        headInit.text = "Разнос (склад отгрузки)"

        val textQuery = "SELECT " +
                    "Warehouse.id as ID, " +
                    "Warehouse.descr as Name " +
                    "FROM " +
                    "\$Спр.Склады as Warehouse (nolock) " +
                    "WHERE " +
                    "Warehouse.ismark = 0 " +
                    "and Warehouse.\$Спр.Склады.ТипСклада = 3 " +
                    "ORDER BY " +
                    "Warehouse.descr"
        receivingWare = ss.executeWithReadNew(textQuery) ?: return

        val countLocal = receivingWare.count()
        if (countLocal != receivingWare.count() && receivingWare.isNotEmpty()) {
            //сменилось количество, обнулим текущую строку
            currentLine0 = 1
        }

        for (dr in receivingWare) {
            lineNom0++

            val recWare = TextView(this)
            val linLay = LinearLayout(this)
            recWare.apply {
                text = dr["Name"]
                setPadding(5)
                setTextColor(BLACK)
                //setBackgroundResource(R.drawable.bg)
            }
            linLay.isClickable = true
            linLay.setBackgroundColor(WHITE)
            linLay.setOnTouchListener{ _, _ ->  //выделение строки при таче
                selectedTab = 0
                var i = 0
                while (i < addressGet.childCount) {
                    if (linLay != addressGet.getChildAt(i)) {
                        if ((addressGet.getChildAt(i).background as ColorDrawable).color == LTGRAY) {
                            addressGet.getChildAt(i).setBackgroundColor(WHITE)
                        }
                    } else {
                        currentLine0 = i
                        linLay.setBackgroundColor(LTGRAY)
                    }
                    i++
                }
                true
            }
            var colorline = WHITE
            if (lineNom0 == currentLine1) {
                colorline = LTGRAY
            }
            linLay.setBackgroundColor(colorline)
            linLay.addView(recWare)
            addressGet.addView(linLay)
        }

        addressGet.getChildAt(currentLine0).setBackgroundColor(LTGRAY)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun selectWarehousePut() {
        addressGet.removeAllViewsInLayout()

        headInit.text = "Разнос (склад выкладки)"

        var lineNom1 = 0
        val textQuery = "SELECT " +
                "Warehouse.id as ID, " +
                "Warehouse.descr as Name " +
                "FROM " +
                "\$Спр.Склады as Warehouse (nolock) " +
                "WHERE " +
                "Warehouse.ismark = 0 " +
                "and Warehouse.\$Спр.Склады.ТипСклада = 0 " +
                "ORDER BY " +
                "Warehouse.descr"
        putWare = ss.executeWithReadNew(textQuery) ?: return

        val countLocal1 = putWare.count()
        if (countLocal1 != putWare.count() && putWare.isNotEmpty()) {
            //сменилось количество, обнулим текущую строку
            currentLine1 = 1
        }

        for (dr in putWare) {
            lineNom1++

            val putWar = TextView(this)
            val linearLay = LinearLayout(this)
            putWar.apply {
                text = dr["Name"]
                setPadding(5)
                setTextColor(BLACK)
                //setBackgroundResource(R.drawable.bg)
            }
            linearLay.isClickable = true
            linearLay.setOnTouchListener{ _, _ ->  //выделение строки при таче
                selectedTab = 1
                var i = 0
                while (i < addressGet.childCount) {
                    if (linearLay != addressGet.getChildAt(i)) addressGet.getChildAt(i).setBackgroundColor(WHITE)
                    else {
                        currentLine1 = i
                        linearLay.setBackgroundColor(LTGRAY)
                    }
                    i++
                }
                true
            }
            var colorline = WHITE
            if (lineNom1 == currentLine1) {
                colorline = LTGRAY
            }
            linearLay.setBackgroundColor(colorline)
            linearLay.addView(putWar)
            addressGet.addView(linearLay)
            addressGet.getChildAt(0).setBackgroundColor(LTGRAY)
        }
    }

    override fun reactionKey(keyCode : Int, event : KeyEvent?): Boolean {

        if (keyCode == 4) {
            clickVoice()
            ss.excStr = "Выберите режим работы"
            val back = Intent(this, Menu::class.java)
            startActivity(back)
            finish()
            return true
        }

        if (keyCode == 67 && selectedTab == 0) {
            tickVoice()
            selectedTab = 1
            outputWarehouse = receivingWare[currentLine0]["ID"].toString()
            Toast.makeText(this, outputWarehouse, Toast.LENGTH_SHORT).show()
            selectWarehousePut()
            return true
        }

        if (keyCode == 67 && selectedTab == 1) {
            tickVoice()
            selectedTab = 2
            inputWarehouse = putWare[currentLine1]["ID"].toString()
            Toast.makeText(this, inputWarehouse, Toast.LENGTH_SHORT).show()
            return true
        }

        if (keyCode == 66 && selectedTab == 2) {
            if (outputWarehouse == "" || inputWarehouse == "") {
                badVoice()
                FExcStr.text = "Выберите склады!"
                return false
            }
            val nextStep = Intent(this, TransferMode::class.java)
            startActivity(nextStep)
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
        tickVoice()
        when (selectedTab) {
            0 -> {
                var res = true
                if (receivingWare.isEmpty()) return false
                addressGet.getChildAt(currentLine0).setBackgroundColor(WHITE)
                if (ss.helper.whatDirection(keyCode) == "Down") {
                    if (currentLine0 < receivingWare.count() - 1) currentLine0++
                    else { currentLine0 = 0; scrollGet.fullScroll(View.FOCUS_UP) }
                } else {
                    if (currentLine0 > 0) currentLine0--
                    else {
                        currentLine0 = receivingWare.count() - 1
                        scrollGet.fullScroll(View.FOCUS_DOWN)
                    }
                }
                if (currentLine0 == (receivingWare.count() - 8)) scrollGet.fullScroll(View.FOCUS_UP)             //переход в начало
                else if (currentLine0 > (receivingWare.count() - 8)) scrollGet.fullScroll(View.FOCUS_DOWN)       //переход в конец
                else if (currentLine0 % 8 == 0) res = false
                //теперь подкрасим строку серым
                addressGet.getChildAt(currentLine0).setBackgroundColor(LTGRAY)
                return res
            }
            1 -> {
                var res = true
                if (putWare.isEmpty()) return false
                addressGet.getChildAt(currentLine1).setBackgroundColor(WHITE)
                if (ss.helper.whatDirection(keyCode) == "Down") {
                    if (currentLine1 < putWare.count() - 1) currentLine1++ else { currentLine1 = 0; scrollGet.fullScroll(View.FOCUS_UP) }
                } else {
                    if (currentLine1 > 0) currentLine1-- else { currentLine1 = putWare.count() - 1; scrollGet.fullScroll(View.FOCUS_DOWN) }
                }
                if (currentLine1 == putWare.count() - 9) scrollGet.fullScroll(View.FOCUS_UP)            //переход в начало
                else if (currentLine1 > (putWare.count() - 9)) scrollGet.fullScroll(View.FOCUS_DOWN)    //переход в конец
                else if (currentLine1 % 9 == 0) res = false
                //теперь подкрасим строку серым
                addressGet.getChildAt(currentLine1).setBackgroundColor(LTGRAY)
                return res
                }
            }
        return false
    }
}