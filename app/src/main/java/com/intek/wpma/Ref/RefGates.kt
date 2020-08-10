package com.intek.wpma.Ref

class RefGates : ARef() {
    override val typeObj: String get() = "Ворота"

    val firstAdress: RefSection get() {

            if (!selected) {
                return RefSection()
            }
            var textQuery = "select dbo.fn_GetFirstAdressZone(:zona) as id"
            textQuery = ss.querySetParam(textQuery, "zona", id)
        val dt = ss.executeWithReadNew(textQuery) ?: return RefSection()
        if (dt.isEmpty()) {
                //Нет строк, ну и что, бывает. Значит нет ни одного адреса с этой зоной
                return RefSection()
            }
            val result = RefSection()
            result.foundID(dt[0]["id"].toString())
            return result

        } // FirstAdress
    val lastAdress: RefSection get() {
        if (!selected) {
            return RefSection()
        }
        var textQuery = "select dbo.fn_GetLastAdressZone(:zona) as id"
        textQuery = ss.querySetParam(textQuery, "zona", id)
        val dt = ss.executeWithReadNew(textQuery) ?: return RefSection()
        if (dt.isEmpty()) {
            //Нет строк, ну и что, бывает. Значит нет ни одного адреса с этой зоной
            return RefSection()
        }
        val result = RefSection()
        result.foundID(dt[0]["id"].toString())
        return result
    } // LastAdress
    val ranges:MutableList<MutableMap<String,String>> get() {

        val result: MutableList<MutableMap<String, String>> = mutableListOf()
        if (!selected) {
            return result
        }
        var textQuery =
            "select " +
                    "ref.descr name, " +
                    // это оригинал, при котором зона выводится списком её кусков "(select top 1 $Спр.Секции.ЗонаАдресов from $Спр.Секции (nolock) where descr > ref.descr and isfolder = 2 and ismark = 0 order by descr) nextZone " +
                    "(select top 1 \$Спр.Секции.ЗонаАдресов from \$Спр.Секции as ref2 (nolock) where ref2.descr > ref.descr and ref2.isfolder = 2 and ref2.ismark = 0 and ref2.\$Спр.Секции.ЗонаАдресов = :zone  order by descr) nextZone " +  //это изменённая строка при которой выводятся только начало и конец зоны
                    "from \$Спр.Секции as ref (nolock) " +
                    "where " +
                    "ref.ismark = 0 " +
                    "and ref.\$Спр.Секции.ЗонаАдресов = :zone " +
                    "order by ref.descr"
        textQuery = ss.querySetParam(textQuery, "zone", id)
        val dt = ss.executeWithReadNew(textQuery)
        if (dt == null || dt.isEmpty()) {
            return result
        }

        var inside = false
        val row: MutableMap<String, String> = mutableMapOf()
        for (i in 0..dt.count()) {
            if (!inside) {
                //начало диапазона
                row["First"] = dt[i]["name"].toString()
                inside = true
            }

            val nextZonde = dt[i]["nextZone"].toString()
            if (nextZonde != id) {
                //Конец диапазона
                row["Last"] = dt[i]["name"].toString()
                result.add(row)
                var row: MutableMap<String, String> = mutableMapOf()
                inside = false
            }
        }
        return result
    }  // Ranges
    init {
        haveName    = true
        haveCode    = true
    }

}