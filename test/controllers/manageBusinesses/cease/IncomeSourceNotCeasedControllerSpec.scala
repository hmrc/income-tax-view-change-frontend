/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.manageBusinesses.cease

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import models.admin.IncomeSourcesNewJourney
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}

import scala.concurrent.Future

class IncomeSourceNotCeasedControllerSpec extends MockAuthActions {

  override lazy val app = applicationBuilderWithAuthBindings
    .build()

  lazy val testController = app.injector.instanceOf[IncomeSourceNotCeasedController]

  def getMessage(incomeSourceType: IncomeSourceType): String = incomeSourceType match {
    case SelfEmployment => messages("incomeSources.cease.error.SE.notCeased.text")
    case UkProperty => messages("incomeSources.cease.error.UK.notCeased.text")
    case _ => messages("incomeSources.cease.error.FP.notCeased.text")
  }
  val incomeSourceTypes = List(SelfEmployment, UkProperty, ForeignProperty)

  mtdAllRoles.foreach { mtdRole =>
    incomeSourceTypes.foreach { incomeSourceType =>
      val isAgent = mtdRole != MTDIndividual
      s"show($isAgent, $incomeSourceType, )" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        val action = testController.show(isAgent, incomeSourceType)
        s"the user is authenticated as a $mtdRole" should {
          "render the not ceased page" in {
            setupMockSuccess(mtdRole)
            enable(IncomeSourcesNewJourney)
            mockUKPropertyIncomeSource()

            val result: Future[Result] = action(fakeRequest)
            val document: Document = Jsoup.parse(contentAsString(result))

            status(result) shouldBe OK
            document.title shouldBe messages("htmlTitle.errorPage", messages("standardError.heading"))
            document.text() should include(getMessage(incomeSourceType))
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}
