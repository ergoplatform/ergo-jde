package ergo

import jde.compiler.model.CompileResult
import kiosk.ergo.{KioskBox, KioskCollByte, KioskInt, KioskLong, StringToBetterString}
import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json

class WalletUtilSpec extends WordSpec with MockitoSugar with Matchers {

  val ergoNode = mock[ErgoNode]

  val walletUtil = new WalletUtil(ergoNode)

  trait Mocks {
    val timeStamp = CompileResult(
      dataInputBoxIds = Seq("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7"),
      inputBoxIds = Seq("4c17e0e9f72122164aa3530453675625dc69941ed3da9de6b0a8659db929709a"),
      inputNanoErgs = 1500000,
      inputTokens = Seq("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea" -> 995),
      outputs = Seq(
        KioskBox(
          address =
            "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi",
          value = 1500000,
          registers = Array(),
          tokens = Array("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea" -> 994)
        ),
        KioskBox(
          address = "4MQyMKvMbnCJG3aJ",
          value = 2000000,
          registers = Array(KioskCollByte("506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7".decodeHex), KioskInt(483600)),
          tokens = Array("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea" -> 1)
        )
      ),
      outputNanoErgs = 3500000,
      outputTokens = Seq("dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea" -> 995),
      fee = None,
      returned = Nil
    )

    val mintStableCoin = CompileResult(
      dataInputBoxIds = Seq("948bbf2edfaa81d36f4c8fd9d5b52e7baf54fdc519b240dea934287e3dbb2b59"),
      inputBoxIds =
        Seq("1b59193c7f59513503d38204cd5dbbb3aea06d029d7dbbf51a1f02c2ec288969", "8e1e05b487ef95dab29a334775457ca59f86f8d39bb04a2608689be350581065"),
      inputNanoErgs = 1344037133024821L,
      inputTokens = Seq(
        "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0" -> 9999302263377L,
        "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04" -> 9999838480540L,
        "7d672d1def471720ca5782fd6473e47e796d9ac0c138d9911346f118b2f6d9d9" -> 1
      ),
      outputs = Seq(
        KioskBox(
          address =
            "MUbV38YgqHy7XbsoXWF5z7EZm524Ybdwe5p9WDrbhruZRtehkRPT92imXer2eTkjwPDfboa1pR3zb3deVKVq3H7Xt98qcTqLuSBSbHb7izzo5jphEpcnqyKJ2xhmpNPVvmtbdJNdvdopPrHHDBbAGGeW7XYTQwEeoRfosXzcDtiGgw97b2aqjTsNFmZk7khBEQywjYfmoDc9nUCJMZ3vbSspnYo3LarLe55mh2Np8MNJqUN9APA6XkhZCrTTDRZb1B4krgFY1sVMswg2ceqguZRvC9pqt3tUUxmSnB24N6dowfVJKhLXwHPbrkHViBv1AKAJTmEaQW2DN1fRmD9ypXxZk8GXmYtxTtrj3BiunQ4qzUCu1eGzxSREjpkFSi2ATLSSDqUwxtRz639sHM6Lav4axoJNPCHbY8pvuBKUxgnGRex8LEGM8DeEJwaJCaoy8dBw9Lz49nq5mSsXLeoC4xpTUmp47Bh7GAZtwkaNreCu74m9rcZ8Di4w1cmdsiK1NWuDh9pJ2Bv7u3EfcurHFVqCkT3P86JUbKnXeNxCypfrWsFuYNKYqmjsix82g9vWcGMmAcu5nagxD4iET86iE2tMMfZZ5vqZNvntQswJyQqv2Wc6MTh4jQx1q2qJZCQe4QdEK63meTGbZNNKMctHQbp3gRkZYNrBtxQyVtNLR8xEY8zGp85GeQKbb37vqLXxRpGiigAdMe3XZA4hhYPmAAU5hpSMYaRAjtvvMT3bNiHRACGrfjvSsEG9G2zY5in2YWz5X9zXQLGTYRsQ4uNFkYoQRCBdjNxGv6R58Xq74zCgt19TxYZ87gPWxkXpWwTaHogG1eps8WXt8QzwJ9rVx6Vu9a5GjtcGsQxHovWmYixgBU8X9fPNJ9UQhYyAWbjtRSuVBtDAmoV1gCBEPwnYVP5GCGhCocbwoYhZkZjFZy6ws4uxVLid3FxuvhWvQrVEDYp7WRvGXbNdCbcSXnbeTrPMey1WPaXX",
          value = 1344037054436314L,
          registers = Array(KioskLong(161519471), KioskLong(697736624)),
          tokens = Array(
            "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04" -> 9999838480530L,
            "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0" -> 9999302263377L,
            "7d672d1def471720ca5782fd6473e47e796d9ac0c138d9911346f118b2f6d9d9" -> 1
          )
        ),
        KioskBox(
          address = "9hz1B19M44TNpmVe8MS4xvXyycehh5uP5aCfj4a6iAowj88hkd2",
          value = 10000000,
          registers = Array(KioskLong(10), KioskLong(21411493)),
          tokens = Array("03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04" -> 10)
        ),
        KioskBox(
          address = "9hz1B19M44TNpmVe8MS4xvXyycehh5uP5aCfj4a6iAowj88hkd2",
          value = 66588507,
          registers = Array(),
          tokens = Array()
        ),
        KioskBox(
          address =
            "2iHkR7CWvD1R4j1yZg5bkeDRQavjAaVPeTDFGGLZduHyfWMuYpmhHocX8GJoaieTx78FntzJbCBVL6rf96ocJoZdmWBL2fci7NqWgAirppPQmZ7fN9V6z13Ay6brPriBKYqLp1bT2Fk4FkFLCfdPpe",
          value = 2000000,
          registers = Array(),
          tokens = Array()
        )
      ),
      outputNanoErgs = 1344037133024821L,
      outputTokens = Seq(
        "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0" -> 9999302263377L,
        "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04" -> 9999838480540L,
        "7d672d1def471720ca5782fd6473e47e796d9ac0c138d9911346f118b2f6d9d9" -> 1
      ),
      fee = Some(2000000),
      returned = Nil
    )
  }

