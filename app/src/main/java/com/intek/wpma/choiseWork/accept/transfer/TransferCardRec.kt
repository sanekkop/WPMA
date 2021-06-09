package com.intek.wpma.choiseWork.accept.transfer

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import com.intek.wpma.Global
import com.intek.wpma.R
import kotlinx.android.synthetic.main.activity_transfer_rec.*

class TransferCardRec : TransferCard() {

    private var itemID = ""
    private var count = 0
    private var transferRec : MutableMap<String, String> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_rec)
        ss.CurrentMode = Global.Mode.TransferYep
        itemID = intent.extras?.getString("itemID").toString()
        count = intent.extras?.getInt("count").toString().toInt()
        transferRec = transferItem

        itemName1.text = transferRec["InvCode"].toString()
        FExcStr1.text = ("НА ПОЛКУ! \n Отсканируйте адрес!")
        //полное наименование товара
        itemName1.text = transferRec["Name"]
        //код товара
        shapka1.text = (transferRec["InvCode"].toString() + " Рекомендуемый адрес")
        //зоны
        zonaHand1.text = (transferItem["AdressMain"].toString()
            .trim() + ": " + transferItem["BalanceMain"].toString() + " шт")
        zonaTech1.text = (transferItem["AdressBuffer"].toString()
            .trim() + ": " + transferItem["BalanceBuffer"].toString() + " шт")

        checkAddress()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == 4) {
            val backTel = Intent(this, TransferCard::class.java)
            clickVoice()
            backTel.putExtra("itemID", itemID)
            backTel.putExtra("count", count)
            startActivity(backTel)
            finish()
            return true
        }
        if (ss.helper.whatDirection(keyCode) == "Right"){
            val backTel = Intent(this, TransferCard::class.java)
            clickVoice()
            backTel.putExtra("itemID", itemID)
            backTel.putExtra("count", count)
            startActivity(backTel)
            finish()
            return true
        }
        return false
    }
}