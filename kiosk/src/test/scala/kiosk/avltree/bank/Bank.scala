package kiosk.avltree.bank

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.script.ScriptUtil

object Bank extends App {
  lazy val minStorageRent = 100000L
  lazy val timeOut = 5
  lazy val bankScript =
    s""" // this box
       | // R4 root hash (Coll[Byte])
       | // R5 bank pub key (GroupElement)
       | // R6 Int (if == 0 indicates bank is not defunct)
       | 
       | // tokens(0) = bankNFT
       | // tokens(1) = bank issues tokens
       | {
       |    val timeOut = $timeOut
       |    
       |    val inCreationHeight = SELF.creationInfo._1
       |    val inLedgerTree = SELF.R4[AvlTree].get
       |    val inBankPubKey = SELF.R5[GroupElement].get
       |    val inIsDefunct = SELF.R6[Int].get != 0
       |    
       |    val out = OUTPUTS(0)
       |    
       |    val outCreationHeight = out.creationInfo._1
       |    val outLedgerTree = out.R4[AvlTree].get
       |    val outBankPubKey = out.R5[GroupElement].get // just access it to ensure there is a group element; no need to validate if its same
       |    val outIsDefunct = out.R6[Int].get != 0
       |     
       |    val validSuccessor = {
       |      out.propositionBytes == SELF.propositionBytes &&
       |      out.value >= $minStorageRent                  &&
       |      out.tokens(0) == SELF.tokens(0)               &&
       |      out.tokens(1)._1 == SELF.tokens(1)._1
       |    }
       |    
       |    val validBankSpend = {
       |      ! outIsDefunct                  &&
       |      proveDlog(inBankPubKey)         && 
       |      outCreationHeight > HEIGHT - 10 
       |    }
       |    
       |    val makeDefunct = {
       |      ! inIsDefunct                        &&
       |      outIsDefunct                         &&
       |      inCreationHeight < HEIGHT - timeOut  &&
       |      outLedgerTree == inLedgerTree        &&
       |      out.tokens == SELF.tokens         
       |    }
       |    
       |    val isWithdraw = {
       |      val withdrawBox = OUTPUTS(1)
       |      val withdrawTokenId = withdrawBox.tokens(0)._1
       |      val withdrawValue = withdrawBox.tokens(0)._2
       |      val withdrawKey = blake2b256(withdrawBox.propositionBytes)
       |
       |      val removeProof = withdrawBox.R4[Coll[Byte]].get
       |      val lookupProof = withdrawBox.R4[Coll[Byte]].get
       |      
       |      val withdrawAmtCollByte = inLedgerTree.get(withdrawKey, lookupProof).get
       |      
       |      val userBalance = byteArrayToLong(withdrawAmtCollByte)
       |      
       |      val removedTree = inLedgerTree.remove(Coll(withdrawKey), removeProof).get
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
  lazy val bankErgoTree = ScriptUtil.compile(Map(), bankScript)
  lazy val bankAddress = getStringFromAddress(getAddressFromErgoTree(bankErgoTree))
  println(bankScript)
  println(bankAddress)
}
