package kiosk.avltree.ops

import kiosk.ergo.{ByteArrayToBetterByteArray, DhtData, KioskAvlTree, KioskBox, KioskCollByte}
import kiosk.tx.TxUtil
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, HttpClientTesting, InputBox}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.authds
import scorex.crypto.authds.avltree.batch.{BatchAVLProver, Insert}
import scorex.crypto.authds.{ADKey, ADValue}
import scorex.crypto.hash.{Blake2b256, Digest32}
import supertagged.@@

class AvlInsertSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {
  property("Insert") {

    val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

    ergoClient.execute { implicit ctx: BlockchainContext =>
      val minStorageRent = 1000000L

      val dummyNanoErgs = 10000000000000L
      val dummyScript = "sigmaProp(true)"
      val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
      val dummyIndex = 1.toShort

      val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"

      val fundingBox = ctx // for funding transactions
        .newTxBuilder()
        .outBoxBuilder
        .value(dummyNanoErgs)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, dummyIndex)

      val inBox = TxUtil
        .createTx(
          Array(fundingBox),
          Array[InputBox](),
          Array(
            KioskBox(
              AvlInsert.address,
              minStorageRent,
              registers = Array(),
              tokens = Array()
            )
          ),
          fee = 1000000L,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )
        .getOutputsToSpend
        .get(0)

      val KL = 32
      val VL = 8

      val dummyKey = ADKey @@ Array.fill(KL)(0.toByte).take(KL)
      val dummyValue = ADValue @@ Array.fill(VL)(1.toByte).take(VL)

      val avlProver = new BatchAVLProver[Digest32, Blake2b256.type](KL, Some(VL))
      avlProver.performOneOperation(Insert(dummyKey, dummyValue))
      avlProver.generateProof()

      val digestIn = avlProver.digest
      val inTree = KioskAvlTree(digestIn, KL, Some(VL))

      val newKey: Array[Byte] @@ authds.ADKey.Tag = ADKey @@ Array.fill(KL)(10.toByte).take(KL)
      val newValue = ADValue @@ Array.fill(KL)(20.toByte).take(VL)

      avlProver.performOneOperation(Insert(newKey, newValue))
      val digestOut = avlProver.digest
      val outTree = KioskAvlTree(digestOut, KL, Some(VL))

      val proof: Array[Byte] = avlProver.generateProof()

      TxUtil.createTx(
        Array(
          inBox,
          fundingBox
        ),
        Array[InputBox](),
        Array(
          KioskBox(
            AvlInsert.address,
            minStorageRent,
            registers = Array(inTree, outTree, KioskCollByte(newKey), KioskCollByte(newValue), KioskCollByte(proof)),
            tokens = Array()
          )
        ),
        fee = 1000000L,
        changeAddress,
        Array[String](),
        Array[DhtData](),
        false
      )
      val x = KioskBox(
        AvlInsert.address,
        minStorageRent,
        registers = Array(inTree, outTree, KioskCollByte(newKey), KioskCollByte(newValue), KioskCollByte(proof)),
        tokens = Array()
      )
    }
  }
}
