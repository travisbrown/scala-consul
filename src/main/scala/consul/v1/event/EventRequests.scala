package consul.v1.event

import consul.v1.common.ConsulRequestBasics
import consul.v1.common.Types._
import play.api.libs.json.Json
import spray.httpx.RequestBuilding
import spray.httpx.marshalling.Marshaller

import scala.concurrent.{ExecutionContext, Future}

trait EventRequests {

  def fire[T](name:String, payload:T,node:Option[NodeId]=Option.empty,service:Option[ServiceId]=Option.empty, tag:Option[ServiceTag]=Option.empty,dc:Option[DatacenterId]=Option.empty)(implicit wrt: Marshaller[T]):Future[Event]
  def list(name:Option[String]=Option.empty):Future[List[Event]]

  def EventId: (String) => EventId = consul.v1.event.EventId
}

object EventRequests{

  private implicit lazy val eventReads = Json.reads[Event]

  def apply(basePath: String)(implicit executionContext: ExecutionContext, rb: ConsulRequestBasics):EventRequests = new EventRequests {

    def fire[T](name:String, payload:T,node:Option[NodeId],service:Option[ServiceId],tag:Option[ServiceTag],dc:Option[DatacenterId])(
                implicit wrt: Marshaller[T]):Future[Event] = {

      val params = node.map("node"->_.toString).toList ++ service.map("service"->_.toString) ++ tag.map("tag"->_.toString)

      rb.jsonDcRequestMaker[Event](fullPathFor(s"fire/$name"),dc,
        uri => RequestBuilding.Put(uri.withQuery(params:_*), payload)
      )
    }

    def list(name: Option[String]): Future[List[Event]] =
      rb.jsonRequestMaker[List[Event]](listPath,
        uri => RequestBuilding.Get(name.map{ case name => uri.withQuery("name"->name) }.getOrElse(uri))
      )

    private lazy val listPath = fullPathFor("list")
    private def fullPathFor(path: String) = s"$basePath/event/$path"
  }
}
