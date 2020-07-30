package com.intek.wpma.Model

import com.intek.wpma.SQL.SQL1S
import com.intek.wpma.SQL.SQLSynchronizer
import java.math.BigDecimal

class Model : SQLSynchronizer() {

    private val FOKEIUnit: String       = "     1   "
    private val FOKEIPack: String       = "     2   "
    private val FOKEIPackage: String    = "     E   "
    private val FOKEIKit: String        = "     A   "
    private val FOKEIOthers: String     = "     0   "
    val OKEIUnit: String get() { return FOKEIUnit }
    val OKEIPack: String get() { return FOKEIPack }
    val OKEIPackage: String get() { return FOKEIPackage }
    val OKEIKit: String get() { return FOKEIKit }
    val OKEIOthers: String get() { return FOKEIOthers }

    data class StrictDoc(
        // оставил только то, что используется в отборе, тк заполнить нужно сразу все поля, иначе структура не создастся
        val ID: String,
        var SelfRemovel: Int,
        var View: String,
        var Rows: Int,
        var FromWarehouseID: String,
        var Client: String,
        var Sum: BigDecimal,
        var Special: Boolean,
        var Box: String,
        var BoxID: String
        //var FromWarehouseName: String,
        //var ToWarehouseID: String,
        //var ToWarehouseName: String,
        //var ToWarehouseSingleAdressMode: Boolean,
        //var FoundDoc: String, // документ основание
        //var Boxes: Int,
        //var AllBoxes: Int,
        //var AdressCollect: String,
        //var Sector: String,
        //var MaxStub: Int,
        //var NumberBill: String,
        //var NumberCC: Int,
        //var MainSectorName: String,
        //var SetterName: String,
        //var IsFirstOrder: Boolean
    )

    data class Section(
        val ID: String,
        val IDD: String,
        var Type: String,
        var Descr: String
    )

    data class StructItemSet(
        // оставил только то, что используется в отборе
        val ID: String,
        var InvCode: String,
        var Name: String,
        var Price: BigDecimal,
        var Count: Int,
        var CountFact: Int,
        var AdressID: String,
        var AdressName: String,
        var CurrLine: Int,
        var Balance: Int,
        var Details: Int,
        var OKEI2Count: Int,
        var OKEI2: String,
        var OKEI2Coef: Int
    )

    data class DocCC(
        val ID: String,
        val Sector: String,
        var Rows: String,
        var TypeSetter: String, //он же наборщик
        var FlagDelivery: Int
    )



}