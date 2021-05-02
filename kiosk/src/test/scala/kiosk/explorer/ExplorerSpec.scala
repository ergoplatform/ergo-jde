package kiosk.explorer

import io.circe.parser
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.PropSpec

class ExplorerSpec extends PropSpec {
  property("Json Test") {
    def parse(string: String) = parser.parse(string).getOrElse(???)
    val explorer = new Explorer
    explorer.getBoxByIdJson("17da0c63a00008ce3659d074a9cf3f2f473045fabdc2b8918114807de9ca5831") shouldBe
      parse(
        """{
          |  "id" : "17da0c63a00008ce3659d074a9cf3f2f473045fabdc2b8918114807de9ca5831",
          |  "txId" : "0ddd6eaaecff8dc297c101c8995561282c260a1dd2d428e743285c34afe37740",
          |  "value" : 2000000,
          |  "index" : 1,
          |  "creationHeight" : 352600,
          |  "ergoTree" : "100c04000402040204000580dac40904000e20dd26438230986cfe7305ad958451b69e55ad5ac37c8a355bfb08d810edd7a20f05000400050a04040e207bd873b8a886daa7a8bfacdad11d36aeee36c248aaf5779bcd8d41a13e4c1604d80bd601c5a7d602b2a4730000d603b2a4730100d604db63087203d605b2a5730200d606c27205d607e4c67202040ed608b2a5730300d609e4c67208040ed60ac27202d60bc27208d1ed937201c57202ecedededed93720b720a92c17208730493db63087208db63087202947209720792b0b5db6501fed9010c63eded938cb2db6308720c73050001730693e4c6720c040e720993e4c6720c050e72017307d9010c41639a8c720c018cb2db63088c720c02730800027309ededededededed938cb27204730a0001730b937204db6308720593e4c6a7040ecb720694c27203720693e4c672030405e4c67205040593e4c672030505e4c67205050593c17203c17205ededed937207720993720a720b93db63087202db6308720893c17202c17208",
          |  "address" : "6Vs43fLottAzin3EiEiswbSD31ETscqBLy9i3zTWCwUVuG79fWuP7S3Kko5PEK56UEBWSTE8GuuXq3ZYzWKCGmzPQ9y5AU4hvwGcTsYPH74qLsm3kmXctHLRnEVAxvsviB5aTRJo41adKHZ4EJSdWJpNUuJM4BJXGpM7BJnjT9cNB63QMtMUrfqaq8Ku8aJ7jM1VtXZFQiNH1pzwNFRQzU4fD4Dg8VqTtaAVWw98zKgGZmm35pq8QbAb5je796CWnQQRzDuGdPxwfBzzVCPjCf5hpFev56odduByacWPGYnd671A7CLrF7iSqae2ZHs4YmeXvAbBUW6s3A7U8YZAdPvaugBjNEkP9eVYZjDR4ppWcGukcrnjWBSqcv9nrBMudcLQLZwS653mVCbD8rqki2u2DjR4PMMtSQLAnH4HkeZFnz3w8Nav7YXkgviAFYR5AEdse8sUUKmb34AhU2uCxCJw6thqRfcMsWwaRHetzkhp6YoAy66EXmpVgM7pDo9RxpE9aRwPbxY6BwZ8",
          |  "assets" : [
          |    {
          |      "tokenId" : "77d14a018507949d1a88a631f76663e8e5101f57305dd5ebd319a41028d80456",
          |      "index" : 0,
          |      "amount" : 1,
          |      "name" : null,
          |      "decimals" : null,
          |      "type" : null
          |    }
          |  ],
          |  "additionalRegisters" : {
          |    "R4" : "0e0101"
          |  },
          |  "spentTransactionId" : "0e433a7fd9a4b31ae6c6d250831302c7fb1d59d1a10496ba75d94120811572d5",
          |  "mainChain" : true
          |}
          |""".stripMargin
      )
    explorer.getBoxByIdJson("9e289deab858c3f14c7056e568cfa1026c01242d151a94b165268b650e9ac966") shouldBe
      parse(
        """{
          |  "id" : "9e289deab858c3f14c7056e568cfa1026c01242d151a94b165268b650e9ac966",
          |  "txId" : "d2673edb1f606d4143d163ceecd05cb3a6a1b2eedc83f7d610c17afea24f4377",
          |  "value" : 104000000,
          |  "index" : 0,
          |  "creationHeight" : 294841,
          |  "ergoTree" : "100d040004000e2012caaacb51c89646fac9a3786eb98d0113bd57d68223ccc11754a4f67281daed0400050004140402048092f401040201010402058092f4010400d803d601b2a5730000d602b5db6501fed9010263ed93e4c67202050ec5a7938cb2db63087202730100017302d603b17202ea02d1edededededed93cbc27201e4c6a7060e917203730393db63087201db6308a793e4c6720104059db072027304d9010441639a8c720401e4c68c72040206057e72030593e4c6720105049ae4c6a70504730592c1720199c1a77e9c9a720373067307058cb07202860273087309d901043c400163d802d6068c720401d6078c72060186029a7207730aeded8c72060293c2b2a5720700d0cde4c68c720402040792c1b2a5720700730b02b2ad7202d9010463cde4c672040407730c00",
          |  "address" : "3vThpSDoLo58CtKKFLBQMmtcD5e5pJeFNNyPKnDRC4zKzhgySeTUkU71fk9mcFgHe23k1b4QuERNdcignnexcULMEenifBffiNeCdiTkgaUiGtH5D9rrsj698mRLDhANmybx8c6NunwUMoKuLsRoEYtYi8rRjuKfbNDN1HfVsgFKSyKMSnwJXa5KAuABSz5dYUgURf6M3i2bxsKKYTe4uQFEoVcbBwvfW4UxXaKqQYGB8xGLASMfHtcs9R5CBFkHyUSXh2sFy17pfdQ5emx8CgE5ZXRqx7YBYzk9jSyGqp2myT5XvBAS2uSeahNKWYKzh1XTqDc3YGLvBPHJ98bksaaSnNX4SwAhia2mXY4iCKsYf6F7p5QPNjYBXqLyzkDFxSzgQJmMg1Ybh3fx6Sg8esE9w5L7KCGEuydPkBE",
          |  "assets" : [
          |    {
          |      "tokenId" : "b662db51cf2dc39f110a021c2a31c74f0a1a18ffffbf73e8a051a7b8c0f09ebc",
          |      "index" : 0,
          |      "amount" : 1,
          |      "name" : null,
          |      "decimals" : null,
          |      "type" : null
          |    }
          |  ],
          |  "additionalRegisters" : {
          |    "R4" : "0590ee88d502",
          |    "R5" : "048eff23",
          |    "R6" : "0e205ea046c8753cbf8bb0acdbd67dd8a5d905df89d67060624282ad757fa3cb670c"
          |  },
          |  "spentTransactionId" : null,
          |  "mainChain" : true
          |}
          |""".stripMargin
      )
  }
}
