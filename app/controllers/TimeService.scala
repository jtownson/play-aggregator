package controllers

import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import org.joda.time.DateTime
import ExecutionContext.Implicits.global

/**
 * A service that returns some business data.
 */
object TimeService extends Controller {

  def current = Action.async{
    Future(Ok(Json.obj("time" -> DateTime.now.getMillis.toString)))
  }
}
