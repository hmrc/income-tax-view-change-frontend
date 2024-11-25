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

import enums.{MTDIndividual, MTDSupportingAgent}
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSources
import models.incomeSourceDetails.viewmodels.AddIncomeSourcesViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import services.SessionService
import testConstants.BusinessDetailsTestConstants.{businessDetailsViewModel, businessDetailsViewModel2, ceasedBusinessDetailsViewModel}
import testConstants.PropertyDetailsTestConstants.{foreignPropertyDetailsViewModel, ukPropertyDetailsViewModel}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future
import scala.util.{Failure, Success}

class AddIncomeSourceControllerSpec extends MockAuthActions
  with ImplicitDateFormatter
  with MockSessionService {

  override def fakeApplication(): Application = applicationBuilderWithAuthBindings()
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  val controller = fakeApplication().injector.instanceOf[AddIncomeSourceController]

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    s"show${if (mtdRole != MTDIndividual) "Agent"}" when {
      val action = if (mtdRole == MTDIndividual) controller.show() else controller.showAgent()
      s"the user is authenticated as a $mtdRole" should {
        s"render the add income source page" when {
          "the user has a Sole Trader Business, a UK property and a Foreign Property" in {
            enable(IncomeSources)
            ukPlusForeignPropertyWithSoleTraderIncomeSource()
            setupMockSuccess(mtdRole)
            setupMockDeleteSession(true)
            when(mockIncomeSourceDetailsService.getAddIncomeSourceViewModel(any()))
              .thenReturn(Success(AddIncomeSourcesViewModel(
                soleTraderBusinesses = List(businessDetailsViewModel, businessDetailsViewModel2),
                ukProperty = Some(ukPropertyDetailsViewModel),
                foreignProperty = None,
                ceasedBusinesses = Nil)))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
          }
        }

        "render the add income source page with no tables or table paragraph text" when {
          "user has no businesses or properties" in {
            disableAllSwitches()
            enable(IncomeSources)
            mockNoIncomeSources()
            setupMockSuccess(mtdRole)
            setupMockDeleteSession(true)
            when(mockIncomeSourceDetailsService.getAddIncomeSourceViewModel(any()))
              .thenReturn(Success(AddIncomeSourcesViewModel(Nil, None, None, Nil)))

            val result = action(fakeRequest)

            val doc: Document = Jsoup.parse(contentAsString(result))

            Option(doc.getElementById("sole-trader-businesses-table")).isDefined shouldBe false

            Option(doc.getElementById("uk-property-table")).isDefined shouldBe false

            Option(doc.getElementById("foreign-property-table")).isDefined shouldBe false

            Option(doc.getElementById("ceased-businesses-table")).isDefined shouldBe false

            Option(doc.getElementById("uk-property-p1")).isDefined shouldBe false
            Option(doc.getElementById("foreign-property-p1")).isDefined shouldBe false
          }
        }
        "render the add income source page with all tables showing" when {
          "user has a ceased business, sole trader business and uk/foreign property" in {
            enable(IncomeSources)
            mockBothPropertyBothBusiness()
            setupMockSuccess(mtdRole)

            setupMockDeleteSession(true)
            when(mockIncomeSourceDetailsService.getAddIncomeSourceViewModel(any()))
              .thenReturn(Success(AddIncomeSourcesViewModel(
                soleTraderBusinesses = List(businessDetailsViewModel),
                ukProperty = Some(ukPropertyDetailsViewModel),
                foreignProperty = Some(foreignPropertyDetailsViewModel),
                ceasedBusinesses = List(ceasedBusinessDetailsViewModel))))

            val result = action(fakeRequest)

            val doc: Document = Jsoup.parse(contentAsString(result))

            Option(doc.getElementById("sole-trader-businesses-table")).isDefined shouldBe true

            Option(doc.getElementById("uk-property-table")).isDefined shouldBe true

            Option(doc.getElementById("foreign-property-table")).isDefined shouldBe true

            Option(doc.getElementById("ceased-businesses-table")).isDefined shouldBe true

            Option(doc.getElementById("uk-property-p1")).isDefined shouldBe true
            Option(doc.getElementById("foreign-property-p1")).isDefined shouldBe true
          }
        }

        "redirect to the homepage" when {
          "incomeSource feature is disabled" in {
            setupMockSuccess(mtdRole)
            mockBothPropertyBothBusiness()

            val result: Future[Result] = action(fakeRequest)
            status(result) shouldBe SEE_OTHER
            val homeUrl = if (mtdRole == MTDIndividual) {
              controllers.routes.HomeController.show().url
            } else {
              controllers.routes.HomeController.showAgent.url
            }
            redirectLocation(result) shouldBe Some(homeUrl)
          }
        }

        "show error page" when {
          s"failed to return incomeSourceViewModel" in {
            enable(IncomeSources)
            mockUkPropertyWithSoleTraderBusiness()
            setupMockSuccess(mtdRole)

            when(mockIncomeSourceDetailsService.getAddIncomeSourceViewModel(any()))
              .thenReturn(Failure(new Exception("UnknownError")))

            val result = action(fakeRequest)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
      }

      if (mtdRole == MTDIndividual) {
        testMTDIndividualAuthFailures(action)
      } else {
        testMTDAgentAuthFailures(action, mtdRole == MTDSupportingAgent)
      }
    }
  }
}