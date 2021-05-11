package cli

import ergo.{ErgoNode, WalletUtil}
import jde.compiler.Compiler.compile

import scala.io.Source

object Request {
  def main(args: Array[String]): Unit = {
    if (args.size != 2) println("Usage: java -cp jde.jar cli.Request <script.json> <nodeUrl>")
    else {
      val compileResult = compile(Source.fromFile(args(0)))
      val request = new WalletUtil(new ErgoNode(args(1))).getRequest(compileResult)
      println(request)
    }
  }
}
