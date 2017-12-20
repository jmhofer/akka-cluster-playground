package de.johoop.cluster

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.pattern.pipe
import de.johoop.cluster.model.{ProductId, ProductWithOffers}
import de.johoop.cluster.model.Implicits._

import scala.concurrent.duration._
import scala.ref.SoftReference

object ProductEntity {
  sealed abstract class Message

  final case class Put(product: ProductWithOffers) extends Message
  final case class Get(id: ProductId) extends Message

  private case object Stop extends Message
  private final case class Persisted(origin: ActorRef, value: ProductWithOffers) extends Message
  private final case class Retrieved(origin: ActorRef, value: Option[ProductWithOffers]) extends Message

  val numberOfShards = 100
  val shardTypeName = "ProductEntity"

  def extractEntityId: ShardRegion.ExtractEntityId = {
    case put @ Put(product) => (product.id.str, put)
    case get @ Get(id) => (id.str, get)

    case _ => throw new IllegalArgumentException("Cannot retrieve ID from unknown message")
  }

  def extractShardId: ShardRegion.ExtractShardId = {
    case Put(product) => hashMod(product.id)
    case Get(id) => hashMod(id)
    case ShardRegion.StartEntity(id) => hashMod(ProductId(id))
  }

  private def hashMod(id: ProductId): String = (math.abs(id.str.hashCode) % numberOfShards).toString

  def props(persistence: Persistence): Props = Props(new ProductEntity(persistence))
}

class ProductEntity(persistence: Persistence) extends Actor with ActorLogging {
  import ProductEntity._
  import ShardRegion.Passivate
  import context.dispatcher

  context.setReceiveTimeout(120 second)

  def receive: Receive = receiveWithState(SoftReference(null))

  def receiveWithState(state: SoftReference[ProductWithOffers]): Receive = {
    case Put(product) =>
      log info s"received Put($product)"
      val origin = sender()
      persistence.store(product) map (_ => Persisted(origin, product)) pipeTo self

    case Persisted(origin, product) =>
      context.become(receiveWithState(SoftReference(product)))
      origin ! Done

    case Get(id) =>
      log info s"received Get($id)"
      val origin = sender()
      state.get match {
        case Some(product) => origin ! Some(product)
        case None =>
          persistence.retrieve(id) map (Retrieved(origin, _)) pipeTo self
      }

    case Retrieved(origin, product) =>
      product foreach { product => context.become(receiveWithState(SoftReference(product))) }
      origin ! product

    case ReceiveTimeout =>
      context.become(receive)
      context.parent ! Passivate(stopMessage = Stop)

    case Stop => context.stop(self)

    case other => log error s"received unknown message: $other"
  }
}
