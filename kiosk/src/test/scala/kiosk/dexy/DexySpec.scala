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
  val tracking98NFT = "261A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val tracking95NFT = "261A3A5250655368566D597133743677397A24432646294A404D635166546A55" // TODO replace with actual
  val tracking101NFT = "261A3A5250655368566D597133743677397A24432646294A404D635166546A58" // TODO replace with actual
  val interventionNFT = "161A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val extractionNFT = "161A3A5250655368566D597133743677397A24432646294A404D635166546A54" // TODO replace with actual
  val freeMintNFT = "061A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val arbitrageMintNFT = "961A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual
  val bankNFT = "861A3A5250655368566D597133743677397A24432646294A404D635166546A57" // TODO replace with actual

  val bankScript =
    s"""{ 
       |  // This box: (dexyUSD bank box)
       |  // 
       |  // TOKENS
       |  //   tokens(0): bankNFT identifying the box
       |  //   tokens(1): dexyUSD tokens to be emitted
       |  // REGISTERS
       |  //   None
       |  // 
       |  // TRANSACTIONS
       |  //   
       |  // [1] Arbitrage Mint
       |  //   Input         |  Output         |   Data-Input
       |  // ------------------------------------------------
       |  // 0 ArbitrageMint |  ArbitrageMint  |   Oracle 
       |  // 1 Bank          |  Bank           |   LP
       |  // 
       |  // [2] Free Mint
       |  //   Input    |  Output   |   Data-Input
       |  // -------------------------------------
       |  // 0 FreeMint |  FreeMint |   Oracle
       |  // 1 Bank     |  Bank     |   LP
       |  // 
       |  // [3] Intervention
       |  //   Input         |  Output        |   Data-Input 
       |  // -----------------------------------------------
       |  // 0 LP            |  LP            |   Oracle
       |  // 1 Bank          |  Bank          |   Tracking (98%)
       |  // 2 Intervention  |  Intervention  |
       |  // 
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
       |  
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
       |  // this box: (arbitrage-mint box)
       |  // 
       |  // TOKENS
       |  //   tokens(0): Arbitrage-mint NFT
       |  // 
       |  // REGISTERS
       |  //   R4: (Int) height at which counter will reset
       |  //   R5: (Long) remaining Dexy tokens available to be purchased before counter is reset
       |  // 
       |  // TRANSACTIONS
       |  // 
       |  // [1] Arbitrage Mint
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
    s"""{  
       |  // this box: (free-mint box)
       |  // 
       |  // TOKENS
       |  //   tokens(0): Free-mint NFT
       |  // 
       |  // REGISTERS
       |  //   R4: (Int) height at which counter will reset
       |  //   R5: (Long) remaining stablecoins available to be purchased before counter is reset
       |  // 
       |  // TRANSACTIONS
       |  // [1] Free Mint
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
       |    // This box: (LP box)
       |    //
       |    // TOKENS
       |    //   Tokens(0): LP NFT to uniquely identify NFT box. (Could we possibly do away with this?) 
       |    //   Tokens(1): LP tokens
       |    //   Tokens(2): Y tokens (Note that X tokens are NanoErgs (the value)
       |    //
       |    // REGISTERS
       |    //   R1 (value): X tokens in NanoErgs 
       |    //   R4: How many LP in circulation (long). This can be non-zero when bootstrapping, to consider the initial token burning in UniSwap v2
       |    //   
       |    // TRANSACTIONS
       |    //
       |    // [1] Intervention
       |    //   Input         |  Output        |   Data-Input 
       |    // -----------------------------------------------
       |    // 0 LP            |  LP            |   Oracle
       |    // 1 Bank          |  Bank          |
       |    // 2 Intervention  |  Intervention  |
       |    //
       |    // [2] Swap
       |    //   Input         |  Output        |   Data-Input 
       |    // -----------------------------------------------
       |    // 0 LP            |  LP            |   Oracle
       |    //
       |    // [3] Redeem LP tokens
       |    //   Input         |  Output        |   Data-Input 
       |    // -----------------------------------------------
       |    // 0 LP            |  LP            |   Oracle
       |    // 
       |    // [4] Mint LP tokens
       |    //   Input         |  Output        |   Data-Input 
       |    // -----------------------------------------------
       |    // 0 LP            |  LP            |   Oracle
       |    // 
       |    // [5] Extract to future
       |    //   Input         |  Output        |   Data-Input 
       |    // -----------------------------------------------
       |    // 0 LP            |  LP            |   Oracle
       |    // 1 Extract       |  Extract       |   Bank
       |    // 3               |                |   Tracking (95%)
       |    //
       |    // [6] Release extracted to future tokens
       |    //   Input         |  Output        |   Data-Input 
       |    // -----------------------------------------------
       |    // 0 LP            |  LP            |   Oracle
       |    // 1 Extract       |  Extract       |   Tracking (101%)
       |    //
       |    // -------------------------------------------------------------
       |    // Notation:
       |    // 
       |    // X is the primary token
       |    // Y is the secondary token 
       |    // When using Erg-USD oracle v1, X is NanoErg and Y is USD   
       |    
       |    val selfOutIndex = 0
       |    val oracleBoxIndex = 0
       |
       |    val interventionBoxIndex = 2 // ToDo: fix if possible, otherwise each tx needs at least 3 inputs (add dummy inputs for now)
       |    val interventionBox = INPUTS(interventionBoxIndex) // see above comment ^ 
       |     
       |    val extractBoxIndex = 1
       |    val extractBox = INPUTS(extractBoxIndex)  
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
       |    val validIntervention = interventionBox.tokens(0)._1 == fromBase64("${Base64.encode(interventionNFT.decodeHex)}") 
       |    val validExtraction = extractBox.tokens(0)._1 == fromBase64("${Base64.encode(extractionNFT.decodeHex)}") 
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
       |    // ToDo: Check if we still need above if we also have the tracking contract?
       |     
       |    val deltaSupplyLP  = supplyLP1 - supplyLP0
       |    val deltaReservesX = reservesX1 - reservesX0
       |    val deltaReservesY = reservesY1 - reservesY0
       |    
       |    // LP formulae below using UniSwap v2 (with initial token burning by bootstrapping with positive R4)
       |    val validMintLP = {
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
       |    val lpAction =
       |        if (deltaSupplyLP == 0)
       |            validSwap
       |        else
       |            if (deltaReservesX > 0 && deltaReservesY > 0) validMintLP
       |            else validRedemption
       |
       |    val dexyAction = validIntervention || // intervention
       |                     validExtraction // extract to future or release in future
       |    sigmaProp(
       |        validSupplyLP1            &&
       |        validSuccessorScript      &&
       |        validOraclePoolBox        &&
       |        preservedLpNFT            &&
       |        validLpBox                &&
       |        validY                    &&
       |        noMoreTokens              &&
       |        (lpAction || dexyAction)  && 
       |        validStorageRent  
       |    )
       |}
       |""".stripMargin

  val trackingScript =
    s"""{   
       |    // This box: Tracking box
       |    // 
       |    // TOKENS
       |    //   tokens(0): Tracking NFT
       |    // 
       |    // REGISTERS
       |    //   R4: Int (num) (explained below)
       |    //   R5: Int (denom) (explained below)
       |    //   R6: Boolean (isBelow) (explained below)
       |    //   R7: Int (trackingHeight) (explained below)
       |    // 
       |    // TRANSACTIONS 
       |    // [1] Update tracking
       |    //   Input         |  Output        |   Data-Input 
       |    // -----------------------------------------------
       |    // 0 Tracking box  |  Tracking box  |   LP
       |    //
       |    // Whenever LP box gets updated, someone must spend this box with LP as data input to update tracker 
       |      
       |    val threshold = 3 // error threshold in crossTrackerLow
       |    
       |    val oracleBoxIndex = 0
       |    val lpBoxIndex = 1
       |    val selfOutIndex = getVar[Int](0).get
       |
       |    val lpBox = CONTEXT.dataInputs(lpBoxIndex)
       |    val oracleBox = CONTEXT.dataInputs(oracleBoxIndex)
       |    val successor = OUTPUTS(selfOutIndex)
       |    
       |    val tokenY     = lpBox.tokens(2)
       |    
       |    val validLp = lpBox.tokens(0)._1 == fromBase64("${Base64.encode(lpNFT.decodeHex)}") // to identify LP box
       |    // this box can only be spent with LP and similarly LP can only be spent with this box.
       |    
       |    val validOraclePoolBox = oracleBox.tokens(0)._1 == fromBase64("${Base64.encode(oracleNFT.decodeHex)}") // to identify oracle pool box
       |    val validSuccessor = successor.tokens == SELF.tokens && successor.propositionBytes == SELF.propositionBytes && SELF.value <= successor.value
       |
       |    val oracleRateXY = oracleBox.R4[Long].get
       |    val reservesX = lpBox.value
       |    val reservesY = tokenY._2
       |    val lpRateXY = reservesX / reservesY  // we can assume that reservesY > 0 (since at least one token must exist)
       |
       |    // R4 contains an Int called num
       |    // R5 contains an Int called denom
       |    // R6 contains an Boolean called isBelown
       |    // R7 contains an Int called trackerHeight
       |    // num is numerator, denom is denominator. Let t = num/denom
       |    // trackerHeight is the height at which the tracker was "triggered". 
       |    // If the tracker "reset", then trackerHeight will store Int.MaxValue
       |    // 
       |    // isBelow tells us if the tracking should be of type "lower" or "higher" 
       |    // Let r be the ratio "oracle pool rate" / "LP rate", where the term "rate" denotes "Ergs per dexy"
       |    // Now, if "isBelow" is true (i.e. "lower" tracking), then the tracker will be triggered when r goes below t and will be reset once r goes above t
       |    
       |    // there will be following tracking boxes
       |    //  num | denom | height | isBelow
       |    //  95  | 100   | _      | true     (for extracting to future)
       |    //  98  | 100   | _      | true     (for arbitrage mint)
       |    //  101 | 100   | _      | false    (for release in future - reverse of extract to future)
       |    
       |    
       |    val denomIn = SELF.R4[Int].get
       |    val numIn = SELF.R5[Int].get
       |    val isBelowIn = SELF.R6[Boolean].get
       |    val trackerHeightIn = SELF.R7[Int].get
       |    
       |    val denomOut = successor.R4[Int].get
       |    val numOut = successor.R5[Int].get
       |    val isBelowOut = successor.R6[Boolean].get
       |    val trackerHeightOut = successor.R7[Int].get
       |    
       |    val validTracking = {
       |        // For a ratio of 95%, set num to 95 and denom to 100 (equivalently 19, 20), and set isBelow to true
       |        // Then trackerHeight will be set when oracle pool rate becomes <= 95% of LP rate 
       |        // and it will be reset to Int.MaxValue when that rate becomes > than 95% of LP rate
       |        // 
       |        // Let oracle pool rate be P and LP rate at earlier point be L0 and currently (via data input) be L1
       |        // Let N and D denote num and denom respectively. Then we can use the following table
       |        // 
       |        // EVENT    | isBelow | INPUT       | OUTPUT
       |        // ---------+---------+-------------+-----------
       |        // trigger  | true    | P/L0 >= N/D | P/L1 <  N/D 
       |        // reset    | true    | P/L0 <  N/D | P/L1 >= N/D (reverse of 1st row)
       |        // ---------+---------+-------------+------------
       |        // trigger  | false   | P/L0 <= N/D | P/L1 >  N/D 
       |        // reset    | false   | P/L0 >  N/D | P/L1 <= N/D (reverse of 1st row) 
       |        
       |        val x = oracleRateXY * denomIn
       |        val y = numIn * lpRateXY
       |        
       |        val notTriggeredEarlier = trackerHeightIn == ${Int.MaxValue}
       |        val triggeredNow = trackerHeightOut >= HEIGHT - threshold && trackerHeightOut <= HEIGHT
       |         
       |        val notResetEarlier = trackerHeightIn < ${Int.MaxValue}
       |        val resetNow = trackerHeightOut == ${Int.MaxValue}
       |         
       |        val trigger = ((isBelowIn && x < y) || (!isBelowIn && x > y)) && notTriggeredEarlier && triggeredNow
       |        val reset = (isBelowIn && x >= y) || (!isBelowIn && x <= y) && notResetEarlier && resetNow   
       |        val correctHeight = trigger || reset  
       |        
       |        numOut == numIn          && 
       |        denomOut == denomIn      && 
       |        isBelowOut == isBelowIn  && 
       |        correctHeight
       |    }
       |    
       |    sigmaProp(validSuccessor && validLp && validTracking && validOraclePoolBox)
       |}
       |""".stripMargin

  val interventionScript =
    s"""{  
       |  // This box: Intervention box
       |  // 
       |  // TOKENS
       |  //   tokens(0): Intervention NFT
       |  //   
       |  // REGISTERS
       |  // 
       |  // TRANSACTIONS
       |  // [1] Intervention
       |  //   Input         |  Output        |   Data-Input 
       |  // -----------------------------------------------
       |  // 0 LP            |  LP            |   Oracle
       |  // 1 Bank          |  Bank          |   Tracking (98%)
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
       |  val tracking98NFT = fromBase64("${Base64.encode(tracking98NFT.decodeHex)}")
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
       |  val validTrackingBox = trackingBox.tokens(0)._1 == tracking98NFT
       |   
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
       |  val trackingHeight = trackingBox.R7[Int].get
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
       |                  validTrackingBox                            &&
       |                  validThreshold                              &&
       |                  validLpIn                                   && 
       |                  validGap
       |   
       |  sigmaProp(validSwap)
       |}
       |""".stripMargin

  val extractScript =
    s"""|{   
        |    // This box: Extract to future
        |    // 
        |    // TOKENS
        |    //   tokens(0): extractionNFT 
        |    //   tokens(1): Dexy tokens
        |    // 
        |    // REGISTERS
        |    //   R3 (creation-info)
        |    // 
        |    // TRANSACTIONS
        |    //
        |    // [1] Extract to future
        |    //   Input         |  Output        |   Data-Input 
        |    // -----------------------------------------------
        |    // 0 LP            |  LP            |   Oracle (unused here)
        |    // 1 Extract       |  Extract       |   Bank   (to check that bank is empty)
        |    // 2               |                |   Tracking (95%)
        |    // 
        |    // [2] Reverse Extract to future (release)
        |    //   Input         |  Output        |   Data-Input 
        |    // -----------------------------------------------
        |    // 0 LP            |  LP            |   Oracle
        |    // 1 Extract       |  Extract       |   Tracking (101%)
        |        
        |    // ToDo: verify following
        |    //   cannot change prop bytes for LP, Extract and Tracking box
        |    //   cannot change tokens/nanoErgs in LP, extract and tracking box except what is permitted
        |    
        |    val lpBoxInIndex = 0
        |    val lpBoxOutIndex = 0
        |    
        |    val selfOutIndex = 1
        |    
        |    // for data inputs
        |    val oracleBoxIndex = 0
        |    val bankBoxIndex = 1
        |    val tracking95BoxIndex = 2
        |    
        |    val tracking101BoxIndex = 1
        |    
        |    val minTokens = 100 // if Dexy tokens less than this number in bank box, then bank is considered "empty"
        |    
        |    val tracking95NFT = fromBase64("${Base64.encode(tracking95NFT.decodeHex)}")
        |    val tracking101NFT = fromBase64("${Base64.encode(tracking101NFT.decodeHex)}")
        |    val bankNFT = fromBase64("${Base64.encode(bankNFT.decodeHex)}")
        |    val lpNFT = fromBase64("${Base64.encode(lpNFT.decodeHex)}")
        |    val oracleNFT = fromBase64("${Base64.encode(oracleNFT.decodeHex)}")
        |    
        |    val T_extract = 10 // blocks for which the rate is below 95%
        |    val T_release = 2 // blocks for which the rate is above 101%
        |    val T_delay = 20 // delay between any burn/release operation  ("T_burn" in the paper)
        |    
        |    val buffer = 3 // allowable error in setting height due to congestion 
        |    
        |    // tracking box should record at least T_extract blocks of < 95%
        |    val tracking95Box = CONTEXT.dataInputs(tracking95BoxIndex)
        |    val tracking101Box = CONTEXT.dataInputs(tracking101BoxIndex)
        |    val oracleBox = CONTEXT.dataInputs(oracleBoxIndex)
        |     
        |    val tracker95Height = tracking95Box.R7[Int].get
        |    val tracker101Height = tracking101Box.R7[Int].get
        |    
        |    val lpBoxIn = INPUTS(lpBoxInIndex)
        |    val lpBoxOut = OUTPUTS(lpBoxInIndex)
        |    
        |    val successor = OUTPUTS(selfOutIndex)
        |    
        |    val lastBurnOrRelease = SELF.creationInfo._1 
        |     
        |    val validDelay = lastBurnOrRelease < HEIGHT - T_delay
        |     
        |    val validSuccessor = successor.tokens(0)._1 == SELF.tokens(0)._1          &&  // NFT preserved
        |                         successor.tokens(1)._1 == SELF.tokens(1)._1          &&  // Dexy token id preserved
        |                         successor.propositionBytes == SELF.propositionBytes  &&
        |                         successor.value == SELF.value                        &&
        |                         successor.creationInfo._1 >= HEIGHT - buffer         
        |                            
        |    val deltaDexy = successor.tokens(1)._2 - SELF.tokens(1)._2 // can be +ve or -ve 
        |    
        |    val validBankBox = if (CONTEXT.dataInputs.size > bankBoxIndex) {
        |      CONTEXT.dataInputs(bankBoxIndex).tokens(0)._1 == bankNFT &&
        |      CONTEXT.dataInputs(bankBoxIndex).tokens(1)._2 <= minTokens
        |    } else false
        |    
        |    val validOracleBox = oracleBox.tokens(0)._1 == oracleNFT 
        |    
        |    val reservesYOut = lpBoxOut.tokens(2)._2
        |    val reservesYIn = lpBoxIn.tokens(2)._2
        |    val reservesXOut = lpBoxOut.value
        |    val reservesXIn = lpBoxIn.value
        |    
        |    val validLpBox = lpBoxIn.tokens(0)._1 == lpNFT                               && // Maybe this check not needed? (see LP box)
        |                     lpBoxOut.tokens(0)._1 == lpBoxIn.tokens(0)._1               && // NFT preserved 
        |                     lpBoxOut.tokens(1) == lpBoxIn.tokens(1)                     && // LP tokens preserved
        |                     lpBoxOut.tokens(2)._1 == lpBoxIn.tokens(2)._1               && // Dexy token Id preserved
        |                     lpBoxOut.tokens(2)._1 == SELF.tokens(1)._1                  && // Dexy token Id is same as tokens stored here
        |                     reservesYOut == (reservesYIn + deltaDexy)                   && // Dexy token qty preserved
        |                     reservesXOut == reservesXIn                                 &&
        |                     lpBoxOut.propositionBytes == lpBoxIn.propositionBytes  
        |     
        |    val validTracking95Box = tracking95Box.tokens(0)._1 == tracking95NFT
        |    val validTracking101Box = tracking101Box.tokens(0)._1 == tracking101NFT
        |    
        |    val oracleRateXY = oracleBox.R4[Long].get
        |    val lpRateXYOut = reservesXOut / reservesYOut
        |    
        |    val validExtractAmount = oracleRateXY * 100 > lpRateXYOut * 98 && // lpRate at output must be >= 0.98 * oracleRate   
        |                             oracleRateXY * 100 < lpRateXYOut * 101   // lpRate at output must be <= 1.01 * oracleRate 
        |                             // ToDo: possibly tweak the 101 requirement (or remove it?)
        |                             
        |    val validReleaseAmount = oracleRateXY * 100 > lpRateXYOut * 101 && // lpRate at output must be >= 1.01 * oracleRate   
        |                             oracleRateXY * 100 < lpRateXYOut * 104    // lpRate at output must be <= 1.04 * oracleRate 
        |                             // ToDo: possibly tweak the 104 requirement (or remove it?)
        |                             
        |    
        |    val validExtract  = (HEIGHT - tracker95Height) > T_extract  && // at least T_extract blocks have passed after crossing below 95% 
        |                        validBankBox                            && 
        |                        deltaDexy > 0                           &&
        |                        validExtractAmount                      &&
        |                        validTracking95Box
        |                        // ToDo: do we need to check that input ratio is < 95%? (its already checked in tracker)
        |
        |    val validRelease  = HEIGHT - tracker101Height > T_release  && // at least T_release blocks have passed after crossing above 101%
        |                        deltaDexy < 0                          && 
        |                        validReleaseAmount                     &&
        |                        validTracking101Box
        |                        // ToDo: do we need to check that input ratio is > 101%? (its already checked in tracker)
        |                         
        |    sigmaProp(validSuccessor && validDelay && validLpBox && (validExtract || validRelease))
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

  val extractErgoTree = ScriptUtil.compile(Map(), extractScript)
  val extractAddress = getStringFromAddress(getAddressFromErgoTree(extractErgoTree))
  println(s"Extract: $extractAddress")
  println(extractScript)
  println()

  val interventionErgoTree = ScriptUtil.compile(Map(), interventionScript)
  val interventionAddress = getStringFromAddress(getAddressFromErgoTree(interventionErgoTree))
  println(s"Intervention: $interventionAddress")
  println(interventionScript)
  println()

}
