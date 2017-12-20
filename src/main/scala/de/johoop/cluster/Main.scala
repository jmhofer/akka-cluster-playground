package de.johoop.cluster

import akka.actor.ActorSystem
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import com.typesafe.config.{Config, ConfigFactory}

object Main {

  val shardTypeName = "processor"

  def main(args: Array[String]): Unit =
    if (args.isEmpty) startup(Array("2551", "2552", "0")) else startup(args)

  def startup(ports: Array[String]): Unit = ports foreach { port =>
    val config: Config = ConfigFactory parseString s"akka.remote.netty.tcp.port=$port" withFallback ConfigFactory.load()

    val clusterSystem = ActorSystem("ClusterSystem", config)

    val persistence: Persistence = new Persistence.StdOut

    ClusterSharding(clusterSystem).start(
      typeName = shardTypeName,
      entityProps = ProductEntity.props(persistence),
      settings = ClusterShardingSettings(clusterSystem),
      extractEntityId = ProductEntity.extractEntityId,
      extractShardId = ProductEntity.extractShardId
    )
  }
}
