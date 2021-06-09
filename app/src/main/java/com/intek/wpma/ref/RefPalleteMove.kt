package com.intek.wpma.ref

class RefPalleteMove : ARef() {
    override val typeObj: String get() = "ПеремещенияПаллет"

    init {
        haveName = false
        haveCode = false
    }

    val adress0: RefSection
        get() {
            return getSectionProperty("Адрес0")
        }
    val adress1: RefSection
        get() {
            return getSectionProperty("Адрес1")
        }
    private val typeMove: Int
        get() {
            return getAttribute("ТипДвижения").toString().toInt()
        }
    val palleteBarcode: String
        get() {
            return getAttribute("ШКПаллеты").toString()
        }
    val pallete: String
        get() {
            if (palleteBarcode.trim().isEmpty()) {
                return "(..)"
            }
            val tmp = "   " + palleteBarcode.substring(4, 12).toInt().toString()
            return tmp.substring(tmp.length - 4, tmp.length)    //4 символа справа
        } // PalleteNumber
    val typeMoveActionDescr: String
        get() {
            return getTypeMoveDescr(true)
        } // get
    val typeMoveDescr: String
        get() {
            return getTypeMoveDescr(false)
        } // get

    private fun getSectionProperty(name: String): RefSection {
        val result = RefSection()
        val currId = getAttribute(name).toString()
        result.foundID(currId)
        return result
    } // GetGatesProperty

    private fun getTypeMoveDescr(thisAction: Boolean): String {
        when (typeMove) {
            1 -> return if (thisAction) "Спустите паллету" else "Спуск с адреса"
            2 -> return if (thisAction) "Возвратите паллету" else "Подъем"
            3 -> return if (thisAction) "Снимите паллету" else "Спуск с антрисоли"
            4 -> return if (thisAction) "Перевезите к лифту" else "Транспорт к лифту"
            5 -> return if (thisAction) "Поднимите паллету" else "Подъем лифт"
        }
        return "(неизвестно)"
    } // GetTypeMoveDescr
}
