/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package views

import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.errorPages.UpliftFailed

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`

class UpliftFailedViewSpec extends ViewSpec {

  def upliftFailedView: Html = app.injector.instanceOf[UpliftFailed].apply()

  "The Uplift Failed Error page" should {

    s"have the heading: ${messages("upliftFailure.title")}" in new Setup(upliftFailedView) {

      document hasPageHeading messages("upliftFailure.title")
    }

    s"have the content ${messages("upliftFailure.content")}" in new Setup(upliftFailedView) {

      layoutContent.select(Selectors.p).text shouldBe messages("upliftFailure.content")
    }

    "not have a back link" in new Setup(upliftFailedView) {

      document doesNotHave Selectors.backLink
    }

    "have the list elements" in new Setup(upliftFailedView) {

      document.select("li").get(2).text() shouldBe messages("upliftFailure.bullet.1")
      document.select("li").get(3).text() shouldBe messages("upliftFailure.bullet.2")
      document.select("li").get(4).text() shouldBe messages("upliftFailure.bullet.3")
    }

    s"have the link ${messages("Return to Sign in")}" in new Setup(upliftFailedView){

      document.select("a").eachText().toList.contains(messages("Return to Sign in"))
    }
  }

}
