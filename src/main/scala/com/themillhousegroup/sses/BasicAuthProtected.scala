package com.themillhousegroup.sses

import play.api.mvc._
import sun.misc.BASE64Decoder

import scala.concurrent.Future

class BasicAuthProtected[R <: Request[_]] (val requiredPassword:String) extends ActionBuilder[Request] with ActionFilter[Request] {

  private lazy val unauthResult = Results.Unauthorized.withHeaders(("WWW-Authenticate", "Basic realm=\"myRealm\""))
  private lazy val challenge = Future.successful(Some(unauthResult))

  //need the space at the end
  private lazy val basicPrefix = "basic "


  private def decodeBasicAuth(auth: String): Option[(String, String)] = {
    if ((auth.length < basicPrefix.length) || (!auth.startsWith(basicPrefix))) {
      None
    } else {
      extractEncodedAuthString(auth.replaceFirst(basicPrefix, ""))
    }
  }

  private def extractEncodedAuthString(basicAuthSt:String) = {
    //BASE64Decoder is not thread safe, don't make it a field of this object
    val decoder = new BASE64Decoder()
    val decodedAuthSt = new String(decoder.decodeBuffer(basicAuthSt), "UTF-8")
    val usernamePassword = decodedAuthSt.split(":")
    if (usernamePassword.length >= 2) {
      //account for ":" in passwords
      Some(usernamePassword(0), usernamePassword.splitAt(1)._2.mkString)
    } else {
      None
    }
  }

  protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    request.headers.get("authorization").fold[Future[Option[Result]]] {
      challenge
    } { basicAuth =>
      decodeBasicAuth(basicAuth).fold[Future[Option[Result]]] {
        challenge
      } { case (user, pass) =>
        if (pass == requiredPassword) {
          Future.successful[Option[Result]](None)
        } else {
          challenge
        }
      }
    }
  }
}

object BasicAuthProtected {
  def apply[A, R[A] <: Request[A]](password: String):ActionBuilder[Request] = {
    new BasicAuthProtected[R[A]](password)
  }
}
