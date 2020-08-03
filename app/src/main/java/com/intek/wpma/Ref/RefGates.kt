package com.intek.wpma.Ref

class RefGates (): ARef() {
    override val TypeObj: String get() = "Ворота"

    val FirstAdress: RefSection get() {

            if (!Selected) {
                return RefSection()
            }
            var textQuery = "select dbo.fn_GetFirstAdressZone(:zona) as id"
            textQuery = SS.QuerySetParam(textQuery, "zona", ID);
            val DT = SS.ExecuteWithReadNew(textQuery)
            if (DT == null)
            {
                return RefSection()
            }
            if (DT.isEmpty()) {
                //Нет строк, ну и что, бывает. Значит нет ни одного адреса с этой зоной
                return RefSection()
            }
            val result = RefSection()
            result.FoundID(DT[0]["id"].toString())
            return result

        } // FirstAdress
    val LastAdress: RefSection get() {
        if (!Selected) {
            return RefSection();
        }
        var textQuery = "select dbo.fn_GetLastAdressZone(:zona) as id"
        textQuery = SS.QuerySetParam(textQuery, "zona", ID);
        val DT = SS.ExecuteWithReadNew(textQuery)
        if (DT == null) {
            return RefSection()
        }
        if (DT.isEmpty()) {
            //Нет строк, ну и что, бывает. Значит нет ни одного адреса с этой зоной
            return RefSection()
        }
        val result = RefSection()
        result.FoundID(DT[0]["id"].toString())
        return result
    } // LastAdress
    val Ranges:MutableList<MutableMap<String,String>> get() {

        var Result: MutableList<MutableMap<String, String>> = mutableListOf()
        if (!Selected) {
            return Result
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
                    "order by ref.descr";
        textQuery = SS.QuerySetParam(textQuery, "zone", ID)
        val DT = SS.ExecuteWithReadNew(textQuery)
        if (DT == null || DT.isEmpty()) {
            return Result
        }

        var inside = false
        var row: MutableMap<String, String> = mutableMapOf()
        for (i in 0..DT.count()) {
            if (!inside) {
                //начало диапазона
                row.put("First", DT[i]["name"].toString())
                inside = true
            }

            val nextZonde = DT[i]["nextZone"].toString()
            if (nextZonde != ID) {
                //Конец диапазона
                row.put("Last", DT[i]["name"].toString())
                Result.add(row)
                var row: MutableMap<String, String> = mutableMapOf()
                inside = false;
            }
        }
        return Result
    }  // Ranges
    init {
        HaveName    = true
        HaveCode    = true
    }

}