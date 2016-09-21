//   Copyright 2014 Commonwealth Bank of Australia
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package au.com.cba.omnia.humbug

import com.twitter.scrooge.frontend._
import com.twitter.scrooge.ast._

import scalaz._, Scalaz._

object Generator {
  def generateDoc(doc: Document): Seq[(String, String)] = {
    val pck = doc.namespace("scala").cata(
      x => s"package ${x.toLowerCase.fullName}\n",
      ""
    )

    doc.structs.collect { case s: Struct => generateStruct(s, doc.namespace("scala")) }
  }

  def generateStruct(struct: Struct, namespace: Option[Identifier]): (String, String) = {
    val className = struct.sid.toTitleCase.fullName
    val fields    = struct.fields

    val pck = namespace.cata(
      x => s"package ${x.toLowerCase.fullName}",
      ""
    )
    val path = namespace.cata(_.toLowerCase.fullName.replaceAll("\\.", "/") + s"/$className.scala", s"$className.scala")
 
    path -> s"""$pck

import scala.collection.Map
import scala.collection.immutable.{Map => _}
import scala.collection.mutable.{ArrayBuffer, HashMap}

import org.apache.thrift.protocol.{TProtocol, TProtocolException, TStruct, TField, TType, TList, TMap}

import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec3}

import au.com.cba.omnia.humbug.HumbugThriftStruct

class $className  extends ThriftStruct with Product with java.io.Serializable with HumbugThriftStruct {
  import $className._

  ${fields.map(generateFieldMembers).mkString("\n  ")}

  override def write(_oprot: TProtocol): Unit = {
    _oprot.writeStructBegin(new TStruct("$className"))
${fields.map(generateFieldWriteAccess).mkString("\n")}
    _oprot.writeFieldStop()
    _oprot.writeStructEnd()
  }

  def productArity: Int = ${fields.length}

  def productElement(n: Int): Any = n match {
    ${fields.map(generateProductElement).mkString("\n    ")}
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }

  override def equals(other: Any) = other match {
    case null => false
    case that: $className =>
      List(${fields.map(f => "this." + varName(f)).mkString(", ")}) == List(${fields.map(f => "that." + varName(f)).mkString(", ")})
    case _ => false
  }

  override def canEqual(other: Any) = other.isInstanceOf[$className]

  override def hashCode(): Int = scala.util.hashing.MurmurHash3.productHash(this)

  override def toString: String = "$className(" + ${fields.map(generateToString)}.mkString(", ") + ")"
}

object $className extends ThriftStructCodec3[$className] {
  val struct = new TStruct("$className")
  ${fields.map(generateFieldVals).mkString("\n  ")}

  override def encode(_item: $className, _oproto: TProtocol): Unit =
    _item.write(_oproto)

  override def decode(_iprot: TProtocol): $className = {
    val item  = new $className()
    var _done = false

    _iprot.readStructBegin()
    while (!_done) {
      val _field = _iprot.readFieldBegin()
      if (_field.`type` == TType.STOP) {
        _done = true
      } else {
        _field.id match {
${fields.map(generateFieldReadAccess).mkString("\n")}
        }
        _iprot.readFieldEnd()
      }
    }   
    _iprot.readStructEnd()

    item
  }

${fields.map(f => generateFieldWriteCode(f) + "\n" + generateFieldReadCode(f)).mkString("\n\n")}

  def ttypeToHuman(byte: Byte) = {
    // from https://github.com/apache/thrift/blob/master/lib/java/src/org/apache/thrift/protocol/TType.java
    byte match {
      case TType.STOP   => "STOP"
      case TType.VOID   => "VOID"
      case TType.BOOL   => "BOOL"
      case TType.BYTE   => "BYTE"
      case TType.DOUBLE => "DOUBLE"
      case TType.I16    => "I16"
      case TType.I32    => "I32"
      case TType.I64    => "I64"
      case TType.STRING => "STRING"
      case TType.STRUCT => "STRUCT"
      case TType.MAP    => "MAP"
      case TType.SET    => "SET"
      case TType.LIST   => "LIST"
      case TType.ENUM   => "ENUM"
      case _            => "UNKNOWN"
    }
  }
}
"""
  }

