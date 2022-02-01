package kiosk.avltree.bank

import kiosk.ErgoUtil.{addressToGroupElement => addr2Grp}
import kiosk.avltree.bank.Bank.{minStorageRent, timeOut}
import kiosk.encoding.ScalaErgoConverters
import kiosk.encoding.ScalaErgoConverters.{stringToGroupElement => str2Grp}
import kiosk.ergo.{DhtData, KioskAvlTree, KioskBox, KioskCollByte, KioskGroupElement, KioskInt}
import kiosk.tx.TxUtil
import org.ergoplatform.appkit._
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.authds
import scorex.crypto.authds.avltree.batch.{BatchAVLProver, Insert}
import scorex.crypto.authds.{ADKey, ADValue}
import scorex.crypto.hash.{Blake2b256, Digest32}
import sigmastate.eval.CostingSigmaDslBuilder.longToByteArray
import supertagged.@@

class MakeDefunctSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val bankNFT = "4b2d8b7beb3eaac8234d9e61792d270898a43934d6a27275e4f3a044609c9f2e"
  val bankTokenId = "7dbf52f31e8ba5b2be2e11201a24a4e286f8f6f8aeb0fa22937e80f11041f28b"
  val bankPubKey = KioskGroupElement(str2Grp(addr2Grp("9eiuh5bJtw9oWDVcfJnwTm1EHfK5949MEm5DStc2sD1TLwDSrpx")))
  val bankSecret = "37cc5cb5b54f98f92faef749a53b5ce4e9921890d9fb902b4456957d50791bd0"
  val bankTokenAmount = 10000L

  val changeAddress = "9f5ZKbECVTm25JTRQHDHGM5ehC8tUw5g1fCBQ4aaE792rWBFrjK"

  val isNotDefunct = KioskInt(0)
  val isDefunct = KioskInt(1)

  // for keeping in the inputs context vars due to AOTC
  val dummyCollByte1 = KioskCollByte("1".getBytes())
  val dummyCollByte2 = KioskCollByte("2".getBytes())

  // for fake funding box
  val fakeNanoErgs = 10000000000000L
  val fakeScript = "sigmaProp(true)"
  val fakeTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyIndex = 1.toShort
  val fakeTokenId = "44743777217A25432A46294A404E635266556A586E3272357538782F413F4428"

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

  val KL = 32
  val VL = 8

  val firstPubKey = ADKey @@ Array.fill(256)(0.toByte)
  val firstValue = ADValue @@ longToByteArray(1234L).toArray
  val firstKey: Array[Byte] @@ authds.ADKey.Tag = ADKey @@ Blake2b256(firstPubKey).take(KL)

  val secondAddress = "9f9q6Hs7vXZSQwhbrptQZLkTx15ApjbEkQwWXJqD2NpaouiigJQ"
  val secondPubKey: Array[Byte] = ScalaErgoConverters.getAddressFromString(secondAddress).script.bytes
  val secondBalance = 100L
  val secondValue = ADValue @@ longToByteArray(secondBalance).toArray
  val secondKey: Array[Byte] @@ authds.ADKey.Tag = ADKey @@ Blake2b256(secondPubKey).take(KL)

  val avlProver = new BatchAVLProver[Digest32, Blake2b256.type](KL, Some(VL))

  avlProver.performOneOperation(Insert(firstKey, firstValue))
  avlProver.performOneOperation(Insert(secondKey, secondValue))
  avlProver.generateProof()

  property("Anyone can make defunct if height is crossed") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      val fundingBox =
        ctx // for funding transactions
          .newTxBuilder()
          .outBoxBuilder
          .value(fakeNanoErgs)
          .tokens(new ErgoToken(bankNFT, 100000000L), new ErgoToken(bankTokenId, 100000000L), new ErgoToken(fakeTokenId, 10000000L))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId, dummyIndex)

      // user can make defunct if height is crossed
      noException shouldBe thrownBy {
        val bankBox = TxUtil
          .createTx(
            Array(fundingBox),
            Array[InputBox](),
            Array(
              KioskBox(
                Bank.bankAddress,
                minStorageRent,
                registers = Array(KioskAvlTree(avlProver.digest, KL, Some(VL)), bankPubKey, isNotDefunct),
                tokens = Array((bankNFT, 1), (bankTokenId, bankTokenAmount)),
                creationHeight = Some(0)
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

        TxUtil.createTx(
          Array(
            bankBox,
            fundingBox
          ),
          Array[InputBox](),
          Array(
            KioskBox(
              Bank.bankAddress,
              minStorageRent,
              registers = Array(KioskAvlTree(avlProver.digest, KL, Some(VL)), bankPubKey, isDefunct),
              tokens = Array((bankNFT, 1), (bankTokenId, bankTokenAmount)),
              creationHeight = Some(ctx.getHeight)
            ),
            KioskBox(
              changeAddress,
              minStorageRent,
              registers = Array(dummyCollByte1, dummyCollByte2),
              tokens = Array((fakeTokenId, 1))
            )
          ),
          fee = 1000000L,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )
      }
    }
  }

  property("Cannot make defunct if height is not crossed") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      val fundingBox =
        ctx // for funding transactions
          .newTxBuilder()
          .outBoxBuilder
          .value(fakeNanoErgs)
          .tokens(new ErgoToken(bankNFT, 100000000L), new ErgoToken(bankTokenId, 100000000L), new ErgoToken(fakeTokenId, 10000000L))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId, dummyIndex)

      the[Exception] thrownBy {
        val avlProver = new BatchAVLProver[Digest32, Blake2b256.type](KL, Some(VL))
        avlProver.performOneOperation(Insert(firstKey, firstValue))
        avlProver.performOneOperation(Insert(secondKey, secondValue))
        avlProver.generateProof()

        val bankBox = TxUtil
          .createTx(
            Array(fundingBox),
            Array[InputBox](),
            Array(
              KioskBox(
                Bank.bankAddress,
                minStorageRent,
                registers = Array(KioskAvlTree(avlProver.digest, KL, Some(VL)), bankPubKey, isNotDefunct),
                tokens = Array((bankNFT, 1), (bankTokenId, bankTokenAmount)),
                creationHeight = Some(ctx.getHeight - timeOut)
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

        TxUtil.createTx(
          Array(
            bankBox,
            fundingBox
          ),
          Array[InputBox](),
          Array(
            KioskBox(
              Bank.bankAddress,
              minStorageRent,
              registers = Array(KioskAvlTree(avlProver.digest, KL, Some(VL)), bankPubKey, isDefunct),
              tokens = Array((bankNFT, 1), (bankTokenId, bankTokenAmount)),
              creationHeight = Some(ctx.getHeight)
            ),
            KioskBox(
              changeAddress,
              minStorageRent,
              registers = Array(dummyCollByte1, dummyCollByte2),
              tokens = Array((fakeTokenId, 1))
            )
          ),
          fee = 1000000L,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )
      } should have message "Script reduced to false"
    }
  }
}
