package com.airmap.examples

import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.{Duration, Future, Promise, Timer}
import io.grpc.stub.StreamObserver
import io.grpc.ServerBuilder

class GrpcServer  {

  implicit private val timer = DefaultTimer

  class ServerImpl extends GreeterGrpc.GreeterImplBase {
    override def sayHello(req: HelloRequest, responseObserver: StreamObserver[HelloReply]): Unit = {
      val reply = HelloReply.newBuilder.setMessage("Hello " + req.getName).build
      responseObserver.onNext(reply)
      responseObserver.onCompleted()
    }

    override def streamHello(req: HelloRequest, responseObserver: StreamObserver[HelloReply]): Unit = {
      def next(left: Int): Future[Unit] = {
        left match {
          case 0 => Future.Done
          case _ => {
            val reply = HelloReply.newBuilder.setMessage(s"$left: Hello ${req.getName}").build
            responseObserver.onNext(reply)
            Future.sleep(Duration.fromMilliseconds(100)).flatMap(_=>next(left-1))
          }
        }
      }

      next(30).map(_ => {
        responseObserver.onCompleted()
      })
    }
  }

  val port = 50051
  private val server = ServerBuilder.forPort(port).addService(new ServerImpl).build.start
  System.out.println("Server started, listening on " + port)
  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run(): Unit = { // Use stderr here since the logger may have been reset by its JVM shutdown hook.
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      server.shutdown
      System.err.println("*** server shut down")
    }
  })
}
