package kiosk.timestamp.v1

import kiosk.ergo.{DhtData, KioskBox, KioskCollByte, KioskInt}
import kiosk.tx.TxUtil
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ErgoToken, HttpClientTesting, InputBox, SignedTransaction}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class TimestampSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))
  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val token = "12caaacb51c89646fac9a3786eb98d0113bd57d68223ccc11754a4f67281daed"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "{sigmaProp(1 < 2)}"

  property("One complete epoch") {
    Timestamp.emissionAddress shouldBe "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"
    Timestamp.masterAddress shouldBe "2vTQnMx5uFfFfJjL6ucuprpWSUeXHAqbyPLkW46DfMgw7ENGFbGBVPHJPVXwJWg5e1DdqPv28syDEJQGQy5vss2Wvh6Srrd98fSSTVfkEb5VcehCqhoGD8826imCkAfC2mDhGcTuYKcFvy4JrC8GoAbx6NZomHZAmESCL8QyQ2utraCF7TebrZGudEDehwho4AMQkq9oDkaVdyQ2NNuYQ8NwtQcBrfCZRFSGGeitPmnoCQgK8vQDxBifiQcW1avYexPYdb9CXHGT8EtKaRj5JXcqcuwwsXp5GXfG"

    ergoClient.execute { implicit ctx: BlockchainContext =>
      val initialTokens = 1000000000L
      val masterBoxInTokens = (token, initialTokens)
      val dummyTokens = new ErgoToken(token, initialTokens)

      val height = ctx.getHeight
      val fee = 1500000

      val masterBoxInToCreate = KioskBox(
        Timestamp.masterAddress,
        value = 2000000,
        registers = Array(),
        tokens = Array(masterBoxInTokens)
      )

      // dummy custom input box for funding various transactions
      val customInputBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000000000L)
        .tokens(dummyTokens)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      val bootstrapTx: SignedTransaction = TxUtil.createTx(
        inputBoxes = Array(customInputBox),
        dataInputs = Array[InputBox](),
        boxesToCreate = Array(masterBoxInToCreate),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      val masterBoxIn: InputBox = bootstrapTx.getOutputsToSpend.get(0)

      val masterBoxOutTokens = (token, initialTokens - 1000)
      val emissionBoxInTokens = (token, 1000L)

      val masterBoxOutToCreate = KioskBox(
        Timestamp.masterAddress,
        value = 2000000,
        registers = Array(),
        tokens = Array(masterBoxOutTokens)
      )

      val emissionBoxInToCreate = KioskBox(
        Timestamp.emissionAddress,
        value = 10000000,
        registers = Array(),
        tokens = Array(emissionBoxInTokens)
      )

      val createEmissionBoxTx: SignedTransaction =
        TxUtil.createTx(
          inputBoxes = Array(masterBoxIn, customInputBox),
          dataInputs = Array[InputBox](),
          boxesToCreate = Array(masterBoxOutToCreate, emissionBoxInToCreate),
          fee,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )

      val masterBoxOut = createEmissionBoxTx.getOutputsToSpend.get(0)
      val emissionBoxIn = createEmissionBoxTx.getOutputsToSpend.get(1)

      val emissionBoxOutTokens = (token, 999L)
      val timestampBoxTokens = (token, 1L)

      val boxToTimestamp: InputBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      val r4TimestampBox = KioskCollByte(boxToTimestamp.getId.getBytes)
      val r5TimestampBox = KioskInt(height + 1)

      val emissionBoxOutToCreate = KioskBox(
        Timestamp.emissionAddress,
        value = emissionBoxInToCreate.value,
        registers = Array(),
        tokens = Array(emissionBoxOutTokens)
      )

      val timestampBoxToCreate = KioskBox(
        "4MQyMKvMbnCJG3aJ", // sigmaProp(false)
        value = 1000000,
        registers = Array(r4TimestampBox, r5TimestampBox),
        tokens = Array(timestampBoxTokens)
      )

      val createTimestampTx: SignedTransaction =
        TxUtil.createTx(
          inputBoxes = Array(emissionBoxIn, customInputBox),
          dataInputs = Array[InputBox](boxToTimestamp),
          boxesToCreate = Array(emissionBoxOutToCreate, timestampBoxToCreate),
          fee,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )

      val masterBox2OutTokens = (token, initialTokens - 2000)
      val masterBox2OutToCreate = KioskBox(
        Timestamp.masterAddress,
        value = 2000000,
        registers = Array(),
        tokens = Array(masterBox2OutTokens)
      )

      val timestampBox = createTimestampTx.getOutputsToSpend.get(1)

      noException should be thrownBy
        TxUtil.createTx(
          inputBoxes = Array(masterBoxOut, customInputBox),
          dataInputs = Array[InputBox](),
          boxesToCreate = Array(masterBox2OutToCreate, emissionBoxInToCreate),
          fee,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )

      the[Exception] thrownBy
        TxUtil.createTx(
          inputBoxes = Array(masterBoxOut, timestampBox, customInputBox),
          dataInputs = Array[InputBox](),
          boxesToCreate = Array(masterBox2OutToCreate, emissionBoxInToCreate),
          fee,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        ) should have message "Script reduced to false"
    }
  }
}
