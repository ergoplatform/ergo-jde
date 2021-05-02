package cli

import jde.compiler.TxBuilder.compileFromSource

import scala.io.Source

object CLI {
  def main(args: Array[String]): Unit = {
    if (args.size != 1) println("Usage java -jar <jarFile> <script.json>")
    else {
      val resp = compileFromSource(Source.fromFile(args(0)))
      println(resp)
    }
  }
}
