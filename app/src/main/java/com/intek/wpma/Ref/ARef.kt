package com.intek.wpma.Ref

import android.os.Build.ID
import com.intek.wpma.SQL.SQL1S
import java.util.*
import java.util.jar.Attributes

abstract class ARef() {

    protected var SS: SQL1S = SQL1S
    protected var FID:String = ""
    protected var FName:String = ""
    protected var FCode:String = ""
    protected var FIsMark:Boolean = false

    protected var HaveName:Boolean = false
    protected var HaveCode:Boolean = false

    private var Attributes: MutableMap<String,Any> = mutableMapOf()

    protected abstract val TypeObj:String get

    val ID:String get() = FID
    val Name:String get() = FName
    val Code:String get() = FCode
    val Selected:Boolean get() = FID != ""
    val IsMark:Boolean get() = FIsMark


   private fun FoundIDDorID (IDDorID: String,ThisID: Boolean): Boolean
    {
        FID = ""
        val prefix = "Спр." + TypeObj + "."
        var DataMap: MutableMap<String, Any> = mutableMapOf()
        var FieldList: MutableList<String>
        FieldList = mutableListOf()
        FieldList.add("ID")
        FieldList.add("ISMARK")
        if (HaveName)
        {
            FieldList.add("DESCR");
        }
        if (HaveCode)
        {
            FieldList.add("CODE");
        }
        if (!ThisID)
        {
            FieldList.add(prefix + "IDD");
        }
        val ServCount: Int = FieldList.count()    //Количество сервисных полей
        SS.AddKnownAttributes(prefix, FieldList)
        DataMap = SS.GetSCData(IDDorID,TypeObj,FieldList, DataMap,ThisID)!!
        FID = DataMap["ID"].toString()
        FIsMark = (DataMap["ISMARK"].toString() == "1")
        FCode = if (HaveCode) DataMap["CODE"].toString() else ""
        FName = if (HaveName) DataMap["DESCR"].toString().trim() else ""
        //Добавляем оставшиеся поля в словарик
        var i:Int = ServCount + 1
        while (i < FieldList.count())
        {
            Attributes.put(FieldList[i].toString().substring(prefix.length),DataMap[FieldList[i]]!!)
            i++
        }
        return true
    }

    public fun FoundIDD(IDD:String ):Boolean
    {
        return FoundIDDorID(IDD, false);
    }
    public fun FoundID(ID:String): Boolean {
        return FoundIDDorID(ID, true);
    }

    fun GetAttribute(Name:String):Any
    {
        //Классная штука ниже, но опасная, может что-то наебнуться. Неохота думать
        //if (!Selected)
        //{
        //    throw new NullReferenceException("Reference element not selected");
        //}
        if (Attributes.containsKey(Name))
        {
            return Attributes.get(Name)!!
        }
        //Подгружаем недостающий атрибут (он добавится в карту соответствия и будет в дальнейшем подгружаться сразу)
        var DataMap: MutableMap<String, Any> = mutableMapOf()
        DataMap = SS.GetSCData(FID,TypeObj,Name, DataMap,true)!!
        val result = DataMap["Спр." + TypeObj + "." + Name];
        Attributes.put(Name, result!!)
        return result;
    }

    open fun Refresh()
    {
        if (Selected)
        {
            FoundIDDorID(ID, true);
        }
    }

/*
    //---------------
    //Две процедуры ниже нужно переработать как-то в одну, я хз как пока

    /// <summary>
    /// write in you property: %Prop% get { return GetSectionProperty(%name%, ref %FProp%); }
    /// </summary>
    /// <param name="name">1C name of prop</param>
    /// <param name="val">field stored object</param>
    /// <returns>value of property</returns>
    protected RefSection GetSectionProperty(string name, ref RefSection val)
    {
        if (val == null)
        {
            val = new RefSection(SS);
        }
        string currId = GetAttribute(name).ToString();
        if (val.ID != currId)
        {
            val.FoundID(currId);
        }
        return val;
    } // GetGatesProperty
    /// <summary>
    /// write in you property: %Prop% get { return GetSectionProperty(%name%, ref %FProp%); }
    /// </summary>
    /// <param name="name">1C name of prop</param>
    /// <param name="val">field stored object</param>
    /// <returns>value of property</returns>
    protected RefGates GetGatesProperty(string name, ref RefGates val)
    {
        if (val == null)
        {
            val = new RefGates(SS);
        }
        string currId = GetAttribute(name).ToString();
        if (val.ID != currId)
        {
            val.FoundID(currId);
        }
        return val;
    } // GetGatesProperty
    */

}
