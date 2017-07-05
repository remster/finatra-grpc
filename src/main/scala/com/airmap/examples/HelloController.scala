package com.airmap.examples

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service, http}
import com.twitter.finatra.http.Controller
import com.twitter.io.Buf.ByteArray

case class Greetings(name: String)

class HelloController extends Controller {

  final val ProtobufMime = "application/protobuf"

  val translate: Service[Request, Response] = Http.client
    .withLabel("clientname")
    .withSessionPool.maxSize(1)
    .newService("localhost:8888")

  get("/hi") { request: Request =>
    info("hi")
    "Hello " + request.params.getOrElse("name", "unnamed")
  }

  post("/hiJSON") { hiRequest: Greetings =>
    translate(
      http.RequestBuilder()
        .setHeader("Content-Type", ProtobufMime)
        .url("http://doesntmatterwhatshere/hiProtoBuf")
        .buildPost(
          ByteArray.Owned(
            HelloRequest.newBuilder().setName(hiRequest.name).build().toByteArray
          )
        )
    )
  }

  post("/hiProtoBuf") { hiRequest: HelloRequest =>
    "Hello " + hiRequest.getName
  }
}
