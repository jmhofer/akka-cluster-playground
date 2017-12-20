package de.johoop.cluster.client

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.sharding.ClusterSharding
import akka.pattern.ask
import akka.util.Timeout
import de.johoop.cluster.ProductEntity
import de.johoop.cluster.ProductEntity.{Get, Put}
import de.johoop.cluster.model.{ProductId, ProductWithOffers}

import scala.concurrent.duration._
import scala.concurrent.Future

class ShardingClient(implicit system: ActorSystem) {
  implicit val timeout = Timeout(10 seconds)

  private val region: ActorRef = ClusterSharding(system).shardRegion(ProductEntity.shardTypeName)

  def put(id: ProductId): Future[Done] = region.ask(Put(ProductWithOffers(id))).mapTo[Done]

  def get(id: ProductId): Future[Option[ProductWithOffers]] = region.ask(Get(id)).mapTo[Option[ProductWithOffers]]
}
