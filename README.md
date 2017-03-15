sses
============================

This is a very simple implementation of Basic Authentication so that you can put
a username and/or password challenge on a Play Framework endpoint with one line of code.

If you're looking for a dead-simple way to protect an endpoint or HTML page from intruders (or possibly just making sure Google can't index your page), and don't mind having the credentials stored in your code, this (together with using an HTTPS connection) is probably "good enough". Use is at your own risk, of course.


### Installation

Bring in the library by adding the following to your ```build.sbt```. 

  - The release repository: 

```
   resolvers ++= Seq(
     "Millhouse Bintray"  at "http://dl.bintray.com/themillhousegroup/maven"
   )
```
  - The dependency itself: 

```
   libraryDependencies ++= Seq(
     "com.themillhousegroup" %% "sses" % "1.0.10"
   )

```

### Usage

Once you have __sses__ added to your project, you can start using it like this:


Take your existing unprotected endpoint(s), that might look like this:

```
def showFoo(fooId: String) = Action {

  // Some code, returning a Result
  ...
}

def showBar(barId: String) = Action.async {

  // Some asynchronous code, returning a Future[Result]
  ...
}

def storeBaz = Action.async(parse.json) { request =>

  // Some asynchronous code that parses the request body into JSON, 
  // returning a Future[Result]
  ...
}
```

Add the import, and your desired protection (username-only, password-only or both). You can still use all of the nice `Action` features like `.async` and `(parse.json)` etc:

```
import com.themillhousegroup.sses._


def showFoo(fooId: String) = UsernameProtected("dave") {

  // Some code, returning a Result
  ...
}

def showBar(barId: String) = PasswordProtected("s3cr3t").async {

  // Some asynchronous code, returning a Future[Result]
  ...
}

def storeBaz = UsernamePasswordProtected("dave", "s3cr3t").async(parse.json) { request =>

  // Some asynchronous code that parses the request body into JSON, 
  // returning a Future[Result]
  ...
}
```




### Credits

The original Basic-Auth code was taken from [http://www.mentful.com/2014/06/14/basic-authentication-filter-for-play-framework/](this blog post)
where the functionality was implemented as a Play Global Filter. 
