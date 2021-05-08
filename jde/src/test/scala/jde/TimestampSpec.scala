package jde

import jde.compiler.model.MatchingOptions.Strict
import jde.compiler.{Compiler, optSeq}
import jde.helpers.TraitTimestamp
import kiosk.ergo.{KioskBox, KioskInt}
import kiosk.explorer.Explorer
import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito._
import play.api.libs.json.JsResultException

class TimestampSpec extends WordSpec with MockitoSugar with Matchers with TraitTimestamp {
  val explorer = mock[Explorer]
  when(explorer.getHeight) thenReturn 12345
  val txBuilder = new Compiler(explorer)

  def someSeq[T](seq: T*): Option[Seq[T]] = Some(seq)

  trait TimestampMocks {
    val fakeDataInputBox = KioskBox(
      address = "9etuNZvRv3PZLq5HigoSMUANgmF6f7jTJLfnmdjuzUKQ7WH9ysD",
      value = 1000000000L,
      registers = Array(),
      tokens = Array(
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 10000),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123)
      ),
      optBoxId = Some("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"),
      spentTxId = None
    )

    val fakeEmissionBoxExtraTokens = KioskBox( // extra tokens
      address =
        "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi",
      value = 1100000L,
      registers = Array(),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 100),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123),
        ("5c674366216d127f7424bfcf1bf52310f9c34cd8d07013c804a95bb8ce9e4f82", 1)
      ),
      optBoxId = Some("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"),
      spentTxId = None
    )

    val fakeEmissionBoxLessTokens = KioskBox(
      address =
        "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi",
      value = 2200000L,
      registers = Array(),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 1)
      ),
      optBoxId = Some("af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"),
      spentTxId = None
    )

    val fakeEmissionBoxExactTokens = KioskBox(
      address =
        "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi",
      value = 3300000L,
      registers = Array(),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 22)
      ),
      optBoxId = Some("43b0c3add1fde20244a3467798a777684f9234d1f56f31ad01a297c86c6d40c7"),
      spentTxId = None
    )
  }

  "Compilation" should {
    "select matched boxes" in new TimestampMocks {
      timestampProgram.inputs.size shouldBe 1
      optSeq(timestampProgram.inputs)(0).options shouldBe Some(Set(Strict))

      when(explorer.getBoxById("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")) thenReturn fakeDataInputBox
      when(
        explorer.getUnspentBoxes(
          "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"
        )
      ) thenReturn Seq(fakeEmissionBoxLessTokens, fakeEmissionBoxExtraTokens, fakeEmissionBoxExactTokens)

      val result = new Compiler(explorer).compile(timestampProgram)

      result.dataInputBoxIds shouldBe Seq("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")
      result.inputBoxIds shouldBe Seq("43b0c3add1fde20244a3467798a777684f9234d1f56f31ad01a297c86c6d40c7")
      val outputs = result.outputs
      outputs(
        0
      ).address shouldBe "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"
      outputs(0).tokens(0)._1 shouldBe "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"
      outputs(0).tokens(0)._2 shouldBe 21
      outputs(0).value shouldBe 3300000

      outputs(1).address shouldBe "4MQyMKvMbnCJG3aJ"
      outputs(1).tokens(0)._1 shouldBe "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"
      outputs(1).tokens(0)._2 shouldBe 1
      outputs(1).value shouldBe 2000000
      outputs(1).registers(0).hex shouldBe "0e20506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"
      outputs(1).registers(0).toString shouldBe "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"
      outputs(1).registers(1).asInstanceOf[KioskInt].value shouldBe 12345
      outputs(1).registers(1).hex shouldBe "04f2c001"
    }

    "reject with no matched inputs" in new TimestampMocks {
      timestampProgram.inputs.size shouldBe 1
      optSeq(timestampProgram.inputs)(0).options shouldBe Some(Set(Strict))

      when(explorer.getBoxById("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")) thenReturn fakeDataInputBox
      when(
        explorer.getUnspentBoxes(
          "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"
        )
      ) thenReturn Seq(fakeEmissionBoxLessTokens, fakeEmissionBoxExtraTokens)

      the[Exception] thrownBy txBuilder.compile(timestampProgram) should have message "No box matched for input at index 0"
    }

    "reject with no matched data inputs" in new TimestampMocks {
      timestampProgram.inputs.size shouldBe 1
      optSeq(timestampProgram.inputs)(0).options shouldBe Some(Set(Strict))

      when(explorer.getBoxById("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7")) thenThrow new JsResultException(Nil)

      the[Exception] thrownBy txBuilder.compile(timestampProgram) should have message "No box matched for data-input at index 0"
    }
  }
}
