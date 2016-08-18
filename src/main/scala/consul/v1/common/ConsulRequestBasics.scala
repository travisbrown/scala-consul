package consul.v1.common

import consul.v1.common.Types.DatacenterId
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import spray.client.pipelining._
import spray.http.{ HttpRequest, HttpResponse, StatusCode, Uri }
import spray.httpx.PlayJsonSupport._

class ConsulRequestBasics(token: Option[String], client: SendReceive) {
  type HttpFunc = Uri => HttpRequest

  def jsonRequestMaker[A: Reads](path: String, httpFunc: HttpFunc)(implicit executionContext: ExecutionContext): Future[A] = {
    genRequestMaker(path,httpFunc)(unmarshal[A].apply)
  }

  def jsonDcRequestMaker[A: Reads](path: String, dc:Option[DatacenterId], httpFunc: HttpFunc)(implicit executionContext: ExecutionContext): Future[A] = {
    jsonRequestMaker(path, withDc(dc).andThen(httpFunc))
  }

  def responseStatusRequestMaker[A](path: String, httpFunc: HttpFunc)(body: StatusCode => A)(implicit executionContext: ExecutionContext): Future[A] = {
    genRequestMaker(path,httpFunc)(res => body(res.status.intValue))
  }

  def responseStatusDcRequestMaker[A](path: String, dc:Option[DatacenterId], httpFunc: HttpFunc)(body: StatusCode => A)(implicit executionContext: ExecutionContext): Future[A] = {
    responseStatusRequestMaker(path, withDc(dc).andThen(httpFunc))(body)
  }

  def erased[A](future:Future[JsResult[A]])(implicit executionContext: ExecutionContext): Future[A] = {
    future.flatMap {
      case err: JsError => Future.failed(Types.ConsulResponseParseException(err))
      case JsSuccess(res, _) => Future.successful(res)
    }
  }

  private def genRequestMaker[A](path: String, httpFunc: HttpFunc)(responseTransformer: HttpResponse => A)(implicit executionContext: ExecutionContext): Future[A] = {
    client(withToken(token).andThen(httpFunc)(Uri(path))).map(responseTransformer)
  }

  private def withToken(token: Option[String]): Uri => Uri = {
    token.map(t => (_: Uri).withQuery("token" -> t)).getOrElse(identity)
  }

  private def withDc(dc: Option[DatacenterId]): Uri => Uri = {
    dc.map(v => (_: Uri).withQuery("dc" -> v.toString)).getOrElse(identity)
  }
}
