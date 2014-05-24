package controllers;


import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import io.Source._
import play.Play
import java.io.File
/**
 *
 */
object Cms extends Controller {

  def get(contentPath: String) = Action.async {
    val theResourceFile:File = Play.application().getFile("conf/resources/cms/" + contentPath)
    val itsContent = fromFile(theResourceFile).mkString
    Future(Ok(itsContent))
  }

}
