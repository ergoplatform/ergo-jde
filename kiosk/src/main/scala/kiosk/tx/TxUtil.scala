package kiosk.tx

import kiosk.encoding.ScalaErgoConverters.getAddressFromString
import kiosk.ergo.{DhtData, KioskBox, KioskType, Token, decodeBigInt}
import org.ergoplatform.appkit.{BlockchainContext, ErgoToken, InputBox, OutBox, OutBoxBuilder, SignedTransaction}
import org.ergoplatform.appkit.impl.ErgoTreeContract

import java.util

object TxUtil {
  private def addTokens(outBoxBuilder: OutBoxBuilder)(tokens: Seq[Token]) = {
    if (tokens.isEmpty) outBoxBuilder
    else {
      outBoxBuilder.tokens(tokens.map { token =>
        val (id, value) = token
        new ErgoToken(id, value)
      }: _*)
    }
  }

  private def addRegisters(
      outBoxBuilder: OutBoxBuilder
  )(registers: Array[KioskType[_]]) = {
    if (registers.isEmpty) outBoxBuilder
    else {
      outBoxBuilder.registers(registers.map(_.getErgoValue): _*)
    }
  }

  def createTx(
      inputBoxes: Array[InputBox],
      dataInputs: Array[InputBox],
      boxesToCreate: Array[KioskBox],
      fee: Long,
      changeAddress: String,
      proveDlogSecrets: Array[String],
      dhtData: Array[DhtData],
      broadcast: Boolean
  )(implicit ctx: BlockchainContext): SignedTransaction = {
    val txB = ctx.newTxBuilder
    val outputBoxes: Array[OutBox] = boxesToCreate.map { box =>
      val outBoxBuilder: OutBoxBuilder = txB
        .outBoxBuilder()
        .value(box.value)
        .creationHeight(box.creationHeight.getOrElse(ctx.getHeight))
        .contract(
          new ErgoTreeContract(getAddressFromString(box.address).script)
        )
      val outBoxBuilderWithTokens: OutBoxBuilder =
        addTokens(outBoxBuilder)(box.tokens)
      val outBox: OutBox =
        addRegisters(outBoxBuilderWithTokens)(box.registers).build
      outBox
    }
    val inputs = new util.ArrayList[InputBox]()

    inputBoxes.foreach(inputs.add)

    val dataInputBoxes = new util.ArrayList[InputBox]()

    dataInputs.foreach(dataInputBoxes.add)

    val txToSign = ctx
      .newTxBuilder()
      .boxesToSpend(inputs)
      .withDataInputs(dataInputBoxes)
      .outputs(outputBoxes: _*)
      .fee(fee)
      .sendChangeTo(getAddressFromString(changeAddress))
      .build()

    val proveDlogSecretsBigInt = proveDlogSecrets.map(decodeBigInt)

    val dlogProver = proveDlogSecretsBigInt.foldLeft(ctx.newProverBuilder()) {
      case (oldProverBuilder, newDlogSecret) =>
        oldProverBuilder.withDLogSecret(newDlogSecret.bigInteger)
    }

    val dhtProver = dhtData.foldLeft(dlogProver) {
      case (oldProverBuilder, dht) =>
        oldProverBuilder.withDHTData(
          dht.g,
          dht.h,
          dht.u,
          dht.v,
          dht.x.bigInteger
        )
    }

    val signedTx = dhtProver.build().sign(txToSign)
    if (broadcast) ctx.sendTransaction(signedTx)
    signedTx
  }
}
