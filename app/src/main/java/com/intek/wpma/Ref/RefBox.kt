package com.intek.wpma.Ref

class RefBox(): ARef() {
    override val TypeObj: String get() = "МестаПогрузки"

    init {
        HaveName    = false
        HaveCode    = false
    }

}