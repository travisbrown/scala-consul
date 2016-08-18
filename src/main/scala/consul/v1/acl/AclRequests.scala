package consul.v1.acl

import consul.v1.common.ConsulRequestBasics
import play.api.libs.json.{JsNull, Json, Reads}

import scala.concurrent.{ExecutionContext, Future}
import spray.http.StatusCodes
import spray.httpx.RequestBuilding
import spray.httpx.PlayJsonSupport._

trait AclRequests {
  def create(Name:Option[String]=Option.empty,Type:Option[String]=Option.empty,Rules:Option[String]=Option.empty):Future[AclIdResponse]
  def create(acl:AclCreate):Future[AclIdResponse]
  def update(ID:AclId,Name:Option[String]=Option.empty,Type:Option[String]=Option.empty,Rules:Option[String]=Option.empty):Future[Boolean]
  def update(acl:AclUpdate):Future[Boolean]
  def destroy(id:AclId):Future[Boolean]
  def info(id:AclId):Future[Option[AclInfo]]
  def clone(id:AclId): Future[AclIdResponse]
  def list():Future[Seq[AclInfo]]

  def AclCreate = consul.v1.acl.AclCreate.apply _
  def AclUpdate = consul.v1.acl.AclUpdate.apply _
  def AclId     = consul.v1.acl.AclId
}

object AclRequests{

  def apply(basePath: String)(implicit executionContext: ExecutionContext, rb: ConsulRequestBasics): AclRequests = new AclRequests{

    def create(acl:AclCreate):Future[AclIdResponse] =
      rb.jsonRequestMaker[AclIdResponse](createPath, uri => RequestBuilding.Put(uri, Json.toJson(acl)))

    def create(Name:Option[String],Type:Option[String],Rules:Option[String]):Future[AclIdResponse] = create(
      AclCreate(Name,Type,Rules)
    )

    def update(acl: AclUpdate): Future[Boolean] =
      rb.responseStatusRequestMaker(updatePath, uri => RequestBuilding.Put(uri, Json.toJson(acl)))(_ == StatusCodes.OK)

    def update(ID:AclId,Name:Option[String],Type:Option[String],Rules:Option[String]): Future[Boolean] =
      update(AclUpdate(ID,Name,Type,Rules))

    def destroy(id:AclId):Future[Boolean] =
      rb.responseStatusRequestMaker(fullPathFor(s"destroy/$id"), RequestBuilding.Put(_, JsNull))(_ == StatusCodes.OK)

    def list():Future[Seq[AclInfo]] =
      rb.jsonRequestMaker[Seq[AclInfo]](listPath, RequestBuilding.Get(_))

    def info(id:AclId): Future[Option[AclInfo]] =
      rb.jsonRequestMaker(fullPathFor(s"info/$id"), RequestBuilding.Get(_))(Reads.optionWithNull[AclInfo], executionContext)

    def clone(id:AclId): Future[AclIdResponse] =
      rb.jsonRequestMaker[AclIdResponse](fullPathFor(s"clone/$id"), RequestBuilding.Put(_, JsNull))

    private lazy val createPath = fullPathFor("create")
    private lazy val updatePath = fullPathFor("update")
    private lazy val listPath   = fullPathFor("list")

    private def fullPathFor(path: String) = s"$basePath/acl/$path"

  }
}
