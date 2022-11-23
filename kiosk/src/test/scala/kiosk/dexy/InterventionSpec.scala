package kiosk.dexy

import kiosk.dexy.DexySpec._
import kiosk.ergo.{DhtData, KioskBoolean, KioskBox, KioskInt, KioskLong}
import kiosk.tx.TxUtil
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ErgoToken, HttpClientTesting}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class InterventionSpec  extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {
  val lpToken = "4b2d8b7beb3eaac8234d9e61792d270898a43934d6a27275e4f3a044609c9f2a" // LP tokens
  val dexyUSD = "4b2d8b7beb3eaac8234d9e61792d270898a43934d6a27275e4f3a044609c9f2b" // Dexy token

  lazy val minStorageRent = 100000L

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))
  val fakeNanoErgs = 10000000000000L
  val dummyNanoErgs = 100000L
  val fakeScript = "sigmaProp(true)"
  val fakeTxId1 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val fakeTxId2 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b808"
  val fakeTxId3 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b807"
  val fakeTxId4 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b806"
  val fakeTxId5 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b105"
  val fakeIndex = 1.toShort
  val changeAddress = "9gQqZyxyjAptMbfW1Gydm3qaap11zd6X9DrABwgEE9eRdRvd27p"

  property("Intervention (transfer Ergs from Bank to Lp and Dexy from Lp to Bank should work") {
    val lpBalanceIn = 100000000L

    val thresholdPercent = 98

    val bankReservesXIn = 1000000000000000L // Nano Ergs
    val bankReservesYIn = 10000000000L // Dexy

    val lpReservesXIn = 100000000000000L
    val lpReservesYIn = 10000000000L

    val lpRateXyIn = lpReservesXIn / lpReservesYIn
    val oracleRateXy = lpRateXyIn * 100 / thresholdPercent + 1

    val depositX = 5000000000000L
    val withdrawY = (BigInt(depositX) * lpReservesYIn / lpReservesXIn).toLong

    val lpReservesXOut = lpReservesXIn + depositX
    val lpReservesYOut = lpReservesYIn - withdrawY

    val bankReservesXOut = bankReservesXIn - depositX // Nano Ergs
    val bankReservesYOut = bankReservesYIn + withdrawY // Dexy

    val lpBalanceOut = lpBalanceIn

    val lpRateXyOut = lpReservesXOut / lpReservesYOut

    assert(lpRateXyOut * 100 >= oracleRateXy * 101)

    ergoClient.execute { implicit ctx: BlockchainContext =>

      val T_int = 20
      val T = 100
      val trackingHeightIn = ctx.getHeight - T_int - 1

      val lastInterventionHeight = ctx.getHeight - T_int - T - 1

      val fundingBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(fakeNanoErgs)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)

      val oracleBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minStorageRent)
          .tokens(new ErgoToken(oracleNFT, 1))
          .registers(KioskLong(oracleRateXy).getErgoValue)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId2, fakeIndex)

      val tracking98Box =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minStorageRent)
          .tokens(new ErgoToken(tracking98NFT, 1))
          .registers(
            KioskInt(49).getErgoValue, // numerator for 98%
            KioskInt(50).getErgoValue, // denominator for 98%
            KioskBoolean(true).getErgoValue, // isBelow
            KioskInt(trackingHeightIn).getErgoValue
          )
          .contract(ctx.compileContract(ConstantsBuilder.empty(), DexySpec.trackingScript))
          .build()
          .convertToInputWith(fakeTxId4, fakeIndex)

      val lpBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(lpReservesXIn)
          .tokens(new ErgoToken(lpNFT, 1), new ErgoToken(lpToken, lpBalanceIn), new ErgoToken(dexyUSD, lpReservesYIn))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), lpScript))
          .build()
          .convertToInputWith(fakeTxId3, fakeIndex)

      val bankBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(bankReservesXIn)
          .tokens(new ErgoToken(bankNFT, 1), new ErgoToken(dexyUSD, bankReservesYIn))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), bankScript))
          .build()
          .convertToInputWith(fakeTxId4, fakeIndex)

      val interventionBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minStorageRent)
          .tokens(new ErgoToken(interventionNFT, 1))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), interventionScript))
          .creationHeight(lastInterventionHeight)
          .build()
          .convertToInputWith(fakeTxId5, fakeIndex)

      val validLpOutBox = KioskBox(
        lpAddress,
        lpReservesXOut,
        registers = Array(),
        tokens = Array((lpNFT, 1), (lpToken, lpBalanceOut), (dexyUSD, lpReservesYOut))
      )

      val validBankOutBox = KioskBox(
        bankAddress,
        bankReservesXOut,
        registers = Array(),
        tokens = Array((bankNFT, 1), (dexyUSD, bankReservesYOut))
      )

      val validInterventionOutBox = KioskBox(
        interventionAddress,
        minStorageRent,
        registers = Array(),
        tokens = Array((interventionNFT, 1))
      )

      // all ok, intervention should work
      noException shouldBe thrownBy {
        TxUtil.createTx(
          Array(lpBox, bankBox, interventionBox, fundingBox),
          Array(oracleBox, tracking98Box),
          Array(validLpOutBox, validBankOutBox, validInterventionOutBox),
          fee = 1000000L,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )
      }
    }
  }

}
