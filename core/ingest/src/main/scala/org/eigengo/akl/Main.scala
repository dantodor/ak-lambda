package org.eigengo.akl

import java.net.InetSocketAddress
import java.nio.file.{Files, Paths}
import java.util.UUID

import akka.actor.ActorSystem
import akka.cluster.ClusterEvent.MemberEvent
import akka.io.{IO, Tcp}
import akka.persistence.Update
import com.typesafe.config.ConfigFactory
import org.eigengo.akl.devices.KnownDevices.GetKnownDevice
import org.eigengo.akl.devices.KnownDevicesProcessor.{KnownDevicesSource, RegisterDevices}
import org.eigengo.akl.devices.{KnownDevices, KnownDevicesProcessor}
import org.eignego.akl.DevelopmentEnvironment

object Main extends App with DevelopmentEnvironment {
  import collection.JavaConversions._
  setup()

  val config = ConfigFactory.load("application.conf")
  val ports = config.getIntList("akka.cluster.jvm-ports")
  ports.par.foreach(startup)

  def startup(port: Integer): Unit = {
    val config = ConfigFactory.load("application.conf")
    implicit val system = ActorSystem("ingest", ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").withFallback(config))

    import akka.actor.ActorDSL._
    implicit val printlnSender = actor(new Act {
      become {
        case x ⇒ println(">>>> " + x)
      }
    })

    val transport = IO(Tcp)
    val endpoint = system.actorOf(MeasurementsEndpoint.props)
    transport ! Tcp.Bind(endpoint, localAddress = new InetSocketAddress("0.0.0.0", 8080 + port))

    val dp = KnownDevicesProcessor.actorOf(system)
    val ds = KnownDevices.actorOf(system)

    system.eventStream.subscribe(printlnSender, classOf[MemberEvent])

    Thread.sleep(10000L)

    if (port == ports.head) dp ! RegisterDevices(KnownDevicesSource.Csv(ClientId.one, Files.readAllBytes(Paths.get(Main.getClass.getResource("/devices-1.csv").toURI))))
    Thread.sleep(1000L)

    ds ! GetKnownDevice(ClientId.one, DeviceId(UUID.fromString("6c7d1b1b-d7eb-4589-8e63-1788eecfe86f")))
    ds ! GetKnownDevice(ClientId.one, DeviceId(UUID.fromString("6c7d1b1b-d7eb-4589-8e63-1788eecfe8ff")))
  }
}
