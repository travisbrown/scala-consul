package consul.v1.catalog

import consul.v1.common.CheckStatus._
import consul.v1.common.Types._
import consul.v1.common.{ConsulRequestBasics, Node, Types}
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import spray.http.StatusCodes
import spray.httpx.RequestBuilding

case class Deregisterable(Node:NodeId,ServiceID:Option[ServiceId],CheckID:Option[CheckId],Datacenter:Option[DatacenterId])
case class Service(ID:ServiceId,Service:ServiceType,Tags:Set[ServiceTag],Address:Option[String],Port:Option[Int])
case class Check(Node:NodeId,CheckID:CheckId,Name:String,Notes:Option[String],Status:CheckStatus,ServiceID:Option[ServiceId])
case class Registerable(Node:NodeId,Address:String,Service:Option[Service],Check:Option[Check],Datacenter:Option[DatacenterId])

trait CatalogRequests {
  def register(registerable:Registerable):Future[Boolean]
  def deregister(deregisterable:Deregisterable):Future[Boolean]
  def datacenters(): Future[Seq[DatacenterId]]
  def nodes(dc:Option[DatacenterId]=Option.empty): Future[Seq[Node]]
  def node(nodeID:NodeId,dc:Option[DatacenterId]=Option.empty):Future[NodeProvidedServices]
  def services(dc:Option[DatacenterId]=Option.empty):Future[Map[ServiceType,Set[String]]]
  def service(service:ServiceType,tag:Option[ServiceTag]=Option.empty, dc:Option[DatacenterId]=Option.empty):Future[Seq[NodeProvidingService]]

  /*convenience methods*/
  def deregisterNode(node:NodeId,dc:Option[DatacenterId]): Future[Boolean] =
    deregister(Deregisterable(node,Option.empty,Option.empty,dc))
  def deregisterService(service:ServiceId,node:NodeId,dc:Option[DatacenterId]): Future[Boolean] =
    deregister(Deregisterable(node,Option(service),Option.empty,dc))
  def deregisterCheck(check:CheckId,node:NodeId,dc:Option[DatacenterId]): Future[Boolean] =
    deregister(Deregisterable(node,Option.empty,Option(check),dc))

  def Registerable: (Types.NodeId, String, Option[Service], Option[Check], Option[Types.DatacenterId]) => Registerable =
    consul.v1.catalog.Registerable.apply _
  def Check: (Types.NodeId, Types.CheckId, String, Option[String], CheckStatus, Option[Types.ServiceId]) => Check =
    consul.v1.catalog.Check.apply _
  def Service: (Types.ServiceId, Types.ServiceType, Set[Types.ServiceTag], Option[String], Option[Int]) => Service =
    consul.v1.catalog.Service.apply _
  def Deregisterable = consul.v1.catalog.Deregisterable.apply _

}

object CatalogRequests {

  private implicit lazy val deregisterWrites = Json.writes[Deregisterable]
  private implicit lazy val registerWrites   = {
    implicit val serviceWrites = Json.writes[Service]
    implicit val checkWrites = Json.writes[Check]

    Json.writes[Registerable]
  }

  def apply(basePath: String)(implicit executionContext: ExecutionContext, rb: ConsulRequestBasics): CatalogRequests = new CatalogRequests {

    def register(registerable: Registerable): Future[Boolean] = rb.responseStatusRequestMaker(
      registerPath,
      uri => RequestBuilding.Put(uri, Json.stringify(Json.toJson(registerable)))
    )(_ == StatusCodes.OK)

    def deregister(deregisterable:Deregisterable):Future[Boolean] = rb.responseStatusRequestMaker(
      deregisterPath,
      uri => RequestBuilding.Put(uri, Json.stringify(Json.toJson(deregisterable)))
    )(_ == StatusCodes.OK)

    def nodes(dc:Option[DatacenterId]) =
      rb.jsonDcRequestMaker[Seq[Node]](fullPathFor("nodes"),dc, RequestBuilding.Get(_))

    def node(nodeID: NodeId, dc:Option[DatacenterId]) =
      rb.jsonDcRequestMaker[NodeProvidedServices](fullPathFor(s"node/$nodeID"),dc, RequestBuilding.Get(_))

    def service(service: ServiceType, tag:Option[ServiceTag], dc:Option[DatacenterId]): Future[Seq[NodeProvidingService]] =
      rb.jsonDcRequestMaker[Seq[NodeProvidingService]](fullPathFor(s"service/$service"),dc,
        uri => RequestBuilding.Get(tag.map{ case tag => uri.withQuery("tag"->tag.toString) }.getOrElse(uri))
      )

    def datacenters(): Future[Seq[DatacenterId]] =
      rb.jsonRequestMaker[Seq[DatacenterId]](datacenterPath, RequestBuilding.Get(_))

    def services(dc:Option[DatacenterId]=Option.empty): Future[Map[Types.ServiceType, Set[String]]] =
      rb.jsonDcRequestMaker(servicesPath, dc, RequestBuilding.Get(_))(
        Reads.of[Map[String,Set[String]]].map(
          _.map{ case (key,value) => ServiceType(key)->value }
        ),
        executionContext
      )

    private lazy val datacenterPath = fullPathFor("datacenters")
    private lazy val servicesPath   = fullPathFor("services")
    private lazy val registerPath   = fullPathFor("register")
    private lazy val deregisterPath = fullPathFor("deregister")

    private def fullPathFor(path: String) = s"$basePath/catalog/$path"

  }

}
