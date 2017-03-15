package com.themillhousegroup.sses

import org.specs2.execute.ResultLike
import org.specs2.matcher.MatchResult
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

  def verifyGetsAllowedUpperCaseAuthorizationHeader(endpoint: Action[AnyContent], auth: String) = {
    val req = FakeRequest().withHeaders(
      "Authorization" -> auth
    )
    val fResult: Future[Result] = endpoint.apply(req)
    val result: Result = Await.result(fResult, timeout)
    result must not beNull

    result.header.status must beEqualTo(200)
    result.header.headers.get("WWW-Authenticate") must beNone
  }

  def verifyEndpoint(endpoint: Action[AnyContent],
    extraChecks: (Action[AnyContent], BASE64Encoder) => MatchResult[_]) = {
    endpoint must not beNull

    verifyWithNoAuthInRequest(endpoint)
    verifyWithBadAuthInRequest(endpoint, "")
    verifyWithBadAuthInRequest(endpoint, "skdsdfsdfjf")
    verifyWithBadAuthInRequest(endpoint, "basic jkhsdfkjhsd")

    val encoder = new BASE64Encoder()
    val zeroParts = encoder.encode("nope".getBytes)
    verifyWithBadAuthInRequest(endpoint, s"basic $zeroParts")

    val userPartBad = encoder.encode("baduser:".getBytes)
    verifyWithBadAuthInRequest(endpoint, s"basic $userPartBad")

    val passPartBad = encoder.encode(":badpass".getBytes)
    verifyWithBadAuthInRequest(endpoint, s"basic $passPartBad")

    val twoPartsBad = encoder.encode("baduser:badpass".getBytes)
    verifyWithBadAuthInRequest(endpoint, s"basic $twoPartsBad")

    extraChecks(endpoint, encoder)
  }

  "Basic Auth Protection (password-only protection)" should {

    def passwordPositiveChecks(endpoint: Action[AnyContent], encoder: BASE64Encoder) = {
      val onePartGood = encoder.encode(s":$testPassword".getBytes)
      verifyGetsAllowed(endpoint, s"basic $onePartGood")
      verifyGetsAllowedUpperCaseAuthorizationHeader(endpoint, s"basic $onePartGood")
      verifyGetsAllowed(endpoint, s"Basic $onePartGood")
      verifyGetsAllowedUpperCaseAuthorizationHeader(endpoint, s"Basic $onePartGood")
    }

    "allow sync endpoint, no request" in {
      val c = new Controller {
        val endpoint = PasswordProtected(testPassword) {
          Ok("ok")
        }
      }

      verifyEndpoint(c.endpoint, passwordPositiveChecks)
    }

    "allow sync endpoint, request" in {
      val c = new Controller {
        val endpoint = PasswordProtected(testPassword) { request =>
          Ok("ok")
        }
      }

      verifyEndpoint(c.endpoint, passwordPositiveChecks)
    }

    "allow async endpoint, no request" in {
      val c = new Controller {
        val endpoint = PasswordProtected(testPassword).async {
          Future.successful(Ok("ok"))
        }
      }

      verifyEndpoint(c.endpoint, passwordPositiveChecks)
    }

    "allow async endpoint, request" in {
      val c = new Controller {
        val endpoint = PasswordProtected(testPassword).async { request =>
          Future.successful(Ok("ok"))
        }
      }

      verifyEndpoint(c.endpoint, passwordPositiveChecks)
    }
  }

  "Basic Auth Protection (username-only protection)" should {

    def usernamePositiveChecks(endpoint: Action[AnyContent], encoder: BASE64Encoder) = {
      val onePartGood = encoder.encode(s"$testUsername:".getBytes)
      verifyGetsAllowed(endpoint, s"basic $onePartGood")
      verifyGetsAllowedUpperCaseAuthorizationHeader(endpoint, s"basic $onePartGood")
      verifyGetsAllowed(endpoint, s"Basic $onePartGood")
      verifyGetsAllowedUpperCaseAuthorizationHeader(endpoint, s"Basic $onePartGood")
    }

    "allow sync endpoint, no request" in {
      val c = new Controller {
        val endpoint = UsernameProtected(testUsername) {
          Ok("ok")
        }
      }

      verifyEndpoint(c.endpoint, usernamePositiveChecks)

    }

    "allow sync endpoint, request" in {
      val c = new Controller {
        val endpoint = UsernameProtected(testUsername) { request =>
          Ok("ok")
        }
      }

      verifyEndpoint(c.endpoint, usernamePositiveChecks)
    }

    "allow async endpoint, no request" in {
      val c = new Controller {
        val endpoint = UsernameProtected(testUsername).async {
          Future.successful(Ok("ok"))
        }
      }

      verifyEndpoint(c.endpoint, usernamePositiveChecks)
    }

    "allow async endpoint, request" in {
      val c = new Controller {
        val endpoint = UsernameProtected(testUsername).async { request =>
          Future.successful(Ok("ok"))
        }
      }

      verifyEndpoint(c.endpoint, usernamePositiveChecks)
    }
  }

  "Basic Auth Protection (username-and-password protection)" should {

    def usernamePasswordPositiveChecks(endpoint: Action[AnyContent], encoder: BASE64Encoder) = {
      val twoPartsGood = encoder.encode(s"$testUsername:$testPassword".getBytes)
      verifyGetsAllowed(endpoint, s"basic $twoPartsGood")
      verifyGetsAllowedUpperCaseAuthorizationHeader(endpoint, s"basic $twoPartsGood")

      verifyGetsAllowed(endpoint, s"Basic $twoPartsGood")
      verifyGetsAllowedUpperCaseAuthorizationHeader(endpoint, s"Basic $twoPartsGood")
    }
    "allow sync endpoint, no request" in {
      val c = new Controller {
        val endpoint = UsernamePasswordProtected(testUsername, testPassword) {
          Ok("ok")
        }
      }

      verifyEndpoint(c.endpoint, usernamePasswordPositiveChecks)
    }

    "allow sync endpoint, request" in {
      val c = new Controller {
        val endpoint = UsernamePasswordProtected(testUsername, testPassword) { request =>
          Ok("ok")
        }
      }

      verifyEndpoint(c.endpoint, usernamePasswordPositiveChecks)
    }

    "allow async endpoint, no request" in {
      val c = new Controller {
        val endpoint = UsernamePasswordProtected(testUsername, testPassword).async {
          Future.successful(Ok("ok"))
        }
      }

      verifyEndpoint(c.endpoint, usernamePasswordPositiveChecks)
    }

    "allow async endpoint, request" in {
      val c = new Controller {
        val endpoint = UsernamePasswordProtected(testUsername, testPassword).async { request =>
          Future.successful(Ok("ok"))
        }
      }

      verifyEndpoint(c.endpoint, usernamePasswordPositiveChecks)
    }
  }
}
