package com.themillhousegroup.sses

import play.api.test._
import play.api.mvc._
import sun.misc.BASE64Encoder

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

class BasicAuthProtectedSpec extends PlaySpecification {

  val timeout = Duration(3, "seconds")

  val testUsername = "username"
  val testPassword = "pass"

  def verifyGetsBlocked(endpoint: Action[AnyContent], request: Request[AnyContent]) = {
    val fResult: Future[Result] = endpoint.apply(request)
    val result: Result = Await.result(fResult, timeout)
    result must not beNull

    result.header.status must beEqualTo(401)
    result.header.headers.get("WWW-Authenticate") must beSome
  }

  def verifyWithNoAuthInRequest(endpoint: Action[AnyContent]) = {
    val req = FakeRequest()
    verifyGetsBlocked(endpoint, req)
  }

  def verifyWithBadAuthInRequest(endpoint: Action[AnyContent], auth: String) = {
    val req = FakeRequest().withHeaders(
      "authorization" -> auth
    )
    verifyGetsBlocked(endpoint, req)
  }

  def verifyGetsAllowed(endpoint: Action[AnyContent], auth: String) = {
    val req = FakeRequest().withHeaders(
      "authorization" -> auth
    )
    val fResult: Future[Result] = endpoint.apply(req)
    val result: Result = Await.result(fResult, timeout)
    result must not beNull

    result.header.status must beEqualTo(200)
    result.header.headers.get("WWW-Authenticate") must beNone
  }

  def verifyEndpoint(endpoint: Action[AnyContent]) = {
    endpoint must not beNull

    verifyWithNoAuthInRequest(endpoint)
    verifyWithBadAuthInRequest(endpoint, "")
    verifyWithBadAuthInRequest(endpoint, "skdsdfsdfjf")
    verifyWithBadAuthInRequest(endpoint, "basic jkhsdfkjhsd")

    val encoder = new BASE64Encoder()
    val zeroParts = encoder.encode("nope".getBytes)
    verifyWithBadAuthInRequest(endpoint, s"basic $zeroParts")

    val onePart = encoder.encode("nope:".getBytes)
    verifyWithBadAuthInRequest(endpoint, s"basic $onePart")

    val twoPartsBad = encoder.encode("nope:badpass".getBytes)
    verifyWithBadAuthInRequest(endpoint, s"basic $twoPartsBad")

    val twoPartsGood = encoder.encode(s"$testUsername:$testPassword".getBytes)
    verifyGetsAllowed(endpoint, s"basic $twoPartsGood")
  }

  "Basic Auth Protection (password-only protection)" should {

    "allow sync endpoint, no request" in {
      val c = new Controller {
        val endpoint = BasicAuthProtected(testPassword) {
          Ok("ok")
        }
      }

      verifyEndpoint(c.endpoint)
    }

    "allow sync endpoint, request" in {
      val c = new Controller {
        val endpoint = BasicAuthProtected(testPassword) { request =>
          Ok("ok")
        }
      }

      verifyEndpoint(c.endpoint)
    }

    "allow async endpoint, no request" in {
      val c = new Controller {
        val endpoint = BasicAuthProtected(testPassword).async {
          Future.successful(Ok("ok"))
        }
      }

      verifyEndpoint(c.endpoint)
    }

    "allow async endpoint, request" in {
      val c = new Controller {
        val endpoint = BasicAuthProtected(testPassword).async { request =>
          Future.successful(Ok("ok"))
        }
      }

      verifyEndpoint(c.endpoint)
    }
  }
}
