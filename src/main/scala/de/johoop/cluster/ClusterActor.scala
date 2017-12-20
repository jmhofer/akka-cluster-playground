package de.johoop.cluster

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.sharding.ShardRegion
import de.johoop.protocol.DomainEvent

object ClusterActor {
  def props: Props = Props[ClusterActor]

  def extractEntityId: ShardRegion.ExtractEntityId = {
    case e: DomainEvent => (e.id, e)
    case _ => throw new IllegalArgumentException("Cannot retrieve ID from unknown message")
  }

  def extractShardId: ShardRegion.ExtractShardId = {
    case e: DomainEvent => e.id
  }
}

class ClusterActor extends Actor with ActorLogging {
  val cluster = Cluster(context.system)

  override def preStart(): Unit =
    cluster subscribe (self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit = cluster unsubscribe self

  def receive: Receive = {
    case MemberUp(member)                      => log info s"member up: ${member.address}"
    case UnreachableMember(member)             => log info s"member unreachable: $member"
    case MemberRemoved(member, previousStatus) => log info s"member removed: ${member.address}, previous status: $previousStatus"
    case other: MemberEvent                    => log info s"other member event: $other"
  }
}