  "CompileResult to Json" should {
    "encode timestamp correctly" in new Mocks {
      import jde.parser.Parser._
      Json.toJson(timeStamp) shouldBe Json.parse(
        """{
          |  "dataInputBoxIds" : [ "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7" ],
          |  "inputBoxIds" : [ "4c17e0e9f72122164aa3530453675625dc69941ed3da9de6b0a8659db929709a" ],
          |  "inputNanoErgs" : 1500000,
          |  "inputTokens" : [ [ "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 995 ] ],
          |  "outputs" : [ {
          |    "address" : "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi",
          |    "value" : 1500000,
          |    "registers" : [ ],
          |    "tokens" : [ [ "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 994 ] ]
          |  }, {
          |    "address" : "4MQyMKvMbnCJG3aJ",
          |    "value" : 2000000,
          |    "registers" : [ "0e20506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", "04a0843b" ],
          |    "tokens" : [ [ "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 1 ] ]
          |  } ],
          |  "outputNanoErgs" : 3500000,
          |  "outputTokens" : [ [ "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 995 ] ],
          |  "returned" : [ ]
          |}""".stripMargin
      )
    }

    "encode mintStableCoin correctly" in new Mocks {
      import jde.parser.Parser._
      Json.toJson(mintStableCoin) shouldBe Json.parse(
        """{
          |  "dataInputBoxIds" : [ "948bbf2edfaa81d36f4c8fd9d5b52e7baf54fdc519b240dea934287e3dbb2b59" ],
          |  "inputBoxIds" : [ "1b59193c7f59513503d38204cd5dbbb3aea06d029d7dbbf51a1f02c2ec288969", "8e1e05b487ef95dab29a334775457ca59f86f8d39bb04a2608689be350581065" ],
          |  "inputNanoErgs" : 1344037133024821,
          |  "inputTokens" : [ [ "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0", 9999302263377 ], [ "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", 9999838480540 ], [ "7d672d1def471720ca5782fd6473e47e796d9ac0c138d9911346f118b2f6d9d9", 1 ] ],
          |  "outputs" : [ {
          |    "address" : "MUbV38YgqHy7XbsoXWF5z7EZm524Ybdwe5p9WDrbhruZRtehkRPT92imXer2eTkjwPDfboa1pR3zb3deVKVq3H7Xt98qcTqLuSBSbHb7izzo5jphEpcnqyKJ2xhmpNPVvmtbdJNdvdopPrHHDBbAGGeW7XYTQwEeoRfosXzcDtiGgw97b2aqjTsNFmZk7khBEQywjYfmoDc9nUCJMZ3vbSspnYo3LarLe55mh2Np8MNJqUN9APA6XkhZCrTTDRZb1B4krgFY1sVMswg2ceqguZRvC9pqt3tUUxmSnB24N6dowfVJKhLXwHPbrkHViBv1AKAJTmEaQW2DN1fRmD9ypXxZk8GXmYtxTtrj3BiunQ4qzUCu1eGzxSREjpkFSi2ATLSSDqUwxtRz639sHM6Lav4axoJNPCHbY8pvuBKUxgnGRex8LEGM8DeEJwaJCaoy8dBw9Lz49nq5mSsXLeoC4xpTUmp47Bh7GAZtwkaNreCu74m9rcZ8Di4w1cmdsiK1NWuDh9pJ2Bv7u3EfcurHFVqCkT3P86JUbKnXeNxCypfrWsFuYNKYqmjsix82g9vWcGMmAcu5nagxD4iET86iE2tMMfZZ5vqZNvntQswJyQqv2Wc6MTh4jQx1q2qJZCQe4QdEK63meTGbZNNKMctHQbp3gRkZYNrBtxQyVtNLR8xEY8zGp85GeQKbb37vqLXxRpGiigAdMe3XZA4hhYPmAAU5hpSMYaRAjtvvMT3bNiHRACGrfjvSsEG9G2zY5in2YWz5X9zXQLGTYRsQ4uNFkYoQRCBdjNxGv6R58Xq74zCgt19TxYZ87gPWxkXpWwTaHogG1eps8WXt8QzwJ9rVx6Vu9a5GjtcGsQxHovWmYixgBU8X9fPNJ9UQhYyAWbjtRSuVBtDAmoV1gCBEPwnYVP5GCGhCocbwoYhZkZjFZy6ws4uxVLid3FxuvhWvQrVEDYp7WRvGXbNdCbcSXnbeTrPMey1WPaXX",
          |    "value" : 1344037054436314,
          |    "registers" : [ "05dedd849a01", "05e0f6b49905" ],
          |    "tokens" : [ [ "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", 9999838480530 ], [ "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0", 9999302263377 ], [ "7d672d1def471720ca5782fd6473e47e796d9ac0c138d9911346f118b2f6d9d9", 1 ] ]
          |  }, {
          |    "address" : "9hz1B19M44TNpmVe8MS4xvXyycehh5uP5aCfj4a6iAowj88hkd2",
          |    "value" : 10000000,
          |    "registers" : [ "0514", "05cadab514" ],
          |    "tokens" : [ [ "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", 10 ] ]
          |  }, {
          |    "address" : "9hz1B19M44TNpmVe8MS4xvXyycehh5uP5aCfj4a6iAowj88hkd2",
          |    "value" : 66588507,
          |    "registers" : [ ],
          |    "tokens" : [ ]
          |  }, {
          |    "address" : "2iHkR7CWvD1R4j1yZg5bkeDRQavjAaVPeTDFGGLZduHyfWMuYpmhHocX8GJoaieTx78FntzJbCBVL6rf96ocJoZdmWBL2fci7NqWgAirppPQmZ7fN9V6z13Ay6brPriBKYqLp1bT2Fk4FkFLCfdPpe",
          |    "value" : 2000000,
          |    "registers" : [ ],
          |    "tokens" : [ ]
          |  } ],
          |  "outputNanoErgs" : 1344037133024821,
          |  "outputTokens" : [ [ "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0", 9999302263377 ], [ "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", 9999838480540 ], [ "7d672d1def471720ca5782fd6473e47e796d9ac0c138d9911346f118b2f6d9d9", 1 ] ],
          |  "fee" : 2000000,
          |  "returned" : [ ]
          |}
          |""".stripMargin
      )
    }
  }

