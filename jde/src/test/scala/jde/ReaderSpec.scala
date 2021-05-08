package jde

import jde.compiler.Compiler
import jde.compiler.model.CompileResult
import jde.parser.Parser
import kiosk.encoding.ScalaErgoConverters
import kiosk.ergo.{KioskBox, KioskErgoTree}
import kiosk.explorer.Explorer
import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito._
import sigmastate.Values.ErgoTree

class ReaderSpec extends WordSpec with MockitoSugar with Matchers {
  val explorer: Explorer = mock[Explorer]
  when(explorer.getHeight) thenReturn 12345
  val txBuilder = new Compiler(explorer)

  def someSeq[T](seq: T*): Option[Seq[T]] = Some(seq)

  trait Mocks {
    val myAddress = "9etuNZvRv3PZLq5HigoSMUANgmF6f7jTJLfnmdjuzUKQ7WH9ysD"
    val boxId1 = "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"
    val boxId2 = "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"
    val boxId3 = "af0e35e1cf5a8890d70cef498c996dcd3e7658cfadd37695425032d4f8327d8a"

    val tokenId1 = "ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f"
    val tokenId2 = "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"
    val tokenId3 = "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"
    val tokenId4 = "5c674366216d127f7424bfcf1bf52310f9c34cd8d07013c804a95bb8ce9e4f82"

    val fakeBox1: KioskBox = KioskBox(
      address = myAddress,
      value = 1000000000L,
      registers = Array(),
      tokens = Array(tokenId1 -> 10000, tokenId2 -> 1),
      optBoxId = Some(boxId1),
      spentTxId = None
    )

    val fakeBox2: KioskBox = KioskBox(
      address = myAddress,
      value = 1100000L,
      registers = Array(),
      tokens = Array(tokenId3 -> 100, tokenId2 -> 1234, tokenId4 -> 1),
      optBoxId = Some(boxId2),
      spentTxId = None
    )

    val fakeBox3: KioskBox = KioskBox(
      address = myAddress,
      value = 2200000L,
      registers = Array(),
      tokens = Array(tokenId3 -> 1),
      optBoxId = Some(boxId3),
      spentTxId = None
    )

    when(explorer.getUnspentBoxes(myAddress)) thenReturn Seq(fakeBox1, fakeBox2, fakeBox3)
    when(explorer.getBoxById(boxId1)) thenReturn fakeBox1
    when(explorer.getBoxById(boxId2)) thenReturn fakeBox2
    when(explorer.getBoxById(boxId3)) thenReturn fakeBox3

    val code: String

    lazy val getResult: CompileResult = new compiler.Compiler(explorer).compile(Parser.parse(code))
  }

