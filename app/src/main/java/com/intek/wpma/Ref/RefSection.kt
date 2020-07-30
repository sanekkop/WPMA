package com.intek.wpma.Ref

import com.intek.wpma.SQL.SQL1S



class RefSection(): ARef() {
    override val TypeObj: String get() = ("Секции")

    init {
        HaveName    = true
        HaveCode    = false
    }

}