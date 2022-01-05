package kiosk.bank

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.script.ScriptUtil

object BankSpec extends App {
  val minStorageRent = 100000L

  val bankScript =
    s""" // this box
       | // tokens(0) = bankNFT
       | // tokens(1) = bank issues tokens
       | {
       |    val inCreationHeight = SELF.creationInfo._1
       |    val inLedgerTree = SELF.R4[AvlTree].get
       |    val inBankPubKey = SELF.R5[GroupElement].get
       |    val inIsDefunct = SELF.R6[Boolean].get
       |    
       |    val out = OUTPUTS(0)
       |    
       |    val outCreationHeight = out.creationInfo._1
       |    val outLedgerTree = out.R4[AvlTree].get
       |    val outBankPubKey = out.R5[GroupElement].get // just access it to ensure there is a group element; no need to validate if its same
       |    val outIsDefunct = out.R6[Boolean].get
       |     
       |    val validSuccessor = {
       |      out.propositionBytes == SELF.propositionBytes &&
       |      out.value >= $minStorageRent                  &&
       |      out.tokens(0) == SELF.tokens(0)               &&
       |      out.tokens(1)._1 == SELF.tokens(1)._1
       |    }
       |    
       |    val validBankSpend = {
       |      ! inIsDefunct                   &&
       |      proveDlog(inBankPubKey)         && 
       |      outCreationHeight > HEIGHT - 10 
       |    }
       |    
       |    val makeDefunct = {
       |      ! inIsDefunct                     &&
       |      outIsDefunct                      &&
       |      inCreationHeight < HEIGHT - 1000  &&
       |      outLedgerTree == inLedgerTree     &&
       |      out.tokens == SELF.tokens         
       |    }
       |    
       |    val isWithdraw = {
       |      val withdrawBox = OUTPUTS(1)
       |      val withdrawTokenId = withdrawBox.tokens(0)._1
       |      val withdrawValue = withdrawBox.tokens(0)._2
       |      val withdrawKey = blake2b256(withdrawBox.propositionBytes)
       |
       |      val removeProof = getVar[Coll[Byte]](0).get
       |      val lookupProof = getVar[Coll[Byte]](1).get
       |      val withdrawIndex = getVar[Int](2).get
       |      
       |      val withdrawAmtCollByte = inLedgerTree.get(withdrawKey, lookupProof).get
       |      val userBalance = byteArrayToLong(withdrawAmtCollByte)
       |      
       |      val removedTree = outLedgerTree.remove(Coll(withdrawKey), removeProof).get
       |        
       |      val correctAmount = withdrawValue == userBalance
       |      val correctBalance = out.tokens(1)._2 == SELF.tokens(1)._2 - withdrawValue 
       |      val correctTokenId = withdrawTokenId == SELF.tokens(1)._1 
       |      
       |      inIsDefunct                  && 
       |      outIsDefunct                 &&
       |      removedTree == outLedgerTree &&
       |      correctAmount                &&
       |      correctBalance               &&
       |      correctTokenId               
       |    }
       |    
       |    sigmaProp((validBankSpend || makeDefunct || isWithdraw) && validSuccessor)
       | }
       |""".stripMargin
  val bankErgoTree = ScriptUtil.compile(Map(), bankScript)
  val bankAddress = getStringFromAddress(getAddressFromErgoTree(bankErgoTree))
  println(bankAddress)

}
