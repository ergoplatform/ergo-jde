package jde

import jde.parser.Parser
import kiosk.explorer.Explorer
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito._

// tests various types
class TypeSpec extends WordSpec with MockitoSugar with Matchers {
  val explorer = mock[Explorer]

  "BigInt to GroupElement" should {
    "Do correct exponentiation with one" in {
      val script =
        s"""{
           |  "constants": [
           |    {
           |      "name": "expected",
           |      "type": "GroupElement",
           |      "value": "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798"
           |    },
           |    {
           |      "name": "one",
           |      "type": "BigInt",
           |      "value": "1"
           |    }
           |  ],
           |  "unaryOps": [
           |    {
           |      "name": "actual",
           |      "from": "one",
           |      "op": "Exp"
           |    }
           |  ],
           |  "postConditions": [
           |    {
           |      "first": "actual",
           |      "second": "expected",
           |      "op": "Eq"
           |    }
           |  ]
           |}""".stripMargin
      new compiler.Compiler(explorer).compile(Parser.parse(script))
    }
    "Do correct exponentiation with small number" in {
      val script =
        s"""{
           |  "constants": [
           |    {
           |      "name": "expected",
           |      "type": "GroupElement",
           |      "value": "03a598a8030da6d86c6bc7f2f5144ea549d28211ea58faa70ebf4c1e665c1fe9b5"
           |    },
           |    {
           |      "name": "bigint",
           |      "type": "BigInt",
           |      "value": "123"
           |    }
           |  ],
           |  "unaryOps": [
           |    {
           |      "name": "actual",
           |      "from": "bigint",
           |      "op": "Exp"
           |    }
           |  ],
           |  "postConditions": [
           |    {
           |      "first": "actual",
           |      "second": "expected",
           |      "op": "Eq"
           |    }
           |  ]
           |}""".stripMargin
      new compiler.Compiler(explorer).compile(Parser.parse(script))
    }
    "Do correct exponentiation with large number" in {
      val script =
        s"""{
           |  "constants": [
           |    {
           |      "name": "expected",
           |      "type": "GroupElement",
           |      "value": "02c4fe4d0f440c735c406450aa7e168b5db3d1100818b1b9527265d342d29526ed"
           |    },
           |    {
           |      "name": "bigint",
           |      "type": "BigInt",
           |      "value": "1232345289345789234758923475927845109847238945729857289523414524652354125235"
           |    }
           |  ],
           |  "unaryOps": [
           |    {
           |      "name": "actual",
           |      "from": "bigint",
           |      "op": "Exp"
           |    }
           |  ],
           |  "postConditions": [
           |    {
           |      "first": "actual",
           |      "second": "expected",
           |      "op": "Eq"
           |    }
           |  ]
           |}""".stripMargin
      new compiler.Compiler(explorer).compile(Parser.parse(script))
    }
    "Fail invalid exponentiation" in {
      val script =
        s"""{
           |  "constants": [
           |    {
           |      "name": "expected",
           |      "type": "GroupElement",
           |      "value": "02c4fe4d0f440c735c406450aa7e168b5db3d1100818b1b9527265d342d29526ed"
           |    },
           |    {
           |      "name": "bigint",
           |      "type": "BigInt",
           |      "value": "1232345289345789234758923475927845109847238945729857289523414524652354125234" 
           |    }
           |  ],
           |  "unaryOps": [
           |    {
           |      "name": "actual",
           |      "from": "bigint",
           |      "op": "Exp"
           |    }
           |  ],
           |  "postConditions": [
           |    {
           |      "first": "actual",
           |      "second": "expected",
           |      "op": "Eq"
           |    }
           |  ]
           |}""".stripMargin

      the[Exception] thrownBy {
        new compiler.Compiler(explorer).compile(Parser.parse(script))
      } should have message """Failed post-condition: actual: (027e8a14f0e94a1cc084029fc0203a8fa05609aea7170e585616481a9880678bc6) Eq expected (02c4fe4d0f440c735c406450aa7e168b5db3d1100818b1b9527265d342d29526ed)"""
    }
  }
  "BigInt" should {
    "Do correct multiplication" in {
      val script =
        s"""{
           |  "constants": [
           |    {
           |      "name": "expected",
           |      "type": "BigInt",
           |      "value": "117213155668880043312875579616453747496692426778362532400959435477829249689999824710052551486244799248969572159560000665300"
           |    },
           |    {
           |      "name": "a",
           |      "type": "BigInt",
           |      "value": "2390845239857257289345728957892457982437598243759423759823475"
           |    },
           |    {
           |      "name": "b",
           |      "type": "BigInt",
           |      "value": "49025823049875918437561232409586023498563094812020493568242908"
           |    }
           |  ],
           |  "binaryOps": [
           |    {
           |      "name": "actual",
           |      "first": "a",
           |      "second": "b",
           |      "op": "Mul"
           |    }
           |  ],
           |  "postConditions": [
           |    {
           |      "first": "actual",
           |      "second": "expected",
           |      "op": "Eq"
           |    }
           |  ]
           |}""".stripMargin
      new compiler.Compiler(explorer).compile(Parser.parse(script))
    }
  }
}
