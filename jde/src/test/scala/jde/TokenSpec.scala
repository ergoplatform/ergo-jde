package jde

import jde.compiler.model.MatchingOptions.{Optional, Strict}
import jde.compiler.model.{FilterOp, MatchingOptions, Output}
import jde.compiler.{TxBuilder, model, optSeq}
import jde.helpers.TraitTokenFilter
import kiosk.ergo.KioskBox
import kiosk.explorer.Explorer
import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito._

class TokenSpec extends WordSpec with MockitoSugar with Matchers with TraitTokenFilter {
  val explorer = mock[Explorer]
  when(explorer.getHeight) thenReturn 12345
  val txBuilder = new TxBuilder(explorer)

  def someSeq[T](seq: T*): Option[Seq[T]] = Some(seq)

  trait TokenMocks {
    val fakeBox0ExactTokens = KioskBox(
      address = "9etuNZvRv3PZLq5HigoSMUANgmF6f7jTJLfnmdjuzUKQ7WH9ysD",
      value = 1000000000L,
      registers = Array(),
      tokens = Array(
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 10000),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123)
      ),
      optBoxId = Some("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"),
      spentTxId = None
    )

    val fakeBox0ExtraTokens = KioskBox( // extra tokens
      address = "9etuNZvRv3PZLq5HigoSMUANgmF6f7jTJLfnmdjuzUKQ7WH9ysD",
      value = 1000000000L,
      registers = Array(),
      tokens = Array(
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 10000),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123),
        ("5c674366216d127f7424bfcf1bf52310f9c34cd8d07013c804a95bb8ce9e4f82", 1)
      ),
      optBoxId = Some("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"),
      spentTxId = None
    )

    val fakeBox1ExactTokens = KioskBox(
      address = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK",
      value = 2000000000L,
      registers = Array(),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 1235),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123),
        ("490ef5a88d33d7b3eb7b16d4062ee9c3e204f9e6123f4bd6d97156a5b05b592a", 12),
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 1)
      ),
      optBoxId = Some("af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"),
      spentTxId = None
    )

    val fakeBox1ExactTokensWrongAmount = KioskBox(
      address = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK",
      value = 2000000000L,
      registers = Array(),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 1233),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123),
        ("490ef5a88d33d7b3eb7b16d4062ee9c3e204f9e6123f4bd6d97156a5b05b592a", 12),
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 1)
      ),
      optBoxId = Some("af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"),
      spentTxId = None
    )

    val fakeBox1ExtraTokens = KioskBox( // extra tokens
      address = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK",
      value = 2000000000L,
      registers = Array(),
      tokens = Array(
        ("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 1235),
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 123),
        ("490ef5a88d33d7b3eb7b16d4062ee9c3e204f9e6123f4bd6d97156a5b05b592a", 12),
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 1),
        ("e3e335a1d34ec7ad4eecde3813a4b066114692cad65b3aa0f3876abba8bb6307", 2)
      ),
      optBoxId = Some("af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"),
      spentTxId = None
    )

    val fakeBox2LessTokens = KioskBox(
      address = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK",
      value = 4000000000L,
      registers = Array(),
      tokens = Array(
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 10),
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 1),
        ("e3e335a1d34ec7ad4eecde3813a4b066114692cad65b3aa0f3876abba8bb6307", 2)
      ),
      optBoxId = Some("879e437e94668ee11f9f4575fc623e420bc04a466fffca31ae0623ce950a861d"),
      spentTxId = None
    )

    val fakeBox2ExactTokens = KioskBox(
      address = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK",
      value = 4000000000L,
      registers = Array(),
      tokens = Array(
        ("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", 10),
        ("ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f", 1),
        ("e3e335a1d34ec7ad4eecde3813a4b066114692cad65b3aa0f3876abba8bb6307", 2)
      ),
      optBoxId = Some("879e437e94668ee11f9f4575fc623e420bc04a466fffca31ae0623ce950a861d"),
      spentTxId = None
    )
  }

  "Compilation for token-filter.json" should {
    "select matched boxes" in new TokenMocks {
      optSeq(tokenFilterProtocol.inputs).size shouldBe 2
      optSeq(tokenFilterProtocol.inputs)(0).options shouldBe Some(Set(Strict))
      optSeq(tokenFilterProtocol.inputs)(1).options shouldBe Some(Set(Strict))
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox1ExactTokens, fakeBox2LessTokens)
      val result = new compiler.TxBuilder(explorer).compile(tokenFilterProtocol)
      result.inputBoxIds shouldBe Seq(
        "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea",
        "af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"
      )
    }

    "select matched boxes in any order" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox2LessTokens, fakeBox1ExactTokens)
      val result = new compiler.TxBuilder(explorer).compile(tokenFilterProtocol)
      result.inputBoxIds shouldBe Seq(
        "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea",
        "af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"
      )
    }

    "reject if invalid amount in non-Optional input" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(
        fakeBox2LessTokens,
        fakeBox1ExactTokensWrongAmount
      )
      the[Exception] thrownBy new compiler.TxBuilder(explorer)
        .compile(
          tokenFilterProtocol.copy(inputs =
            someSeq(optSeq(tokenFilterProtocol.inputs)(0), optSeq(tokenFilterProtocol.inputs)(1).copy(options = None))
          )
        ) should have message "No box matched for input at index 1"
    }

    "accept if invalid amount in Optional input" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(
        fakeBox2LessTokens,
        fakeBox1ExactTokensWrongAmount
      )
      val result = new compiler.TxBuilder(explorer)
        .compile(
          tokenFilterProtocol.copy(inputs =
            someSeq(optSeq(tokenFilterProtocol.inputs)(0), optSeq(tokenFilterProtocol.inputs)(1).copy(options = Some(Set(MatchingOptions.Optional))))
          )
        )
      result.inputBoxIds shouldBe Seq("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")
    }

    "reject if invalid amount in Optional input containing a target used in a non-Optional output" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(
        fakeBox2LessTokens,
        fakeBox1ExactTokensWrongAmount
      )
      the[Exception] thrownBy new compiler.TxBuilder(explorer)
        .compile(
          tokenFilterProtocol.copy(
            inputs = someSeq(
              optSeq(tokenFilterProtocol.inputs)(0),
              optSeq(tokenFilterProtocol.inputs)(1).copy(options = Some(Set(MatchingOptions.Optional)))
            ),
            outputs = Some(
              Seq(
                Output(
                  address = model.Address(name = None, value = Some("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")),
                  registers = None,
                  tokens = None,
                  nanoErgs = model.Long(name = None, value = Some("thirdTokenAmount"), filter = None),
                  options = None
                )
              )
            )
          )
        ) should have message "Output declaration generated zero boxes (use 'Optional' flag to prevent this error): Output(unnamed: Address,None,None,unnamed: Long,None)"
    }

    "accept if invalid amount in Optional input containing a target used in an Optional output" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(
        fakeBox2LessTokens,
        fakeBox1ExactTokensWrongAmount
      )
      val result = new compiler.TxBuilder(explorer)
        .compile(
          tokenFilterProtocol.copy(
            inputs = someSeq(
              optSeq(tokenFilterProtocol.inputs)(0),
              optSeq(tokenFilterProtocol.inputs)(1).copy(options = Some(Set(MatchingOptions.Optional)))
            ),
            outputs = someSeq(
              Output(
                address = model.Address(name = None, value = Some("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")),
                registers = None,
                tokens = None,
                nanoErgs = model.Long(name = None, value = Some("thirdTokenAmount"), filter = None),
                options = Some(Set(Optional))
              )
            )
          )
        )
      result.inputBoxIds shouldBe Seq("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")
      result.outputs shouldBe Nil
    }

    "reject if output contains the Strict option" in new TokenMocks {
      the[Exception] thrownBy new compiler.TxBuilder(explorer)
        .compile(
          tokenFilterProtocol.copy(
            outputs = someSeq(
              Output(
                address = model.Address(name = None, value = Some("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")),
                registers = None,
                tokens = None,
                nanoErgs = model.Long(name = None, value = Some("thirdTokenAmount"), filter = None),
                options = Some(Set(Strict))
              )
            )
          )
        ) should have message "'Strict' option not allowed in output"
    }

    // ToDo: Add tests for 'Multi' option in outputs

    "reject boxes with extra tokens and Strict for input 0" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExtraTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox1ExactTokens, fakeBox2LessTokens)
      the[Exception] thrownBy new compiler.TxBuilder(explorer).compile(tokenFilterProtocol) should have message "No box matched for input at index 0"
    }

    "select boxes with extra tokens and no Strict for input 0" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExtraTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox1ExactTokens, fakeBox2LessTokens)
      val result = new compiler.TxBuilder(explorer)
        .compile(
          tokenFilterProtocol.copy(inputs =
            someSeq(optSeq(tokenFilterProtocol.inputs)(0).copy(options = None), optSeq(tokenFilterProtocol.inputs)(1))
          )
        )
      result.inputBoxIds shouldBe Seq(
        "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea",
        "af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"
      )
    }

    "reject boxes with extra tokens and Strict for input 1" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox2LessTokens, fakeBox1ExtraTokens)
      the[Exception] thrownBy new compiler.TxBuilder(explorer).compile(tokenFilterProtocol) should have message "No box matched for input at index 1"
    }

    "select boxes with extra tokens and no Strict for input 1" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox2LessTokens, fakeBox1ExtraTokens)
      val result = new compiler.TxBuilder(explorer)
        .compile(
          tokenFilterProtocol.copy(inputs =
            someSeq(optSeq(tokenFilterProtocol.inputs)(0), optSeq(tokenFilterProtocol.inputs)(1).copy(options = None))
          )
        )
      result.inputBoxIds shouldBe Seq(
        "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea",
        "af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"
      )
    }

    "select boxes with exact tokens in both inputs and Strict option for both inputs" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox1ExactTokens, fakeBox2ExactTokens)
      val result = new compiler.TxBuilder(explorer).compile(tokenFilterProtocol)
      result.inputBoxIds shouldBe Seq(
        "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea",
        "af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"
      )
    }

    "reject if post-condition fails" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox1ExactTokens, fakeBox2ExactTokens)
      the[Exception] thrownBy new compiler.TxBuilder(explorer)
        .compile(
          tokenFilterProtocol.copy(postConditions = tokenFilterProtocol.postConditions.map(_.map(_.copy(op = FilterOp.Eq))))
        ) should have message "Failed post-condition: myTokenAmount: (123) Eq thirdTokenAmount (12)"
    }

    "reject if post-condition contains undefined pointer" in new TokenMocks {
      when(explorer.getBoxById("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea")) thenReturn fakeBox0ExactTokens
      when(explorer.getUnspentBoxes("9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK")) thenReturn Seq(fakeBox1ExactTokens, fakeBox2ExactTokens)

      val firstException = the[Exception] thrownBy new compiler.TxBuilder(explorer)
        .compile(
          tokenFilterProtocol.copy(postConditions = Some(Seq(tokenFilterProtocol.postConditions.get(0).copy(first = "undefined"))))
        )

      val secondException = firstException.getCause

      val thirdException = secondException.getCause

      firstException should have message "Error evaluating condition Gt"

      secondException should have message "Error pairing undefined and thirdTokenAmount"

      thirdException should have message "key not found: undefined"
    }
  }
}
