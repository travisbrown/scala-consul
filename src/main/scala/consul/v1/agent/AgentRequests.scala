package consul.v1.agent

import consul.v1.agent.check.CheckRequests
import consul.v1.agent.service.ServiceRequests

import consul.v1.common.{ConsulRequestBasics, Service, Types}
import consul.v1.common.Types._
import consul.v1.health.Check
import play.api.libs.json.{JsNull, JsObject, Reads}

import scala.concurrent.{ExecutionContext, Future}
import spray.http.StatusCodes
import spray.httpx.RequestBuilding
import spray.httpx.PlayJsonSupport._

trait AgentRequests {

  def self(): Future[JsObject]
  def join(address:String,wan:Boolean=false):Future[Boolean]
  def `force-leave`(node:NodeId):Future[Boolean]
  def maintenance(enable:Boolean,reason:Option[String]):Future[Boolean]

  def checks():Future[Map[CheckId,Check]]
  def services():Future[Map[ServiceId,Service]]


  def service: ServiceRequests
  def check: CheckRequests
}

object AgentRequests {

  def apply(basePath: String)(implicit executionContext: ExecutionContext, rb: ConsulRequestBasics): AgentRequests = new AgentRequests {

    def self() =
      rb.jsonRequestMaker[JsObject](fullPathFor("self"), RequestBuilding.Get(_))

    def join(address: String,wan:Boolean): Future[Boolean] = rb.responseStatusRequestMaker(
      fullPathFor(s"join/$address"),
      uri => RequestBuilding.Get(if(wan) uri.withQuery(("wan","1")) else uri)
    )( _ == StatusCodes.OK )

    def `force-leave`(node: Types.NodeId): Future[Boolean] = rb.responseStatusRequestMaker(
      fullPathFor(s"force-leave/$node"), RequestBuilding.Get(_)
    )( _ == StatusCodes.OK )

    def maintenance(enable:Boolean,reason:Option[String]): Future[Boolean] = {
      lazy val params = Seq(("enable",enable.toString)) ++ reason.map("reason"->_)
      rb.responseStatusRequestMaker( maintenancePath, uri => RequestBuilding.Put(uri.withQuery(params:_*), JsNull) )(_ == StatusCodes.OK)
    }

    def checks(): Future[Map[CheckId, Check]] =
      rb.jsonRequestMaker(checksPath, RequestBuilding.Get(_))(
        Reads.of[Map[String,Check]].map(_.map{ case (key,value) => CheckId(key)->value }),
        executionContext
      )

    def services(): Future[Map[ServiceId,Service]] =
      rb.jsonRequestMaker(servicesPath, RequestBuilding.Get(_))(
        Reads.of[Map[String,Service]].map(_.map{ case (key,value) => ServiceId(key)->value }),
        executionContext
      )

    lazy val service: ServiceRequests = ServiceRequests(currPath)
    lazy val check:CheckRequests = CheckRequests(currPath)

    private lazy val maintenancePath = fullPathFor("maintenance")
    private lazy val checksPath = fullPathFor("checks")
    private lazy val servicesPath = fullPathFor("services")

    private lazy val currPath = s"$basePath/agent"
    private def fullPathFor(path: String) = s"$currPath/$path"

  }
}
