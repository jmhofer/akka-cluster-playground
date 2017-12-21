package de.johoop.cluster

package object model {
  final case class ProductId(id: String) extends AnyVal

  final case class ProductWithOffers(id: ProductId, payload: Array[Int])

  object Implicits {
    implicit class EnrichedId[A](id: A)(implicit ev: Id[A]) {
      def str: String = ev.str(id)
    }

    implicit val productId: Id[ProductId] = Id.id(ProductId, _.id)
  }
}