  "Reader" should {
    "select first box when filtered by address" in new Mocks {
      lazy val code: String = s"""{
                         |  "constants":[
                         |    {
                         |      "name": "myAddress",
                         |      "value": "$myAddress",
                         |      "type": "Address"
                         |    }
                         |  ],
                         |  "inputs": [
                         |    {
                         |      "address": {
                         |        "value": "myAddress"
                         |      }
                         |    }
                         |  ]
                         |}
                         |""".stripMargin

      getResult.inputBoxIds shouldBe Seq(boxId1)
    }

    "select second box when filtered by id" in new Mocks {
      lazy val code: String = s"""{
                         |  "constants":[
                         |    {
                         |      "name": "boxIds",
                         |      "value": "$boxId2",
                         |      "type": "CollByte"
                         |    }
                         |  ],
                         |  "inputs": [
                         |    {
                         |      "id": {
                         |         "value": "boxIds"
                         |      }
                         |    }
                         |  ]
                         |}
                         |""".stripMargin

      getResult.inputBoxIds shouldBe Seq(boxId2)
    }

    "should throw exception when filtered by address and non-existent id" in new Mocks {
      lazy val code: String = s"""{
                         |  "constants":[
                         |    {
                         |      "name": "boxId",
                         |      "value": "$tokenId1",
                         |      "type": "CollByte"
                         |    },
                         |    {
                         |      "name": "someAddress",
                         |      "value": "$myAddress",
                         |      "type": "Address"
                         |    }
                         |  ],
                         |  "inputs": [
                         |    {
                         |      "id": {
                         |         "value": "boxId"
                         |      },
                         |      "address": {
                         |         "value": "someAddress"
                         |      }
                         |    }
                         |  ]
                         |}
                         |""".stripMargin

      the[Exception] thrownBy getResult should have message "No box matched for input at index 0"
    }

    "should throw exception when filtered by non-existent address and boxId" in new Mocks {
      lazy val code: String = s"""{
                         |  "constants":[
                         |    {
                         |      "name": "boxId",
                         |      "value": "$boxId1",
                         |      "type": "CollByte"
                         |    },
                         |    {
                         |      "name": "someAddress",
                         |      "value": "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi",
                         |      "type": "Address"
                         |    }
                         |  ],
                         |  "inputs": [
                         |    {
                         |      "id": {
                         |         "value": "boxId"
                         |      },
                         |      "address": {
                         |         "value": "someAddress"
                         |      }
                         |    }
                         |  ]
                         |}
                         |""".stripMargin

      the[Exception] thrownBy getResult should have message "No box matched for input at index 0"
    }

    "select no boxes when filtered by address and non-existent id with 'optional' option" in new Mocks {
      lazy val code: String = s"""{
                         |  "constants":[
                         |    {
                         |      "name": "boxIds",
                         |      "value": "$tokenId1",
                         |      "type": "CollByte"
                         |    },
                         |    {
                         |      "name": "someAddress",
                         |      "value": "$myAddress",
                         |      "type": "Address"
                         |    }
                         |  ],
                         |  "inputs": [
                         |    {
                         |      "id": {
                         |         "value": "boxIds"
                         |      },
                         |      "address": {
                         |         "value": "someAddress"
                         |      },
                         |      "options": [
                         |        "optional"
                         |      ]
                         |    }
                         |  ]
                         |}
                         |""".stripMargin

      getResult.inputBoxIds shouldBe Nil
    }

    "select no boxes when filtered by non-existent address and id with 'optional' option" in new Mocks {
      lazy val code: String = s"""{
                         |  "constants":[
                         |    {
                         |      "name": "boxIds",
                         |      "value": "$boxId2",
                         |      "type": "CollByte"
                         |    },
                         |    {
                         |      "name": "someAddress",
                         |      "value": "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi",
                         |      "type": "Address"
                         |    }
                         |  ],
                         |  "inputs": [
                         |    {
                         |      "id": {
                         |         "value": "boxIds"
                         |      },
                         |      "address": {
                         |         "value": "someAddress"
                         |      },
                         |      "options": [
                         |        "optional"
                         |      ]
                         |    }
                         |  ]
                         |}
                         |""".stripMargin

      getResult.inputBoxIds shouldBe Nil
    }

    "select all boxes when filtered by address with 'multi' option" in new Mocks {
      lazy val code: String = s"""{
                         |  "constants":[
                         |    {
                         |      "name": "myAddress",
                         |      "value": "$myAddress",
                         |      "type": "Address"
                         |    }
                         |  ],
                         |  "inputs": [
                         |    {
                         |      "address": {
                         |        "value": "myAddress"
                         |      },
                         |      "options": [
                         |        "multi"
                         |      ]
                         |    }
                         |  ]
                         |}
                         |""".stripMargin

      getResult.inputBoxIds shouldBe Seq(boxId1, boxId2, boxId3)
    }

    "select first and second boxes when filtered by multiple ids with 'multi' option" in new Mocks {
      lazy val code: String = s"""{
                         |  "constants":[
                         |    {
                         |      "name": "boxIds",
                         |      "values": [
                         |         "$boxId1",
                         |         "$boxId2"
                         |      ],
                         |      "type": "CollByte"
                         |    }
                         |  ],
                         |  "inputs": [
                         |    {
                         |      "id": {
                         |         "value": "boxIds"
                         |      },
                         |      "options": [
                         |        "multi"
                         |      ]
                         |    }
                         |  ]
                         |}
                         |""".stripMargin
      getResult.inputBoxIds shouldBe Seq(boxId1, boxId2)
    }

    "select second box when filtered by address and token" in new Mocks {
      lazy val code: String = s"""{
                        |  "constants":[
                        |    {
                        |      "name": "myAddress",
                        |      "value": "$myAddress",
                        |      "type": "Address"
                        |    },
                        |    {
                        |      "name": "tokenId2",
                        |      "value": "$tokenId2",
                        |      "type": "CollByte"
                        |    },
                        |    {
                        |      "name": "one",
                        |      "value": "1",
                        |      "type": "Long"
                        |    }
                        |  ],
                        |  "inputs": [
                        |    {
                        |      "address": {
                        |        "value": "myAddress"
                        |      },
                        |      "tokens": [
                        |        {
                        |          "id": {
                        |            "value": "tokenId2"
                        |          },
                        |          "amount": {
                        |            "value": "one",
                        |            "filter": "Gt"
                        |          }
                        |        }
                        |      ]
                        |    }
                        |  ]
                        |}
                        |""".stripMargin

      getResult.inputBoxIds shouldBe Seq(boxId2)
    }

    "select second box when filtered by address and id" in new Mocks {
      lazy val code: String = s"""{
                       |  "constants":[
                       |    {
                       |      "name": "myAddress",
                       |      "value": "$myAddress",
                       |      "type": "Address"
                       |    },
                       |    {
                       |      "name": "boxId2",
                       |      "value": "$boxId2",
                       |      "type": "CollByte"
                       |    }
                       |  ],
                       |  "inputs": [
                       |    {
                       |      "address": {
                       |        "value": "myAddress"
                       |      },
                       |      "id": {
                       |         "value": "boxId2"
                       |      }
                       |    }
                       |  ]
                       |}
                       |""".stripMargin

      getResult.inputBoxIds shouldBe Seq(boxId2)
    }

    "select third box when filtered by address and multiple ids" in new Mocks {
      lazy val code: String = s"""{
                       |  "constants":[
                       |    {
                       |      "name": "myAddress",
                       |      "value": "$myAddress",
                       |      "type": "Address"
                       |    },
                       |    {
                       |      "name": "boxIds",
                       |      "values": [
                       |         "$boxId3",
                       |         "$boxId2"
                       |      ],
                       |      "type": "CollByte"
                       |    }
                       |  ],
                       |  "inputs": [
                       |    {
                       |      "address": {
                       |        "value": "myAddress"
                       |      },
                       |      "id": {
                       |         "value": "boxIds"
                       |      }
                       |    }
                       |  ]
                       |}
                       |""".stripMargin

      getResult.inputBoxIds shouldBe Seq(boxId3)
    }

    "select third and second boxes when filtered by address and multiple ids with 'multi' option" in new Mocks {
      lazy val code: String = s"""{
                       |  "constants":[
                       |    {
                       |      "name": "myAddress",
                       |      "value": "$myAddress",
                       |      "type": "Address"
                       |    },
                       |    {
                       |      "name": "boxIds",
                       |      "values": [
                       |         "$boxId3",
                       |         "$boxId2"
                       |      ],
                       |      "type": "CollByte"
                       |    }
                       |  ],
                       |  "inputs": [
                       |    {
                       |      "address": {
                       |        "value": "myAddress"
                       |      },
                       |      "id": {
                       |         "value": "boxIds"
                       |      },
                       |      "options": [
                       |        "multi"
                       |      ]
                       |    }
                       |  ]
                       |}
                       |""".stripMargin

      getResult.inputBoxIds shouldBe Seq(boxId3, boxId2)
    }

    "Assign correct names when when filtered by address and multiple ids with 'multi' option" in new Mocks {
      lazy val code: String = s"""{
                       |  "constants":[
                       |    {
                       |      "name": "myAddress",
                       |      "value": "$myAddress",
                       |      "type": "Address"
                       |    },
                       |    {
                       |      "name": "boxIds",
                       |      "values": [
                       |         "$boxId3",
                       |         "$boxId2",
                       |         "$tokenId1"
                       |      ],
                       |      "type": "CollByte"
                       |    }
                       |  ],
                       |  "inputs": [
                       |    {
                       |      "address": {
                       |        "name": "myNewAddress",
                       |        "value": "myAddress"
                       |      },
                       |      "id": {
                       |        "name": "matched",
                       |        "value": "boxIds"
                       |      },
                       |      "options": [
                       |        "multi"
                       |      ]
                       |    }
                       |  ],
                       |  "returns": [
                       |    "myNewAddress",
                       |    "matched",
                       |    "boxIds"
                       |  ]
                       |}
                       |""".stripMargin

      val result = getResult
      result.inputBoxIds shouldBe Seq(boxId3, boxId2)
      result.returned
        .flatMap(v => v.values.map(_.toString)) shouldBe Seq(
        s"ErgoTree of $myAddress",
        s"ErgoTree of $myAddress",
        boxId3,
        boxId2,
        boxId3,
        boxId2,
        tokenId1
      )
    }

    "Assign correct names when when filtered by address with 'multi' option" in new Mocks {
      lazy val code: String = s"""{
                       |  "constants":[
                       |    {
                       |      "name": "myAddress",
                       |      "value": "$myAddress",
                       |      "type": "Address"
                       |    },
                       |    {
                       |      "name": "boxIds",
                       |      "values": [
                       |         "$boxId3",
                       |         "$boxId2"
                       |      ],
                       |      "type": "CollByte"
                       |    }
                       |  ],
                       |  "inputs": [
                       |    {
                       |      "address": {
                       |        "name": "myNewAddress",
                       |        "value": "myAddress"
                       |      },
                       |      "id": {
                       |        "name": "matched"
                       |      },
                       |      "options": [
                       |        "multi"
                       |      ]
                       |    }
                       |  ],
                       |  "returns": [
                       |    "myNewAddress",
                       |    "matched"
                       |  ]
                       |}
                       |""".stripMargin

      val result = getResult
      result.inputBoxIds shouldBe Seq(boxId1, boxId2, boxId3)
      result.returned
        .flatMap(v => v.values.map(_.toString)) shouldBe Seq(
        s"ErgoTree of $myAddress",
        s"ErgoTree of $myAddress",
        s"ErgoTree of $myAddress",
        boxId1,
        boxId2,
        boxId3
      )

    }
  }
}
