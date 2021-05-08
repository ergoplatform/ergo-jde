package jde.helpers

import jde.parser.Parser
import kiosk.ergo.usingSource

trait TraitTimestamp {
  val timestampSource = usingSource(scala.io.Source.fromFile("src/test/resources/timestamp.json"))(_.getLines.mkString)
  val timestampProgram = Parser.parse(timestampSource)
}
