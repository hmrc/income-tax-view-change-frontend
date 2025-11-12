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

package controllers.manageBusinesses.add

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType, JourneyType}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.UIJourneySessionData
import models.incomeSourceDetails.AddIncomeSourceData
import org.jsoup.Jsoup
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.SessionService
import testConstants.BaseTestConstants.testSessionId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

class IncomeSourceAddedBackErrorControllerSpec extends MockAuthActions with MockSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService))
    .build()

  lazy val testIncomeSourceAddedBackErrorController = app.injector.instanceOf[IncomeSourceAddedBackErrorController]

  val title: String = messages("cannotGoBack.heading")
  val warningMessage: String = s"! Warning ${messages("cannotGoBack.warningMessage")}"

  def getTitle(isAgent: Boolean): String = {
    if (isAgent) messages("htmlTitle.agent", s"$title") else messages("htmlTitle", s"$title")
  }

  def sessionData(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceId = Some("1234"))))

  def mockMongo(journeyType: JourneyType): Unit = {
    setupMockGetMongo(Right(Some(sessionData(journeyType))))
    setupMockGetSessionKeyMongoTyped[String](Right(Some("1234")))
  }

  val incomeSourceTypes: List[IncomeSourceType] = List(SelfEmployment, UkProperty, ForeignProperty)

  incomeSourceTypes.foreach { incomeSourceType =>
    mtdAllRoles.foreach { mtdRole =>
      s"show${if (mtdRole != MTDIndividual) "Agent"}(incomeSourceType = $incomeSourceType)" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        val action = if (mtdRole == MTDIndividual) testIncomeSourceAddedBackErrorController.show(incomeSourceType) else testIncomeSourceAddedBackErrorController.showAgent(incomeSourceType)
        s"the user is authenticated as a $mtdRole" should {
          s"render the you cannot go back error page" in {
            setupMockSuccess(mtdRole)

            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockMongo(IncomeSourceJourneyType(Add, SelfEmployment))

            val result = action(fakeRequest)
            val document = Jsoup.parse(contentAsString(result))

            status(result) shouldBe OK
            document.title shouldBe getTitle(mtdRole != MTDIndividual)
            document.getElementById("warning-message").text() shouldBe warningMessage
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }

      s"submit${if (mtdRole != MTDIndividual) "Agent"}(incomeSourceType = $incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testIncomeSourceAddedBackErrorController.submit(incomeSourceType) else testIncomeSourceAddedBackErrorController.submitAgent(incomeSourceType)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
        s"the user is authenticated as a $mtdRole" should {
          s"return ${Status.SEE_OTHER} and redirect to $incomeSourceType reporting method page" in {
            disableAllSwitches()

            mockNoIncomeSources()
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockMongo(IncomeSourceJourneyType(Add, incomeSourceType))

            val result = action(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.manageBusinesses.add.routes.IncomeSourceReportingFrequencyController.show(mtdRole != MTDIndividual, false, incomeSourceType).url)
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}