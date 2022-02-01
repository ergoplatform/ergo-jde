package kiosk.avltree.ops

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.script.ScriptUtil

object AvlNotExist extends App {
  lazy val script =
    s"""{  
       |   val selfOut = OUTPUTS(0)
       |  
       |   val tree = selfOut.R4[AvlTree].get
       |   val key = selfOut.R5[Coll[Byte]].get
       |   val proof = selfOut.R6[Coll[Byte]].get
       |   
       |   val notExist = tree.get(key, proof).isDefined == false
       |   
       |   val validSpend = SELF.propositionBytes == selfOut.propositionBytes && 
       |                    notExist
       |                       
       |   sigmaProp(validSpend)
       |}
       |""".stripMargin
  lazy val ergoTree = ScriptUtil.compile(Map(), script)
  lazy val address = getStringFromAddress(getAddressFromErgoTree(ergoTree))
  println(address)

}
