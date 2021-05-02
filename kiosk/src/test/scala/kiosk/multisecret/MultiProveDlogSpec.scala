package kiosk.multisecret

import kiosk.ergo._
import kiosk.tx.TxUtil
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, HttpClientTesting, InputBox}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants
import special.sigma.GroupElement

class MultiProveDlogSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {
  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val dummyTxId1 = "d9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyTxId2 = "e9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "{sigmaProp(1 < 2)}"

  val secret1 = BigInt("1111111111111111111111111111111111111111111111111111111")
  val secret2 = BigInt("9999999999999999999999999999999999999999999999999")

  private val defaultGenerator: GroupElement = CryptoConstants.dlogGroup.generator
  private val gX: GroupElement = defaultGenerator.exp(secret1.bigInteger)

  property("Multi proveDlog") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      val fee = 1500000
      val contract1 = ctx.compileContract(
        ConstantsBuilder
          .create()
          .item(
            "gX",
            gX
          )
          .build(),
        "proveDlog(gX)"
      )

      val box1 = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(100000000L)
        .contract(contract1)
        .build()
        .convertToInputWith(dummyTxId1, 0)

      val box2 = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(101500000L)
        .contract(contract1)
        .build()
        .convertToInputWith(dummyTxId2, 0)

      val dummyOutput = KioskBox(
        changeAddress,
        value = 200000000L,
        registers = Array(),
        tokens = Array()
      )

      noException should be thrownBy TxUtil.createTx(
        inputBoxes = Array(box1, box2),
        dataInputs = Array[InputBox](),
        boxesToCreate = Array(dummyOutput),
        fee,
        changeAddress,
        Array[String](secret1.toString(10), secret2.toString(10)),
        Array[DhtData](),
        false
      )

    }
  }
}
