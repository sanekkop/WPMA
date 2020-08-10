package com.intek.wpma.Ref

class RefBox : ARef() {
    override val typeObj: String get() = "МестаПогрузки"

    init {
        haveName    = false
        haveCode    = false
    }

}