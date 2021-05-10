package ergo

import jde.compiler.model.CompileResult
import kiosk.ergo.{KioskBox, KioskType, Token}
import play.api.libs.json.Json

class WalletUtil(ergoNode: ErgoNode) {
  private def getBoxesRaw(ids: Seq[String]): Seq[String] =
    ids.map { id => ergoNode.getBoxRaw(id).getOrElse(throw new Exception(s"Unable to get box with id $id")) }

  private def toTokenRequest(token: Token) = {
    val (id, amount) = token
    s"""{
       |  "tokenId": "$id",
       |  "amount": $amount
       |}""".stripMargin
  }

  private def toRegisterRequest(value: KioskType[_], index: Int) = {
    s""""R${index + 4}": "${value.hex}"""".stripMargin
  }

  private def toOutputRequest(output: KioskBox) = {
    s"""
       |{
       |  "address": "${output.address}",
       |  "value": ${output.value},
       |  "assets": [
       |    ${output.tokens.map(toTokenRequest).mkString(",")}
       |  ],
       |  "registers": {
       |    ${output.registers.zipWithIndex.map { case (value, index) => toRegisterRequest(value, index) }.mkString(",")}
       |  }
       |}""".stripMargin
  }

  private def toInputRequest(inputRaw: String) = {
    s""""$inputRaw""""
  }

  def getRequest(compileResult: CompileResult) = {
    import compileResult._
    if (inputNanoErgs != outputNanoErgs)
      throw new Exception(s"Input nanoErgs ($inputNanoErgs) - Output nanoErgs ($outputNanoErgs) != 0 (${inputNanoErgs - outputNanoErgs})")
    if (inputTokens.toSet != outputTokens.toSet) throw new Exception(s"Input tokens != Output tokens")
    val inputs = getBoxesRaw(inputBoxIds)
    val dataInputs = getBoxesRaw(dataInputBoxIds)
    Json.prettyPrint(
      Json.parse(
        s"""|{
            |  "requests": [
            |    ${outputs.map(toOutputRequest).mkString(",")}
            |  ],
            |  "inputsRaw": [
            |    ${inputs.map(toInputRequest).mkString(",")}
            |  ],
            |  "dataInputsRaw": [
            |    ${dataInputs.map(toInputRequest).mkString(",")}
            |  ]
            |}
            |""".stripMargin
      )
    )
  }

  def generateTx(compileResult: CompileResult) = {
    ergoNode.generate(getRequest(compileResult)) match {
      case Right(json) => json
      case Left(ex) =>
        ex.printStackTrace()
        throw new Exception(ex)
    }
  }

  def sendTx(compileResult: CompileResult) = {
    ergoNode.send(getRequest(compileResult)) match {
      case Right(json) => json
      case Left(ex) =>
        ex.printStackTrace()
        throw new Exception(ex)
    }
  }
}
