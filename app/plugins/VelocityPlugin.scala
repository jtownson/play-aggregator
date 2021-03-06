/*
 * Copyright (C) 2013 FURYU CORPORATION
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Includes Apache Velocity
 *
 *   http://velocity.apache.org/
 *
 * Copyright (C) 2000-2007 The Apache Software Foundation
 */
package plugins

import java.io.StringWriter
import java.util.{ Iterator => JavaIterator, Properties }

import play.api.templates.Html
import play.api.Application
import play.api.Plugin

import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.VelocityContext
import org.apache.velocity.util.introspection.UberspectImpl
import org.apache.velocity.util.introspection.Info
import org.apache.velocity.util.introspection.VelPropertyGet
import org.apache.velocity.util.introspection.UberspectImpl.VelGetterImpl
import org.apache.velocity.runtime.log.Log
import org.apache.velocity.util.introspection.Introspector
import org.apache.velocity.runtime.parser.node.PropertyExecutor
import org.apache.velocity.runtime.parser.node.MapGetExecutor

/**
 * Butchered from https://github.com/Furyu/play-velocity-plugin
 */
class VelocityPlugin(app: Application) extends Plugin {

  private lazy val logger = play.api.Logger("jp.furyu.play.velocity.VelocityPlugin")
  private val VelocityPluginRuntimeProperties = "velocity_plugin.properties"

  lazy val engine: VelocityEngine = {
    // initialize velocity engine
    val prop = new Properties
    val is = this.getClass().getResourceAsStream("/" + VelocityPluginRuntimeProperties)
    if (is != null) {
      logger.info("setup engine in [%s]".format(VelocityPluginRuntimeProperties))
      prop.load(is)
    }

    val engine = new VelocityEngine(prop)
    engine.init

    engine
  }

  override def onStart() {
    logger.info("initialize engine")
    engine
  }

  override val enabled: Boolean = true
}

package object mvc {

  /**
   * marge velocity template to Html.
   *
   * @param templatePath relative path of template file to "file.resource.loader.path"
   * @param attributes request attributes (default empty)
   * @param charset encoding template charset (default utf-8)
   * @return Html
   * @throws ResourceNotFoundException not found template file
   * @throws ParseErrorException template invalid velocity format
   * @throws MethodInvocationException error occur when evaluate template in object of context
   */
  def VM(templatePath: String, attributes: Map[String, Any] = Map.empty, charset: String = "utf-8"): Html = {
    val plugin = play.api.Play.current.plugin[VelocityPlugin]
      .getOrElse(throw new IllegalStateException("VelocityPlugin not installed"))

    // create context and set attributes
    val context = new VelocityContext
    attributes.foreach { case (key, value) => context.put(key, value) }

    // evaluate template by velocity
    val writer = new StringWriter
    plugin.engine.mergeTemplate(templatePath, charset, context, writer)

    // wrap Html
    play.api.templates.HtmlFormat.raw(writer.toString)
  }

}

/**
 * Uberspect for Scala.
 *
 * <p>
 * Velocity uses introspection/reflection to access properties and methods on an object.<br>
 * Uberspect have the responsibility of it.<br>
 * This implementation of Uberspect customize for Scala.
 * </p>
 */
class ScalaUberspect extends UberspectImpl {

  import ScalaUberspect.{ ScalaMapGetExecutor, ScalaPropertyExecutor }

  override def getIterator(obj: java.lang.Object, i: Info): JavaIterator[_] = {
    def makeJavaIterator(iter: Iterator[_]) = new JavaIterator[AnyRef] {
      override def hasNext() = iter.hasNext
      override def next() = iter.next().asInstanceOf[AnyRef]
      override def remove() = throw new java.lang.UnsupportedOperationException("Remove not supported")
    }

    obj match {
      case i: Iterable[_] => makeJavaIterator(i.iterator)
      case i: Iterator[_] => makeJavaIterator(i)
      case _ => super.getIterator(obj, i)
    }
  }

  override def getPropertyGet(obj: java.lang.Object, identifier: String, i: Info): VelPropertyGet = {
    if (obj != null) {
      val claz = obj.getClass()

      val executor = obj match {
        case m: Map[_, _] => new ScalaMapGetExecutor(log, claz, identifier)
        case _ => new ScalaPropertyExecutor(log, introspector, claz, identifier)
      }

      if (executor.isAlive) {
        new VelGetterImpl(executor)
      } else {
        super.getPropertyGet(obj, identifier, i)
      }
    } else {
      null
    }
  }
}
object ScalaUberspect {

  private class ScalaPropertyExecutor(log: Log, introspector: Introspector, clazz: java.lang.Class[_], property: String) extends PropertyExecutor(log, introspector, clazz, property) {
    override def discover(clazz: java.lang.Class[_], property: String) = {
      setMethod(introspector.getMethod(clazz, property, Array[java.lang.Object]()))
      if (!isAlive()) {
        super.discover(clazz, property)
      }
    }
  }

  private class ScalaMapGetExecutor(val llog: Log, val clazz: java.lang.Class[_], val property: String) extends MapGetExecutor(llog, clazz, property) {
    override def isAlive = true
    override def execute(o: AnyRef) = o.asInstanceOf[Map[String, AnyRef]].getOrElse[AnyRef](property, null).asInstanceOf[java.lang.Object]
  }

}