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

import controllers.routes
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSourcesNewJourney
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers._
import play.api
import play.api.Application
import play.api.mvc.Result
import play.api.test.Helpers._
import services.SessionService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, ukPlusForeignPropertyAndSoleTraderNoLatency}

import scala.concurrent.Future

class IncomeSourceReportingMethodNotSavedControllerSpec extends MockAuthActions with MockSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[IncomeSourceReportingMethodNotSavedController]

  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  def getParagraphText(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => TestConstants.paragraphTextSelfEmployment
      case UkProperty => TestConstants.paragraphTextUkProperty
      case ForeignProperty => TestConstants.paragraphTextForeignProperty
    }
  }

  def getContinueButtonLink(incomeSourceType: IncomeSourceType, isAgent: Boolean): String = {
    (incomeSourceType, isAgent) match {
      case (SelfEmployment, false) => TestConstants.selfEmploymentAddedUrl
      case (UkProperty, false) => TestConstants.ukPropertyAddedUrl
      case (ForeignProperty, false) => TestConstants.foreignPropertyAddedUrl
      case (SelfEmployment, true) => TestConstants.selfEmploymentAddedAgentUrl
      case (UkProperty, true) => TestConstants.ukPropertyAddedAgentUrl
      case (ForeignProperty, true) => TestConstants.foreignPropertyAddedAgentUrl
    }
  }

  def getPageTitle(isAgent: Boolean): String = {
    if (isAgent) {
      TestConstants.titleAgent
    } else {
      TestConstants.titleIndividual
    }
  }

  object TestConstants {
    val title: String = messages("incomeSources.add.error.standardError")
    val titleAgent: String = s"${messages("htmlTitle.agent", title)}"
    val titleIndividual: String = s"${messages("htmlTitle", title)}"

    val selfEmployment: String = messages("incomeSources.add.error.reportingMethodNotSaved.se.incomeSource")
    val ukProperty: String = messages("incomeSources.add.error.reportingMethodNotSaved.uk.incomeSource")
    val foreignProperty: String = messages("incomeSources.add.error.reportingMethodNotSaved.fp.incomeSource")
    val paragraphTextSelfEmployment: String = messages("incomeSources.add.error.reportingMethodNotSaved.p1", selfEmployment)
    val paragraphTextUkProperty: String = messages("incomeSources.add.error.reportingMethodNotSaved.p1", ukProperty)
    val paragraphTextForeignProperty: String = messages("incomeSources.add.error.reportingMethodNotSaved.p1", foreignProperty)

    val selfEmploymentAddedUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceAddedController.show(SelfEmployment).url
    val ukPropertyAddedUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceAddedController.show(UkProperty).url
    val foreignPropertyAddedUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceAddedController.show(ForeignProperty).url

    val selfEmploymentAddedAgentUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceAddedController.showAgent(SelfEmployment).url
    val ukPropertyAddedAgentUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceAddedController.showAgent(UkProperty).url
    val foreignPropertyAddedAgentUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceAddedController.showAgent(ForeignProperty).url
  }

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    incomeSourceTypes.foreach { incomeSourceType =>
      s"show${if (mtdRole != MTDIndividual) "Agent"}($incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testController.show(incomeSourceType) else testController.showAgent(incomeSourceType)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "render the reporting method not saved page" when {
            s"income sources feature is enabled" in {
              enable(IncomeSourcesNewJourney)
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(ukPlusForeignPropertyAndSoleTraderNoLatency)

              val result: Future[Result] = action(fakeRequest)

              val document: Document = Jsoup.parse(contentAsString(result))
              status(result) mustBe OK
              status(result) mustBe OK
              document.title shouldBe getPageTitle(isAgent)
              document.getElementById("paragraph-1").text() shouldBe getParagraphText(incomeSourceType)
              document.getElementById("continue-button").attr("href") shouldBe getContinueButtonLink(incomeSourceType, isAgent)
            }
          }

          s"redirect to the home page" when {
            "when feature switch is disabled" in {
              disable(IncomeSourcesNewJourney)
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              val result: Future[Result] = action(fakeRequest)

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) mustBe (if (isAgent) Some(routes.HomeController.showAgent().url) else Some(routes.HomeController.show().url))
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}
