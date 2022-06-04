package kiosk.mixer

import kiosk.ergo.{DhtData, KioskBox, KioskGroupElement, KioskType}
import kiosk.tx.TxUtil
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, HttpClientTesting, InputBox, _}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.hash.Blake2b256
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants
import special.sigma.GroupElement

import scala.util.Random

class MixSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val changeAddress = "9gQqZyxyjAptMbfW1Gydm3qaap11zd6X9DrABwgEE9eRdRvd27p"
  val minStorageRent = 1000000L
  val dummyNanoErgs = 10000000000000L
  val dummyScript = "sigmaProp(true)"
  val dummyTxId = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val dummyIndex = 1.toShort

  val g: GroupElement = CryptoConstants.dlogGroup.generator
  val mixerX = BigInt(Blake2b256("mixer")) // secret

  val dummyRegisters: Array[KioskType[_]] = Array(KioskGroupElement(g), KioskGroupElement(g)) // dummy registers needed in withdraw due to AOTC

  property("Valid mix with R4 = g") {

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
      def boxToCreate(r4: GroupElement, r5: GroupElement, r6: GroupElement): KioskBox = {
        KioskBox(
          Mix.address,
          minStorageRent,
          registers = Array(KioskGroupElement(r4), KioskGroupElement(r5), KioskGroupElement(r6)),
          tokens = Array()
        )
      }

      def getOutputBox(inBox: InputBox): (KioskBox, DhtData) = {
        val array = new Array[Byte](32)
        Random.nextBytes(array)

        val secret = BigInt(array)

        val oldR4: GroupElement = inBox.getRegisters.get(0).asInstanceOf[ErgoValue[GroupElement]].getValue
        val oldR5: GroupElement = inBox.getRegisters.get(1).asInstanceOf[ErgoValue[GroupElement]].getValue
        val oldR6: GroupElement = inBox.getRegisters.get(2).asInstanceOf[ErgoValue[GroupElement]].getValue

        val newR4 = oldR4.exp(secret.bigInteger)
        val newR5 = oldR5.exp(secret.bigInteger)
        val newR6 = oldR6

        (boxToCreate(newR4, newR5, newR6), DhtData(oldR4, oldR5, newR4, newR5, secret))
      }

      def mix(aliceInBox: InputBox, bobInBox: InputBox): (InputBox, InputBox, Boolean) = {
        val (aliceOutBox, aliceDhtData) = getOutputBox(aliceInBox)
        val (boxOutBox, bobDhtData) = getOutputBox(bobInBox)
        val b = Random.nextBoolean()
        val outputs = if (b) Array(aliceOutBox, boxOutBox) else Array(boxOutBox, aliceOutBox)

        val tx = TxUtil.createTx(
          Array(
            aliceInBox,
            bobInBox,
            fundingBox
          ),
          Array[InputBox](),
          outputs,
          fee = 1000000L,
          changeAddress,
          Array[String](mixerX.toString),
          Array[DhtData](aliceDhtData, bobDhtData),
          false
        )
        (tx.getOutputsToSpend.get(0), tx.getOutputsToSpend.get(1), b)
      }

      def withdraw(input: InputBox, dlogSecret: BigInt): InputBox = {
        val h = input.getRegisters.get(0).asInstanceOf[ErgoValue[GroupElement]].getValue
        val w = input.getRegisters.get(1).asInstanceOf[ErgoValue[GroupElement]].getValue

        val tx = TxUtil.createTx(
          Array(
            input,
            fundingBox
          ),
          Array[InputBox](),
          Array(
            KioskBox(changeAddress, input.getValue, registers = dummyRegisters, tokens = Array()),
            KioskBox(changeAddress, input.getValue + 1, registers = dummyRegisters, tokens = Array()) // dummy output needed due to AOTC
          ),
          fee = 1000000L,
          changeAddress,
          Array[String](),
          Array[DhtData](DhtData(h, h, w, w, dlogSecret)),
          false
        )
        tx.getOutputsToSpend.get(0)
      }

      val aliceX = BigInt(Blake2b256("alice")) // secret
      val bobX = BigInt(Blake2b256("bob")) // secret

      val aliceR4 = g
      val bobR4 = g

      val aliceR5: GroupElement = aliceR4.exp(aliceX.bigInteger)
      val bobR5: GroupElement = bobR4.exp(bobX.bigInteger)

      val mixerR6: GroupElement = g.exp(mixerX.bigInteger)

      val aliceBoxToCreate: KioskBox = boxToCreate(aliceR4, aliceR5, mixerR6)
      val bobBoxToCreate: KioskBox = boxToCreate(bobR4, bobR5, mixerR6)

      val aliceInBox: InputBox = getInputBox(aliceBoxToCreate)
      val bobInBox: InputBox = getInputBox(bobBoxToCreate)

      noException should be thrownBy {
        val (round0_out0, round0_out1, round0_bit) = mix(aliceInBox, bobInBox)
        val (aliceBox_round0, bobBox_round0) = if (round0_bit) (round0_out0, round0_out1) else (round0_out1, round0_out0)

        // withdraw
        withdraw(aliceBox_round0, aliceX)
        withdraw(bobBox_round0, bobX)

        // remix (we are mixing same boxes but in reality different boxes to be mixed when remixing)
        val (round1_out0, round1_out1, round1_bit) = mix(round0_out0, round0_out1)
        val (aliceBox_round1, bobBox_round1) = if (round1_bit) (round1_out0, round1_out1) else (round1_out1, round1_out0)

        // withdraw
        withdraw(aliceBox_round1, aliceX)
        withdraw(bobBox_round1, bobX)

        // due to a bug, this and next pass
        // see https://github.com/ScorexFoundation/sigmastate-interpreter/pull/636
        // also see https://github.com/ergoplatform/ergo-appkit/blob/3bf3213d394759e822ff029e6a1aee3104f3d4ca/appkit/src/test/scala/org/ergoplatform/appkit/MultiProveDHTSpec.scala
        withdraw(bobBox_round0, aliceX)
        withdraw(aliceBox_round0, 123)
      }
    }
  }
}
