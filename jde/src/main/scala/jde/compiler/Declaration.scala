package jde.compiler

import jde.compiler.model.{Multiple, Variable}
import kiosk.ergo.{DataType, KioskType}

trait Declaration {
  var `type`: DataType.Type

  // the name of the object declared. Option because not every Declaration needs a name, only those that must be referenced in another Declaration
  protected val maybeTargetId: Option[String]
  protected val pointerNames: Seq[String] // names of the other Declarations referenced by this
  protected val pointerTypes: Seq[DataType.Type] // types of the other Declarations referenced by this

  val isLazy: Boolean

  val canPointToOnChain: Boolean

  lazy val targetId = maybeTargetId.getOrElse(randId)

  lazy val isOnChain = maybeTargetId.isDefined && canPointToOnChain

  lazy val onChainVariable: Option[Variable] = if (isOnChain) Some(Variable(randId, `type`)) else None

  lazy val pointers: Seq[Variable] = (pointerNames zip pointerTypes).map {
    case (pointerName, pointerType) => new Variable(pointerName, pointerType)
  } ++ onChainVariable

  def updateType(newType: DataType.Type) = `type` = newType

  def getValue(implicit dictionary: Dictionary): Multiple[KioskType[_]] =
    if (isOnChain) dictionary.getOnChainValue(onChainVariable.get.name) else dictionary.getDeclaration(pointers.head.name).getValue

  override def toString = s"${maybeTargetId.getOrElse("unnamed")}: ${`type`}"
}
