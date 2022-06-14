package kiosk.dexy

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import scorex.util.encode.Base64
import kiosk.ergo._
import kiosk.script.ScriptUtil
import org.ergoplatform.appkit.HttpClientTesting
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class DexySpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val oraclePoolNFT = "472B4B6250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val lpNFT = "361A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val trackingNFT = "261A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val interventionNFT = "161A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val bankNFT = "061A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val bankScript =
    s"""{ 
       |  // This box: (dexyUSD bank box)
       |  //   tokens(0): bankNFT identifying the box
       |  //   tokens(1): dexyUSD tokens to be emitted
       |  
       |  val selfOutIndex = getVar[Int](0).get
       |  
       |  val oraclePoolNFT = fromBase64("${Base64.encode(oraclePoolNFT.decodeHex)}") // to identify oracle pool box
       |  val interventionNFT = fromBase64("${Base64.encode(interventionNFT.decodeHex)}") // to identify intervention box for future use
       |  val lpNFT = fromBase64("${Base64.encode(lpNFT.decodeHex)}")
       |   
       |  val validBank = {
       |    val oraclePoolBox = CONTEXT.dataInputs(0) // oracle-pool (v1 and v2) box containing rate in R4
       |    
       |    val lpBox = CONTEXT.dataInputs(1) 
       |  
       |    val validOP = oraclePoolBox.tokens(0)._1 == oraclePoolNFT
       |    
       |    val validLP = lpBox.tokens(0)._1 == lpNFT
       |  
       |    val oraclePoolRate = oraclePoolBox.R4[Long].get // can assume always > 0 (ref oracle pool contracts) NanoErgs per USD
       |
       |    val lpReservesX = lpBox.value
       |    
       |    val lpReservesY = lpBox.tokens(2)._2
       |    
       |    val lpRate = lpReservesX / lpReservesY  
       |    
       |    val validRateFreeMint = 98 * lpRate < oraclePoolRate * 100 &&  
       |                            oraclePoolRate * 100 < 102 * lpRate 
       |     
       |    val selfOut = OUTPUTS(selfOutIndex)
       |  
       |    val validSelfOut = selfOut.tokens(0) == SELF.tokens(0) && // bankNFT and quantity preserved
       |                       selfOut.propositionBytes == SELF.propositionBytes && // script preserved
       |                       selfOut.tokens(1)._1 == SELF.tokens(1)._1 && // dexyUSD tokenId preserved
       |                       selfOut.value > SELF.value // can only purchase dexyUSD, not sell it
       |                     
       |    val inTokens = SELF.tokens(1)._2
       |    val outTokens = selfOut.tokens(1)._2
       |  
       |    val deltaErgs = selfOut.value - SELF.value // deltaErgs must be (+)ve because ergs must increase
       |  
       |    val deltaTokens = inTokens - outTokens // outTokens must be < inTokens (see below)
       |  
       |    val validDelta = deltaErgs >= deltaTokens * oraclePoolRate // deltaTokens must be (+)ve, since both deltaErgs and oraclePoolRate are (+)ve
       |  
       |    validOP && validLP && validSelfOut && validDelta && validRateFreeMint
       |  }
       |  
       |  val validIntervention = INPUTS(0).tokens(0)._1 == interventionNFT
       |  
       |  sigmaProp(validBank || validIntervention)
       |}
       |""".stripMargin

  // below contract is adapted from N2T DEX contract in EIP-14 https://github.com/ergoplatform/eips/blob/de30f94ace1c18a9772e1dd0f65f00caf774eea3/eip-0014.md?plain=1#L558-L636
  val lpScript =
    s"""{
       |    // Notation:
       |    // 
       |    // X is the primary token
       |    // Y is the secondary token 
       |    // When using Erg-USD oracle v1, X is NanoErg and Y is USD   
       |
       |    // This box: (LP box)
       |    //   R1 (value): X tokens in NanoErgs 
       |    //   R4: How many LP in circulation (long). This can be non-zero when bootstrapping, to consider the initial token burning in UniSwap v2
       |    //   R5: Stores the height where oracle pool rate becomes lower than LP rate. Reset to Long.MaxValue when rate crossed back. Called crossTracker below
       |    //   Tokens(0): LP NFT to uniquely identify NFT box. (Could we possibly do away with this?) 
       |    //   Tokens(1): LP tokens
       |    //   Tokens(2): Y tokens (Note that X tokens are NanoErgs (the value) 
       |    //   
       |    // Data Input #0: (oracle pool box)
       |    //   R4: Rate in units of X per unit of Y
       |    //   Token(0): OP NFT to uniquely identify Oracle Pool
       |     
       |    // constants
       |    val threshold = 3 // error threshold in crossTracker
       |    val feeNum = 3 // 0.3 % 
       |    val feeDenom = 1000
       |    val minStorageRent = 10000000L  // this many number of nanoErgs are going to be permanently locked
       |    
       |    val successor = OUTPUTS(0) // copy of this box after exchange
       |    val oraclePoolBox = CONTEXT.dataInputs(0) // oracle pool box
       |    val validOraclePoolBox = oraclePoolBox.tokens(0)._1 == fromBase64("${Base64
      .encode(oraclePoolNFT.decodeHex)}") // to identify oracle pool box 
       |    
       |    val lpNFT0    = SELF.tokens(0)
       |    val reservedLP0 = SELF.tokens(1)
       |    val tokenY0     = SELF.tokens(2)
       |
       |    val lpNFT1    = successor.tokens(0)
       |    val reservedLP1 = successor.tokens(1)
       |    val tokenY1     = successor.tokens(2)
       |
       |    val supplyLP0 = SELF.R4[Long].get       // LP tokens in circulation in input LP box
       |    val supplyLP1 = successor.R4[Long].get  // LP tokens in circulation in output LP box
       |
       |    val validSuccessorScript = successor.propositionBytes == SELF.propositionBytes
       |    
       |    val preservedLpNFT     = lpNFT1 == lpNFT0
       |    val validLP              = reservedLP1._1 == reservedLP0._1
       |    val validY               = tokenY1._1 == tokenY0._1
       |    val validSupplyLP1       = supplyLP1 >= 0
       |       
       |    // since tokens can be repeated, we ensure for sanity that there are no more tokens
       |    val noMoreTokens         = successor.tokens.size == 3
       |  
       |    val validStorageRent     = successor.value > minStorageRent
       |
       |    val reservesX0 = SELF.value
       |    val reservesY0 = tokenY0._2
       |    val reservesX1 = successor.value
       |    val reservesY1 = tokenY1._2
       |
       |    val oraclePoolRateXY = oraclePoolBox.R4[Long].get 
       |    val lpRateXY0 = reservesX0 / reservesY0  // we can assume that reservesY0 > 0 (since at least one token must exist) 
       |    val lpRateXY1 = reservesX1 / reservesY1  // we can assume that reservesY1 > 0 (since at least one token must exist)
       |    val isCrossing = (lpRateXY0 - oraclePoolRateXY) * (lpRateXY1 - oraclePoolRateXY) < 0 // if (and only if) oracle pool rate falls in between, then this will be negative
       |     
       |    val crossTrackerIn = SELF.R5[Int].get
       |    val crossTrackerOut = successor.R5[Int].get
       |    
       |    val validCrossCounter = {
       |      if (isCrossing) {
       |        if (lpRateXY1 > oraclePoolRateXY) {
       |          crossTrackerOut >= HEIGHT - threshold 
       |        } else crossTrackerOut == ${Long.MaxValue}L
       |      } else crossTrackerOut == crossTrackerIn
       |    } 
       |     
       |    val validRateForRedeemingLP = oraclePoolRateXY > lpRateXY0 * 98 / 100 // lpRate must be >= 0.98 * oraclePoolRate // these parameters need to be tweaked
       |    // Do we need above if we also have the tracking contract?
       |     
       |    val deltaSupplyLP  = supplyLP1 - supplyLP0
       |    val deltaReservesX = reservesX1 - reservesX0
       |    val deltaReservesY = reservesY1 - reservesY0
       |    
       |    // LP formulae below using UniSwap v2 (with initial token burning by bootstrapping with positive R4)
       |    val validDepositing = {
       |        val sharesUnlocked = min(
       |            deltaReservesX.toBigInt * supplyLP0 / reservesX0,
       |            deltaReservesY.toBigInt * supplyLP0 / reservesY0
       |        )
       |        deltaSupplyLP <= sharesUnlocked
       |    }
       |
       |    val validRedemption = {
       |        val _deltaSupplyLP = deltaSupplyLP.toBigInt
       |        // note: _deltaSupplyLP, deltaReservesX and deltaReservesY are negative
       |        deltaReservesX.toBigInt * supplyLP0 >= _deltaSupplyLP * reservesX0 && deltaReservesY.toBigInt * supplyLP0 >= _deltaSupplyLP * reservesY0
       |    } && validRateForRedeemingLP
       |
       |    val validSwap =
       |        if (deltaReservesX > 0)
       |            reservesY0.toBigInt * deltaReservesX * feeNum >= -deltaReservesY * (reservesX0.toBigInt * feeDenom + deltaReservesX * feeNum)
       |        else
       |            reservesX0.toBigInt * deltaReservesY * feeNum >= -deltaReservesX * (reservesY0.toBigInt * feeDenom + deltaReservesY * feeNum)
       |
       |    val validAction =
       |        if (deltaSupplyLP == 0)
       |            validSwap
       |        else
       |            if (deltaReservesX > 0 && deltaReservesY > 0) validDepositing
       |            else validRedemption
       |
       |    sigmaProp(
       |        validSupplyLP1 &&
       |        validSuccessorScript &&
       |        validOraclePoolBox &&
       |        preservedLpNFT &&
       |        validLP &&
       |        validY &&
       |        noMoreTokens &&
       |        validAction && 
       |        validStorageRent && 
       |        validCrossCounter
       |    )
       |}
       |""".stripMargin

  val interventionScript =
    s"""{  
       |  val lastIntervention = SELF.creationInfo._1
       |  val buffer = 3 // error margin in height
       |  val T = 100 // from paper, gap between two interventions
       |  val T_int = 20 // blocks after which a trigger swap event can be completed, provided rate has not crossed oracle pool rate 
       |  val bankNFT = fromBase64("${Base64.encode(bankNFT.decodeHex)}") 
       |  val lpNFT = fromBase64("${Base64.encode(lpNFT.decodeHex)}") 
       |  val oraclePoolNFT = fromBase64("${Base64.encode(oraclePoolNFT.decodeHex)}")
       |  
       |  val thresholdPercent = 98 // 98% or less value (of LP in terms of OraclePool) will trigger action (ensure less than 100) 
       |  
       |  val oraclePoolBox = CONTEXT.dataInputs(0)
       |  
       |  val lpBoxIn = INPUTS(0)
       |  val bankBoxIn = INPUTS(1)
       |
       |  val lpBoxOut = OUTPUTS(0)
       |  val bankBoxOut = OUTPUTS(1)
       |  
       |  val successor = OUTPUTS(2) // SELF should be INPUTS(2)
       |  
       |  val tokenYIn    = lpBoxIn.tokens(2)
       |  val tokenYOut    = lpBoxOut.tokens(2)
       |  
       |  val reservesXIn = lpBoxIn.value
       |  val reservesYIn = tokenYIn._2
       |  
       |  val reservesXOut = lpBoxOut.value
       |  val reservesYOut = tokenYOut._2
       |  
       |  val lpRateXYIn  = reservesXIn / reservesYIn  // we can assume that reservesYIn > 0 (since at least one token must exist)
       |  val lpRateXYOut  = reservesXOut / reservesYOut  // we can assume that reservesYOut > 0 (since at least one token must exist)
       |  
       |  val oraclePoolRateXY = oraclePoolBox.R4[Long].get
       |   
       |  val validThreshold = lpRateXYIn * 100 < thresholdPercent * oraclePoolRateXY
       |   
       |  val validOraclePoolBox = oraclePoolBox.tokens(0)._1 == oraclePoolNFT 
       |  val validLpBox = lpBoxIn.tokens(0)._1 == lpNFT
       |  
       |  val validSuccessor = successor.propositionBytes == SELF.propositionBytes &&
       |                       successor.tokens == SELF.tokens &&
       |                       successor.value == SELF.value &&
       |                       successor.creationInfo._1 >= HEIGHT - buffer
       |  
       |  val validBankBoxIn = bankBoxIn.tokens(0)._1 == bankNFT 
       |  val validBankBoxOut = bankBoxOut.tokens(0) == bankBoxIn.tokens(0) &&
       |                        bankBoxOut.tokens(1)._1 == bankBoxIn.tokens(1)._1
       |  
       |  val validGap = lastIntervention < HEIGHT - T
       |  
       |  val deltaBankTokens =  bankBoxOut.tokens(1)._2 - bankBoxIn.tokens(1)._2
       |  val deltaBankErgs = bankBoxIn.value - bankBoxOut.value
       |  val deltaLpX = reservesXOut - reservesXIn
       |  val deltaLpY = reservesYIn - reservesYOut
       |
       |  val validLpIn = lpBoxIn.R5[Int].get < HEIGHT - T_int // at least T_int blocks have passed since the tracking started
       |                  
       |  val lpRateXYOutTimes100 = lpRateXYOut * 100
       |  
       |  val validSwap = lpRateXYOutTimes100 >= oraclePoolRateXY * 105 && // new rate must be >= 1.05 times oracle rate
       |                  lpRateXYOutTimes100 <= oraclePoolRateXY * 110 && // new rate must be <= 1.1 times oracle rate
       |                  deltaBankErgs <= deltaLpX && // ergs reduced in bank box must be <= ergs gained in LP 
       |                  deltaBankTokens >= deltaLpY && // tokens gained in bank box must be >= tokens reduced in LP 
       |                  validBankBoxIn &&
       |                  validBankBoxOut &&
       |                  validSuccessor &&
       |                  validLpBox &&
       |                  validOraclePoolBox &&
       |                  validThreshold &&
       |                  validLpIn && 
       |                  validGap
       |   
       |  sigmaProp(validSwap)
       |}
       |""".stripMargin

  val bankErgoTree = ScriptUtil.compile(Map(), bankScript)
  val bankAddress = getStringFromAddress(getAddressFromErgoTree(bankErgoTree))
  println(bankAddress)

  val lpErgoTree = ScriptUtil.compile(Map(), lpScript)
  val lpAddress = getStringFromAddress(getAddressFromErgoTree(lpErgoTree))
  println(lpAddress)

  val interventionErgoTree = ScriptUtil.compile(Map(), interventionScript)
  val interventionAddress = getStringFromAddress(getAddressFromErgoTree(interventionErgoTree))
  println(interventionAddress)

}
