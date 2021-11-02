package kiosk.dexy

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import scorex.util.encode.Base64
import kiosk.ergo._
import kiosk.script.ScriptUtil
import org.ergoplatform.appkit.HttpClientTesting
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class DexySpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val poolNFT = "472B4B6250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val lpNFT = "361A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val emissionScript =
    s"""{ 
       |  // this box: (dexyUSD emission box)
       |  // tokens(0): emissionNFT identifying the box
       |  // tokens(1): dexyUSD tokens to be emitted
       |  
       |  val poolNFT = fromBase64("${Base64.encode(poolNFT.decodeHex)}") 
       |  val lpNFT = fromBase64("${Base64.encode(lpNFT.decodeHex)}") // to identify LP box for future use
       |  
       |  val poolBox = CONTEXT.dataInputs(0) // oracle-pool (v1 and v2) box containing rate in R4
       |  
       |  val validPool = poolBox.tokens(0)._1 == poolNFT
       |  
       |  val poolRate = poolBox.R4[Long].get // can assume always > 0 (ref oracle pool contracts)
       |  
       |  val selfOut = OUTPUTS(0)
       |  
       |  val validSelfOut = selfOut.tokens(0) == SELF.tokens(0) && // emissionNFT and quantity preserved
       |                     selfOut.propositionBytes == SELF.propositionBytes && // script preserved
       |                     selfOut.tokens(1)._1 == SELF.tokens(1)._1 && // dexyUSD tokenId preserved
       |                     selfOut.value > SELF.value // can only purchase dexyUSD, not sell it
       |                     
       |  val inTokens = SELF.tokens(1)._2
       |  val outTokens = selfOut.tokens(1)._2
       |  
       |  val deltaErgs = selfOut.value - SELF.value // deltaErgs must be (+)ve because ergs must increase
       |  
       |  val deltaTokens = inTokens - outTokens // outTokens must be < inTokens (see below)
       |  
       |  val validDelta = deltaErgs >= deltaTokens * poolRate // deltaTokens must be (+)ve, since both deltaErgs and poolRate are (+)ve
       |  
       |  sigmaProp(validPool && validSelfOut && validDelta)
       |}
       |""".stripMargin

  val emissionErgoTree = ScriptUtil.compile(Map(), emissionScript)
  val emissionAddress = getStringFromAddress(getAddressFromErgoTree(emissionErgoTree))
  println(emissionAddress)

}
