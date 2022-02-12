package kiosk.context

import kiosk.ergo.{DhtData, KioskBox}
import kiosk.tx.TxUtil
import org.ergoplatform.appkit._
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.hash.Blake2b256

class ContextSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val changeAddress = "9gQqZyxyjAptMbfW1Gydm3qaap11zd6X9DrABwgEE9eRdRvd27p"
  val minStorageRent = 1000000L

  property("Valid values") {

    val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

    ergoClient.execute { implicit ctx: BlockchainContext =>
      val dummyNanoErgs = 10000000000000L
      val dummyScript = "sigmaProp(true)"
      val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
      val dummyIndex = 1.toShort

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
              Context.address,
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

      val data = "hello world".getBytes()
      val hash: Array[Byte] = Blake2b256(data)

      TxUtil.createTx(
        Array(
          inBox.withContextVars(
            new ContextVar(0, ErgoValue.of(data)),
            new ContextVar(1, ErgoValue.of(hash))
          ),
          fundingBox
        ),
        Array[InputBox](),
        Array(
          KioskBox(
            Context.address,
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
    }
  }

  property("Invalid values") {

    val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

    ergoClient.execute { implicit ctx: BlockchainContext =>
      val dummyNanoErgs = 10000000000000L
      val dummyScript = "sigmaProp(true)"
      val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
      val dummyIndex = 1.toShort

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
              Context.address,
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

      val data = "hello world".getBytes()
      val hash: Array[Byte] = Blake2b256(data)

      the[Exception] thrownBy TxUtil.createTx(
        Array(
          inBox.withContextVars(
            new ContextVar(0, ErgoValue.of("wrong data".getBytes())),
            new ContextVar(1, ErgoValue.of(hash))
          ),
          fundingBox
        ),
        Array[InputBox](),
        Array(
          KioskBox(
            Context.address,
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
      ) should have message "Script reduced to false"
    }
  }
}
