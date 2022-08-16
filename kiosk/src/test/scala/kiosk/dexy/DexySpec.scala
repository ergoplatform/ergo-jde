package kiosk.dexy

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import scorex.util.encode.Base64
import kiosk.ergo._
import kiosk.script.ScriptUtil
import org.ergoplatform.appkit.HttpClientTesting
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class DexySpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val oracleNFT = "472B4B6250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val lpNFT = "361A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val trackingNFT = "261A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val interventionNFT = "161A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val freeMintNFT = "061A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val arbitrageMintNFT = "961A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val bankNFT = "861A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual

  val bankScript =
    s"""{ 
       |  // This box: (dexyUSD bank box)
       |  //   tokens(0): bankNFT identifying the box
       |  //   tokens(1): dexyUSD tokens to be emitted
       |  
       |  // Bank box will be spent as follows
       |  //   Arbitrage Mint
       |  //   Input         |  Output         |   Data-Input
       |  // ------------------------------------------------
       |  // 0 ArbitrageMint |  ArbitrageMint  |   Oracle 
       |  // 1 Bank          |  Bank           |   LP
       |
       |  //   Free Mint
       |  //   Input    |  Output   |   Data-Input
       |  // -------------------------------------
       |  // 0 FreeMint |  FreeMint |   Oracle
       |  // 1 Bank     |  Bank     |   LP
       |
       |  //   Intervention
       |  //   Input         |  Output        |   Data-Input 
       |  // -----------------------------------------------
       |  // 0 LP            |  LP            |   Oracle
       |  // 1 Bank          |  Bank          |
       |  // 2 Intervention  |  Intervention  |
       |  
       |  val selfOutIndex = 1        // 2nd output is self copy
       |  val mintInIndex = 0         // 1st input is mint or LP box
       |  val interventionInIndex = 2 // 3rd input is intervention box
       |  
       |  val interventionNFT = fromBase64("${Base64.encode(interventionNFT.decodeHex)}") // to identify intervention box for future use
       |  val freeMintNFT = fromBase64("${Base64.encode(freeMintNFT.decodeHex)}") 
       |  val arbitrageMintNFT = fromBase64("${Base64.encode(arbitrageMintNFT.decodeHex)}") 
       |  
       |  val selfOut = OUTPUTS(selfOutIndex)
       |  val validSelfOut = selfOut.tokens(0) == SELF.tokens(0) && // bankNFT and quantity preserved
       |                     selfOut.propositionBytes == SELF.propositionBytes && // script preserved
       |                     selfOut.tokens(1)._1 == SELF.tokens(1)._1 // dexyUSD tokenId preserved
       |       
       |  val validMint = INPUTS(mintInIndex).tokens(0)._1 == freeMintNFT || 
       |                  INPUTS(mintInIndex).tokens(0)._1 == arbitrageMintNFT
       |  
       |  val validIntervention = INPUTS(interventionInIndex).tokens(0)._1 == interventionNFT
       |  
       |  sigmaProp(validSelfOut && (validMint || validIntervention))
       |}
       |""".stripMargin

  // arbitrage mint box
  val arbitrageMintScript =
    s"""{ 
       |  
       |  // this box: (arbitrage-mint box)
       |  //   tokens(0): Arbitrage-mint NFT
       |  // 
       |  //   R4: (Int) height at which counter will reset
       |  //   R5: (Long) remaining stablecoins available to be purchased before counter is reset
       |  
       |  // Arbitrage Mint box will be spent as follows
       |  //   Arbitrage Mint
       |  //   Input         |  Output         |   Data-Input
       |  // ------------------------------------------------
       |  // 0 ArbitrageMint |  ArbitrageMint  |   Oracle 
       |  // 1 Bank          |  Bank           |   LP
       |
       |  val bankInIndex = 1
       |  val selfOutIndex = 0
       |  val bankOutIndex = 1
       |  val oracleBoxIndex = 0
       |  val lpBoxIndex = 1
       |  
       |  val oracleNFT = fromBase64("${Base64.encode(oracleNFT.decodeHex)}") // to identify oracle pool box
       |  val bankNFT = fromBase64("${Base64.encode(bankNFT.decodeHex)}") 
       |  val lpNFT = fromBase64("${Base64.encode(lpNFT.decodeHex)}")
       |  val T_arb = 30 // 30 blocks = 1 hour
       |  val thresholdPercent = 101 // 101% or more value (of LP in terms of OraclePool) will trigger action
       |  
       |  val feeNum = 5
       |  val feeDenom = 1000 
       |  // actual fee ratio is feeNum / feeDenom
       |  // example if feeNum = 5 and feeDenom = 1000 then fee = 0.005 = 0.5 %
       |  
       |  val oracleBox = CONTEXT.dataInputs(oracleBoxIndex) // oracle-pool (v1 and v2) box containing rate in R4
       |  val lpBox = CONTEXT.dataInputs(lpBoxIndex)
       |  val bankBoxIn = INPUTS(bankInIndex)
       |   
       |  val selfOut = OUTPUTS(selfOutIndex)
       |  val bankBoxOut = OUTPUTS(bankOutIndex)
       |  
       |  val selfInR4 = SELF.R4[Int].get
       |  val selfInR5 = SELF.R5[Long].get
       |  val selfOutR4 = selfOut.R4[Int].get
       |  val selfOutR5 = selfOut.R5[Long].get
       |
       |  val isCounterReset = HEIGHT > selfInR4
       |  
       |  val oracleRateWithoutFee = oracleBox.R4[Long].get // can assume always > 0 (ref oracle pool contracts) NanoErgs per USD
       |  val oracleRate = oracleRateWithoutFee * (feeNum + feeDenom) / feeDenom 
       |  
       |  val lpReservesX = lpBox.value
       |  val lpReservesY = lpBox.tokens(2)._2 // dexyReserves
       |  val lpRate = lpReservesX / lpReservesY
       |  
       |  val dexyMinted = bankBoxIn.tokens(1)._2 - bankBoxOut.tokens(1)._2
       |  val ergsAdded = bankBoxOut.value - bankBoxIn.value
       |  val validDelta = ergsAdded >= dexyMinted * oracleRate && ergsAdded > 0 // dexyMinted must be (+)ve, since both ergsAdded and oracleRate are (+)ve
       |  
       |  val maxAllowedIfReset = (lpReservesX - oracleRate * lpReservesY) / oracleRate
       |    
       |  // above formula: 
       |  // Before mint rate is lpReservesX / lpReservesY, which should be greater than oracleRate
       |  // After mint rate is lpReservesX / (lpReservesY + dexyMinted), which should be same or less than than oracleRate 
       |  //  Thus:
       |  //   lpReservesX / lpReservesY > oracleRate    
       |  //   lpReservesX / (lpReservesY + dexyMinted) <= oracleRate
       |  // above gives min value of dexyMinted = (lpReservesX - oracleRate * lpReservesY) / oracleRate
       |  
       |  val availableToMint = if (isCounterReset) maxAllowedIfReset else selfInR5
       |   
       |  val validAmount = dexyMinted <= availableToMint 
       |   
       |  val validSelfOutR4 = selfOutR4 == (if (isCounterReset) HEIGHT + T_arb else selfInR4)     
       |  val validSelfOutR5 = selfOutR5 == availableToMint - dexyMinted
       |
       |  val validBankBoxInOut = bankBoxIn.tokens(0)._1 == bankNFT && bankBoxOut.tokens(0)._1 == bankNFT
       |  val validLpBox = lpBox.tokens(0)._1 == lpNFT
       |  val validOracleBox = oracleBox.tokens(0)._1 == oracleNFT
       |  val validSelfOut = selfOut.tokens == SELF.tokens && // NFT preserved
       |                     selfOut.propositionBytes == SELF.propositionBytes && // script preserved
       |                     selfOut.value > SELF.value && validSelfOutR5 && validSelfOutR4  
       |
       |  val validDelay = lpBox.R5[Int].get < HEIGHT - T_arb // at least T_arb blocks have passed since the tracking started 
       |  val validThreshold = lpRate * 100 > thresholdPercent * oracleRate                
       |                 
       |  sigmaProp(validDelay && validThreshold && validAmount && validBankBoxInOut && validLpBox && validOracleBox && validSelfOut && validDelta)
       |}
       |""".stripMargin

  // free mint box
  val freeMintScript =
    s"""{ // ToDo: Add fee
       |  // 
       |  // this box: (free-mint box)
       |  //   tokens(0): Free-mint NFT
       |  // 
       |  //   R4: (Int) height at which counter will reset
       |  //   R5: (Long) remaining stablecoins available to be purchased before counter is reset
       |  
       |  //   Free Mint box will be spent as follows:
       |  //   Free Mint
       |  //   Input    |  Output   |   Data-Input
       |  // -------------------------------------
       |  // 0 FreeMint |  FreeMint |   Oracle
       |  // 1 Bank     |  Bank     |   LP
       |
       |  val bankInIndex = 1
       |  val selfOutIndex = 0
       |  val bankOutIndex = 1
       |  val oracleBoxIndex = 0
       |  val lpBoxIndex = 1
       |
       |  val oracleNFT = fromBase64("${Base64.encode(oracleNFT.decodeHex)}") // to identify oracle pool box
       |  val bankNFT = fromBase64("${Base64.encode(bankNFT.decodeHex)}") 
       |  val lpNFT = fromBase64("${Base64.encode(lpNFT.decodeHex)}")
       |  val t_free = 100
       |  
       |  val feeNum = 10
       |  val feeDenom = 1000 
       |  // actual fee ratio is feeNum / feeDenom
       |  // example if feeNum = 10 and feeDenom = 1000 then fee = 0.01 = 1 %
       |
       |  val oracleBox = CONTEXT.dataInputs(oracleBoxIndex) // oracle-pool (v1 and v2) box containing rate in R4
       |  val lpBox = CONTEXT.dataInputs(lpBoxIndex)
       |  val bankBoxIn = INPUTS(bankInIndex)
       |   
       |  val selfOut = OUTPUTS(selfOutIndex)
       |  val bankBoxOut = OUTPUTS(bankOutIndex)
       |  
       |  val selfInR4 = SELF.R4[Int].get
       |  val selfInR5 = SELF.R5[Long].get
       |  val selfOutR4 = selfOut.R4[Int].get
       |  val selfOutR5 = selfOut.R5[Long].get
       |
       |  val isCounterReset = HEIGHT > selfInR4
       |  
       |  val oracleRateWithoutFee = oracleBox.R4[Long].get // can assume always > 0 (ref oracle pool contracts) NanoErgs per USD
       |  val oracleRate = oracleRateWithoutFee * (feeNum + feeDenom) / feeDenom 
       |
       |  val lpReservesX = lpBox.value
       |  val lpReservesY = lpBox.tokens(2)._2 // dexyReserves
       |  val lpRate = lpReservesX / lpReservesY
       |  
       |  val validRateFreeMint = 98 * lpRate < oracleRate * 100 &&  
       |                          oracleRate * 100 < 102 * lpRate 
       |    
       |  val dexyMinted = bankBoxIn.tokens(1)._2 - bankBoxOut.tokens(1)._2
       |  val ergsAdded = bankBoxOut.value - bankBoxIn.value
       |  val validDelta = ergsAdded >= dexyMinted * oracleRate && ergsAdded > 0 // dexyMinted must be (+)ve, since both ergsAdded and oracleRate are (+)ve
       |  
       |  val maxAllowedIfReset = lpReservesY / 100 
       |  
       |  val availableToMint = if (isCounterReset) maxAllowedIfReset else selfInR5
       |   
       |  val validAmount = dexyMinted <= availableToMint 
       |   
       |  val validSelfOutR4 = selfOutR4 == (if (isCounterReset) HEIGHT + t_free else selfInR4)     
       |  val validSelfOutR5 = selfOutR5 == availableToMint - dexyMinted
       |
       |  val validBankBoxInOut = bankBoxIn.tokens(0)._1 == bankNFT && bankBoxOut.tokens(0)._1 == bankNFT
       |  val validLpBox = lpBox.tokens(0)._1 == lpNFT
       |  val validOracleBox = oracleBox.tokens(0)._1 == oracleNFT
       |  val validSelfOut = selfOut.tokens == SELF.tokens && // NFT preserved
       |                     selfOut.propositionBytes == SELF.propositionBytes && // script preserved
       |                     selfOut.value > SELF.value && validSelfOutR5 && validSelfOutR4  
       |
       |  sigmaProp(validAmount && validBankBoxInOut && validLpBox && validOracleBox && validSelfOut && validDelta && validRateFreeMint)
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
       |    //   R5: Stores the height where oracle pool rate becomes lower than LP rate. Reset to Long.MaxValue when rate crossed back. Called crossTrackerLow
       |    //   R6: Stores the height where oracle pool rate becomes higher than LP rate. Reset to Long.MaxValue when rate crossed back. Called crossTrackerHigh
       |    //   Tokens(0): LP NFT to uniquely identify NFT box. (Could we possibly do away with this?) 
       |    //   Tokens(1): LP tokens
       |    //   Tokens(2): Y tokens (Note that X tokens are NanoErgs (the value) 
       |    //   
       |    // Data Input #0: (oracle pool box)
       |    //   R4: Rate in units of X per unit of Y
       |    //   Token(0): OP NFT to uniquely identify Oracle Pool
       |    
       |    // LP box will be spent as follows
       |    //   Intervention
       |    //   Input         |  Output        |   Data-Input 
       |    // -----------------------------------------------
       |    // 0 LP            |  LP            |   Oracle
       |    // 1 Bank          |  Bank          |
       |    // 2 Intervention  |  Intervention  |
       |    // 3 Tracking Box  |  Tracking Box  |
       |
       |    //   Swap
       |    //   Input         |  Output        |   Data-Input 
       |    // -----------------------------------------------
       |    // 0 LP            |  LP            |   Oracle
       |    // 1 Tracking Box  |  Tracking Box  |    
       |
       |    //   Redeem LP tokens
       |    //   Input         |  Output        |   Data-Input 
       |    // -----------------------------------------------
       |    // 0 LP            |  LP            |   Oracle
       |    // 1 Tracking Box  |  Tracking Box  |
       |
       |    //   Mint LP tokens
       |    //   Input         |  Output        |   Data-Input 
       |    // -----------------------------------------------
       |    // 0 LP            |  LP            |   Oracle
       |    // 1 Tracking Box  |  Tracking Box  |
       |
       |    val selfOutIndex = 0
       |    val oracleBoxIndex = 0
       |    val trackingBoxInIndex = getVar[Int](0).get
       |    
       |    val trackingBox = INPUTS(trackingBoxInIndex)
       |     
       |    // constants
       |    val feeNum = 3 // 0.3 % if feeDenom is 1000
       |    val feeDenom = 1000
       |    
       |    // the value feeNum / feeDenom is the fraction of fee
       |    // for example if feeNum = 3 and feeDenom = 1000 then fee is 0.003 = 0.3%
       |    
       |    val minStorageRent = 10000000L  // this many number of nanoErgs are going to be permanently locked
       |    
       |    val successor = OUTPUTS(selfOutIndex) // copy of this box after exchange
       |    val oracleBox = CONTEXT.dataInputs(oracleBoxIndex) // oracle pool box
       |    val validOraclePoolBox = oracleBox.tokens(0)._1 == fromBase64("${Base64.encode(oracleNFT.decodeHex)}") // to identify oracle pool box 
       |    val validTrackingBox = trackingBox.tokens(0)._1 == fromBase64("${Base64.encode(trackingNFT.decodeHex)}") // to identify tracking box
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
       |    val preservedLpNFT       = lpNFT1 == lpNFT0
       |    val validLpBox           = reservedLP1._1 == reservedLP0._1
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
       |    val oracleRateXY = oracleBox.R4[Long].get 
       |    val lpRateXY0 = reservesX0 / reservesY0  // we can assume that reservesY0 > 0 (since at least one token must exist)
       |     
       |    val validRateForRedeemingLP = oracleRateXY > lpRateXY0 * 98 / 100 // lpRate must be >= 0.98 * oracleRate // these parameters need to be tweaked
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
       |        validSupplyLP1       &&
       |        validSuccessorScript &&
       |        validOraclePoolBox   &&
       |        validTrackingBox     && 
       |        preservedLpNFT       &&
       |        validLpBox           &&
       |        validY               &&
       |        noMoreTokens         &&
       |        validAction          && 
       |        validStorageRent  
       |    )
       |}
       |""".stripMargin

  val trackingScript =
    s"""{
       |    val threshold = 3 // error threshold in crossTrackerLow
       |    
       |    val oracleBoxIndex = 0
       |    val lpBoxInIndex = 0
       |    val lpBoxOutIndex = 0
       |    val selfOutIndex = getVar[Int](0).get
       |
       |    val lpBoxIn = INPUTS(lpBoxInIndex)
       |    val lpBoxOut = OUTPUTS(lpBoxOutIndex)
       |    val oracleBox = CONTEXT.dataInputs(oracleBoxIndex)
       |    val successor = OUTPUTS(selfOutIndex)
       |    
       |    val tokenY0     = lpBoxIn.tokens(2)
       |    val tokenY1     = lpBoxOut.tokens(2)
       |    
       |    val validLp = lpBoxIn.tokens(0)._1 == fromBase64("${Base64.encode(lpNFT.decodeHex)}") // to identify LP box
       |    // this box can only be spent with LP and similarly LP can only be spent with this box.
       |    
       |    val validOraclePoolBox = oracleBox.tokens(0)._1 == fromBase64("${Base64.encode(oracleNFT.decodeHex)}") // to identify oracle pool box
       |    val validSuccessor = successor.tokens == SELF.tokens && successor.propositionBytes == SELF.propositionBytes && SELF.value <= successor.value
       |
       |    val oracleRateXY = oracleBox.R4[Long].get
       |    val reservesX0 = lpBoxIn.value
       |    val reservesY0 = tokenY0._2
       |    val reservesX1 = lpBoxOut.value
       |    val reservesY1 = tokenY1._2
       |    val lpRateXY0 = reservesX0 / reservesY0  // we can assume that reservesY0 > 0 (since at least one token must exist)
       |    val lpRateXY1 = reservesX1 / reservesY1  // we can assume that reservesY1 > 0 (since at least one token must exist)
       |
       |    // R4 contains a tuple of type (Int, Int), (Long, Boolean). Let these be ((num, denom), (height, isBelow))
       |    // num is numerator, denom is denominator
       |    // height is the height at which the event was "activated" (will store Long.MaxValue once deactivated)
       |    // isBelow tells us if the tracking should be for "lower" or "higher"
       |    // Let r be the ratio "oracle pool rate" / "LP rate", where the term "rate" denotes "Ergs per dexy"
       |    // Now, if "isBelow" is true, then the tracker will be activated (i.e., will be set to the current height) when r goes 
       |    // below num/denom and will continue to be so as long as r remains below num/denom. 
       |    // Once r goes above num/denom, this tracker will be set to Long.MaxValue
       |    
       |    // tracking will have following elements
       |    // index | num | denom | height | isBelow
       |    // 0     | 95  | 100   | _      | true     (for extracting to future, burn)
       |    // 1     | 98  | 100   | _      | true     (for arbitrage mint)
       |    // 2     | 1   | 1     | _      | true     (not needed yet)  
       |    
       |
       |    
       |    val inTrackers = SELF.R4[Coll[((Int, Int), (Long, Boolean))]].get
       |    val outTrackers = successor.R4[Coll[((Int, Int), (Long, Boolean))]].get
       |    val indices = inTrackers.indices
       |    
       |    val validTracking = indices.forall(
       |      { (index: Int) =>
       |        val inTracker = inTrackers(index)
       |        val outTracker = outTrackers(index)
       |        
       |        val numDenomIn = inTracker._1
       |        val numDenomOut = outTracker._1
       |        
       |        val isBelowIn = inTracker._2._2
       |        val isBelowOut = outTracker._2._2
       |
       |        val heightIn = inTracker._2._1
       |        val heightOut = outTracker._2._1
       |        
       |        val num = numDenomIn._1     // numerator
       |        val denom = numDenomIn._2   // denominator
       |        
       |        // For a ratio of 95%, set num to 95 and denom to 100 (equivalently 19, 20), and set fourth parameter to true
       |        // Then the third param (tracker height) will be set when oracle pool rate becomes <= 95% of LP rate 
       |        // and it will be reset to Long.MaxValue when that rate becomes > than 95% of LP rate
       |        // 
       |        // Let oracle pool rate be P and LP rate at input be L0 and at output be L1
       |        // Let N and D denote num and denom respectively. Then we can use the following table
       |        // 
       |        // EVENT    | isBelow | INPUT       | OUTPUT
       |        // ---------+---------+-------------+-----------
       |        // trigger  | true    | P/L0 >= N/D | P/L1 <  N/D 
       |        // preserve | true    | P/L0 <  N/D | P/L1 <  N/D 
       |        // reset    | true    | P/L0 <  N/D | P/L1 >= N/D (reverse of 1st row)
       |        // ---------+---------+-------------+------------
       |        // trigger  | false   | P/L0 <= N/D | P/L1 >  N/D 
       |        // preserve | false   | P/L0 >  N/D | P/L1 >  N/D 
       |        // reset    | false   | P/L0 >  N/D | P/L1 <= N/D (reverse of 1st row) 
       |        
       |        val x = oracleRateXY * denom
       |        val y0 = num * lpRateXY0
       |        val y1 = num * lpRateXY1
       |        
       |        val trigger = ((isBelowIn && x >= y0 && x < y1) || (!isBelowIn && x <= y0 && x > y1)) && heightOut >= HEIGHT - threshold && heightOut <= HEIGHT
       |        val preserve = ((x < y0 && x < y1) || (x > y0 && x > y1)) && heightIn == heightOut  
       |        val reset = (isBelowIn && x < y0 && x >= y1) || (!isBelowIn && x > y0 && x <= y1) && heightOut == ${Long.MaxValue}L   
       |        val correctHeight = trigger || preserve || reset  
       |        
       |        numDenomIn == numDenomOut && // 1st and 2nd params preserved
       |        isBelowIn == isBelowOut   && // 4th param preserved
       |        correctHeight
       |      }
       |    )
       |    
       |    /* Old crossing counter. Kept for checking 
       |    val isCrossing = (lpRateXY0 - oracleRateXY) * (lpRateXY1 - oracleRateXY) < 0 
       |    // if (and only if) oracle pool rate falls in between, then this will be negative
       |    
       |    // cross tracking start
       |    val crossTrackerLowIn = lpBoxIn.R5[Int].get // ToDo: move to this box from LP box
       |    val crossTrackerLowOut = lpBoxOut.R5[Int].get // ToDo: move to this box from LP box
       |    
       |    val crossTrackerHighIn = lpBoxIn.R6[Int].get // ToDo: move to this box from LP box
       |    val crossTrackerHighOut = lpBoxOut.R6[Int].get // ToDo: move to this box from LP box
       |    
       |    // ToDo: add custom tracker logic 
       |    
       |    val validCrossCounter = {
       |      if (isCrossing) {
       |        if (lpRateXY1 > oracleRateXY) {
       |          crossTrackerLowOut >= HEIGHT - threshold &&
       |          crossTrackerHighOut == ${Long.MaxValue}L 
       |        } else {
       |          crossTrackerHighOut >= HEIGHT - threshold &&
       |          crossTrackerLowOut == ${Long.MaxValue}L
       |        }
       |      } else {
       |        crossTrackerLowOut == crossTrackerLowIn &&
       |        crossTrackerHighOut == crossTrackerHighIn 
       |      }
       |    } 
       |    // cross tracking end
       |    */
       |    
       |    sigmaProp(validSuccessor && validLp && validTracking && validOraclePoolBox) // probably validOraclePoolBox is not needed as its already in LP
       |}
       |""".stripMargin

  val interventionScript =
    s"""{  
       |  
       |  // Intervention box will be spent as follows
       |  //   Intervention
       |  //   Input         |  Output        |   Data-Input 
       |  // -----------------------------------------------
       |  // 0 LP            |  LP            |   Oracle
       |  // 1 Bank          |  Bank          |   Tracking
       |  // 2 Intervention  |  Intervention  |
       |  
       |  val lpInIndex = 0
       |  val lpOutIndex = 0
       |  val bankInIndex = 1
       |  val bankOutIndex = 1
       |  val selfOutIndex = 2    // SELF should be third input
       |  val oracleBoxIndex = 0 
       |  val trackingBoxIndex = 1
       |
       |  val lastIntervention = SELF.creationInfo._1
       |  val buffer = 3 // error margin in height
       |  val T = 100 // from paper, gap between two interventions
       |  val T_int = 20 // blocks after which a trigger swap event can be completed, provided rate has not crossed oracle pool rate 
       |  val bankNFT = fromBase64("${Base64.encode(bankNFT.decodeHex)}") 
       |  val lpNFT = fromBase64("${Base64.encode(lpNFT.decodeHex)}") 
       |  val oracleNFT = fromBase64("${Base64.encode(oracleNFT.decodeHex)}")
       |  
       |  val thresholdPercent = 98 // 98% or less value (of LP in terms of OraclePool) will trigger action (ensure less than 100) 
       |  
       |  val oracleBox = CONTEXT.dataInputs(oracleBoxIndex)
       |  val trackingBox = CONTEXT.dataInputs(trackingBoxIndex)
       |  
       |  val lpBoxIn = INPUTS(lpInIndex)
       |  val bankBoxIn = INPUTS(bankInIndex)
       |
       |  val lpBoxOut = OUTPUTS(lpOutIndex)
       |  val bankBoxOut = OUTPUTS(bankOutIndex)
       |  
       |  val successor = OUTPUTS(selfOutIndex) 
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
       |  val oracleRateXY = oracleBox.R4[Long].get
       |   
       |  val validThreshold = lpRateXYIn * 100 < thresholdPercent * oracleRateXY
       |   
       |  val validOraclePoolBox = oracleBox.tokens(0)._1 == oracleNFT 
       |  val validLpBox = lpBoxIn.tokens(0)._1 == lpNFT
       |  
       |  val validSuccessor = successor.propositionBytes == SELF.propositionBytes  &&
       |                       successor.tokens == SELF.tokens                      &&
       |                       successor.value == SELF.value                        &&
       |                       successor.creationInfo._1 >= HEIGHT - buffer
       |  
       |  val validBankBoxIn = bankBoxIn.tokens(0)._1 == bankNFT 
       |  
       |  val validBankBoxOut = bankBoxOut.tokens(0) == bankBoxIn.tokens(0)        &&
       |                        bankBoxOut.tokens(1)._1 == bankBoxIn.tokens(1)._1
       |  
       |  val validGap = lastIntervention < HEIGHT - T
       |  
       |  val deltaBankTokens =  bankBoxOut.tokens(1)._2 - bankBoxIn.tokens(1)._2
       |  val deltaBankErgs = bankBoxIn.value - bankBoxOut.value
       |  val deltaLpX = reservesXOut - reservesXIn
       |  val deltaLpY = reservesYIn - reservesYOut
       |  
       |  val trackingTupleArray = trackingBox.R4[Coll[((Int, Int), (Long, Boolean))]].get
       |  
       |  val trackingHeight = trackingTupleArray(1)._2._1 // second element of tracking array has 98%
       |  
       |  val validLpIn = trackingHeight < HEIGHT - T_int // at least T_int blocks have passed since the tracking started
       |                  
       |  val lpRateXYOutTimes100 = lpRateXYOut * 100
       |  
       |  val validSwap = lpRateXYOutTimes100 >= oracleRateXY * 105   && // new rate must be >= 1.05 times oracle rate
       |                  lpRateXYOutTimes100 <= oracleRateXY * 110   && // new rate must be <= 1.1 times oracle rate
       |                  deltaBankErgs <= deltaLpX                   && // ergs reduced in bank box must be <= ergs gained in LP 
       |                  deltaBankTokens >= deltaLpY                 && // tokens gained in bank box must be >= tokens reduced in LP 
       |                  validBankBoxIn                              &&
       |                  validBankBoxOut                             &&
       |                  validSuccessor                              &&
       |                  validLpBox                                  &&
       |                  validOraclePoolBox                          &&
       |                  validThreshold                              &&
       |                  validLpIn                                   && 
       |                  validGap
       |   
       |  sigmaProp(validSwap)
       |}
       |""".stripMargin

  val bankErgoTree = ScriptUtil.compile(Map(), bankScript)
  val bankAddress = getStringFromAddress(getAddressFromErgoTree(bankErgoTree))
  println(s"Bank: $bankAddress")
  println(bankScript)
  println()

  val arbitrageMintErgoTree = ScriptUtil.compile(Map(), arbitrageMintScript)
  val arbitrageMintAddress = getStringFromAddress(getAddressFromErgoTree(arbitrageMintErgoTree))
  println(s"ArbitrageMint: $arbitrageMintAddress")
  println(arbitrageMintScript)
  println()

  val freeMintErgoTree = ScriptUtil.compile(Map(), freeMintScript)
  val freeMintAddress = getStringFromAddress(getAddressFromErgoTree(freeMintErgoTree))
  println(s"FreeMint: $freeMintAddress")
  println(freeMintScript)
  println()

  val lpErgoTree = ScriptUtil.compile(Map(), lpScript)
  val lpAddress = getStringFromAddress(getAddressFromErgoTree(lpErgoTree))
  println(s"LP: $lpAddress")
  println(lpScript)
  println()

  val trackingErgoTree = ScriptUtil.compile(Map(), trackingScript)
  val trackingAddress = getStringFromAddress(getAddressFromErgoTree(trackingErgoTree))
  println(s"Tracking: $trackingAddress")
  println(trackingScript)
  println()

  val interventionErgoTree = ScriptUtil.compile(Map(), interventionScript)
  val interventionAddress = getStringFromAddress(getAddressFromErgoTree(interventionErgoTree))
  println(s"Intervention: $interventionAddress")
  println(interventionScript)
  println()

}
