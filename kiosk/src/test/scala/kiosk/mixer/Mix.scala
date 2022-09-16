package kiosk.mixer

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.script.ScriptUtil

object Mix extends App {
  lazy val script =
    s"""{
        |val a = SELF.R4[GroupElement].get  // current base for dLog
        |val b = SELF.R5[GroupElement].get
        |val m = SELF.R6[GroupElement].get  // part of public key of the mixer (base)
        |val n = SELF.R7[GroupElement].get  // part of public key of the mixer (base ^ secret)
        | // Note: we are using two elements (m, n) to represent mixer, rather than single element because
        | // we want the possibility of "stealth" locking a box to a mixer (i.e., without revealing the mixer identity)
        | // So mixer will publish public key (u, v), two group elements.
        | // In order to lock the mix box to the mixer, Alice will set (m, n) = (u^r, v^r)
        |
        |val h = SELF.creationInfo._1 // height at which box was created
        |
        |val lockTime = 50 // number of blocks for which box is time-locked
        |
        |val owner = proveDHTuple(a, a, b, b)  // = proveDlog(a, b)
        |val mixer = proveDHTuple(m, m, n, n)  // = proveDlog(m, n)
        |
        |val timeOut = HEIGHT > h + lockTime
        |
        |val mix = {
        |  val out0 = OUTPUTS(0)   // first output
        |  val out1 = OUTPUTS(1)   // second output
        |  
        |  val a0 = out0.R4[GroupElement].get  // register a of first output
        |  val a1 = out1.R4[GroupElement].get  // register a of second output
        |  val b0 = out0.R5[GroupElement].get  // register b of first output
        |  val b1 = out1.R5[GroupElement].get  // register b of second output
        |  
        |  val m0 = out0.R6[GroupElement].get  // access group element to ensure it exists (not used)
        |  val m1 = out1.R6[GroupElement].get  // access group element to ensure it exists (not used)
        |  val n0 = out0.R7[GroupElement].get  // access group element to ensure it exists (not used)
        |  val n1 = out1.R7[GroupElement].get  // access group element to ensure it exists (not used)
        |  // ToDo: Check whether JITC will execute the above 4 lines, if they are never accessed.  
        |  
        |  val h0 = out0.creationInfo._1  // height at which first output is created 
        |  val h1 = out1.creationInfo._1  // height at which second output is created  
        |  
        |  // ensure outputs have same script as this box and have the same value
        |  val validOuts = out0.propositionBytes == SELF.propositionBytes &&
        |                  out1.propositionBytes == SELF.propositionBytes &&
        |                  out0.value == SELF.value                       && 
        |                  out1.value == SELF.value                       &&
        |                  a0 != b0                                       && // rule out point at infinity
        |                  a1 != b1                                       && // rule out point at infinity        
        |                  h0 <= HEIGHT                                   && // ensure that h0 is not too high
        |                  h1 <= HEIGHT                                      // ensure that h1 is not too high
        |      
        |  // at least one of the outputs has the right relationship between R4, R5
        |  val validAB = proveDHTuple(a, b, a0, b0) || proveDHTuple(a, b, a1, b1)
        |  
        |  validAB && validOuts && (mixer || timeOut)
        |}
        |
        |owner || mix
        |}
       |""".stripMargin
  lazy val ergoTree = ScriptUtil.compile(Map(), script)
  lazy val address = getStringFromAddress(getAddressFromErgoTree(ergoTree))
  println(address)

}
