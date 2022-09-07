package kiosk.encryption

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.script.ScriptUtil

object ElGamal extends App {
  lazy val script =
    s"""{
       |  // ElGamal encryption with plaintext awareness
       |  // public key (g, g^x)
       |  // secret key x
       |  // 
       |  // message m less than 32 bytes given as Coll[Byte]
       |  // generate secret y 
       |  // compute c = encode((g^x)^y) XOR m
       |  // ciphertext is (c, g^y)
       |  // 
       |  // to decrypt:
       |  // compute m = c XOR encode((g^y)^x)
       |  // 
       |  // plaintext knowledge
       |  // observe that knowledge of y is equivalent to knowledge of (g^x)^y, 
       |  // which is equivalent to knowledge of m
       |  
       |  val g: GroupElement = groupGenerator
       |  val u = SELF.R4[GroupElement].get // u = g^x
       |  val h = SELF.getVar[GroupElement].get // h = g^y
       |  val c = SELF.getVar[Coll[Byte]].get // not used; dummy read
       |  proveDlog(h) // proves that spender knows some unknown m, whose ciphertext is (c, g^y)
       |}""".stripMargin

  lazy val ergoTree = ScriptUtil.compile(Map(), script)
  lazy val address = getStringFromAddress(getAddressFromErgoTree(ergoTree))
  println(address)

}
