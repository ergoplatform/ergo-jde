package jde.compiler.model

import jde.compiler.{Declaration, Dictionary}
import kiosk.ergo.{DataType, KioskType}

case class DictionaryObject(isUnresolved: Boolean, declaration: Declaration)

case class Variable(name: String, `type`: DataType.Type)

case class OnChain(name: String, var `type`: DataType.Type) extends Declaration {
  override lazy val maybeTargetId = Some(name)
  override lazy val pointerNames = Nil
  override lazy val pointerTypes = Nil
  override lazy val isLazy = true
  override def getValue(implicit dictionary: Dictionary): Multiple[KioskType[_]] = dictionary.getOnChainValue(name)
  override lazy val canPointToOnChain: Boolean = false
}
