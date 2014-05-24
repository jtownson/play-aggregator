package controllers

import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._
import plugins.mvc.VM
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import java.net.URLEncoder

/**
 *
 */
object Aggregator extends Controller {

  def page(path:String) = Action.async { request =>

    for {
      content <- pageContent(request)
    } yield {

      val view = (content \\ "page-template").head.as[String]

      val model = Map[String, Any](
        "request" -> request,
        "requestTool" -> new RequestTool(request),
        "pageContent" -> new JsonTool(content))

      Ok(VM(view, model))
    }
  }

  private def pageContent(request: Request[AnyContent]): Future[JsValue] = {

    val page: String = request.host.replaceAll("\\:\\d+", "") + request.path

    WS.url("http://localhost:9000/cms/" + URLEncoder.encode(page)).get.map ( response => Json.parse(response.body) )
  }

  def component(component: String, businessUrl: String) = Action.async { request =>

    if (request.headers.get("X-VARNISH").isDefined) {
      Future(Ok(VM("vm/esi.vm", Map("url"->("http://" + request.host + request.uri)))))
    } else {
      for {
        content <- pageContent(request)
        businessData <- businessData(businessUrl)
      } yield {

        val view = (content \\ "page-template").head.as[String]

        val model = Map[String, Any](
          "request" -> request,
          "businessData" -> new JsonTool(businessData),
          "pageContent" -> new JsonTool(content))

        Ok(VM(view, model))
      }
    }
  }

  private def businessData(url: String): Future[JsValue] = {
    WS.url(url).get.map(r => Json.parse(r.body))
  }
}

class JsonTool(jsValue: JsValue) {
  def get(path: String) = {
    (jsValue \\ path).head.as[String]
  }
}

class RequestTool(request: Request[AnyContent]) {

  def render(url: String): String = {
    if (request.headers.get("X-VARNISH").isDefined) {
      Await.result(get(url, Seq(("X-VARNISH", ""))), 5 seconds)
    } else {
      Await.result(get(url), 5 seconds)
    }
  }

  def get(url: String, headers: Seq[(String, String)] = Seq.empty): Future[String] = {
      WS.url(url).withHeaders(headers:_*).get().map(r=> r.body)
  }
}