  def generateFieldMembers(field: Field) = {
    val name = varName(field)
    val typ  = scalaType(field)
    s"""var $name: $typ = ${defaultValue(field)}
  def _${field.index}: $typ = $name
  def _${field.index}_=(x: $typ): Unit = $name = x
"""
  }

  def generateFieldWriteAccess(field: Field) = field.fieldType match {
    case TByte | TI16 | TI32 | TI64 | TDouble | TBool =>
      s"""    ${writeFieldName(field)}(${varName(field)}, _oprot)"""
    case _ =>
      s"""    if (${varName(field)} ne null) ${writeFieldName(field)}(${varName(field)}, _oprot)"""
  }

  def generateFieldReadAccess(field: Field) = {
    val idx = field.index
    s"          case $idx => item._${idx} = ${readFieldName(field)}(_field.`type`, _iprot)"
  }

  def generateFieldWriteCode(field: Field) = {
    s"  private def ${writeFieldName(field)}(item: ${scalaType(field)}, _oprot: TProtocol): Unit = " +
    wrapWhen(optional(field), s =>
      "item match {\n"               +
      "   case None       => {}\n"   +
     s"   case Some(item) => $s\n" +
      "   }\n"
    )( 
        "{\n" +
     s"""    _oprot.writeFieldBegin(new TField("${field.originalName}", TType.${constType(field.fieldType)},${field.index}))\n""" +
       s"    ${generateElementWriteCode("item", field.fieldType)}\n" +
        "    _oprot.writeFieldEnd()"
    ) + "  }"
  }

  def generateElementWriteCode(name: String, typ: FieldType): String = typ match {
    case TByte | TI16 | TI32 | TI64 | TDouble | TBool | TString =>
      s"_oprot.${writeThriftName(typ)}($name)"
    case ListType(eltType, _) =>
      val innerWrite = generateElementWriteCode("_elem", eltType)
      s"""
      _oprot.writeListBegin(new TList(TType.${constType(eltType)}, ${name}.size))
      ${name}.foreach { _elem => $innerWrite }
      _oprot.writeListEnd()
      """
    case MapType(keyType, valType, _) =>
      val keyWrite = generateElementWriteCode("_key", keyType)
      val valWrite = generateElementWriteCode("_val", valType)
      s"""
       _oprot.writeMapBegin(new TMap(TType.${constType(keyType)}, TType.${constType(valType)}, ${name}.size))
       $name.foreach { case (_key, _val) => 
         $keyWrite
         $valWrite
       }
       _oprot.writeMapEnd()
      """
    case t => throw new Exception(s"Unsupported field type: $t")
  }

  def generateFieldReadCode(field: Field) = {
    val ttype = "TType." + constType(field.fieldType)
    val read  = wrapWhen(optional(field), s => s"Option($s)")(generateElementReadCode(field.fieldType))
    
    s"""  private def ${readFieldName(field)}(_typ: Byte, _iprot: TProtocol): ${scalaType(field)} = _typ match {
    case $ttype => $read
    case _actualType => throw new TProtocolException(
      "Received wrong type for field ${field.originalName} (expected=$ttype, actual=%s).".format(
        ttypeToHuman(_actualType)
      )
    )}"""
  }

