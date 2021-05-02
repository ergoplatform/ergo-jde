package kiosk

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo._
import kiosk.explorer.Explorer
import scorex.util.encode.Base58
import sigmastate.basics.SecP256K1
import sigmastate.eval.{SigmaDsl, _}
import special.sigma.GroupElement

import java.security.SecureRandom

object ErgoUtil {
  private val explorer = new Explorer

  def randBigInt: BigInt = {
    val random = new SecureRandom()
    val values = new Array[Byte](32)
    random.nextBytes(values)
    BigInt(values).mod(SecP256K1.q)
  }

  def hexToDecimal(hex: String) = BigInt(hex, 16)

  def hexToBase58(hex: String) = Base58.encode(hex.decodeHex)

  def getBoxById(id: String) = explorer.getBoxById(id)

  def gX(x: BigInt) = {
    SigmaDsl.GroupElement(SecP256K1.generator).exp(x.bigInteger)
  }

  def hX(h: String, x: BigInt): GroupElement = {
    ScalaErgoConverters.stringToGroupElement(h).exp(x.bigInteger)
  }

  def addressToGroupElement(address: String) = {
    /*
      encoding is as follows:

      group element
      ErgoTree serialized:      0008cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798
      group element:                  0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798
      group element serialized:     070279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798

      address:                     9fSgJ7BmUxBQJ454prQDQ7fQMBkXPLaAmDnimgTtjym6FYPHjAV
     */
    val ergoTree = ScalaErgoConverters.getAddressFromString(address).script.bytes.encodeHex
    if (ergoTree.size != 72) throw new Exception("A proveDlog ergotree should be 72 chars long")
    if (ergoTree.take(6) != "0008cd") throw new Exception("Invalid address prefix for proveDlog")
    ergoTree.drop(6)
  }
}
