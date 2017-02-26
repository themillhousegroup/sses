package com.themillhousegroup.sses

import play.api.mvc._
import sun.misc.BASE64Decoder

import scala.concurrent.Future

class BasicAuthProtected[R <: Request[_]]( credentialMatcher: (String, String) => Boolean) extends ActionBuilder[Request] with ActionFilter[Request] {

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
        if (credentialMatcher(user, pass)) {
          Future.successful[Option[Result]](None)
        } else {
          challenge
        }
      }
    }
  }
}

object UsernameProtected {
  def apply[A, R[A] <: Request[A]](requiredUsername: String):ActionBuilder[Request] = {

    def matcher(username:String, password:String) = username == requiredUsername

    new BasicAuthProtected[R[A]](matcher)
  }
}

object PasswordProtected {
  def apply[A, R[A] <: Request[A]](requiredPassword: String):ActionBuilder[Request] = {

    def matcher(username:String, password:String) = password == requiredPassword

    new BasicAuthProtected[R[A]](matcher)
  }
}

object UsernamePasswordProtected {
  def apply[A, R[A] <: Request[A]](requiredUsername: String, requiredPassword: String):ActionBuilder[Request] = {

    def matcher(username:String, password:String) = {
      username == requiredUsername &&
      password == requiredPassword
    }

    new BasicAuthProtected[R[A]](matcher)
  }
}
