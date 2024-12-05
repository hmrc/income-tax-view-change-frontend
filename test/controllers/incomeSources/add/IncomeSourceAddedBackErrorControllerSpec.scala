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

package controllers.incomeSources.add

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSources
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import org.jsoup.Jsoup
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.SessionService
import testConstants.BaseTestConstants.testSessionId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

class IncomeSourceAddedBackErrorControllerSpec extends MockAuthActions
  with MockSessionService {

  override def fakeApplication(): Application = applicationBuilderWithAuthBindings()
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService))
    .build()

  val testIncomeSourceAddedBackErrorController = fakeApplication().injector.instanceOf[IncomeSourceAddedBackErrorController]

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
      s"show${if (mtdRole != MTDIndividual) "Agent"}(incomeSourceType = ${incomeSourceType.key})" when {
        val fakeRequest = getFakeRequestBasedOnMTDUserType(mtdRole)
        val action = if (mtdRole == MTDIndividual) testIncomeSourceAddedBackErrorController.show(incomeSourceType) else testIncomeSourceAddedBackErrorController.showAgent(incomeSourceType)
        s"the user is authenticated as a $mtdRole" should {
          s"render the you cannot go back error page" in {
            enable(IncomeSources)
            setupMockSuccess(mtdRole)

            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
            mockMongo(JourneyType(Add, SelfEmployment))

            val result = action(fakeRequest)
            val document = Jsoup.parse(contentAsString(result))

            status(result) shouldBe OK
            document.title shouldBe getTitle(mtdRole != MTDIndividual)
            document.getElementById("warning-message").text() shouldBe warningMessage
          }

          s"redirect to home page" when {
            "the incomeSources is disabled" in {
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

              val result = action(fakeRequest)

              status(result) shouldBe SEE_OTHER
              val homeUrl = if (mtdRole == MTDIndividual) {
                controllers.routes.HomeController.show().url
              } else {
                controllers.routes.HomeController.showAgent.url
              }
              redirectLocation(result) shouldBe Some(homeUrl)
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }

      s"submit${if (mtdRole != MTDIndividual) "Agent"}(incomeSourceType = $incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testIncomeSourceAddedBackErrorController.submit(incomeSourceType) else testIncomeSourceAddedBackErrorController.submitAgent(incomeSourceType)
        val fakeRequest = getFakeRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
        s"the user is authenticated as a $mtdRole" should {
          s"return ${Status.SEE_OTHER} and redirect to $incomeSourceType reporting method page" in {
            disableAllSwitches()
            enable(IncomeSources)

            mockNoIncomeSources()
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
            mockMongo(JourneyType(Add, incomeSourceType))

            val result = action(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourceReportingMethodController.show(mtdRole != MTDIndividual, incomeSourceType).url)
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}
