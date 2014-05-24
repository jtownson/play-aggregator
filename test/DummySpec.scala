import org.specs2.mutable._
import play.libs.Json

/**
 *
 */
class DummySpec extends Specification {

  "This test" should {
    "escape the json for me" in {
      val urlMap = Map(
        ("localhost:9000/business-tier/time", "http://localhost:9000/cms/$site/time.vm"),
        ("localhost:9000/business-tier/count", "http://localhost:9000/cms/$site/count.vm"),
        ("localhost:9000/business-tier/article", "http://localhost:9000/cms/$site/article.vm"))

      val jsonObject = Json.toJson(urlMap)

      println(jsonObject)

      "Hello world" must have size(11)
    }
  }

}
