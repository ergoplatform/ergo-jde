package cli

import ergo.{ErgoNode, WalletUtil}
import jde.compiler.Compiler.compile

import scala.io.Source

object Generate {
  def main(args: Array[String]): Unit = {
    if (args.size != 3) println("Usage: java -cp jde.jar cli.Generate <script.json> <nodeUrl> <api-key>")
    else {
      val compileResult = compile(Source.fromFile(args(0)))
      val request = new WalletUtil(new ErgoNode(args(1), Some(args(2)))).generateTx(compileResult)
      println(request)
    }
  }
}
