package consul.v1.session

import consul.v1.common.ConsulRequestBasics
import consul.v1.common.Types._
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import spray.http.StatusCodes
import spray.httpx.RequestBuilding
import spray.httpx.PlayJsonSupport._

trait SessionRequests {

  def create(sessionDef:SessionDef=SessionDef(),dc:Option[DatacenterId]=Option.empty):Future[SessionIDHolder]
  def destroy(id:SessionId,dc:Option[DatacenterId]=Option.empty):Future[Boolean]
  def info(id:SessionId,dc:Option[DatacenterId]=Option.empty):Future[Seq[SessionInfo]]
  def node(node:NodeId,dc:Option[DatacenterId]=Option.empty):Future[Seq[SessionInfo]]
  def list(dc:Option[DatacenterId]=Option.empty):Future[Seq[SessionInfo]]
  def renew(id:SessionId,dc:Option[DatacenterId]=Option.empty):Future[Seq[SessionInfo]]

  def SessionDef(LockDelay:Option[String]=Option.empty, Name:Option[String]=Option.empty,
                 Node:Option[String]=Option.empty, Checks:Option[Seq[CheckId]]=Option.empty,
                 Behavior:Option[Behaviour.Value]=Option.empty, TTL:Option[String]=Option.empty): SessionDef =
    consul.v1.session.SessionDef(LockDelay,Name,Node,Checks,Behavior,TTL)
  def SessionId: (String) => SessionId = consul.v1.session.SessionId
  lazy val Behavior: Behaviour.type = consul.v1.session.Behaviour
}

object SessionRequests{

  private lazy implicit val SessionIDHolderReads = Json.reads[SessionIDHolder]
  private lazy implicit val SessionInfoReads     = Json.reads[SessionInfo]
  private lazy implicit val SessionDefWrites     = Json.writes[SessionDef]

  def apply(basePath: String)(implicit executionContext: ExecutionContext, rb: ConsulRequestBasics): SessionRequests = new SessionRequests{

    def create(sessionDef: SessionDef,dc:Option[DatacenterId]): Future[SessionIDHolder] =
      rb.jsonDcRequestMaker[SessionIDHolder](
        createPath,dc,
         uri => RequestBuilding.Put(uri, Json.toJson(sessionDef))
      )

    def node(node:NodeId,dc:Option[DatacenterId]=Option.empty):Future[Seq[SessionInfo]] =
      rb.jsonDcRequestMaker[Seq[SessionInfo]](
        fullPathFor(s"node/$node"),dc, RequestBuilding.Get(_)
      )

    def destroy(id:SessionId,dc:Option[DatacenterId]):Future[Boolean] = rb.responseStatusDcRequestMaker(
      fullPathFor(s"destroy/$id"),dc, uri => RequestBuilding.Put(uri, "")
    )(_ == StatusCodes.OK)

    def list(dc:Option[DatacenterId]):Future[Seq[SessionInfo]] =
      rb.jsonDcRequestMaker[Seq[SessionInfo]](
        listPath,dc, RequestBuilding.Get(_)
      )

    def renew(id:SessionId,dc:Option[DatacenterId]=Option.empty):Future[Seq[SessionInfo]] =
      rb.jsonDcRequestMaker[Seq[SessionInfo]](
        fullPathFor(s"renew/$id"),dc, uri => RequestBuilding.Put(uri, "")
      )

    def info(id:SessionId,dc:Option[DatacenterId]=Option.empty):Future[Seq[SessionInfo]] =
      rb.jsonDcRequestMaker[Seq[SessionInfo]](
        fullPathFor(s"info/$id"),dc, RequestBuilding.Get(_)
      )

    private lazy val createPath = fullPathFor("create")
    private lazy val listPath = fullPathFor("list")
    private def fullPathFor(path: String) = s"$basePath/session/$path"

  }

}
