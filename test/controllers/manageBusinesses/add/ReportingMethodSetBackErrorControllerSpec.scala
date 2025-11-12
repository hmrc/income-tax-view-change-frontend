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
import play.api.http.Status.OK
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services.SessionService
import testConstants.BaseTestConstants.testSessionId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

class ReportingMethodSetBackErrorControllerSpec extends MockAuthActions with MockSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[ReportingMethodSetBackErrorController]

  val title: String = messages("cannotGoBack.heading")
  val messageSE: String = messages("cannotGoBack.soleTraderAdded")
  val messageUK: String = messages("cannotGoBack.ukPropertyAdded")
  val messageFP: String = messages("cannotGoBack.foreignPropertyAdded")

  def getTitle(incomeSourceType: IncomeSourceType, isAgent: Boolean): String = {
    (isAgent, incomeSourceType) match {
      case (false, _) => messages("htmlTitle", s"$title")
      case (true, _) => messages("htmlTitle.agent", s"$title")
    }
  }

  def getSubHeading(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => messageSE
      case UkProperty => messageUK
      case ForeignProperty => messageFP
    }
  }


  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  def sessionData(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceId = Some("1234"))))

  def mockMongo(journeyType: JourneyType): Unit = {
    setupMockGetMongo(Right(Some(sessionData(journeyType))))
    setupMockGetSessionKeyMongoTyped[String](Right(Some("1234")))
  }

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    incomeSourceTypes.foreach { incomeSourceType =>
      s"show${if (mtdRole != MTDIndividual) "Agent"}($incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testController.show(incomeSourceType) else testController.showAgent(incomeSourceType)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "render the you cannot go back error page" in {
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            mockMongo(IncomeSourceJourneyType(Add, incomeSourceType))

            val result = action(fakeRequest)

            status(result) shouldBe OK
            val document = Jsoup.parse(contentAsString(result))
            document.title shouldBe getTitle(incomeSourceType, isAgent)
            document.getElementById("subheading").text() shouldBe getSubHeading(incomeSourceType)
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}
