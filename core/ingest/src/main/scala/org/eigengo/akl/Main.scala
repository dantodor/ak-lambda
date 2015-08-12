package org.eigengo.akl

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.io.{IO, Tcp}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory
import org.eignego.akl.{RK, DevelopmentEnvironment}

object Main extends App with DevelopmentEnvironment {
  setup()
  implicit val system = ActorSystem("ingest", ConfigFactory.load("application.conf"))
  implicit val materializer = ActorMaterializer()

  val transport = IO(Tcp)
  val endpoint = system.actorOf(MeasurementsEndpoint.props)
  transport ! Tcp.Bind(endpoint, localAddress = new InetSocketAddress("0.0.0.0", 8080))

  Source(() ⇒ Iterator("some such", "none at all")).to(Sink(RK.publish(RK.producerProperties))).run()

}