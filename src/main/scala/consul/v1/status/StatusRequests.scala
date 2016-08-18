package consul.v1.status

import consul.v1.common.ConsulRequestBasics
import play.api.libs.json.Reads
import scala.concurrent.{ExecutionContext, Future}
import spray.httpx.RequestBuilding

trait StatusRequests {
  def leader():Future[Option[String]]
  def peers(): Future[Seq[String]]
}
object StatusRequests{

  def apply(basePath: String)(implicit executionContext: ExecutionContext, rb: ConsulRequestBasics): StatusRequests = new StatusRequests{

    def leader(): Future[Option[String]] =
      rb.jsonRequestMaker(fullPathFor("leader"), RequestBuilding.Get(_))(Reads.optionWithNull[String], executionContext)

    def peers(): Future[Seq[String]] =
      rb.jsonRequestMaker[Seq[String]](fullPathFor("peers"), RequestBuilding.Get(_))

    private def fullPathFor(path: String) = s"$basePath/status/$path"
  }

}
