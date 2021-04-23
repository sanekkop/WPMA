package com.intek.wpma.ChoiseWork.Shipping

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import com.intek.wpma.*
import kotlinx.android.synthetic.main.activity_new_complectation.*
import kotlinx.android.synthetic.main.activity_show_route.*
import kotlinx.android.synthetic.main.activity_show_route.FExcStr
import kotlinx.android.synthetic.main.activity_show_route.Shapka
import kotlinx.android.synthetic.main.activity_show_route.btnCansel
import kotlinx.android.synthetic.main.activity_show_route.btnKey1
import kotlinx.android.synthetic.main.activity_show_route.btnScan

class ShowRoute: NewComplectation() {

    /*
    //region шапка с необходимыми функциями для работы сканеров перехватчиков кнопок и т.д.
    override val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования
                    try {
                        barcode = intent.getStringExtra("data")
                        reactionBarcode(barcode)
                    } catch (e: Exception) {
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
        if (scanRes != null) {
            try {
                 barcode = scanRes.toString()
                 codeId = scanCodeId.toString()
                 reactionBarcode(barcode)
            } catch (e: Exception) {
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
        scanRes = null
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


 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_route)
        title = ss.title
        ss.CurrentMode == Global.Mode.ShowRoute
        //docDown["ID"] = intent.extras?.getString("docDownID").toString()
        //docDown["Sector"] = intent.extras?.getString("docDownSector").toString()
        //ss.CurrentMode = Global.Mode.ShowRoute
        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@ShowRoute, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "NewComplectation")
                startActivity(scanAct)
            }
        }
        btnCansel.setOnClickListener {
            if (preMode == Global.Mode.NewComplectation) {
                repealNewComplectation()
            } else {
                if (needAdressComplete != ss.getVoidID()) {
                    FExcStr.text = "Адрес полон, фиксирую..."
                    if (!adressFull()) {
                        lastGoodAdress = ""
                        docDown = mutableMapOf()
                        needAdressComplete = ss.getVoidID()
                        toModeNewComplectationComplete()
                    }
                }
            }

        }
        btnKey1.setOnClickListener {
            if (preMode == Global.Mode.NewComplectation) {
                remain =
                    docDown["AllBoxes"].toString().toInt() - docDown["Boxes"].toString().toInt()
                if (docDown["MaxStub"].toString().toInt() <= remain) {
                    //Можно завершить
                    FExcStr.text = ("Закрываю остальные $remain места...")
                    endCCNewComp()
                    refreshActivity()
                }
                return@setOnClickListener
            }

            if (lastGoodAdress != "") {
                //Можно завершить
                FExcStr.text = "Комплектую остальные..."
                if (completeAll()) {
                    goodVoise()
                } else {
                    badVoise()
                }
                refreshActivity()
            }
        }

        var oldx = 0F
        FExcStr.setOnTouchListener(fun(_: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                return true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                   ss.CurrentMode = preMode
                   finish()
                }
            }
            return true
        })
        refreshRoute()
    }
    override fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        if (ss.helper.whatDirection(keyCode) == "Left") {
            clickVoise()
            ss.CurrentMode = preMode
            finish()
            return true
        }
        else if (ss.helper.whatDirection(keyCode) == "Right") {
            clickVoise()
            return true
        }
        return super.reactionKey(keyCode, event)
    }
    override fun refreshActivity() {

        table.removeAllViewsInLayout()
        if (preMode == Global.Mode.NewComplectation) {
            btnKey1.visibility = if (docDown["MaxStub"].toString()
                    .toInt() <= remain
            ) View.VISIBLE else View.INVISIBLE
            btnKey1.text = "Все"
        } else if (preMode == Global.Mode.NewComplectationComplete) {
            btnKey1.visibility = if (lastGoodAdress == "") View.INVISIBLE else View.VISIBLE
            btnKey1.text = ("TAB - Все")
            btnCansel.visibility = View.INVISIBLE
            btnCansel.isEnabled = false
            btnCansel.text = ("DEL - ПОЛОН")
            if (needAdressComplete != ss.getVoidID()) {
                btnCansel.visibility = View.VISIBLE
                btnCansel.isEnabled = true
            }
        }

        var cvet = Color.rgb(192, 192, 192)
        Shapka.text =
            ("""Комплектация в ${if (preMode == Global.Mode.NewComplectation) "тележку" else "адрес"} (новая)""")
        var row = TableRow(this)
        var linearLayout = LinearLayout(this)

        var gate = TextView(this)
        gate.text = "Вр"
        gate.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.1).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        gate.gravity = Gravity.CENTER_HORIZONTAL
        gate.textSize = 20F
        gate.setTextColor(Color.BLACK)

        var num = TextView(this)
        num.text = "Заявка"
        num.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.25).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        num.gravity = Gravity.CENTER_HORIZONTAL
        num.textSize = 20F
        num.setTextColor(Color.BLACK)

        var sector = TextView(this)
        sector.text = "Лист"
        sector.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.15).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        sector.gravity = Gravity.START
        sector.textSize = 20F
        sector.setTextColor(Color.BLACK)

        var count = TextView(this)
        count.text = "М"
        count.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.1).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        count.gravity = Gravity.START
        count.textSize = 20F
        count.setTextColor(Color.BLACK)

        var address = TextView(this)
        address.text = "Адрес"
        address.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.4).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        address.gravity = Gravity.START
        address.textSize = 20F
        address.setTextColor(Color.BLACK)


        linearLayout.setPadding(3, 3, 3, 3)
        linearLayout.addView(gate)
        linearLayout.addView(num)
        linearLayout.addView(sector)
        linearLayout.addView(count)
        linearLayout.addView(address)

        row.setBackgroundColor(Color.LTGRAY)
        row.addView(linearLayout)
        table.addView(row)

        if (ccrp.isEmpty() and ccrpOld.isEmpty()) return

        for (dr in ccrp) {
            linearLayout = LinearLayout(this)
            row = TableRow(this)

            gate = TextView(this)
            gate.text = dr["Gate"].toString().trim()
            gate.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.1).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gate.gravity = Gravity.CENTER_HORIZONTAL
            gate.textSize = 20F
            gate.setTextColor(Color.BLACK)

            num = TextView(this)
            num.text = dr["Bill"]
            num.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.25).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            num.gravity = Gravity.CENTER_HORIZONTAL
            num.textSize = 20F
            num.setTextColor(Color.BLACK)

            sector = TextView(this)
            sector.text = dr["CC"]
            sector.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.15).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            sector.gravity = Gravity.START
            sector.textSize = 20F
            sector.setTextColor(Color.BLACK)

            count = TextView(this)
            count.text = dr["Boxes"]
            count.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.1).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            count.gravity = Gravity.START
            count.textSize = 20F
            count.setTextColor(Color.BLACK)

            address = TextView(this)
            address.text = dr["Adress"]
            address.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.4).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            address.gravity = Gravity.START
            address.textSize = 20F
            address.setTextColor(Color.BLACK)


            linearLayout.setPadding(3, 3, 3, 3)
            linearLayout.addView(gate)
            linearLayout.addView(num)
            linearLayout.addView(sector)
            linearLayout.addView(count)
            linearLayout.addView(address)

            row.addView(linearLayout)

            table.addView(row)

        }
        for (dr in ccrpOld) {
            linearLayout = LinearLayout(this)
            row = TableRow(this)

            gate = TextView(this)
            gate.text = dr["Gate"].toString().trim()
            gate.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.1).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gate.gravity = Gravity.CENTER_HORIZONTAL
            gate.textSize = 20F
            gate.setTextColor(Color.LTGRAY)

            num = TextView(this)
            num.text = dr["Bill"]
            num.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.25).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            num.gravity = Gravity.CENTER_HORIZONTAL
            num.textSize = 20F
            num.setTextColor(Color.LTGRAY)

            sector = TextView(this)
            sector.text = dr["CC"]
            sector.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.15).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            sector.gravity = Gravity.START
            sector.textSize = 20F
            sector.setTextColor(Color.LTGRAY)

            count = TextView(this)
            count.text = dr["Boxes"]
            count.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.1).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            count.gravity = Gravity.START
            count.textSize = 20F
            count.setTextColor(Color.LTGRAY)

            address = TextView(this)
            address.text = dr["Adress"]
            address.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.4).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            address.gravity = Gravity.START
            address.textSize = 20F
            address.setTextColor(Color.LTGRAY)


            linearLayout.setPadding(3, 3, 3, 3)
            linearLayout.addView(gate)
            linearLayout.addView(num)
            linearLayout.addView(sector)
            linearLayout.addView(count)
            linearLayout.addView(address)

            row.addView(linearLayout)

            table.addView(row)

        }

    }

}