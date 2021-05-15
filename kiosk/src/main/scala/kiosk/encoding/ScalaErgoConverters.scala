package kiosk.encoding

import java.math.BigInteger
import kiosk.ergo._
import kiosk.script.ScriptUtil
import org.ergoplatform.ErgoAddress
import sigmastate.Values.{ConstantNode, ErgoTree}
import sigmastate._
import sigmastate.eval.{bigIntToBigInteger, _}
import sigmastate.interpreter.CryptoConstants.EcPointType
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import sigmastate.serialization.{GroupElementSerializer, ValueSerializer}
import special.collection.Coll
import special.sigma.{BigInt, GroupElement}

import scala.util.Try
object ScalaErgoConverters {

  def stringToGroupElement(hex: String): GroupElement = {
    val groupElement: EcPointType = GroupElementSerializer.parse(hex.decodeHex)
    groupElement
  }

  def groupElementToString(groupElement: GroupElement): String = KioskGroupElement(groupElement).toString

  def stringToErgoTree(hex: String): ErgoTree = DefaultSerializer.deserializeErgoTree(hex.decodeHex)

  def ergoTreeToString(tree: ErgoTree): String = KioskErgoTree(tree).hex

  def getAddressFromErgoTree(ergoTree: ErgoTree) = ScriptUtil.ergoAddressEncoder.fromProposition(ergoTree).get

  def getStringFromAddress(ergoAddress: ErgoAddress): String = ScriptUtil.ergoAddressEncoder.toString(ergoAddress)

  def getAddressFromString(string: String) =
    Try(ScriptUtil.ergoAddressEncoder.fromString(string).get).getOrElse(throw new Exception(s"Invalid address [$string]"))

  def deserialize(hex: String): KioskType[_] = {
    val bytes = hex.decodeHex
    val value: Values.Value[SType] = ValueSerializer.deserialize(bytes)

    value match {
      case ConstantNode(g, SGroupElement) => KioskGroupElement(g.asInstanceOf[GroupElement])
      case ConstantNode(i, SBigInt) =>
        val bigInteger: BigInteger = i.asInstanceOf[BigInt]
        KioskBigInt(scala.BigInt(bigInteger))
      case ConstantNode(l, SLong) => KioskLong(l.asInstanceOf[Long])
      case ConstantNode(c, _: SCollection[_]) =>
        KioskCollByte(c.asInstanceOf[Coll[Byte]].toArray) // ToDo: handle other collections such as Coll[Coll[Byte]]
      case ConstantNode(i, SInt) => KioskInt(i.asInstanceOf[Int])
      case any                   => throw new Exception(s"Unsupported encoded data $hex (decoded as $any)")
    }
  }
}
