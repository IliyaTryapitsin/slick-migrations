package slick.migration.dialect

import scala.collection.JavaConverters._
import com.typesafe.config._

object TestConfig {
  val ref = ConfigFactory.parseResources(getClass, "/test-dialects.conf")
  val defaults = ref.getObject("defaults").toConfig

  def getStrings(config: Config, path: String): Option[Seq[String]] = {
    if (config.hasPath(path)) {
      config.getValue(path).unwrapped() match {
        case l: java.util.List[_] => Some(l.asScala.map(_.toString))
        case o                    => Some(List(o.toString))
      }
    } else None
  }

  def testConfig(name: String) = {
    val c = if (ref.hasPath(name)) ref.getConfig(name).withFallback(defaults) else defaults
    c.resolve()
  }
}

