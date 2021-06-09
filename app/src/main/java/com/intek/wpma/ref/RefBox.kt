package com.intek.wpma.ref

class RefBox : ARef() {
    override val typeObj: String get() = "МестаПогрузки"

    init {
        haveName    = false
        haveCode    = false
    }

}