package kiosk.ergo

import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import kiosk.ErgoUtil.{addressToGroupElement => addr2Grp}
import kiosk.encoding.ScalaErgoConverters.{stringToGroupElement => str2Grp}

class ModelSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks {
  property("Serialization of ErgoValue matches ValueSerializer") {
    val grp1 = str2Grp(addr2Grp("9eiuh5bJtw9oWDVcfJnwTm1EHfK5949MEm5DStc2sD1TLwDSrpx"))
    val grp2 = str2Grp(addr2Grp("9hsQktNvigHUZV5r8QYxXNfgdVgCjCX6K9wEMQ8hjZz1E1SpEmA"))

    val kioskTypes = Seq(
      KioskCollByte("hello world".getBytes),
      KioskInt(99),
      KioskLong(234L),
      KioskGroupElement(grp1),
      KioskAvlTree("4b2d8b7beb3eaac8234d9e61792d270898a43934d6a27275e4f3a044609c9f2e".decodeHex, 20),
      KioskBoolean(true),
      KioskBigInt(BigInt("1234245235423523525")),
      KioskCollGroupElement(Array(grp1, grp2)),
      KioskCollCollByte(Array("hello".getBytes, "world".getBytes))
    )

    kioskTypes.foreach {
      case kioskType =>
        val s1 = kioskType.serialize.encodeHex
        val s2 = kioskType.getErgoValue.toHex.decodeHex.encodeHex
        s1 shouldBe s2
    }
  }
}
