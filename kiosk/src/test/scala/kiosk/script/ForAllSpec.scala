package kiosk.script

import kiosk.appkit.Client
import kiosk.ergo.ByteArrayToBetterByteArray
import org.ergoplatform.appkit.ConstantsBuilder
import org.scalatest.{Matchers, WordSpec}

class ForAllSpec extends WordSpec with Matchers {
  val script =
    """{
      |    val tracker = SELF.R4[Coll[((Int, Int), Long)]].get // ((numerator, denominator), height or longMax
      |    val bool = tracker.forall(
      |      { (t: ((Int, Int), Long)) =>
      |        val numDenom = t._1
      |        val storedHeight = t._2
      |        val num = numDenom._1
      |        val denom = numDenom._2
      |        num > denom && t._2 > 0
      |      }
      |    )
      |    sigmaProp(bool)
      |}
      |""".stripMargin

  Client.usingContext { implicit ctx =>
    val appkitErgoTree = ctx
      .compileContract(
        ConstantsBuilder.empty(),
        script
      )
      .getErgoTree
      .bytes
      .encodeHex

    val kioskErgoTree = ScriptUtil.compile(Map(), script).bytes.encodeHex

    kioskErgoTree shouldBe appkitErgoTree
  }

}
