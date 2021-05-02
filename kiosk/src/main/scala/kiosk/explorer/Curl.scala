package kiosk.explorer

import io.circe.Json

import java.io.InputStream
import java.net.HttpURLConnection
import scala.util.{Failure, Success, Try}

object Curl {
  import java.net.URL

  import io.circe.parser._

  import scala.io.Source
  val requestProperties = Map(
    "User-Agent" -> "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.1) Gecko/20061204 Firefox/2.0.0.1",
    "Accept" -> "application/json"
  )

  private def is2Str(is: InputStream) = {
    Try(Source.fromInputStream(is).getLines.mkString("\n")) match {
      case Success(s)         => s
      case Failure(exception) => exception.getMessage
    }
  }

  def getOrError(url: String): Either[Throwable, Option[Json]] = {
    Try {
      val connection = new URL(url).openConnection
      requestProperties.foreach { case (name, value) => connection.setRequestProperty(name, value) }
      val httpConn = connection.asInstanceOf[HttpURLConnection]
      (httpConn.getResponseCode, httpConn)
    } match {
      case Success((200, httpConn))      => Try(Some(parse(is2Str(httpConn.getInputStream)).right.get)).toEither
      case Success((404, _))             => Right(None) // not found; we want to consider this as a "good" case (implies box has 0 confirmation or does not exist)
      case Success((httpCode, httpConn)) => Left(new Exception(s"http:$httpCode,error:${is2Str(httpConn.getErrorStream)}"))
      case Failure(ex)                   => Left(ex)
    }
  }

  def get(url: String): Json = {
    getOrError(url) match {
      case Right(Some(json)) => json
      case Right(None)       => throw new Exception(s"Explorer returned 404 for url $url")
      case Left(ex)          => throw new Exception(s"Explorer returned error $ex for url $url")
    }
  }

  def post(url: String, body: String): Either[Throwable, Option[Json]] = {
    Try {
      val connection = new URL(url).openConnection
      requestProperties.foreach { case (name, value) => connection.setRequestProperty(name, value) }
      val httpConn = connection.asInstanceOf[HttpURLConnection]
      httpConn.setRequestMethod("POST")
      httpConn.setRequestProperty("Content-Type", "application/json; utf-8")
      httpConn.setRequestProperty("Accept", "application/json")
      httpConn.setDoOutput(true)
      val os = httpConn.getOutputStream
      val input = body.getBytes("utf-8")
      os.write(input, 0, input.length)
      (httpConn.getResponseCode, httpConn)
    } match {
      case Success((200, httpConn))      => Try(Some(parse(is2Str(httpConn.getInputStream)).right.get)).toEither
      case Success((404, _))             => Right(None) // not found; we want to consider this as a "good" case (implies box has 0 confirmation or does not exist)
      case Success((httpCode, httpConn)) => Left(new Exception(s"http:$httpCode,error:${is2Str(httpConn.getErrorStream)}"))
      case Failure(ex)                   => Left(ex)
    }
  }

}
