package de.johoop.cluster

import akka.Done
import de.johoop.cluster.model.{ProductId, ProductWithOffers}

import scala.concurrent.{ExecutionContext, Future}

trait Persistence {
  def store(a: ProductWithOffers)(implicit ec: ExecutionContext): Future[Done]
  def retrieve(id: ProductId): Future[Option[ProductWithOffers]]
}

object Persistence {
  class StdOut extends Persistence {
    def store(product: ProductWithOffers)(implicit ec: ExecutionContext): Future[Done] = Future {
      println(s"persisted to stdout: $product")
      Done
    }

    def retrieve(id: ProductId): Future[Option[ProductWithOffers]] = {
      println(s"failed to retrieve $id, because I'm too dumb")
      Future successful None
    }
  }
}
