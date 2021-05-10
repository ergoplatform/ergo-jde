package ergo

import io.circe.Json

case class ErgoNode(baseUrl: String, password: Option[String] = None) {
  def getInfo: Either[Throwable, Json] = Curl.get(s"$baseUrl/info")

  def blake2b(string: String): Either[Throwable, Json] = Curl.post(s"$baseUrl/utils/hash/blake2b", s""""$string"""")

  def getBoxRaw(boxId: String): Either[Throwable, String] = {
    for {
      json <- Curl.get(s"$baseUrl/utxo/byIdBinary/$boxId")
    } yield (json \\ "bytes").map(v => v.asString.get).apply(0)
  }
  private def authEndpoint(endpoint: String, rawData: String): Either[Throwable, Json] = {
    password
      .map(value => Curl.post(s"$baseUrl/$endpoint", rawData, Map("api_key" -> value)))
      .getOrElse(Left(new Exception("Password must be defined for authenticated method")))
  }
  def generate(request: String): Either[Throwable, Json] = authEndpoint("wallet/transaction/generate", request)
  def send(request: String): Either[Throwable, Json] = authEndpoint("wallet/transaction/send", request)
}
