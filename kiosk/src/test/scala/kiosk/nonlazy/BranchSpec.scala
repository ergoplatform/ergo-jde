package kiosk.nonlazy

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.ergo.{DhtData, KioskBox, KioskCollByte, KioskInt, KioskLong}
import kiosk.script.ScriptUtil
import kiosk.tx.TxUtil
import org.ergoplatform.appkit._
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
   This test shows non-lazy evaluation in Ergo (which should actually be lazy as per design, but is not due to AOTC issue)
 */

class BranchSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val branchScript =
    s"""{ 
       |  val ok = if (OUTPUTS(0).R4[Int].get == 1) {
       |    CONTEXT.dataInputs(0).R4[Long].get <= SELF.value
       |  } else { // assume Coll[Byte]
       |    CONTEXT.dataInputs(0).R4[Coll[Byte]].get != SELF.propositionBytes
       |  }
       |  sigmaProp(ok)
       |}
       |""".stripMargin

  val branchErgoTree = ScriptUtil.compile(Map(), branchScript)

  val branchBoxAddress = getStringFromAddress(getAddressFromErgoTree(branchErgoTree))

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))
  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "{sigmaProp(1 < 2)}"

  property("Not-so-lazy evaluation") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      assert(branchBoxAddress == "88dwYDNXcCq9UyA7VBcSdqJRgooKVqS8ixprCknxcm2sba4jbhQYGphjutEebtr3ZeC4tmT9oEWKS2Bq")

      val fee = 1500000

      val branchBoxToCreate = KioskBox(
        branchBoxAddress,
        value = 2000000,
        registers = Array(),
        tokens = Array()
      )

      // dummy custom input box for funding various transactions
      val customInputBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      val branchBoxCreationTx: SignedTransaction = TxUtil.createTx(
        inputBoxes = Array(customInputBox),
        dataInputs = Array[InputBox](),
        boxesToCreate = Array(branchBoxToCreate),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      val dataBoxWithLong = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .registers(KioskLong(1L).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      val branchBox = branchBoxCreationTx.getOutputsToSpend.get(0)

      val dataBoxWithCollByte = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(10000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .registers(KioskCollByte("hello".getBytes()).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      val longSelectionBox = KioskBox(
        changeAddress,
        value = 2000000,
        registers = Array(KioskInt(1)),
        tokens = Array()
      )

      val collByteSelectionBox = KioskBox(
        changeAddress,
        value = 2000000,
        registers = Array(KioskInt(2)),
        tokens = Array()
      )

      noException should be thrownBy TxUtil.createTx(
        inputBoxes = Array(branchBox, customInputBox),
        dataInputs = Array[InputBox](dataBoxWithCollByte),
        boxesToCreate = Array(collByteSelectionBox),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      // below should work ideally (with truly lazy evaluation). However, it currently fails
      an[Exception] should be thrownBy
        TxUtil.createTx(
          inputBoxes = Array(branchBox, customInputBox),
          dataInputs = Array[InputBox](dataBoxWithLong),
          boxesToCreate = Array(longSelectionBox),
          fee,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )
    }
  }
}