  def generateElementReadCode(typ: FieldType): String = typ match {
    case TByte | TI16 | TI32 | TI64 | TDouble | TBool | TString =>
      s"_iprot.${readThriftName(typ)}"
    case ListType(eltType, _) =>
      // Follow same  behaviour as scrooge which is why we use mutable data structures.
      s"""
      val _list = _iprot.readListBegin()
      if (_list.size == 0) {
        _iprot.readListEnd()
        Nil
      } else {
        val _rv = new ArrayBuffer[${rawType(eltType)}](_list.size)
        var _i  = 0
        while (_i < _list.size) {
          _rv += { ${generateElementReadCode(eltType)} }
          _i  += 1
        }
        _iprot.readListEnd()
        _rv
      }
      """
    case MapType(keyType, valType, _) =>
      // Follow same  behaviour as scrooge which is why we use mutable data structures.
      val kt = rawType(keyType)
      val vt = rawType(valType)
      s"""
      val _map = _iprot.readMapBegin()
      if (_map.size == 0) {
        _iprot.readMapEnd()
        Map.empty[$kt, $vt]
      } else {
        val _rv = new HashMap[$kt, $vt]
        var _i = 0
        while (_i < _map.size) {
          val _key   = { ${generateElementReadCode(keyType)} }
          val _value = { ${generateElementReadCode(valType)} }
          _rv(_key) = _value
          _i += 1
        }
        _iprot.readMapEnd()
        _rv
      }
      """
    case t => throw new Exception(s"Unsupported field type: $t")
  }

  def generateProductElement(field: Field) = 
    s"case ${field.index - 1} => _${field.index}"

  def generateEquals(field: Field) = s"this.${varName(field)} == that.${varName(field)}"
  def generateToString(field: Field) = s"this.${varName(field)}"

  def generateFieldVals(field: Field) = {
    val ttype        = "TType." + constType(field.fieldType)
    val origName     = field.originalName
    val name         = field.sid.toTitleCase.fullName +  "Field"
    val manifestName = name +  "Manifest"
    val typ          = rawType(field.fieldType)

    s"""val $name = new TField("$origName", $ttype, ${field.index})
  val $manifestName = implicitly[Manifest[$typ]]"""
  }

  def varName(field: Field) = field.sid.toCamelCase.fullName
  def writeFieldName(field: Field) = "write" + field.sid.toTitleCase.fullName
  def readFieldName(field: Field) = "read" + field.sid.toTitleCase.fullName
  def optional(field: Field) = field.requiredness.isOptional

  def scalaType(field: Field) = {
    val typ = rawType(field.fieldType)
    wrapWhen(optional(field), s => s"Option[$s]")(typ)
  }

  def rawType(typ: FieldType): String = typ match {
    case TBool                        => "Boolean"
    case TByte                        => "Byte"
    case TI16                         => "Short"
    case TI32                         => "Int"
    case TI64                         => "Long"
    case TDouble                      => "Double"
    case TString                      => "String"
    case ListType(elemType, _)        => s"Seq[${rawType(elemType)}]"
    case MapType(keyType, valType, _) => s"Map[${rawType(keyType)}, ${rawType(valType)}]"
    case t                            => throw new Exception(s"Unsupported field type: $t")
  }

  def constType(typ: FieldType) = typ match {
    case TBool            => "BOOL"
    case TByte            => "BYTE"
    case TI16             => "I16"
    case TI32             => "I32"
    case TI64             => "I64"
    case TDouble          => "DOUBLE"
    case TString          => "STRING"
    case ListType(_, _)   => "LIST"
    case MapType(_, _, _) => "MAP"
    case t       => throw new Exception(s"Unsupported field type: $t")
  }

  def defaultValue(field: Field) = {
    val default = field.fieldType match {
      case TBool => "false"
      case TByte | TI16 | TI32 | TI64 => "0"
      case TDouble => "0.0"
      case _ => "null"
    }

    if (optional(field)) "None" else default
  }

  def readThriftName(typ: FieldType)  = "read" + typ.toString.tail
  def writeThriftName(typ: FieldType) = "write" + typ.toString.tail

  def wrapWhen(cond: Boolean, wrapper: String => String)(content: String): String =
    if (cond) wrapper(content) else content
}
