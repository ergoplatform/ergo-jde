package kiosk.script

import kiosk.ErgoUtil
import kiosk.appkit.Client
import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo._
import org.bouncycastle.util.encoders.Hex
import org.ergoplatform.appkit._
import org.ergoplatform.{ErgoAddress, Pay2SAddress}
import org.scalatest.{Matchers, WordSpec}
import sigmastate.Values
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants

class ScriptSpec extends WordSpec with Matchers {
  "Kiosk" should {
    "compile script #1 correctly" in {
      val ergoScript =
        """{
          |  val g = groupGenerator
          |  val c1 = SELF.R4[GroupElement].get
          |  val c2 = SELF.R5[GroupElement].get
          |  val gX = SELF.R6[GroupElement].get
          |  proveDlog(c2) ||          // either c2 is g^y
          |  proveDHTuple(g, c1, gX, c2) // or c1 is g^y and c2 = gX^y = g^xy
          |}""".stripMargin

      Client.usingContext { implicit ctx =>
        val appkitErgoTree = ctx
          .compileContract(
            ConstantsBuilder.empty(),
            ergoScript
          )
          .getErgoTree
          .bytes
          .encodeHex

        val kioskErgoTree = ScriptUtil.compile(Map(), ergoScript).bytes.encodeHex

        kioskErgoTree shouldBe appkitErgoTree
      }
    }

    "compile script #2 correctly" in {
      val ergoScript =
        """{
          |  val g = groupGenerator
          |  val c1 = SELF.R4[GroupElement].get
          |  val c2 = SELF.R5[GroupElement].get
          |  proveDlog(c2) ||          // either c2 is g^y
          |  proveDHTuple(g, c1, gX, c2) // or c1 is g^y and c2 = gX^y = g^xy
          |}""".stripMargin

      val x = BigInt("1120347812374928374923042340293450928435285028435028435")
      Client.usingContext { implicit ctx =>
        val kioskErgoTree = ScriptUtil.compile(Map("gX" -> KioskGroupElement(ErgoUtil.gX(x))), ergoScript).bytes.encodeHex

        // then compute using appkit
        val appkitErgoTree: String = ctx
          .compileContract(
            ConstantsBuilder
              .create()
              .item(
                "gX",
                CryptoConstants.dlogGroup.generator.exp(x.bigInteger)
              )
              .build(),
            ergoScript
          )
          .getErgoTree
          .bytes
          .encodeHex

        kioskErgoTree shouldBe appkitErgoTree
      }
    }

    "compile script #3 correctly" in {
      val ergoScript =
        """{
          |  (blake2b256(OUTPUTS(0).propositionBytes) == hash) && getVar[Int](0).get == int &&
          |  getVar[Long](1).get == long && getVar[BigInt](2).get == bigInt && SELF.R4[GroupElement].get == gX
          |}""".stripMargin

      val x = BigInt("1120347812374928374923042340293450928435285028435028435")
      val hash: Array[Byte] = "1000d801d601e4c6a70507eb02cd7201cedb6a01dde4c6a70407e4c6a706077201".decodeHex
      val int = 238528959
      val long = 209384592083L
      val bigInt = BigInt("230948092384598209582958205802850298529085")

      Client.usingContext { implicit ctx =>
        val map = Map(
          "gX" -> KioskGroupElement(ErgoUtil.gX(x)),
          "hash" -> KioskCollByte(hash),
          "bigInt" -> KioskBigInt(bigInt),
          "long" -> KioskLong(long),
          "int" -> KioskInt(int)
        )

        val kioskErgoTree = ScriptUtil.compile(map, ergoScript).bytes.encodeHex

        val appkitErgoTree: String = ctx
          .compileContract(
            ConstantsBuilder
              .create()
              .item(
                "hash",
                hash
              )
              .item(
                "int",
                int
              )
              .item(
                "long",
                long
              )
              .item(
                "bigInt",
                SigmaDsl.BigInt(bigInt.bigInteger)
              )
              .item(
                "gX",
                CryptoConstants.dlogGroup.generator.exp(x.bigInteger)
              )
              .build(),
            ergoScript
          )
          .getErgoTree
          .bytes
          .encodeHex

        kioskErgoTree shouldBe appkitErgoTree
      }
    }

    "compile script #4 correctly" in {
      val ergoScript =
        """{
          |  sigmaProp(blake2b256(OUTPUTS(0).propositionBytes) == hash)
          |}""".stripMargin

      val hash: Array[Byte] = "1000d801d601e4c6a70507eb02cd7201cedb6a01dde4c6a70407e4c6a706077201".decodeHex

      Client.usingContext { implicit ctx =>
        val kioskErgoTree = ScriptUtil.compile(Map("hash" -> KioskCollByte(hash)), ergoScript).bytes.encodeHex

        val appkitErgoTree: String = ctx
          .compileContract(
            ConstantsBuilder
              .create()
              .item(
                "hash",
                hash
              )
              .build(),
            ergoScript
          )
          .getErgoTree
          .bytes
          .encodeHex

        kioskErgoTree shouldBe appkitErgoTree
      }
    }

    "decode address #1 correctly" in {
      // Addresses have some quirks;
      // for example fANwcUDKxKD3btGmknic2kE7mEzLR2CFTYzEKPh5iyPMUMwfwjuxsJP and 9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw contain the same ErgoTree

      val address: String = "9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw"
      val ergoAddress: ErgoAddress = ScalaErgoConverters.getAddressFromString(address)

      ergoAddress.toString shouldBe address

      val script: Values.ErgoTree = ergoAddress.script
      val scriptHex: String = script.bytes.encodeHex

      import ScriptUtil.ergoAddressEncoder

      val p2sAddress: Pay2SAddress = Pay2SAddress(script)
      val p2sAddressScript: Values.ErgoTree = p2sAddress.script
      val p2sAddressScriptBytes: Array[Byte] = p2sAddress.scriptBytes

      scriptHex shouldBe p2sAddressScript.bytes.encodeHex
      scriptHex shouldBe p2sAddressScriptBytes.encodeHex

      val p2sAddressString: String = ScalaErgoConverters.getStringFromAddress(p2sAddress)
      val p2sAddressToErgoAddress: ErgoAddress = ScalaErgoConverters.getAddressFromString(p2sAddressString)
      val p2SAddressStringToScript: Values.ErgoTree = p2sAddressToErgoAddress.script

      scriptHex shouldBe p2SAddressStringToScript.bytes.encodeHex

      p2sAddressString shouldBe "fANwcUDKxKD3btGmknic2kE7mEzLR2CFTYzEKPh5iyPMUMwfwjuxsJP" // ToDo: check why this address is encoded differently from the original

      ScalaErgoConverters.getStringFromAddress(ScalaErgoConverters.getAddressFromString(p2sAddressString)) shouldBe p2sAddressString
    }

    "decode address #2 correctly" in {
      val address: String = "9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw"
      val ergoAddress: ErgoAddress = ScalaErgoConverters.getAddressFromString(address)

      ergoAddress.toString shouldBe address

      import ScriptUtil.ergoAddressEncoder

      val address1 = ergoAddressEncoder.fromString("fANwcUDKxKD3btGmknic2kE7mEzLR2CFTYzEKPh5iyPMUMwfwjuxsJP").get
      val address2 = ergoAddressEncoder.fromString("9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw").get
      val hex1 = Hex.toHexString(address1.script.bytes)
      val hex2 = Hex.toHexString(address2.script.bytes)
      hex1 shouldBe hex2
      hex1 shouldBe "0008cd03836fd1f810cbfa6aa9516530709ae6e591bccb9523e9b65c49c09586319d10de"

      Pay2SAddress(address2.script).toString() shouldBe "fANwcUDKxKD3btGmknic2kE7mEzLR2CFTYzEKPh5iyPMUMwfwjuxsJP"
      ergoAddressEncoder.fromProposition(address1.script).get.toString shouldBe "9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw"

      val ergoTree = ScalaErgoConverters.getAddressFromString("9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw").script
      val address3 = Pay2SAddress(ergoTree).toString
      val address4 = ScalaErgoConverters.getAddressFromErgoTree(ergoTree).toString
      address3 shouldBe "fANwcUDKxKD3btGmknic2kE7mEzLR2CFTYzEKPh5iyPMUMwfwjuxsJP"
      address4 shouldBe "9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw"
      ergoAddressEncoder.toString(
        ScalaErgoConverters.getAddressFromString("9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw")
      ) shouldBe "9hTh4u6CDXktMQb9BoRo5nTPnmFN8G5u4PUCURvoUCXmtaaDYdw"
    }

  }

}
