package views.incomeSources.cease

import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.incomeSources.cease.CheckCeaseUKPropertyDetails

class CheckCeaseUKPropertyDetailsViewSpec extends TestSupport{
  val checkCeaseUKPropertyDetailsView = app.injector.instanceOf[CheckCeaseUKPropertyDetails]

  class Setup(isAgent: Boolean, error: Boolean = false) {
    lazy val view: HtmlFormat.Appendable = checkCeaseUKPropertyDetailsView(isAgent)(FakeRequest(), implicitly)
  }

  "CheckCeaseUKPropertyDetailsView - Individual" should {

  }
}
