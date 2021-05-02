package jde.helpers

import jde.parser.Parser
import kiosk.ergo.usingSource

trait TraitTokenFilter {
  val tokenFilterSource: String = usingSource(scala.io.Source.fromFile("src/test/resources/token-filter.json"))(_.getLines.mkString)
  val tokenFilterProtocol = Parser.parse(tokenFilterSource)
}
