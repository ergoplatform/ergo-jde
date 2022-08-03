package kiosk.context

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.script.ScriptUtil

object Context extends App {
  lazy val script =
    s"""{  
       |   val selfOut = OUTPUTS(0)
       |  
       |   val data = getVar[Coll[Byte]](0).get
       |   val hash = getVar[Coll[Byte]](1).get 
       |   val correctHash = blake2b256(data) == hash
       |   
       |   val validSpend = SELF.propositionBytes == selfOut.propositionBytes && 
       |                    correctHash
       |   val prop = sigmaProp(validSpend)
       |   sigmaProp(hash == prop.propBytes) || prop 
       |}
       |""".stripMargin
  lazy val ergoTree = ScriptUtil.compile(Map(), script)
  lazy val address = getStringFromAddress(getAddressFromErgoTree(ergoTree))
  println(address)

}
