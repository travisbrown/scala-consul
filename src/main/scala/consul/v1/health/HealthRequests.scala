package consul.v1.health

import consul.v1.common.CheckStatus.CheckStatus
import consul.v1.common.ConsulRequestBasics
import consul.v1.common.Types.{NodeId, ServiceType,ServiceTag,DatacenterId}
import play.api.libs.json.{Json, Reads}
import spray.httpx.RequestBuilding

import scala.concurrent.{ExecutionContext, Future}

trait HealthRequests{
  def node(nodeID: NodeId,dc:Option[DatacenterId]=Option.empty): Future[Seq[Check]]
  def service(service: ServiceType,tag:Option[ServiceTag]=Option.empty,passing:Boolean=false,dc:Option[DatacenterId]=Option.empty): Future[Seq[NodesHealthService]]
  def checks(serviceID:ServiceType,dc:Option[DatacenterId]=Option.empty): Future[Seq[Check]]
  def state(state:CheckStatus,dc:Option[DatacenterId]=Option.empty): Future[Seq[Check]]
}

object HealthRequests {

  implicit private val NodesHealthServiceReads: Reads[NodesHealthService] = Json.reads[NodesHealthService]

  def apply(basePath: String)(implicit executionContext: ExecutionContext, rb: ConsulRequestBasics): HealthRequests = new HealthRequests {
    def service(service: ServiceType, tag:Option[ServiceTag], passing:Boolean=false,dc:Option[DatacenterId]): Future[Seq[NodesHealthService]] = {
      lazy val params = (if(passing) List(("passing","")) else List.empty ) ++ tag.map{ case tag => (("tag",tag.toString)) }

      rb.jsonDcRequestMaker[Seq[NodesHealthService]](
        fullPathFor(s"service/$service"),dc,
        uri => RequestBuilding.Get(uri.withQuery(params:_*))
      )
    }

    def node(nodeID: NodeId,dc:Option[DatacenterId]) =
      rb.jsonDcRequestMaker[Seq[Check]](fullPathFor(s"node/$nodeID"),dc, RequestBuilding.Get(_))

    def checks(serviceID:ServiceType,dc:Option[DatacenterId]) =
      rb.jsonDcRequestMaker[Seq[Check]](fullPathFor(s"checks/$serviceID"),dc, RequestBuilding.Get(_))

    def state(state:CheckStatus,dc:Option[DatacenterId]): Future[Seq[Check]] =
      rb.jsonDcRequestMaker[Seq[Check]](fullPathFor(s"state/$state"),dc, RequestBuilding.Get(_))

    private def fullPathFor(path: String) = s"$basePath/health/$path"

  }

}
