package ergo

import jde.parser.Parser
import kiosk.ergo.KioskLong
import kiosk.explorer.Explorer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object GetErgUsdRate extends App {
  private val oracleRateScript =
    """{
      |  "constants": [
      |    {
      |      "name": "oraclePoolNFT",
      |      "type": "CollByte",
      |      "value": "011d3364de07e5a26f0c4eef0852cddb387039a921b7154ef3cab22c6eda887f"
      |    },
      |    {
      |      "name": "poolAddresses",
      |      "type": "Address",
      |      "values": [
      |        "NTkuk55NdwCXkF1e2nCABxq7bHjtinX3wH13zYPZ6qYT71dCoZBe1gZkh9FAr7GeHo2EpFoibzpNQmoi89atUjKRrhZEYrTapdtXrWU4kq319oY7BEWmtmRU9cMohX69XMuxJjJP5hRM8WQLfFnffbjshhEP3ck9CKVEkFRw1JDYkqVke2JVqoMED5yxLVkScbBUiJJLWq9BSbE1JJmmreNVskmWNxWE6V7ksKPxFMoqh1SVePh3UWAaBgGQRZ7TWf4dTBF5KMVHmRXzmQqEu2Fz2yeSLy23sM3pfqa78VuvoFHnTFXYFFxn3DNttxwq3EU3Zv25SmgrWjLKiZjFcEcqGgH6DJ9FZ1DfucVtTXwyDJutY3ksUBaEStRxoUQyRu4EhDobixL3PUWRcxaRJ8JKA9b64ALErGepRHkAoVmS8DaE6VbroskyMuhkTo7LbrzhTyJbqKurEzoEfhYxus7bMpLTePgKcktgRRyB7MjVxjSpxWzZedvzbjzZaHLZLkWZESk1WtdM25My33wtVLNXiTvficEUbjA23sNd24pv1YQ72nY1aqUHa2",
      |        "EfS5abyDe4vKFrJ48K5HnwTqa1ksn238bWFPe84bzVvCGvK1h2B7sgWLETtQuWwzVdBaoRZ1HcyzddrxLcsoM5YEy4UnqcLqMU1MDca1kLw9xbazAM6Awo9y6UVWTkQcS97mYkhkmx2Tewg3JntMgzfLWz5mACiEJEv7potayvk6awmLWS36sJMfXWgnEfNiqTyXNiPzt466cgot3GLcEsYXxKzLXyJ9EfvXpjzC2abTMzVSf1e17BHre4zZvDoAeTqr4igV3ubv2PtJjntvF2ibrDLmwwAyANEhw1yt8C8fCidkf3MAoPE6T53hX3Eb2mp3Xofmtrn4qVgmhNonnV8ekWZWvBTxYiNP8Vu5nc6RMDBv7P1c5rRc3tnDMRh2dUcDD7USyoB9YcvioMfAZGMNfLjWqgYu9Ygw2FokGBPThyWrKQ5nkLJvief1eQJg4wZXKdXWAR7VxwNftdZjPCHcmwn6ByRHZo9kb4Emv3rjfZE"
      |      ]
      |    }
      |  ],
      |  "auxInputs": [
      |    {
      |      "address": {
      |        "value": "poolAddresses"
      |      },
      |      "tokens": [
      |        { 
      |          "index": 0,
      |          "id": {
      |             "value": "oraclePoolNFT" 
      |          }
      |        }
      |      ],
      |      "registers": [
      |        {
      |          "num": "R4",
      |          "name": "rateUsd",
      |          "type": "Long"
      |        }
      |      ]
      |    }
      |  ],
      |  "returns": [
      |    "rateUsd"
      |  ]
      |}
      |""".stripMargin

  private val program = Parser.parse(oracleRateScript)

  private val explorer = new Explorer
  private val compiler = new jde.compiler.Compiler(explorer)

  def nanoErgsPerUsdCent =
    Future {
      compiler.compile(program).returned.find(_.name == "rateUsd").map(_.values.head.asInstanceOf[KioskLong].value)
    }

  println("Attempting to obtain Erg-USD oracle box")
  println("NanoErgs per USD: " + Await.result(nanoErgsPerUsdCent, 5 seconds).getOrElse(0))
}
