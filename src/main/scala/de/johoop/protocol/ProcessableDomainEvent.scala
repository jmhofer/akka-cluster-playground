package de.johoop.protocol

sealed trait DomainEvent {
  val id: String
}
case class ProcessableDomainEvent(id: String, payload: String) extends DomainEvent
