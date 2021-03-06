package kiosk.ergo

import kiosk.encoding.ScalaErgoConverters

// Used in JDE
object DataType extends MyEnum {
  type Type = Value
  val Long, Int, Boolean, GroupElement, CollByte, ErgoTree, Address, BigInt, Unknown = Value

  def getValue(stringValue: String, `type`: DataType.Type): KioskType[_] = {
    `type` match {
      case Long         => KioskLong(stringValue.toLong)
      case Int          => KioskInt(stringValue.toInt)
      case Boolean      => KioskBoolean(stringValue.toBoolean)
      case GroupElement => KioskGroupElement(ScalaErgoConverters.stringToGroupElement(stringValue))
      case CollByte     => KioskCollByte(stringValue.decodeHex)
      case ErgoTree     => KioskErgoTree(ScalaErgoConverters.stringToErgoTree(stringValue))
      case Address      => KioskErgoTree(ScalaErgoConverters.getAddressFromString(stringValue).script)
      case BigInt       => KioskBigInt(scala.BigInt(stringValue))
      case any          => throw new Exception(s"Unknown type $any")
    }
  }

  def isValid(value: KioskType[_], `type`: DataType.Type) = {
    (`type`, value) match {
      case (Long, _: KioskLong)                 => true
      case (Int, _: KioskInt)                   => true
      case (Boolean, _: KioskBoolean)           => true
      case (CollByte, _: KioskCollByte)         => true
      case (GroupElement, _: KioskGroupElement) => true
      case (ErgoTree, _: KioskErgoTree)         => true
      case (Address, _: KioskErgoTree)          => true
      case (BigInt, _: KioskBigInt)             => true
      case _                                    => false
    }
  }
}
