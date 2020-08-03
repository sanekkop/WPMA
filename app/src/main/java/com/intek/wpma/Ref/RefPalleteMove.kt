package com.intek.wpma.Ref

class RefPalleteMove(): ARef() {
    override val TypeObj: String get() = "ПеремещенияПаллет"

    init {
        HaveName = false
        HaveCode = false
    }

    val Adress0: RefSection
        get() {
            return GetSectionProperty("Адрес0")
        }
    val Adress1: RefSection
        get() {
            return GetSectionProperty("Адрес1")
        }
    val TypeMove: Int
        get() {
            return GetAttribute("ТипДвижения").toString().toInt()
        }
    val PalleteBarcode: String
        get() {
            return GetAttribute("ШКПаллеты").toString()
        }
    val Pallete: String
        get() {
            if (PalleteBarcode.trim().length == 0) {
                return "(..)";
            }
            val tmp = "   " + PalleteBarcode.substring(4, 12).toInt().toString()
            return tmp.substring(tmp.length - 4, tmp.length);    //4 символа справа
        } // PalleteNumber
    val TypeMoveActionDescr: String
        get() {
            return GetTypeMoveDescr(true)
        } // get
    val TypeMoveDescr: String
        get() {
            return GetTypeMoveDescr(false)
        } // get

    fun GetSectionProperty(name: String): RefSection {
        var result = RefSection()
        val currId = GetAttribute(name).toString()
        result.FoundID(currId);
        return result
    } // GetGatesProperty

    fun GetTypeMoveDescr(thisAction: Boolean): String {
        when (TypeMove) {
            1 -> return if (thisAction) "Спустите паллету" else "Спуск с адреса"
            2 -> return if (thisAction) "Возвратите паллету" else "Подъем"
            3 -> return if (thisAction) "Снимите паллету" else "Спуск с антрисоли"
            4 -> return if (thisAction) "Перевезите к лифту" else "Транспорт к лифту"
            5 -> return if (thisAction) "Поднимите паллету" else "Подъем лифт"
        }
        return "(неизвестно)"
    } // GetTypeMoveDescr
}
