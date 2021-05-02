package jde.compiler.model

import jde.compiler.tree2str
import kiosk.ergo._
import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo.{KioskBox, KioskCollByte, KioskErgoTree, KioskLong, KioskType}

case class OnChainBox(
    boxId: KioskCollByte,
    ergoTree: KioskErgoTree,
    nanoErgs: KioskLong,
    tokenIds: Seq[KioskCollByte],
    tokenAmounts: Seq[KioskLong],
    registers: Seq[KioskType[_]]
) {
  lazy val stringTokenIds = tokenIds.map(_.stringValue)
  lazy val stringBoxId = boxId.stringValue
  lazy val address = tree2str(ergoTree)
  require(tokenIds.size == tokenAmounts.size, s"tokenIds.size (${tokenIds.size}) != tokenAmounts.size (${tokenAmounts.size})")
}

object OnChainBox {
  def fromKioskBox(kioskBox: KioskBox): OnChainBox = {
    kioskBox.spentTxId.map(_ => throw new Exception(s"Box id ${kioskBox.optBoxId.get} has been spent"))
    val address = KioskErgoTree(ScalaErgoConverters.getAddressFromString(kioskBox.address).script)
    val nanoErgs = KioskLong(kioskBox.value)
    val boxIdHex = kioskBox.optBoxId.getOrElse(throw new Exception(s"No box id found in $kioskBox"))
    val boxId = KioskCollByte(boxIdHex.decodeHex)
    val registers = kioskBox.registers.toSeq
    val (tokenIdsHex, tokenValues) = kioskBox.tokens.unzip
    val tokenIds = tokenIdsHex.map(tokenIdHex => KioskCollByte(tokenIdHex.decodeHex)).toSeq
    val tokenAmounts = tokenValues.map(tokenValue => KioskLong(tokenValue)).toSeq
    OnChainBox(boxId, address, nanoErgs, tokenIds, tokenAmounts, registers)
  }
}
