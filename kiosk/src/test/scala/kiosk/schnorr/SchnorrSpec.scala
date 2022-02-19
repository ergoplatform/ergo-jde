package kiosk.schnorr

import kiosk.ergo.{DhtData, KioskBox, KioskCollByte, KioskGroupElement}
import kiosk.tx.TxUtil
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, HttpClientTesting, InputBox, _}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.hash.{Blake2b256, Sha256}
import sigmastate.basics.SecP256K1
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants
import special.sigma.GroupElement

import java.security.SecureRandom

class SchnorrSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val changeAddress = "9gQqZyxyjAptMbfW1Gydm3qaap11zd6X9DrABwgEE9eRdRvd27p"
  val minStorageRent = 1000000L
  val dummyNanoErgs = 10000000000000L
  val dummyScript = "sigmaProp(true)"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyIndex = 1.toShort

  val g: GroupElement = CryptoConstants.dlogGroup.generator
  val modulus = SecP256K1.order

  val x = BigInt(Blake2b256("alice")) // secret
  val Y = g.exp(x.bigInteger) // pubKey
  val M = "hello world".getBytes("UTF16") // message

  property("Valid signature") {

    val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

    ergoClient.execute { implicit ctx: BlockchainContext =>
      val fundingBox = ctx // for funding transactions
        .newTxBuilder()
        .outBoxBuilder
        .value(dummyNanoErgs)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, dummyIndex)

      def getInputBox(boxToCreate: KioskBox): InputBox = {
        TxUtil
          .createTx(
            Array(fundingBox),
            Array[InputBox](),
            Array(boxToCreate),
            fee = 1000000L,
            changeAddress,
            Array[String](),
            Array[DhtData](),
            false
          )
          .getOutputsToSpend
          .get(0)
      }
      def boxToCreate(pubKey: GroupElement, message: Array[Byte]): KioskBox = {
        KioskBox(
          Schnorr.address,
          minStorageRent,
          registers = Array(KioskGroupElement(pubKey), KioskCollByte(message)),
          tokens = Array()
        )
      }

      def sign(input: InputBox, M: Array[Byte], x: BigInt): InputBox = {
        val nonceBytes = new Array[Byte](32)
        SecureRandom.getInstanceStrong.nextBytes(nonceBytes)
        val r = BigInt(nonceBytes)
        val U = g.exp(r.bigInteger)
        val c: BigInt = BigInt(Sha256(U.getEncoded.toArray ++ M))
        val s = (r - c * x).mod(modulus).bigInteger
        input.withContextVars(new ContextVar(0, ErgoValue.of(c.toByteArray)), new ContextVar(1, ErgoValue.of(s)))
      }

      def verify(signedBox: InputBox) = {
        TxUtil.createTx(
          Array(signedBox, fundingBox),
          Array[InputBox](),
          Array(
            KioskBox(
              changeAddress,
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

      noException should be thrownBy {
        val unsignedInput: InputBox = getInputBox(boxToCreate(Y, M))
        val signedInput = sign(unsignedInput, M, x)
        verify(signedInput)
      }
    }
  }

  property("Invalid signature") {

    val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

    ergoClient.execute { implicit ctx: BlockchainContext =>
      val fundingBox = ctx // for funding transactions
        .newTxBuilder()
        .outBoxBuilder
        .value(dummyNanoErgs)
        .contract(ctx.compileContract(ConstantsBuilder.empty(), dummyScript))
        .build()
        .convertToInputWith(dummyTxId, dummyIndex)

      def getInputBox(boxToCreate: KioskBox): InputBox = {
        TxUtil
          .createTx(
            Array(fundingBox),
            Array[InputBox](),
            Array(boxToCreate),
            fee = 1000000L,
            changeAddress,
            Array[String](),
            Array[DhtData](),
            false
          )
          .getOutputsToSpend
          .get(0)
      }
      def boxToCreate(pubKey: GroupElement, message: Array[Byte]): KioskBox = {
        KioskBox(
          Schnorr.address,
          minStorageRent,
          registers = Array(KioskGroupElement(pubKey), KioskCollByte(message)),
          tokens = Array()
        )
      }

      def sign(input: InputBox, M: Array[Byte], x: BigInt): InputBox = { // wrong sign method, outputs invalid signature
        val nonceBytes = new Array[Byte](32)
        SecureRandom.getInstanceStrong.nextBytes(nonceBytes)
        val r = BigInt(nonceBytes)
        val U = g.exp(r.bigInteger)
        val c: BigInt = BigInt(Sha256(U.getEncoded.toArray ++ M))
        val s = (r - c * x + 1).mod(modulus).bigInteger // add 1 to s, making signature invalid
        input.withContextVars(new ContextVar(0, ErgoValue.of(c.toByteArray)), new ContextVar(1, ErgoValue.of(s)))
      }

      def verify(signedBox: InputBox) = {
        TxUtil.createTx(
          Array(signedBox, fundingBox),
          Array[InputBox](),
          Array(
            KioskBox(
              changeAddress,
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

      the[Exception] thrownBy {
        val unsignedInput: InputBox = getInputBox(boxToCreate(Y, M))
        val signedInput = sign(unsignedInput, M, x)
        verify(signedInput)
      } should have message "Script reduced to false"
    }
  }
}
