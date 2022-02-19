package kiosk.mixer

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.script.ScriptUtil

object Mix extends App {
  lazy val script =
    s"""{  
       |   val h = SELF.R4[GroupElement].get
       |   val w = SELF.R5[GroupElement].get
       |   
       |   val owner = proveDHTuple(h, h, w, w) // = proveDlog(h, w)
       |   
       |   val mix = {
       |     val out1 = OUTPUTS(0)
       |     val out2 = OUTPUTS(1)
       |     
       |     val hOut1 = out1.R4[GroupElement].get
       |     val hOut2 = out2.R4[GroupElement].get
       |     
       |     val wOut1 = out1.R5[GroupElement].get
       |     val wOut2 = out2.R5[GroupElement].get
       |     
       |     val validOuts = out1.propositionBytes == SELF.propositionBytes &&
       |                     out2.propositionBytes == SELF.propositionBytes &&
       |                     out1.value == SELF.value && 
       |                     out2.value == SELF.value &&
       |                     hOut1 != wOut1 && // to rule out point at infinity
       |                     hOut2 != wOut2    // to rule out point at infinity        
       |                       
       |     val validW = proveDHTuple(h, w, hOut1, wOut1) || proveDHTuple(h, w, hOut2, wOut2)
       |      
       |     validW && validOuts
       |   }
       |   
       |   mix || owner
       |}
       |""".stripMargin
  lazy val ergoTree = ScriptUtil.compile(Map(), script)
  lazy val address = getStringFromAddress(getAddressFromErgoTree(ergoTree))
  println(address)

}
