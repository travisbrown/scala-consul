package consul.v1.agent.service

import consul.v1.agent.service.LocalService.{apply => applied}
import consul.v1.common.ConsulRequestBasics
import consul.v1.common.Types._
import play.api.libs.json.{JsNull, Json, Writes}

import scala.concurrent.{ExecutionContext, Future}
import spray.http.StatusCodes
import spray.httpx.RequestBuilding
import spray.httpx.PlayJsonSupport._

trait ServiceRequests {
  def register(localService: LocalService): Future[Boolean]

  def deregister(serviceID:ServiceId):Future[Boolean]

  def maintenance(serviceID:ServiceId,enable:Boolean,reason:Option[String]):Future[Boolean]

  def LocalService = applied(_: ServiceId, _: ServiceType, _: Set[ServiceTag], _: Option[Int], _: Option[Check])

  def ttlCheck(ttl: String): Check = Check(Option.empty, Option.empty,Option.empty, Option(ttl))

  def scriptCheck(script: String, interval: String): Check = Check(Option(script), Option.empty,Option(interval), Option.empty)

  def httpCheck(http:String,interval:String):Check = Check(Option.empty, Option(http), Option(interval), Option.empty)
}

object ServiceRequests {

  private implicit lazy val CheckWrites: Writes[Check] = Json.writes[Check]

  def apply(basePath: String)(implicit executionContext: ExecutionContext, rb: ConsulRequestBasics): ServiceRequests = new ServiceRequests{

    def maintenance(serviceID: ServiceId,enable:Boolean,reason:Option[String]): Future[Boolean] = {
      lazy val params = Seq(("enable",enable.toString)) ++ reason.map("reason"->_)
      rb.responseStatusRequestMaker(
        fullPathFor(s"maintenance/$serviceID"),
        uri => RequestBuilding.Put(uri.withQuery(params:_*), JsNull)
      )(_ == StatusCodes.OK)
    }

    def register(localService: LocalService): Future[Boolean] =
      rb.responseStatusRequestMaker(fullPathFor("register"), uri => RequestBuilding.Put(uri, Json.toJson(localService)))(_ == StatusCodes.OK)

    def deregister(serviceID: ServiceId): Future[Boolean] =
      rb.responseStatusRequestMaker(fullPathFor(s"deregister/$serviceID"), RequestBuilding.Get(_))(_ == StatusCodes.OK)

    private def fullPathFor(path: String) = s"$basePath/service/$path"

  }

}
