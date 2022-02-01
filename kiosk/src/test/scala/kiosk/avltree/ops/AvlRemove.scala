package kiosk.avltree.ops

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.script.ScriptUtil

object AvlRemove extends App {
  lazy val script =
    s"""{  
       |   val selfOut = OUTPUTS(0)
       |  
       |   val inTree = selfOut.R4[AvlTree].get
       |   val outTree = selfOut.R5[AvlTree].get
       |   val key = selfOut.R6[Coll[Byte]].get
       |   val value = selfOut.R7[Coll[Byte]].get
       |   val proof = selfOut.R8[Coll[Byte]].get
       |   
       |   val newTree = inTree.remove(Coll(key), proof).get
       |   
       |   val validSpend = SELF.propositionBytes == selfOut.propositionBytes && 
       |                    outTree == newTree
       |                       
       |   sigmaProp(validSpend)
       |}
       |""".stripMargin
  lazy val ergoTree = ScriptUtil.compile(Map(), script)
  lazy val address = getStringFromAddress(getAddressFromErgoTree(ergoTree))
  println(address)

}
