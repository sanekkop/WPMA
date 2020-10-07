package com.intek.wpma.Ref

import com.intek.wpma.SQL.SQL1S

abstract class ARef {

    var ss: SQL1S = SQL1S
    var fID: String = ""
    var fName: String = ""
    private var fCode: String = ""
    private var fIsMark: Boolean = false

    var haveName: Boolean = false
    var haveCode: Boolean = false

    private var attributes: MutableMap<String, Any> = mutableMapOf()

    protected abstract val typeObj: String

    val id: String get() = fID
    val name: String get() = fName
    val code: String get() = fCode
    val selected: Boolean get() = fID != ""
    val isMark: Boolean get() = fIsMark


    private fun foundIDDorID(IDDorID: String, ThisID: Boolean): Boolean {
        fID = ""
        val prefix = "Спр.$typeObj."
        var dataMap: MutableMap<String, Any> = mutableMapOf()
        val fieldList: MutableList<String> = mutableListOf()
        fieldList.add("ID")
        fieldList.add("ISMARK")
        if (haveName) {
            fieldList.add("DESCR")
        }
        if (haveCode) {
            fieldList.add("CODE")
        }
        if (!ThisID) {
            fieldList.add(prefix + "IDD")
        }
        val servCount: Int = fieldList.count()    //Количество сервисных полей
        ss.addKnownAttributes(prefix, fieldList)
        dataMap = ss.getSCData(IDDorID, typeObj, fieldList, dataMap, ThisID) ?: return false
        fID = dataMap["ID"].toString()
        fIsMark = (dataMap["ISMARK"].toString() == "1")
        fCode = if (haveCode) dataMap["CODE"].toString() else ""
        fName = if (haveName) dataMap["DESCR"].toString().trim() else ""
        //Добавляем оставшиеся поля в словарик
        var i: Int = servCount + 1
        while (i < fieldList.count()) {
            attributes[fieldList[i].substring(prefix.length)] = dataMap[fieldList[i]]!!
            i++
        }
        return true
    }

    fun foundIDD(IDD: String): Boolean {
        return foundIDDorID(IDD, false)
    }

    fun foundID(ID: String): Boolean {
        return foundIDDorID(ID, true)
    }

    fun getAttribute(Name: String): Any {
        //Классная штука ниже, но опасная, может что-то наебнуться. Неохота думать
        //if (!Selected)
        //{
        //    throw new NullReferenceException("Reference element not selected");
        //}
        if (attributes.containsKey(Name)) {
            return attributes[Name]!!
        }
        //Подгружаем недостающий атрибут (он добавится в карту соответствия и будет в дальнейшем подгружаться сразу)
        var dataMap: MutableMap<String, Any> = mutableMapOf()
        dataMap = ss.getSCData(fID, typeObj, Name, dataMap, true)!!
        val result = dataMap["Спр.$typeObj.$Name"]
        attributes[Name] = result!!
        return result
    }

    fun getGatesProperty(name:String):RefGates    {
        val result = RefGates()
        val currId = getAttribute(name).toString()
        result.foundID(currId)
        return result
    } // GetGatesProperty

    open fun refresh() {
        if (selected) {
            foundIDDorID(id, true)
        }
    }


}
