package cli

import jde.compiler.Compiler.compileToJson

import scala.io.Source

object Compile {
  def main(args: Array[String]): Unit = {
    if (args.size != 1) println("Usage: java -jar jde.jar <script.json>")
    else {
      val resp = compileToJson(Source.fromFile(args(0)))
      println(resp)
    }
  }
}
