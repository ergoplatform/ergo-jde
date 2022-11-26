package kiosk.dexy

import kiosk.dexy.DexySpec._
import kiosk.ergo.{DhtData, KioskBox, KioskLong}
import kiosk.tx.TxUtil
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ErgoToken, HttpClientTesting}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

// Test Lp contracts for following path
// Swap Erg and Dexy tokens using constant product formula after taking fee into account
// ToDo: Check if any edge-cases remain and add tests for them
class LpSwapSpec  extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {
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

  property("Swap (sell Ergs) should work") {
    val oracleRateXy = 10000L
    val lpBalance = 100000000L
    val reservesXIn = 1000000000000L
    val reservesYIn = 100000000L

    val rate = reservesYIn.toDouble / reservesXIn
    val sellX = 10000000L
    val buyY = (sellX * rate * (feeDenomLp - feeNumLp) / feeDenomLp).toLong
    assert(buyY == 997)

    val reservesXOut = reservesXIn + sellX
    val reservesYOut = reservesYIn - buyY

    val deltaReservesX = reservesXOut - reservesXIn
    val deltaReservesY = reservesYOut - reservesYIn

    assert(BigInt(deltaReservesY) * reservesXIn * feeDenomLp >= BigInt(deltaReservesX) * (feeNumLp - feeDenomLp) * reservesYIn )

    ergoClient.execute { implicit ctx: BlockchainContext =>

      val fundingBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(fakeNanoErgs)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)

      val dummyBox = // ToDo: see if this can be removed
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(dummyNanoErgs)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId5, fakeIndex)

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

