package jde

import jde.compiler.model.ReturnedValue
import jde.compiler.{Compiler, optSeq}
import jde.helpers.{TraitDummyProgram, TraitTokenFilter}
import jde.parser.Parser
import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo.{KioskBox, KioskCollByte, KioskErgoTree, KioskGroupElement, KioskInt, KioskLong, KioskType, StringToBetterString}
import kiosk.explorer.Explorer
import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar

class ComputeSpec extends WordSpec with MockitoSugar with Matchers with TraitDummyProgram {
  val explorer = mock[Explorer]
  when(explorer.getHeight) thenReturn 12345
  val txBuilder = new Compiler(explorer)
  trait DummyProgramMocks {
    val fakeBox1 = KioskBox(
      address =
        "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi",
      value = 1000000000L,
      registers = Array(KioskCollByte("1234".decodeHex)),
      tokens = Array(
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 10000),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123)
      ),
      optBoxId = Some("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"),
      spentTxId = None
    )

    val fakeBox2 = KioskBox( // extra tokens
      address = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK",
      value = 1100000L,
      registers = Array(KioskCollByte("123456".decodeHex)),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 101),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 12345678)
      ),
      optBoxId = Some("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"),
      spentTxId = None
    )

    val fakeBox3 = KioskBox( // extra tokens
      address = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK",
      value = 123456789L,
      registers = Array(KioskCollByte("123456".decodeHex), KioskLong(12345678L)),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 123),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 1)
      ),
      optBoxId = Some("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"),
      spentTxId = None
    )
  }

  "Compilation for dummy-program.json" should {
    "compute result correctly" in new DummyProgramMocks {
      optSeq(dummyProgramFromJson.dataInputs).size shouldBe 2
      dummyProgramFromJson.inputs.size shouldBe 1

      when(explorer.getBoxById("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")) thenReturn fakeBox1
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox2, fakeBox3)
      txBuilder.compile(dummyProgramFromJson)
      val returnedValues: Seq[ReturnedValue] = txBuilder.compile(dummyProgramFromJson).returned
      val result = returnedValues.map(returnedValue => returnedValue.name -> returnedValue.values).toMap

      def toCollByte(string: String) = KioskCollByte(string.decodeHex).hex
      def toErgoTree(string: String) = KioskErgoTree(ScalaErgoConverters.stringToErgoTree(string))
      def toGrp(string: String) = KioskGroupElement(ScalaErgoConverters.stringToGroupElement(string))

      result.values.foreach(_.size shouldBe 1)
      result("myLong6").head shouldBe KioskLong(12340)
      result("myLong5").head shouldBe KioskLong(7404)
      result("someLong1").head shouldBe KioskLong(123L)
      result("input1NanoErgs").head shouldBe KioskLong(1000000000L)
      result("myLong2").head shouldBe KioskLong(2468L)
      result("myLong7").head shouldBe KioskLong(-2468)
      result("myLong3").head shouldBe KioskLong(2468)
      result("someLong3").head shouldBe KioskLong(12345678)
      result("myLong8").head shouldBe KioskLong(2468)
      result("myIntToLong").head shouldBe KioskLong(1234)
      result("myLong4").head shouldBe KioskLong(4936)
      result("myLong1").head shouldBe KioskLong(1234)

      result("HEIGHT").head shouldBe KioskInt(12345)
      result("myInt").head shouldBe KioskInt(1234)

      result("myGroupElement").head shouldBe toGrp("028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67")

      result("myErgoTree1").head shouldBe toErgoTree("10010101d17300")
      result("myErgoTree2").head shouldBe toErgoTree("0008cd028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67")
      result("myAddress").head shouldBe toErgoTree("0008cd0249c9a2fb6e42dad1239c7a8fcf57abe24ab88a7f477aefdc2aabf8bb8530daf2")
      result("myAddressName").head shouldBe
        toErgoTree(
          "100a0400040004000402040005020400040a0e0710010100d173000400d805d601b2a5730000d602b2db63087201730100d603b2db6308a7730200d6048c720301d605b2a5730300d1ededed93c5a7c5b2a4730400ed93c27201c2a792c17201c1a7ed938c7202017204938c7203029a8c7202027305ededed93e4c67205040ec5b2db6501fe73060092e4c67205050499a3730793c272057308938cb2db63087205730900017204"
        )

      result("myRegister3").head.hex shouldBe toCollByte("1234")
      result("myRegister4").head.hex shouldBe toCollByte("123456")
      result("myCollByte2").head.hex shouldBe toCollByte("0008cd028182257d34ec7dbfedee9e857aadeb8ce02bb0c757871871cff378bb52107c67")
      result("myTokenId").head.hex shouldBe toCollByte("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f")
      result("myCollByte").head.hex shouldBe toCollByte("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")
      result("myToken1Id").head.hex shouldBe toCollByte("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")
      result("unreferencedToken2Id").head.hex shouldBe toCollByte("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")
      result("myRegister2").head.hex shouldBe toCollByte("123456")
      result("randomName").head.hex shouldBe toCollByte("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")
      result("myRegister1").head.hex shouldBe toCollByte("123456")
    }
  }

  "Address to GroupElement" should {
    "generate correct GroupElement" in {
      val goodTestVectors = Seq(
        "9fPQAEACfqiyy5p2UQwRjiJmNHW4WMRfKqbCms22BQ446QBGWZv" -> "02724cd911e757db4dd0bcac7582d1812ad7c938faa77bf57c171b2a7daaef24b7",
        "9gLGZ9AtJ6AFHdpkH6pnJ2LNx98STt4E23ZrkH2T1pFysTCCbjh" -> "02eee2fe4a7e75e91661d7a8453d82519453c06d7e412d6a927526fe5cf1d9301a",
        "9htmJjmkqVm2ixtQZK7wb7ykZPX8V9axC56DjuEvnUdknyaNq3w" -> "03bc5e485f9d940a846c8184f503e92979263ebb01c42958303fe2ad1b9e5a9d21"
      )
      val badTestVectors = Seq( // change last byte of group element representation
        "9fPQAEACfqiyy5p2UQwRjiJmNHW4WMRfKqbCms22BQ446QBGWZv" -> ("02724cd911e757db4dd0bcac7582d1812ad7c938faa77bf57c171b2a7daaef24b7" -> "02724cd911e757db4dd0bcac7582d1812ad7c938faa77bf57c171b2a7daaef24b8"),
        "9gLGZ9AtJ6AFHdpkH6pnJ2LNx98STt4E23ZrkH2T1pFysTCCbjh" -> ("02eee2fe4a7e75e91661d7a8453d82519453c06d7e412d6a927526fe5cf1d9301a" -> "02eee2fe4a7e75e91661d7a8453d82519453c06d7e412d6a927526fe5cf1d9301b"),
        "9htmJjmkqVm2ixtQZK7wb7ykZPX8V9axC56DjuEvnUdknyaNq3w" -> ("03bc5e485f9d940a846c8184f503e92979263ebb01c42958303fe2ad1b9e5a9d21" -> "03bc5e485f9d940a846c8184f503e92979263ebb01c42958303fe2ad1b9e5a9d23")
      )
      def script(address: String, groupElement: String) =
        s"""{
           |  "constants":[
           |    {
           |      "name": "address",
           |      "value": "$address",
           |      "type": "Address"
           |    },
           |    {
           |      "name": "groupElement",
           |      "value": "$groupElement",
           |      "type": "GroupElement"
           |    }
           |  ],
           |  "unaryOps":[
           |    {
           |      "name": "groupElementFromAddress",
           |      "from": "address",
           |      "op": "ToGroupElement"
           |    },
           |    {
           |      "name": "ergoTreeFromGroupElement",
           |      "from": "groupElement",
           |      "op": "proveDlog"
           |    },
           |    {
           |      "name": "addressFromErgoTree",
           |      "from": "ergoTreeFromGroupElement",
           |      "op": "ToAddress"
           |    }
           |  ],
           |  "postConditions":[
           |    {
           |      "first": "groupElementFromAddress",
           |      "second": "groupElement",
           |      "op": "Eq"
           |    },
           |    {
           |      "first": "addressFromErgoTree",
           |      "second": "address",
           |      "op": "Eq"
           |    }
           |  ]
           |}
           |""".stripMargin

      goodTestVectors.foreach {
        case (address, groupElement) =>
          val programFromJson = Parser.parse(script(address, groupElement))
          noException should be thrownBy txBuilder.compile(programFromJson)
      }

      badTestVectors.foreach {
        case (address, (goodGroupElement, badGroupElement)) =>
          val programFromJson = Parser.parse(script(address, badGroupElement))
          the[Exception] thrownBy txBuilder.compile(
            programFromJson
          ) should have message s"""Failed post-condition: groupElementFromAddress: ($goodGroupElement) Eq groupElement ($badGroupElement)"""
      }

    }
  }
}
