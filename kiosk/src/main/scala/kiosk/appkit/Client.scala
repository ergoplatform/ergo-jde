package kiosk.appkit

import org.ergoplatform.appkit.{BlockchainContext, ErgoClient, NetworkType, RestApiErgoClient}

object Client {
  lazy val clients: Seq[Client] = Nodes.urls.map(url => new Client(s"http://$url"))

  def usingContext[T](f: BlockchainContext => T): T = {
    clients.head.usingContext(f)
  }
}

class Client(url: String) {
  private val restApiErgoClient: ErgoClient =
    RestApiErgoClient.create(url, NetworkType.MAINNET, "no-api-key", RestApiErgoClient.defaultMainnetExplorerUrl)

  private def usingContext[T](f: BlockchainContext => T): T = {
    restApiErgoClient.execute { ctx =>
      f(ctx)
    }
  }
}
