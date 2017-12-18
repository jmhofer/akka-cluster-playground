package de.johoop.cluster

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}

object Main {
  def main(args: Array[String]): Unit =
    if (args.isEmpty) startup(Array("2551", "2552", "0")) else startup(args)

  def startup(ports: Array[String]): Unit = ports foreach { port =>
    val config: Config = ConfigFactory parseString s"akka.remote.netty.tcp.port=$port" withFallback ConfigFactory.load()

    ActorSystem("ClusterSystem", config).actorOf(ClusterActor.props, "clusterActor")
  }
}
