package kiosk.nonlazy

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.ergo.{DhtData, KioskBox, KioskCollByte, KioskInt, KioskLong}
import kiosk.script.ScriptUtil
import kiosk.tx.TxUtil
import org.ergoplatform.appkit._
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.hash.Blake2b256

/*
  This example shows how we can overcome the non-lazy evaluation in branching code by splitting it into two boxes
 */
class SplitSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val splitScript =
    s"""{
       |  val validIn = SELF.id == INPUTS(0).id
       |  val ok = if (OUTPUTS(0).R4[Int].get == 1) {
       |    blake2b256(INPUTS(1).propositionBytes) == leftBranchBytesHash
       |  } else {
       |    blake2b256(INPUTS(1).propositionBytes) == rightBranchBytesHash
       |  }
       |  sigmaProp(ok && validIn)
       |}
       |""".stripMargin

  val leftScript =
    s"""{
       |  sigmaProp(CONTEXT.dataInputs(0).R4[Long].get <= INPUTS(0).value)
       |}""".stripMargin

  val rightScript =
    s"""{
       |  sigmaProp(CONTEXT.dataInputs(0).R4[Coll[Byte]].get != INPUTS(0).propositionBytes)
       |}""".stripMargin

  val leftErgoTree = ScriptUtil.compile(Map(), leftScript)
  val rightErgoTree = ScriptUtil.compile(Map(), rightScript)

  val splitErgoTree = ScriptUtil.compile(
    Map(
      "leftBranchBytesHash" -> KioskCollByte(Blake2b256(leftErgoTree.bytes)),
      "rightBranchBytesHash" -> KioskCollByte(Blake2b256(rightErgoTree.bytes))
    ),
    splitScript
  )

  val splitAddress = getStringFromAddress(getAddressFromErgoTree(splitErgoTree))
  val leftAddress = getStringFromAddress(getAddressFromErgoTree(leftErgoTree))
  val rightAddress = getStringFromAddress(getAddressFromErgoTree(rightErgoTree))

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))
  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyScript = "{sigmaProp(1 < 2)}"

  property("Not-so-lazy evaluation") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      assert(
        splitAddress == "2PELCgrp5nNgVKMAEky7GjT8VxL7Xsc7z7ocVcEW4e1zhKSrzwVSavg3C4AbbN2xM4vRSFQv4EVDarTChJnwg6wwEURFj5VjMv7nVpAm8jaahzZZoJJqJRHaEu2zteSzMXsYBHGsQDD5m5JPsp3hkZ8qzXcgBd29TzTfEqh9i8FnFe3X"
      )

      val fee = 1500000

      val splitBoxToCreate = KioskBox(
        splitAddress,
        value = 2000000,
        registers = Array(),
        tokens = Array()
      )

      val leftBranchBoxToCreate = KioskBox(
        leftAddress,
        value = 2000000,
        registers = Array(),
        tokens = Array()
      )

      val rightBranchBoxToCreate = KioskBox(
        rightAddress,
        value = 2000000,
        registers = Array(),
        tokens = Array()
      )

      // dummy custom input box for funding various transactions
      val customInputBox = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(20000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, 0)

      val splitBoxCreationTx: SignedTransaction = TxUtil.createTx(
        inputBoxes = Array(customInputBox),
        dataInputs = Array[InputBox](),
        boxesToCreate = Array(splitBoxToCreate, leftBranchBoxToCreate, rightBranchBoxToCreate),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      val splitBox = splitBoxCreationTx.getOutputsToSpend.get(0)
      val leftBranchBox = splitBoxCreationTx.getOutputsToSpend.get(1)
      val rightBranchBox = splitBoxCreationTx.getOutputsToSpend.get(2)

      val leftSelectionBox = KioskBox(
        changeAddress,
        value = 2000000,
        registers = Array(KioskInt(1)),
        tokens = Array()
      )

      val rightSelectionBox = KioskBox(
        changeAddress,
        value = 2000000,
        registers = Array(KioskInt(2)),
        tokens = Array()
      )

      val dataBoxWithLong = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(1000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .registers(KioskLong(1L).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      val dataBoxWithCollByte = ctx
        .newTxBuilder()
        .outBoxBuilder
        .value(1000000L)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .registers(KioskCollByte("hello".getBytes()).getErgoValue)
        .build()
        .convertToInputWith(dummyTxId, 0)

      noException should be thrownBy TxUtil.createTx(
        inputBoxes = Array(splitBox, leftBranchBox),
        dataInputs = Array[InputBox](dataBoxWithLong),
        boxesToCreate = Array(leftSelectionBox),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )

      noException should be thrownBy TxUtil.createTx(
        inputBoxes = Array(splitBox, rightBranchBox),
        dataInputs = Array[InputBox](dataBoxWithCollByte),
        boxesToCreate = Array(rightSelectionBox),
        fee,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )
    }
  }
}
