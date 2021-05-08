package kiosk

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree => tree2addr, getStringFromAddress => addr2Str}
import sigmastate.basics.SecP256K1
import sigmastate.eval.SigmaDsl
import special.sigma.GroupElement

import scala.io.BufferedSource
import scala.util.Try

package object ergo {
//  class BetterString(string: String) {
//    def decodeHex = Hex.decode(string)
//  }

  implicit def ByteArrayToBetterByteArray(bytes: Array[Byte]) = new BetterByteArray(bytes)

//  class BetterByteArray(bytes: Seq[Byte]) {
//    def encodeHex: String = Hex.toHexString(bytes.toArray).toLowerCase
//  }

  implicit def StringToBetterString(string: String) = new BetterString(string)

  lazy val PointAtInfinity = KioskGroupElement(SigmaDsl.GroupElement(SecP256K1.identity))

  def tree2str(ergoTree: KioskErgoTree): String = addr2Str(tree2addr(ergoTree.value))

  implicit def groupElementToKioskGroupElement(g: GroupElement) = KioskGroupElement(g)

  type ID = String
  type Amount = Long

  type Token = (ID, Amount)
  type Tokens = Array[Token]

  def decodeBigInt(encoded: String): BigInt = Try(BigInt(encoded, 10)).recover { case ex => BigInt(encoded, 16) }.get

  def usingSource[B](param: BufferedSource)(f: BufferedSource => B): B =
    try f(param)
    finally param.close

}
