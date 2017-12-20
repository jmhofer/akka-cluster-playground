package de.johoop.cluster

import java.util.UUID

import akka.actor.ActorSystem
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.pattern.after
import com.typesafe.config.{Config, ConfigFactory}
import de.johoop.cluster.client.ShardingClient
import de.johoop.cluster.model.ProductId

import scala.concurrent.Future
import scala.concurrent.duration._

object Main {

  def main(args: Array[String]): Unit =
    if (args.isEmpty) startup(Array("2551", "2552", "0")) else startup(args)

  def startup(ports: Array[String]): Unit = ports foreach { port =>
    val config: Config = ConfigFactory parseString s"akka.remote.netty.tcp.port=$port" withFallback ConfigFactory.load()

    implicit val system = ActorSystem("ClusterSystem", config)

    if (port != "0") {
      // shard cluster member
      val persistence: Persistence = new Persistence.StdOut

      ClusterSharding(system).start(
        typeName = ProductEntity.shardTypeName,
        entityProps = ProductEntity.props(persistence),
        settings = ClusterShardingSettings(system),
        extractEntityId = ProductEntity.extractEntityId,
        extractShardId = ProductEntity.extractShardId
      )

    } else {
      import system.dispatcher

      val id = ProductId(UUID.randomUUID().toString)

      after(10 seconds, system.scheduler)(for {
        client <- Future successful new ShardingClient
        _ <- client.put(id)
        product <- client.get(id)
      } yield product) foreach { result =>
        println(s"meep: $result")
      }
    }
  }
}
