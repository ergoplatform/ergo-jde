package jde.compiler

import jde.compiler.model._
import jde.parser.Parser._
import kiosk.ergo
import kiosk.ergo.{Amount, ID, usingSource}
import kiosk.explorer.Explorer
import play.api.libs.json.Json

import scala.io.BufferedSource

object Compiler {
  def compileFromSource(bufferedSource: BufferedSource) = {
    val script = usingSource(bufferedSource)(_.mkString)
    Json.prettyPrint(Json.toJson(new Compiler(new Explorer).compile(parse(script))))
  }
}

class Compiler(explorer: Explorer) {
  def compile(program: Program) = {
    implicit val dictionary = new Dictionary(explorer.getHeight)
    // Step 1. validate that constants are properly encoded
    optSeq(program.constants).map(_.getValue)
    // Step 2. load declarations (also does semantic validation)
    (new OffChainLoader).load(program)
    // Step 3. load on-chain declarations
    new OnChainLoader(explorer).load(program)
    // Step 4. validate post-conditions
    optSeq(program.postConditions).foreach(_.validate)
    // Step 5. build outputs
    val outputs = (new Builder).buildOutputs(program)
    // Step 6. compute values to return
    val returnedValues = optSeq(program.returns).map { name =>
      val declaration: Declaration = dictionary.getDeclaration(name)
      val values: Seq[ergo.KioskType[_]] = declaration.getValue.seq
      ReturnedValue(name, declaration.`type`, values)
    }
    val outTokens: Map[ID, Amount] = outputs.flatMap(_.tokens).groupBy(_._1).map { case (id, seq) => id -> seq.map(_._2).sum }
    // Return final result
    CompileResult(
      dictionary.getDataInputBoxIds,
      dictionary.getInputBoxIds,
      dictionary.getInputNanoErgs,
      dictionary.getInputTokens,
      outputs,
      outputs.map(_.value).sum,
      outTokens.toSeq,
      program.fee,
      returnedValues
    )
  }
}
