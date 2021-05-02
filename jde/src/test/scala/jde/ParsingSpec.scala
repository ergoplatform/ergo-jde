package jde

import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo
import kiosk.ergo.{DataType, KioskErgoTree, KioskGroupElement}
import jde.compiler.model.BinaryOperator._
import jde.compiler.model.{BinaryOp, Constant, Multiple}
import jde.compiler.{Dictionary, optSeq}
import jde.helpers.{TraitDummyProtocol, TraitTimestamp, TraitTokenFilter}
import jde.parser.Parser
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito._
import play.api.libs.json.JsValue

class ParsingSpec extends WordSpec with MockitoSugar with Matchers with TraitTokenFilter with TraitTimestamp with TraitDummyProtocol {
  "Protocol parser" should {
    "parse constants from token-filter.json correctly" in {
      val constants = tokenFilterProtocol.constants.get
      constants(0) shouldEqual Constant(
        name = "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7",
        `type` = DataType.CollByte,
        value = Some("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"),
        None
      )
      constants(1) shouldEqual Constant(
        name = "ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f",
        `type` = DataType.CollByte,
        value = Some("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f"),
        None
      )
      constants(2) shouldEqual Constant(
        name = "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea",
        `type` = DataType.CollByte,
        value = Some("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"),
        None
      )
      constants(3) shouldEqual Constant(
        name = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK",
        DataType.Address,
        value = Some("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"),
        None
      )
      constants(4) shouldEqual Constant(name = "1234", DataType.Long, value = Some("1234"), None)
      constants(5) shouldEqual Constant(name = "1", DataType.Long, value = Some("1"), None)
    }

    "parse operations from token-filter.json correctly" in {
      val binaryOps = tokenFilterProtocol.binaryOps.get
      binaryOps(0) shouldEqual BinaryOp(name = "myTokenAmount+1234", first = "myTokenAmount", Add, second = "1234")
    }

    "parse constants from timestamp.json correctly" in {
      val constants = timestampProtocol.constants.get
      constants(0) shouldEqual Constant(
        name = "myBoxId",
        `type` = DataType.CollByte,
        value = Some("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"),
        None
      )
      constants(1) shouldEqual Constant(
        name = "emissionAddress",
        `type` = DataType.Address,
        value = Some(
          "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"
        ),
        None
      )
      constants(2) shouldEqual Constant(name = "timestampAddress", `type` = DataType.Address, value = Some("4MQyMKvMbnCJG3aJ"), None)
      constants(3) shouldEqual Constant(
        name = "myTokenId",
        `type` = DataType.CollByte,
        value = Some("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"),
        None
      )
      constants(4) shouldEqual Constant(name = "minTokenAmount", `type` = DataType.Long, value = Some("2"), None)
      constants(5) shouldEqual Constant(name = "one", DataType.Long, value = Some("1"), None)
      constants(6) shouldEqual Constant(name = "minStorageRent", DataType.Long, value = Some("2000000"), None)
    }

    "parse operations from timestamp.json correctly" in {
      val binaryOps = timestampProtocol.binaryOps.get
      binaryOps(0) shouldEqual BinaryOp(name = "balanceTokenAmount", first = "inputTokenAmount", Sub, second = "one")
    }

    "parse and un-parse correctly" in {
      val protocolToJson: JsValue = Parser.unparse(dummyProtocol) // convert to json
      val protocolToJsonToProtocol = Parser.parse(protocolToJson.toString) // convert back to protocol
      protocolToJsonToProtocol shouldBe dummyProtocol
    }

    "parse protocol from dummy-protocol.json correctly" in {
      dummyProtocolFromJson shouldBe dummyProtocol
    }

    "parse constants from dummy-protocol.json correctly" in {
      implicit val dictionary = new Dictionary(12345)
      val constants = optSeq(dummyProtocolFromJson.constants)
      constants(0) shouldEqual Constant("myLong1", DataType.Long, Some("1234"), None)
      constants(1) shouldEqual Constant(
        "myCollByte",
        DataType.CollByte,
        Some("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"),
        None
      )
      constants(2) shouldEqual Constant("myInt", DataType.Int, Some("1234"), None)
      constants(3) shouldEqual Constant(
        "myTokenId",
        DataType.CollByte,
        Some("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f"),
        None
      )
      constants(4) shouldEqual Constant(
        "myGroupElement",
        DataType.GroupElement,
        Some("028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67"),
        None
      )
      constants(5) shouldEqual Constant("myErgoTree1", DataType.ErgoTree, Some("10010101D17300"), None)
      constants(6) shouldEqual Constant("myAddress", DataType.Address, Some("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"), None)

      val values: Seq[Multiple[ergo.KioskType[_]]] = constants.map(_.getValue)
      values(0).seq.head.value shouldEqual 1234L
      values(1).seq.head.toString shouldEqual "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"
      values(2).seq.head.value shouldEqual 1234
      values(3).seq.head.toString shouldEqual "ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f"
      values(4).seq.head shouldEqual KioskGroupElement(
        ScalaErgoConverters.stringToGroupElement("028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67")
      )
      values(5).seq.head shouldEqual KioskErgoTree(ScalaErgoConverters.stringToErgoTree("10010101D17300"))
      values(6).seq.head shouldEqual KioskErgoTree(
        ScalaErgoConverters.getAddressFromString("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK").script
      )
    }
  }
}
