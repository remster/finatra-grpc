
package com.airmap.examples

import java.io.InputStream

import com.google.protobuf.GeneratedMessageV3
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import com.twitter.finatra.http.marshalling.MessageBodyReader
import com.twitter.finatra.http.routing.HttpRouter

object ServerMain extends Server

class HelloRequestReader extends MessageBodyReader[GeneratedMessageV3] {

  override def parse[M <: GeneratedMessageV3](request: Request)(implicit man: Manifest[M]): M = {
    Class.forName(man.runtimeClass.getName)
      .getDeclaredMethod("parseFrom", classOf[InputStream])
      .invoke(null, request.getInputStream())
      .asInstanceOf[M]
  }
}

class Server extends HttpServer {

  override def configureHttp(router: HttpRouter) {

    injector.instance[MessageBodyManager].add(new HelloRequestReader)

    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[HelloController]
  }
}