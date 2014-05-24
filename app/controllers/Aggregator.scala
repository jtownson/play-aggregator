package controllers

import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._
import plugins.mvc.VM
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

/**
 * Controller that uses site/page CMS lookup to determine
 * rendering and business data for the page in question.
 * Prevents implicit duplication of site organisation and markup
 * of CMS in the webapp.
 */
object Aggregator extends Controller {

  /**
   * Serves a complete web page using the usual "model + view and status 200" algorithm
   */
  def page(path:String) = Action.async { request =>

    serveDirectly(request, None)
  }

  /**
   * Serves a rendered page component, getting the required business data
   * from the businessUrl and using template defined in CMS page-template
   * value for this site/page.
   *
   * If X-VARNISH is defined, just render an ESI directive with the businessUrl.
   */
  def component(component: String, businessUrl: String) = Action.async { request =>

    if (request.headers.get("X-VARNISH").isDefined) {
      Future(Ok(VM("vm/esi.vm", Map("url"->("http://" + request.host + request.uri)))))
    } else {
      serveDirectly(request, Some(businessUrl))
    }
  }

  private def serveDirectly(request: Request[AnyContent], businessUrl: Option[String]): Future[SimpleResult] = {
    for {
      content <- pageContent(request)
      businessData <- businessData(businessUrl)
    } yield {

      val view = (content \\ "page-template").head.as[String]

      val model = Map[String, Any](
        "request" -> request,
        "requestTool" -> new RequestTool(request),
        "pageContent" -> new JsonTool(content),
        "businessData" -> new JsonTool(businessData))

      Ok(VM(view, model))
    }
  }

  /**
   * Defines the lookup between page URL and CMS content for the page.
   * An apocryphal and grossly simplified implementation!
   */
  private def pageContent(request: Request[AnyContent]): Future[JsValue] = {

    val page: String = request.host.replaceAll("\\:\\d+", "") + request.path

    WS.url("http://localhost:9000/cms/" + page).get.map ( response => Json.parse(response.body) )
  }

  /**
   * Fetch business data as JSON (or just return an empty Json value)
   */
  private def businessData(url: Option[String]): Future[JsValue] = url match {

    case Some(u) => businessData(u)
    case None => Future(Json.parse("{}"))
  }

  private def businessData(url: String): Future[JsValue] = {
    WS.url(url).get.map(r => Json.parse(r.body))
  }
}

/**
 * Helps templates get strings from json
 */
class JsonTool(jsValue: JsValue) {
  def get(path: String) = {
    (jsValue \\ path).head.as[String]
  }
}

/**
 * Helps page templates inject HTML rendered business components.
 */
class RequestTool(request: Request[AnyContent]) {

  // TODO: can this be made asynchronous?
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
