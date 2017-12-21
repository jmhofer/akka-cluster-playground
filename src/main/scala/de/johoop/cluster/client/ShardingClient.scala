package de.johoop.cluster.client

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.sharding.ClusterSharding
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.Timeout
import de.johoop.cluster.ProductEntity
import de.johoop.cluster.ProductEntity.{Get, Put}
import de.johoop.cluster.model.{ProductId, ProductWithOffers}

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random

object ShardingClient {
  val payload = Array.fill(12000)(Random.nextInt)
  val numIds = 1000000

  def generateId: ProductId = ProductId(Random.nextInt(numIds).toString)
}

class ShardingClient(implicit system: ActorSystem) {
  import ShardingClient._

  implicit val mat = ActorMaterializer()
  implicit val timeout = Timeout(10 seconds)

  private lazy val region: ActorRef = ClusterSharding(system).shardRegion(ProductEntity.shardTypeName)

  def put(id: ProductId): Future[Done] = region.ask(Put(ProductWithOffers(id, payload))).mapTo[Done]

  def get(id: ProductId): Future[Option[ProductWithOffers]] = region.ask(Get(id)).mapTo[Option[ProductWithOffers]]

  def generateLotsOfStuff(after: FiniteDuration = Duration.Zero): Future[Done] = {
    Source
      .tick(after, Duration.Zero, ())
      .zipWithIndex
      .map { case (_, n) => ProductId((math.abs(n) % numIds).toString) }
      .mapAsync(parallelism = 4)(put)
      .toMat(Sink.ignore)(Keep.right)
      .run()
  }

  def retrieveAllTheThings(after: FiniteDuration = Duration.Zero): Future[Done] = {
    Source
      .tick(after, 5 millis, ())
      .map(_ => generateId)
      .mapAsync(parallelism = 1)(get)
      .map(_.isDefined)
      .grouped(1000)
      .map { hitsMisses => hitsMisses.count(identity) * 100 / hitsMisses.size }
      .toMat(Sink.foreach { hitsRatio =>
        println(s"Cache Hits: $hitsRatio %")
      })(Keep.right)
      .run()
  }
}