  "CompileResult to Request" should {
    "encode request correctly" in new Mocks {
      when(ergoNode.getBoxRaw("948bbf2edfaa81d36f4c8fd9d5b52e7baf54fdc519b240dea934287e3dbb2b59")).thenReturn(
        Right(
          "f0a8c1dc041014040004000e208c27dd9d8a35aac1e3167d58858c0a8b4059b277da790552e37eba22df9b903504000400040204020101040205a0c21e040204080500040c040204a0c21e0402050a05c8010402d806d601b2a5730000d602b5db6501fed9010263ed93e4c67202050ec5a7938cb2db63087202730100017302d603b17202d604e4c6b272027303000605d605d90105049590720573047204e4c6b272029972057305000605d606b07202860273067307d901063c400163d803d6088c720601d6098c720801d60a8c72060286029a72097308ededed8c72080293c2b2a5720900d0cde4c6720a040792c1b2a5720900730992da720501997209730ae4c6720a0605ea02d1ededededededed93cbc27201e4c6a7060e927203730b93db63087201db6308a793e4c6720104059db07202730cd9010741639a8c720701e4c68c72070206057e72030593e4c6720105049ae4c6a70504730d92c1720199c1a77e9c9a7203730e730f058c72060292da720501998c72060173109972049d9c720473117312b2ad7202d9010763cde4c672070407e4c6b2a5731300040400f5cc1d01011d3364de07e5a26f0c4eef0852cddb387039a921b7154ef3cab22c6eda887f010305a8cb98c80104fa993b0e20f7ef73c4a4ab91b84bb0a2905108d534114472ec057be3a57a9dfc9b1fbd85c11fdba26584adb538f23cbc76365c9b6b486d1dd4bab1d68d188bba34ecf87c5200"
        )
      )
      when(ergoNode.getBoxRaw("1b59193c7f59513503d38204cd5dbbb3aea06d029d7dbbf51a1f02c2ec288969")).thenReturn(
        Right(
          "b5baac8fd0ccb102102a0400040004000e20011d3364de07e5a26f0c4eef0852cddb387039a921b7154ef3cab22c6eda887f0400040204020400040004020500050005c8010500050005feffffffffffffffff0105000580897a05000580897a040405c80104c0933805c00c0580a8d6b907050005c8010580dac40905000500040404040500050005a0060101050005a0060100040004000e20239c170b7e82f94e6b05416f14b8a2a57e0bfff0e3c93f4abbcd160b6a5b271ad801d601db6501fed1ec9591b172017300d821d602b27201730100d603938cb2db63087202730200017303d604b2a5730400d605c17204d606db6308a7d607b27206730500d6088c720702d609db63087204d60ab27209730600d60b8c720a02d60c947208720bd60db27206730700d60e8c720d02d60fb27209730800d6108c720f02d61194720e7210d612e4c6a70505d613e4c672040505d614e4c6a70405d615e4c672040405d616b2a5730900d617e4c672160405d61895720c730a7217d61995720c7217730bd61ac1a7d61be4c672160505d61c9de4c672020405730cd61da2a1721a9c7214721c730dd61e9572119ca1721c95937214730e730f9d721d72147218d801d61e99721a721d9c9593721e7310731195937212731273139d721e72127219d61f9d9c721e7e7314057315d6209c7215721cd6219591a3731673177318d62295937220731972219d9c7205731a7220edededed7203ededededed927205731b93c27204c2a7edec720c7211efed720c7211ed939a720872129a720b7213939a720e72149a72107215edededed939a721472187215939a721272197213939a721a721b7205927215731c927213731deded938c720f018c720d01938c720a018c720701938cb27209731e00018cb27206731f000193721b9a721e958f721f7320f0721f721f957211959172187321927222732273239591721973249072227221927222732572037326938cb2db6308b2a4732700732800017329d8cc1d0303faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf049c91c8a684a302003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0d184f0a682a3027d672d1def471720ca5782fd6473e47e796d9ac0c138d9911346f118b2f6d9d9010205cadd849a0105e0f6b49905f59f21d2604be22a78fbca7d70ca5490a6a40718f4803099ce7a1a5202ca013200"
        )
      )
      when(ergoNode.getBoxRaw("8e1e05b487ef95dab29a334775457ca59f86f8d39bb04a2608689be350581065")).thenReturn(
        Right(
          "80c2d72f0008cd03c843a6cb59a2aebc8bc2c4a2f6fbd526ebb2a1d9f4a90954b5e2128f2d08981ddaff160000ede75a0b6a7f39ce2c44f7dcc90e2d136e940214ef0da98c9767f0fb65bd6ade00"
        )
      )
      val result = walletUtil.getRequest(mintStableCoin)
      Json.parse(result) shouldBe Json.parse(
        """
          |{
          |   "requests":[
          |      {
          |         "address":"MUbV38YgqHy7XbsoXWF5z7EZm524Ybdwe5p9WDrbhruZRtehkRPT92imXer2eTkjwPDfboa1pR3zb3deVKVq3H7Xt98qcTqLuSBSbHb7izzo5jphEpcnqyKJ2xhmpNPVvmtbdJNdvdopPrHHDBbAGGeW7XYTQwEeoRfosXzcDtiGgw97b2aqjTsNFmZk7khBEQywjYfmoDc9nUCJMZ3vbSspnYo3LarLe55mh2Np8MNJqUN9APA6XkhZCrTTDRZb1B4krgFY1sVMswg2ceqguZRvC9pqt3tUUxmSnB24N6dowfVJKhLXwHPbrkHViBv1AKAJTmEaQW2DN1fRmD9ypXxZk8GXmYtxTtrj3BiunQ4qzUCu1eGzxSREjpkFSi2ATLSSDqUwxtRz639sHM6Lav4axoJNPCHbY8pvuBKUxgnGRex8LEGM8DeEJwaJCaoy8dBw9Lz49nq5mSsXLeoC4xpTUmp47Bh7GAZtwkaNreCu74m9rcZ8Di4w1cmdsiK1NWuDh9pJ2Bv7u3EfcurHFVqCkT3P86JUbKnXeNxCypfrWsFuYNKYqmjsix82g9vWcGMmAcu5nagxD4iET86iE2tMMfZZ5vqZNvntQswJyQqv2Wc6MTh4jQx1q2qJZCQe4QdEK63meTGbZNNKMctHQbp3gRkZYNrBtxQyVtNLR8xEY8zGp85GeQKbb37vqLXxRpGiigAdMe3XZA4hhYPmAAU5hpSMYaRAjtvvMT3bNiHRACGrfjvSsEG9G2zY5in2YWz5X9zXQLGTYRsQ4uNFkYoQRCBdjNxGv6R58Xq74zCgt19TxYZ87gPWxkXpWwTaHogG1eps8WXt8QzwJ9rVx6Vu9a5GjtcGsQxHovWmYixgBU8X9fPNJ9UQhYyAWbjtRSuVBtDAmoV1gCBEPwnYVP5GCGhCocbwoYhZkZjFZy6ws4uxVLid3FxuvhWvQrVEDYp7WRvGXbNdCbcSXnbeTrPMey1WPaXX",
          |         "value":1344037054436314,
          |         "assets":[
          |            {
          |               "tokenId":"03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
          |               "amount":9999838480530
          |            },
          |            {
          |               "tokenId":"003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
          |               "amount":9999302263377
          |            },
          |            {
          |               "tokenId":"7d672d1def471720ca5782fd6473e47e796d9ac0c138d9911346f118b2f6d9d9",
          |               "amount":1
          |            }
          |         ],
          |         "registers":{
          |            "R4":"05dedd849a01",
          |            "R5":"05e0f6b49905"
          |         }
          |      },
          |      {
          |         "address":"9hz1B19M44TNpmVe8MS4xvXyycehh5uP5aCfj4a6iAowj88hkd2",
          |         "value":10000000,
          |         "assets":[
          |            {
          |               "tokenId":"03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
          |               "amount":10
          |            }
          |         ],
          |         "registers":{
          |            "R4":"0514",
          |            "R5":"05cadab514"
          |         }
          |      },
          |      {
          |         "address":"9hz1B19M44TNpmVe8MS4xvXyycehh5uP5aCfj4a6iAowj88hkd2",
          |         "value":66588507,
          |         "assets":[
          |            
          |         ],
          |         "registers":{
          |            
          |         }
          |      },
          |      {
          |         "address":"2iHkR7CWvD1R4j1yZg5bkeDRQavjAaVPeTDFGGLZduHyfWMuYpmhHocX8GJoaieTx78FntzJbCBVL6rf96ocJoZdmWBL2fci7NqWgAirppPQmZ7fN9V6z13Ay6brPriBKYqLp1bT2Fk4FkFLCfdPpe",
          |         "value":2000000,
          |         "assets":[
          |            
          |         ],
          |         "registers":{
          |            
          |         }
          |      }
          |   ],
          |   "inputsRaw":[
          |      "b5baac8fd0ccb102102a0400040004000e20011d3364de07e5a26f0c4eef0852cddb387039a921b7154ef3cab22c6eda887f0400040204020400040004020500050005c8010500050005feffffffffffffffff0105000580897a05000580897a040405c80104c0933805c00c0580a8d6b907050005c8010580dac40905000500040404040500050005a0060101050005a0060100040004000e20239c170b7e82f94e6b05416f14b8a2a57e0bfff0e3c93f4abbcd160b6a5b271ad801d601db6501fed1ec9591b172017300d821d602b27201730100d603938cb2db63087202730200017303d604b2a5730400d605c17204d606db6308a7d607b27206730500d6088c720702d609db63087204d60ab27209730600d60b8c720a02d60c947208720bd60db27206730700d60e8c720d02d60fb27209730800d6108c720f02d61194720e7210d612e4c6a70505d613e4c672040505d614e4c6a70405d615e4c672040405d616b2a5730900d617e4c672160405d61895720c730a7217d61995720c7217730bd61ac1a7d61be4c672160505d61c9de4c672020405730cd61da2a1721a9c7214721c730dd61e9572119ca1721c95937214730e730f9d721d72147218d801d61e99721a721d9c9593721e7310731195937212731273139d721e72127219d61f9d9c721e7e7314057315d6209c7215721cd6219591a3731673177318d62295937220731972219d9c7205731a7220edededed7203ededededed927205731b93c27204c2a7edec720c7211efed720c7211ed939a720872129a720b7213939a720e72149a72107215edededed939a721472187215939a721272197213939a721a721b7205927215731c927213731deded938c720f018c720d01938c720a018c720701938cb27209731e00018cb27206731f000193721b9a721e958f721f7320f0721f721f957211959172187321927222732273239591721973249072227221927222732572037326938cb2db6308b2a4732700732800017329d8cc1d0303faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf049c91c8a684a302003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0d184f0a682a3027d672d1def471720ca5782fd6473e47e796d9ac0c138d9911346f118b2f6d9d9010205cadd849a0105e0f6b49905f59f21d2604be22a78fbca7d70ca5490a6a40718f4803099ce7a1a5202ca013200",
          |      "80c2d72f0008cd03c843a6cb59a2aebc8bc2c4a2f6fbd526ebb2a1d9f4a90954b5e2128f2d08981ddaff160000ede75a0b6a7f39ce2c44f7dcc90e2d136e940214ef0da98c9767f0fb65bd6ade00"
          |   ],
          |   "dataInputsRaw":[
          |      "f0a8c1dc041014040004000e208c27dd9d8a35aac1e3167d58858c0a8b4059b277da790552e37eba22df9b903504000400040204020101040205a0c21e040204080500040c040204a0c21e0402050a05c8010402d806d601b2a5730000d602b5db6501fed9010263ed93e4c67202050ec5a7938cb2db63087202730100017302d603b17202d604e4c6b272027303000605d605d90105049590720573047204e4c6b272029972057305000605d606b07202860273067307d901063c400163d803d6088c720601d6098c720801d60a8c72060286029a72097308ededed8c72080293c2b2a5720900d0cde4c6720a040792c1b2a5720900730992da720501997209730ae4c6720a0605ea02d1ededededededed93cbc27201e4c6a7060e927203730b93db63087201db6308a793e4c6720104059db07202730cd9010741639a8c720701e4c68c72070206057e72030593e4c6720105049ae4c6a70504730d92c1720199c1a77e9c9a7203730e730f058c72060292da720501998c72060173109972049d9c720473117312b2ad7202d9010763cde4c672070407e4c6b2a5731300040400f5cc1d01011d3364de07e5a26f0c4eef0852cddb387039a921b7154ef3cab22c6eda887f010305a8cb98c80104fa993b0e20f7ef73c4a4ab91b84bb0a2905108d534114472ec057be3a57a9dfc9b1fbd85c11fdba26584adb538f23cbc76365c9b6b486d1dd4bab1d68d188bba34ecf87c5200"
          |   ]
          |}""".stripMargin
      )
    }
  }
}
