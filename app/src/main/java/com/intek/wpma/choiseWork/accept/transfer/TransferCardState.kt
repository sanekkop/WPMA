package com.intek.wpma.choiseWork.accept.transfer

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import com.intek.wpma.Global
import com.intek.wpma.R
import kotlinx.android.synthetic.main.activity_transfer_state.*

class TransferCardState : TransferCard() {

    private var itemID = ""
    private var count = 0
    private var transferState : MutableMap<String, String> = mutableMapOf()
    private val stringArr : Array<String> = arrayOf("Адрес", "Состояние", "Кол-во")
    private val widthArr : Array<Double> = arrayOf(0.33, 0.33, 0.33)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_state)
        ss.CurrentMode = Global.Mode.TransferYep
        itemID = intent.extras?.getString("itemID").toString()
        count = intent.extras?.getInt("count").toString().toInt()
        transferState = transferItem

        itemName0.text = transferState["InvCode"].toString()
        FExcStr0.text = ("НА ПОЛКУ! \n Отсканируйте адрес!")
        //полное наименование товара
        itemName0.text = transferState["Name"]
        //код товара
        shapka0.text = (transferState["InvCode"].toString() + " Состояние")
        //зоны
        zonaHand0.text = (transferItem["AdressMain"].toString()
            .trim() + ": " + transferItem["BalanceMain"].toString() + " шт")
        zonaTech0.text = (transferItem["AdressBuffer"].toString()
            .trim() + ": " + transferItem["BalanceBuffer"].toString() + " шт")

        customTable(this, 2, stringArr, widthArr, stateTab, "head")

        stateBuffer()
    }

    private fun stateBuffer() {
        var textQuery =
            "DECLARE @curdate DateTime; " +
                    "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
                    "SELECT " +
                    "min(Section.descr) as Adress, " +
                    "CASE " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = -10 THEN '-10 Автокорректировка' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = -2 THEN '-2 В излишке' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = -1 THEN '-1 В излишке (пересчет)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 0 THEN '00 Не существует' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 1 THEN '01 Приемка' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 2 THEN '02 Хороший на месте' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 3 THEN '03 Хороший (пересчет)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 4 THEN '04 Хороший (движение)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 7 THEN '07 Бракованный на месте' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 8 THEN '08 Бракованный (пересчет)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 9 THEN '09 Бракованный (движение)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 12 THEN '12 Недостача' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 13 THEN '13 Недостача (пересчет)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 14 THEN '14 Недостача (движение)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 17 THEN '17 Недостача подтвержденная' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 18 THEN '18 Недостача подт.(пересчет)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 19 THEN '19 Недостача подт.(движение)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 22 THEN '22 Пересорт излишек' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 23 THEN '23 Пересорт недостача' " +
                    "ELSE rtrim(cast(RegAOT.\$Рег.АдресОстаткиТоваров.Состояние as char)) + ' <неизвестное состояние>' END as Condition, " +
                    "cast(sum(RegAOT.\$Рег.АдресОстаткиТоваров.Количество ) as int) as Count " +
                    "FROM " +
                    "RG\$Рег.АдресОстаткиТоваров as RegAOT (nolock) " +
                    "LEFT JOIN \$Спр.Секции as Section (nolock) " +
                    "ON Section.id = RegAOT.\$Рег.АдресОстаткиТоваров.Адрес " +
                    "WHERE " +
                    "RegAOT.period = @curdate " +
                    "and RegAOT.\$Рег.АдресОстаткиТоваров.Товар = :ItemID " +
                    "and RegAOT.\$Рег.АдресОстаткиТоваров.Склад = :Warehouse " +
                    "GROUP BY " +
                    "RegAOT.\$Рег.АдресОстаткиТоваров.Адрес , " +
                    "RegAOT.\$Рег.АдресОстаткиТоваров.Товар , " +
                    "RegAOT.\$Рег.АдресОстаткиТоваров.Состояние " +
                    "HAVING sum(RegAOT.\$Рег.АдресОстаткиТоваров.Количество ) <> 0 " +
                    "ORDER BY Adress, Condition"
        textQuery = ss.querySetParam(textQuery, "ItemID", itm.id)
        textQuery = ss.querySetParam(textQuery, "Warehouse", ss.Const.mainWarehouse)
        val statTab = ss.executeWithReadNew(textQuery) ?: return

        for (dr in statTab) {
            val stringArray = arrayOf(
                dr["Adress"].toString(),
                dr["Condition"].toString(),
                dr["Count"].toString()
            )
        customTable(this, 2, stringArray, widthArr, stateTab, "body")
        }
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
        if (ss.helper.whatDirection(keyCode) == "Left"){
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