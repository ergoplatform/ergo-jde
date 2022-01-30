package kiosk.ergo

import kiosk.encoding.ScalaErgoConverters
import org.bouncycastle.util.encoders.Hex
import org.ergoplatform.appkit._
import scorex.crypto.authds.ADDigest
import sigmastate.Values.{AvlTreeConstant, BooleanConstant, ByteArrayConstant, CollectionConstant, ErgoTree}
import sigmastate.eval.SigmaDsl
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer
import sigmastate.serialization.ValueSerializer
import sigmastate.{AvlTreeData, AvlTreeFlags, SGroupElement}
import special.collection.Coll
import special.sigma
import special.sigma.{AvlTree, GroupElement}

class BetterString(string: String) {
  def decodeHex = Hex.decode(string)
}

class BetterByteArray(bytes: Seq[Byte]) {
  def encodeHex: String = Hex.toHexString(bytes.toArray).toLowerCase
}

sealed trait KioskType[T] {
  val serialize: Array[Byte]
  val value: T
  lazy val hex = serialize.encodeHex
  def getErgoValue: ErgoValue[_]

  val typeName: String
  override def toString = value.toString
}

case class KioskCollByte(arrayBytes: Array[Byte]) extends KioskType[Coll[Byte]] {
  override val value: Coll[Byte] = sigmastate.eval.Colls.fromArray(arrayBytes)
  override val serialize: Array[Byte] = ValueSerializer.serialize(ByteArrayConstant(value))
  lazy val stringValue = arrayBytes.encodeHex
  override def toString: String = stringValue
  override val typeName: String = "Coll[Byte]"
  override def getErgoValue: ErgoValue[Coll[Byte]] = ErgoValue.of(arrayBytes)
}

case class KioskAvlTree(digest: Array[Byte], keyLength: Int, valueLengthOpt: Option[Int] = None) extends KioskType[AvlTree] {
  def this(avlTree: AvlTree) = this(avlTree.digest.toArray, avlTree.keyLength, avlTree.valueLengthOpt)
  private lazy val avlTreeData = AvlTreeData(ADDigest @@ digest, AvlTreeFlags.AllOperationsAllowed, keyLength, valueLengthOpt)
  override val value: AvlTree = sigmastate.eval.avlTreeDataToAvlTree(avlTreeData)
  override lazy val serialize: Array[Byte] = ValueSerializer.serialize(AvlTreeConstant(value))
  override val typeName: String = "AvlTree"
  override lazy val toString = serialize.encodeHex
  override def getErgoValue: ErgoValue[AvlTree] = ErgoValue.of(avlTreeData)
}

case class KioskBoolean(value: Boolean) extends KioskType[Boolean] {
  private lazy val booleanConstant = BooleanConstant(value)
  override val serialize: Array[Byte] = ValueSerializer.serialize(booleanConstant)
  override val typeName: String = "Boolean"
  override def getErgoValue = ErgoValue.fromHex(serialize.encodeHex)
}

case class KioskCollGroupElement(groupElements: Array[GroupElement]) extends KioskType[Coll[GroupElement]] {
  override val value: Coll[GroupElement] = sigmastate.eval.Colls.fromArray(groupElements)
  override val serialize: Array[Byte] = ValueSerializer.serialize(CollectionConstant[SGroupElement.type](value, SGroupElement))
  override def toString: String = "[" + groupElements.map(_.getEncoded.toArray.encodeHex).reduceLeft(_ + "," + _) + "]"
  override val typeName: String = "Coll[GroupElement]"
  override def getErgoValue: ErgoValue[Coll[GroupElement]] = ErgoValue.of(groupElements, ErgoType.groupElementType)
}

case class KioskInt(value: Int) extends KioskType[Int] {
  override val serialize: Array[Byte] = ValueSerializer.serialize(value)
  override val typeName: String = "Int"
  override def getErgoValue = ErgoValue.of(value)
}

case class KioskLong(value: Long) extends KioskType[Long] {
  override val serialize: Array[Byte] = ValueSerializer.serialize(value)
  override val typeName: String = "Long"
  override def getErgoValue = ErgoValue.of(value)
}

case class KioskBigInt(bigInt: BigInt) extends KioskType[sigma.BigInt] {
  override val value: sigma.BigInt = SigmaDsl.BigInt(bigInt.bigInteger)
  override val serialize: Array[Byte] = ValueSerializer.serialize(value)
  override val typeName: String = "BigInt"
  override def toString: String = bigInt.toString(10)
  override def getErgoValue = ErgoValue.of(bigInt.bigInteger)
}

case class KioskGroupElement(value: GroupElement) extends KioskType[GroupElement] {
  override val serialize: Array[Byte] = ValueSerializer.serialize(value)
  override def toString: String = value.getEncoded.toArray.encodeHex
  override val typeName: String = "GroupElement"
  override def getErgoValue = ErgoValue.of(value)
  def +(that: KioskGroupElement) = KioskGroupElement(value.multiply(that.value))
}

case class KioskErgoTree(value: ErgoTree) extends KioskType[ErgoTree] {
  override val serialize: Array[Byte] = DefaultSerializer.serializeErgoTree(value)
  override val typeName: String = "ErgoTree"

  override def getErgoValue = ??? // should never be needed
  lazy val address = tree2str(this)
  override def toString: ID = s"ErgoTree of $address"
}

case class DhtData(g: GroupElement, h: GroupElement, u: GroupElement, v: GroupElement, x: BigInt)

case class KioskBox(
    address: String,
    value: Long,
    registers: Array[KioskType[_]],
    tokens: Tokens,
    optBoxId: Option[String] = None,
    spentTxId: Option[String] = None,
    creationHeight: Option[Int] = None
) {
  def toOutBox(implicit ctx: BlockchainContext): OutBox = {
    ctx
      .newTxBuilder()
      .outBoxBuilder
      .value(value)
      .tokens(tokens.map(token => new ErgoToken(token._1, token._2)): _*)
      .contract(ctx.newContract(ScalaErgoConverters.getAddressFromString(address).script))
      .creationHeight(creationHeight.getOrElse(ctx.getHeight))
      .registers(registers.map(register => register.getErgoValue): _*)
      .build()
  }
  def toInBox(txId: String, txIndex: Short)(implicit ctx: BlockchainContext): InputBox = {
    val outBox = toOutBox
    outBox.convertToInputWith(txId, txIndex)
  }
}

abstract class MyEnum extends Enumeration {
  def fromString(str: String): Value =
    values
      .find(value => value.toString.equalsIgnoreCase(str))
      .getOrElse(throw new Exception(s"Invalid op $str. Permitted options are ${values.map(_.toString).reduceLeft(_ + ", " + _)}"))
  def toString(op: Value): String = op.toString
}
