package de.johoop.cluster

import akka.actor.ActorSystem
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import com.typesafe.config.{Config, ConfigFactory}
import de.johoop.cluster.client.ShardingClient
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main {

  def main(args: Array[String]): Unit =
    if (args.isEmpty) startup(Array("2551", "2552", "0")) else startup(args)

  def startup(ports: Array[String]): Unit = ports foreach { port =>
    val config: Config = ConfigFactory parseString s"akka.remote.netty.tcp.port=$port" withFallback ConfigFactory.load()

    implicit val system = ActorSystem("ClusterSystem", config)

    // shard cluster member
    val persistence: Persistence = new Persistence.StdOut

    ClusterSharding(system).start(
      typeName = ProductEntity.shardTypeName,
      entityProps = ProductEntity.props(persistence),
      settings = ClusterShardingSettings(system),
      extractEntityId = ProductEntity.extractEntityId,
      extractShardId = ProductEntity.extractShardId
    )

    if (port == "0") {
      import system.dispatcher

      val client = new ShardingClient

      val doneGenerating = client.generateLotsOfStuff(10 seconds)
      doneGenerating onComplete {
        case Success(result) => println(s"meep: $result")
        case Failure(e) => println(s"the whole generation is defeated: $e")
      }

      val doneReading = client.retrieveAllTheThings(120 seconds)
      doneReading onComplete {
        case Success(_) => println(s"done reading, yay")
        case Failure(e) => println(s"utter reading defeat: $e")
      }
    }
  }
}