      val lpBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(reservesXIn)
          .tokens(new ErgoToken(lpNFT, 1), new ErgoToken(lpToken, lpBalance), new ErgoToken(dexyUSD, reservesYIn))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), lpScript))
          .build()
          .convertToInputWith(fakeTxId3, fakeIndex)

      val validLpOutBox = KioskBox(
        lpAddress,
        reservesXOut,
        registers = Array(),
        tokens = Array((lpNFT, 1), (lpToken, lpBalance), (dexyUSD, reservesYOut))
      )

      val dummyOutputBox = KioskBox(
        changeAddress,
        dummyNanoErgs,
        registers = Array(),
        tokens = Array((dexyUSD, buyY))
      )

      // all ok, swap should work
      noException shouldBe thrownBy {
        TxUtil.createTx(Array(lpBox, dummyBox, fundingBox), Array(oracleBox),
          Array(validLpOutBox, dummyOutputBox),
          fee = 1000000L,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )
      }
    }
  }

  property("Swap (sell Ergs) should fail if Lp address changed") {
    val oracleRateXy = 10000L
    val lpBalance = 100000000L
    val reservesXIn = 1000000000000L
    val reservesYIn = 100000000L

    val rate = reservesYIn.toDouble / reservesXIn
    val sellX = 10000000L
    val buyY = (sellX * rate * (feeDenomLp - feeNumLp) / feeDenomLp).toLong
    assert(buyY == 997)

    val reservesXOut = reservesXIn + sellX
    val reservesYOut = reservesYIn - buyY

    val deltaReservesX = reservesXOut - reservesXIn
    val deltaReservesY = reservesYOut - reservesYIn

    assert(BigInt(deltaReservesY) * reservesXIn * feeDenomLp >= BigInt(deltaReservesX) * (feeNumLp - feeDenomLp) * reservesYIn )

    ergoClient.execute { implicit ctx: BlockchainContext =>

      val fundingBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(fakeNanoErgs)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)

      val dummyBox = // ToDo: see if this can be removed
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(dummyNanoErgs)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId5, fakeIndex)

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

      val lpBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(reservesXIn)
          .tokens(new ErgoToken(lpNFT, 1), new ErgoToken(lpToken, lpBalance), new ErgoToken(dexyUSD, reservesYIn))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), lpScript))
          .build()
          .convertToInputWith(fakeTxId3, fakeIndex)

      val validLpOutBox = KioskBox(
        changeAddress,  // <--------------- this value is changed
        reservesXOut,
        registers = Array(),
        tokens = Array((lpNFT, 1), (lpToken, lpBalance), (dexyUSD, reservesYOut))
      )

      val dummyOutputBox = KioskBox(
        changeAddress,
        dummyNanoErgs,
        registers = Array(),
        tokens = Array((dexyUSD, buyY))
      )

      the[Exception] thrownBy {
        TxUtil.createTx(Array(lpBox, dummyBox, fundingBox), Array(oracleBox),
          Array(validLpOutBox, dummyOutputBox),
          fee = 1000000L,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )
      } should have message "Script reduced to false"
    }
  }

  property("Swap (sell Ergs) should work irrespective of oracle rate") {
    val lpInCirc = 10000L
    val oracleRateXy = 0L // set oracle rate to 0
    val lpBalance = 100000000L
    val reservesXIn = 1000000000000L
    val reservesYIn = 100000000L

    val rate = reservesYIn.toDouble / reservesXIn
    val sellX = 10000000L
    val buyY = (sellX * rate * (feeDenomLp - feeNumLp) / feeDenomLp).toLong
    assert(buyY == 997)

    val reservesXOut = reservesXIn + sellX
    val reservesYOut = reservesYIn - buyY

    val deltaReservesX = reservesXOut - reservesXIn
    val deltaReservesY = reservesYOut - reservesYIn

    assert(BigInt(deltaReservesY) * reservesXIn * feeDenomLp >= BigInt(deltaReservesX) * (feeNumLp - feeDenomLp) * reservesYIn)

    ergoClient.execute { implicit ctx: BlockchainContext =>

      val fundingBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(fakeNanoErgs)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)

      val dummyBox = // ToDo: see if this can be removed
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(dummyNanoErgs)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId5, fakeIndex)

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

      val lpBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(reservesXIn)
          .tokens(new ErgoToken(lpNFT, 1), new ErgoToken(lpToken, lpBalance), new ErgoToken(dexyUSD, reservesYIn))
          .registers(KioskLong(lpInCirc).getErgoValue)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), lpScript))
          .build()
          .convertToInputWith(fakeTxId3, fakeIndex)

      val validLpOutBox = KioskBox(
        lpAddress,
        reservesXOut,
        registers = Array(KioskLong(lpInCirc)),
        tokens = Array((lpNFT, 1), (lpToken, lpBalance), (dexyUSD, reservesYOut))
      )

      val dummyOutputBox = KioskBox(
        changeAddress,
        dummyNanoErgs,
        registers = Array(),
        tokens = Array((dexyUSD, buyY))
      )

      // all ok, swap should work
      noException shouldBe thrownBy {
        TxUtil.createTx(Array(lpBox, dummyBox, fundingBox), Array(oracleBox),
          Array(validLpOutBox, dummyOutputBox),
          fee = 1000000L,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )
      }
    }
  }

  property("Swap (sell Ergs) should not work if more Dexy taken") {
    val oracleRateXy = 10000L
    val lpBalance = 100000000L
    val reservesXIn = 1000000000000L
    val reservesYIn = 100000000L

    val rate = reservesYIn.toDouble / reservesXIn
    val sellX = 10000000L
    val buyY = (sellX * rate * (feeDenomLp - feeNumLp) / feeDenomLp).toLong + 1 // taken 1 more dexy token than allowed
    assert(buyY == 998)

    val reservesXOut = reservesXIn + sellX
    val reservesYOut = reservesYIn - buyY

    val deltaReservesX = reservesXOut - reservesXIn
    val deltaReservesY = reservesYOut - reservesYIn

    assert(BigInt(deltaReservesY) * reservesXIn * feeDenomLp < BigInt(deltaReservesX) * (feeNumLp - feeDenomLp) * reservesYIn )

    ergoClient.execute { implicit ctx: BlockchainContext =>

      val fundingBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(fakeNanoErgs)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)

      val dummyBox = // ToDo: see if this can be removed
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(dummyNanoErgs)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId5, fakeIndex)

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

      val lpBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(reservesXIn)
          .tokens(new ErgoToken(lpNFT, 1), new ErgoToken(lpToken, lpBalance), new ErgoToken(dexyUSD, reservesYIn))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), lpScript))
          .build()
          .convertToInputWith(fakeTxId3, fakeIndex)

      val validLpOutBox = KioskBox(
        lpAddress,
        reservesXOut,
        registers = Array(),
        tokens = Array((lpNFT, 1), (lpToken, lpBalance), (dexyUSD, reservesYOut))
      )

      val dummyOutputBox = KioskBox(
        changeAddress,
        dummyNanoErgs,
        registers = Array(),
        tokens = Array((dexyUSD, buyY))
      )

      an[Exception] shouldBe thrownBy {
        TxUtil.createTx(Array(lpBox, dummyBox, fundingBox), Array(oracleBox),
          Array(validLpOutBox, dummyOutputBox),
          fee = 1000000L,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )
      }
    }
  }

  property("Swap (sell Dexy) should work") {
    val oracleRateXy = 10000L
    val lpBalance = 100000000L
    val reservesXIn = 1000000000000L
    val reservesYIn = 100000000L

    val rate = reservesXIn.toDouble / reservesYIn
    val sellY = 1000L
    val buyX = (sellY * rate * (feeDenomLp - feeNumLp) / feeDenomLp).toLong
    assert(buyX == 9970000)

    val reservesXOut = reservesXIn - buyX
    val reservesYOut = reservesYIn + sellY

    val deltaReservesX = reservesXOut - reservesXIn
    val deltaReservesY = reservesYOut - reservesYIn

    assert(BigInt(deltaReservesX) * reservesYIn * feeDenomLp >= BigInt(deltaReservesY) * (feeNumLp - feeDenomLp) * reservesXIn)

    ergoClient.execute { implicit ctx: BlockchainContext =>

      val fundingBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(fakeNanoErgs)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)

      val dummyBox = // ToDo: see if this can be removed
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(dummyNanoErgs)
          .tokens(new ErgoToken(dexyUSD, sellY))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId5, fakeIndex)

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

      val lpBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(reservesXIn)
          .tokens(new ErgoToken(lpNFT, 1), new ErgoToken(lpToken, lpBalance), new ErgoToken(dexyUSD, reservesYIn))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), lpScript))
          .build()
          .convertToInputWith(fakeTxId3, fakeIndex)

      val validLpOutBox = KioskBox(
        lpAddress,
        reservesXOut,
        registers = Array(),
        tokens = Array((lpNFT, 1), (lpToken, lpBalance), (dexyUSD, reservesYOut))
      )

      val dummyOutputBox = KioskBox(
        changeAddress,
        dummyNanoErgs - deltaReservesX,
        registers = Array(),
        tokens = Array()
      )

      // all ok, swap should work
      noException shouldBe thrownBy {
        TxUtil.createTx(Array(lpBox, dummyBox, fundingBox), Array(oracleBox),
          Array(validLpOutBox, dummyOutputBox),
          fee = 1000000L,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )
      }
    }
  }

  property("Swap (sell Dexy) should not work if more Ergs taken") {
    val oracleRateXy = 10000L
    val lpBalance = 100000000L
    val reservesXIn = 1000000000000L
    val reservesYIn = 100000000L

    val rate = reservesXIn.toDouble / reservesYIn
    val sellY = 1000L
    val buyX = (sellY * rate * (feeDenomLp - feeNumLp) / feeDenomLp).toLong  + 1 // take one NanoErg extra
    assert(buyX == 9970001)

    val reservesXOut = reservesXIn - buyX
    val reservesYOut = reservesYIn + sellY

    val deltaReservesX = reservesXOut - reservesXIn
    val deltaReservesY = reservesYOut - reservesYIn

    assert(BigInt(deltaReservesX) * reservesYIn * feeDenomLp < BigInt(deltaReservesY) * (feeNumLp - feeDenomLp) * reservesXIn)

    ergoClient.execute { implicit ctx: BlockchainContext =>

      val fundingBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(fakeNanoErgs)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)

      val dummyBox = // ToDo: see if this can be removed
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(dummyNanoErgs)
          .tokens(new ErgoToken(dexyUSD, sellY))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId5, fakeIndex)

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

      val lpBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(reservesXIn)
          .tokens(new ErgoToken(lpNFT, 1), new ErgoToken(lpToken, lpBalance), new ErgoToken(dexyUSD, reservesYIn))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), lpScript))
          .build()
          .convertToInputWith(fakeTxId3, fakeIndex)

      val validLpOutBox = KioskBox(
        lpAddress,
        reservesXOut,
        registers = Array(),
        tokens = Array((lpNFT, 1), (lpToken, lpBalance), (dexyUSD, reservesYOut))
      )

      val dummyOutputBox = KioskBox(
        changeAddress,
        dummyNanoErgs - deltaReservesX,
        registers = Array(),
        tokens = Array()
      )

      an[Exception] shouldBe thrownBy {
        TxUtil.createTx(Array(lpBox, dummyBox, fundingBox), Array(oracleBox),
          Array(validLpOutBox, dummyOutputBox),
          fee = 1000000L,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )
      }
    }
  }

  property("No change should work") {
    val oracleRateXy = 10000L
    val lpBalance = 100000000L
    val reservesXIn = 1000000000000L
    val reservesYIn = 100000000L

    val reservesXOut = reservesXIn
    val reservesYOut = reservesYIn

    ergoClient.execute { implicit ctx: BlockchainContext =>

      val fundingBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(fakeNanoErgs)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)

      val dummyBox = // ToDo: see if this can be removed
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(dummyNanoErgs)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId5, fakeIndex)

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

      val lpBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(reservesXIn)
          .tokens(new ErgoToken(lpNFT, 1), new ErgoToken(lpToken, lpBalance), new ErgoToken(dexyUSD, reservesYIn))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), lpScript))
          .build()
          .convertToInputWith(fakeTxId3, fakeIndex)

      val validLpOutBox = KioskBox(
        lpAddress,
        reservesXOut,
        registers = Array(),
        tokens = Array((lpNFT, 1), (lpToken, lpBalance), (dexyUSD, reservesYOut))
      )

      val dummyOutputBox = KioskBox(
        changeAddress,
        dummyNanoErgs,
        registers = Array(),
        tokens = Array()
      )

      // all ok, swap should work
      noException shouldBe thrownBy {
        TxUtil.createTx(Array(lpBox, dummyBox, fundingBox), Array(oracleBox),
          Array(validLpOutBox, dummyOutputBox),
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
