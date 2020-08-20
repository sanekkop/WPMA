package com.intek.wpma.Ref

import com.intek.wpma.SQL.SQL1S


class Doc
{
    protected var ss: SQL1S = SQL1S
    private var fRowCount = 0
    private var fID = ""
    private var headerAttributes: MutableMap<String,String> = mutableMapOf()  //Атрибуты ШАПКИ, известные на данный момент.
    private var commonAttributes: MutableMap<String,String> = mutableMapOf() //Атрибуты общие, из таблицы _1sjourn
    private var fModified = false

    val id: String get() {return fID}
    val idd: String get() {return commonAttributes["IDD"].toString()}
    val typeDoc: String get() {return commonAttributes["IDDOCDEF"].toString()}
    val isMark: Boolean get() {return commonAttributes["ISMARK"].toString() == "1"}
    val dateDoc: String get() {return commonAttributes["DATE_TIME_IDDOC"].toString()}
    val numberDoc: String get() {return commonAttributes["DOCNO"].toString()}
    val selected: Boolean get() {return fID != ""}
    val view: String get() {return commonAttributes["IDDOCDEF"].toString()+ " " + commonAttributes["DOCNO"].toString() + " (" +  commonAttributes["DATE_TIME_IDDOC"].toString() + ")"}
    val rowCount: Int get() {
        checkSelect()
        var textQuery =
        "select count(*) row_count " +
                "from DT$" + commonAttributes["IDDOCDEF"].toString() + " (nolock) " +
                "where iddoc = :iddoc "
        textQuery = ss.querySetParam(textQuery, "iddoc", id)
        val dt = ss.executeWithReadNew(textQuery) ?: return fRowCount   //не срослось что-то, вернем сохраненное значение
        fRowCount = dt[0]["row_count"].toString().toInt()
        return fRowCount
    } // RowCount

    val modified: Boolean get() {return fModified}
    fun giveDocById(id:String): Doc? {
        val result = Doc()
        if (!result.foundID(id))
        {
            return null
        }
        return result
    }

    private fun checkSelect() {
        if (!selected)
        {
            try {
                throw NullPointerException("Document not selected")
            }
            catch (e: NullPointerException) {
            }
        }
    } // CheckSelect
    private fun foundIDDorID(iddorID:String, thisID:Boolean):Boolean {
        commonAttributes = ss.getDoc(iddorID, thisID) ?: return false
        fID = commonAttributes["ID"].toString()
        fModified = false
        return true
    } // FoundIDDorID

    fun foundIDD(idd:String):Boolean {
        headerAttributes.clear()   //при перепозиционировании все очищается
        return foundIDDorID(idd, false)
    } // FoundIDD
   fun foundID(id:String):Boolean    {
       headerAttributes.clear()   //при перепозиционировании все очищается
       return foundIDDorID(id, true)
    } // FoundID
    fun getAttributeHeader(name:String):String  {
        checkSelect()
        if (headerAttributes.containsKey(name))
        {
            return headerAttributes[name].toString()
        }
        headerAttributes["$typeDoc.$name"] = ""
        refresh()
        return headerAttributes["$typeDoc.$name"].toString()
    } // GetAttributeHeader
    fun SetAttributeHeader(name:String, value:String) {
        checkSelect()
        headerAttributes[typeDoc + "." + name] = value
        fModified = true
    } // SetAttributeHeader
    fun refresh() {
        checkSelect()
        fModified = false
        //ПОДСОСЕМ ИЗ ЖУРНАЛА
        foundIDDorID(id, true)

        //ПОДСОСЕМ ИЗ ШАПКИ
        //формируем список атрибутов шапки
        if (headerAttributes.isNotEmpty())
        {
            val fieldList:MutableList<String> = mutableListOf()
            for (pair in headerAttributes)
            {
                fieldList.add(pair.key)
            }
            headerAttributes = ss.getDocData(id,typeDoc,fieldList) ?: return
        }
    } // Refresh
   fun save():Boolean {
        checkSelect()
        if (!modified)
        {
            return true    //документ не был изменен
        }
        var textQuery =
        "update DH$" + typeDoc + " set "
        for (pair in headerAttributes)
        {
            textQuery += "$" + pair.key + " = :param, "
            textQuery = ss.querySetParam(textQuery, "param", pair.value)
        }
        //режим последнюю запятую (с пробелом)
        textQuery = textQuery.substring(0, textQuery.length - 2)
        textQuery += " where iddoc = :iddoc"
        textQuery = ss.querySetParam(textQuery, "iddoc", id)
        return ss.executeWithoutRead(textQuery)
    } // Save
}