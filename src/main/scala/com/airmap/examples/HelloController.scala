package com.airmap.examples

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service, http}
import com.twitter.finatra.http.Controller
import com.twitter.io.Buf.ByteArray
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver

case class Greetings(name: String)

class HelloController extends Controller {

  final val ProtobufMime = "application/protobuf"
  private val greeterServer = new GrpcServer
  private val greeterClient = GreeterGrpc.newStub(
    ManagedChannelBuilder.forAddress("localhost", greeterServer.port).usePlaintext(true).build
  )

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
    ).map(_ => {
      greeterClient.streamHello(
        HelloRequest.newBuilder().setName("dupa").build,
        new StreamObserver[HelloReply] {
          override def onError(t: Throwable): Unit = {
            System.err.println(t.toString)
          }

          override def onCompleted(): Unit = {
            System.out.println("finished")
          }

          override def onNext(value: HelloReply): Unit = {
            System.out.println(value.getMessage)
          }
      })
    })
  }

  post("/hiProtoBuf") { hiRequest: HelloRequest =>
    "Hello " + hiRequest.getName
  }
}
