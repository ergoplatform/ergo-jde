package jde

import jde.compiler.model.{Constant, Multiple}
import kiosk.ergo.{DataType, KioskType}

import java.util.UUID
import scala.util.Try

package object compiler {

  def height(actualHeight: Int) = Constant("HEIGHT", DataType.Int, Some(actualHeight.toString), values = None)

  def randId = UUID.randomUUID.toString

  def noGapsInIndices(sorted: Seq[(Int, _)]): Boolean = sorted.map(_._1).zipWithIndex.forall { case (int, index) => int == index }

  def getMultiPairs(first: String, second: String)(implicit dictionary: Dictionary): Multiple[(KioskType[_], KioskType[_])] = {
    Try(dictionary.getDeclaration(first).getValue zip dictionary.getDeclaration(second).getValue).fold(
      ex => throw new Exception(s"Error pairing $first and $second").initCause(ex),
      pairs => pairs
    )
  }

  def to[B](kioskTypes: Multiple[KioskType[_]]): Multiple[B] = kioskTypes.map(_.asInstanceOf[B])

  def to[B](kioskTypes: Seq[KioskType[_]]): Seq[B] = kioskTypes.map(_.asInstanceOf[B])

  type T = (Option[_], String)

  def exactlyOne(obj: Any)(names: String*)(options: Option[_]*): Unit =
    if (options.count(_.isDefined) != 1) throw new Exception(s"Exactly one of {${names.toSeq.reduceLeft(_ + "," + _)}} must be defined in $obj")

  def atLeastOne(obj: Any)(names: String*)(options: Option[_]*): Unit =
    if (options.count(_.isDefined) == 0) throw new Exception(s"At least one of {${names.toSeq.reduceLeft(_ + "," + _)}} must be defined in $obj")

  def optSeq[B](s: Option[Seq[B]]): Seq[B] = s.toSeq.flatten

  def requireEmpty(data: T*): Unit = {
    data.foreach {
      case (opt, message) => if (opt.isDefined) throw new Exception(s"$message cannot be defined: ${opt.get}")
    }
  }
  def requireDefined(data: T*): Unit = {
    data.foreach {
      case (opt, message) => if (opt.isEmpty) throw new Exception(s"$message cannot be empty")
    }
  }
}
