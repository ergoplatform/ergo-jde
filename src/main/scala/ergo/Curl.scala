package ergo

import io.circe.Json

import java.io.InputStream
import java.net.HttpURLConnection
import scala.util.{Failure, Success, Try}

object Curl {
  import io.circe.parser._

  import java.net.URL
  import scala.io.Source
  val requestProperties = Map(
    "Accept" -> "application/json"
  )

  private def is2Str(is: InputStream) = {
    Try(Source.fromInputStream(is).getLines.mkString("\n")) match {
      case Success(s)         => s
      case Failure(exception) => exception.getMessage
    }
  }

  private def usingHttpConn(url: String)(f: HttpURLConnection => Unit): Either[Throwable, Json] =
    Try {
      val httpConn = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
      requestProperties.foreach { case (name, value) => httpConn.setRequestProperty(name, value) }
      f(httpConn)
      (httpConn.getResponseCode, httpConn)
    } match {
      case Success((200, httpConn))      => Try(parse(is2Str(httpConn.getInputStream)).right.get).toEither
      case Success((httpCode, httpConn)) => Left(new RuntimeException(s"http:$httpCode,error:${is2Str(httpConn.getErrorStream)}"))
      case Failure(ex)                   => Left(ex)
    }

  def get(url: String): Either[Throwable, Json] =
    usingHttpConn(url) { httpConn => () }
//
//  private def authHeader =
//    Array(
//      ("accept", "application/json"),
//      ("api_key", apiKey),
//      ("Content-Type", "application/json")
//    )
//
  def post(url: String, body: String, additionalHeaders: Map[String, String] = Map()): Either[Throwable, Json] =
    usingHttpConn(url) { httpConn =>
      httpConn.setRequestMethod("POST")
      additionalHeaders.foreach { case (key, value) => httpConn.setRequestProperty(key, value) }
      httpConn.setRequestProperty("Content-Type", "application/json")
      httpConn.setDoOutput(true)
      val os = httpConn.getOutputStream
      val input = body.getBytes("utf-8")
      os.write(input, 0, input.length)
    }
}
