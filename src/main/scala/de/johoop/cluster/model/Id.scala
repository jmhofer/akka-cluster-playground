package de.johoop.cluster.model

trait Id[A] {
  def id(str: String): A
  def str(id: A): String
}

object Id {
  def id[A](apply: String => A, unapply: A => String): Id[A] = new Id[A] {
    def id(str: String) = apply(str)
    def str(id: A) = unapply(id)
  }
}
