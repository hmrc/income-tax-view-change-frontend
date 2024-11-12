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

package controllers

import enums.{MTDPrimaryAgent, MTDSupportingAgent}
import models.admin.{CreditsRefundsRepay, IncomeSources, IncomeSourcesNewJourney, ReviewAndReconcilePoa}
import models.financialDetails._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.Injecting
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, businessesAndPropertyIncomeCeased}

import java.time.LocalDate
import scala.concurrent.Future

class HomeControllerSupportingAgentSpec extends HomeControllerHelperSpec with Injecting {

  val testHomeController = fakeApplication().injector.instanceOf[HomeController]

  trait Setup {
    val controller = testHomeController
    when(mockDateService.getCurrentDate) thenReturn fixedDate

    lazy val homePageTitle = s"${messages("htmlTitle.agent", messages("home.agent.heading"))}"
    lazy val homePageCaption = "You are signed in as a supporting agent"
    lazy val homePageHeading = s"${messages("home.agent.headingWithClientName", "Test User")}"

  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
  }


  "show()" when {
    val agentType = MTDSupportingAgent
    val isSupportingAgent = true
    val fakeRequest = fakeRequestConfirmedClient(isSupportingAgent = true)
    s"the user is authenticated $agentType" should {
      "render the home page controller with the next updates tile" when {
        "there is a future update date to display" in new Setup {
          setupMockAgentWithClientAuth(true)
          mockGetDueDates(Right(futureDueDates))
          mockSingleBusinessIncomeSource()

          val result: Future[Result] = controller.showAgent()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("h1").text() shouldBe homePageHeading
          document.getElementsByClass("govuk-caption-xl").text() shouldBe homePageCaption
          document.select("#updates-tile p:nth-child(2)").text() shouldBe "1 January 2100"
        }

        "there is an overdue update date to display" in new Setup {
          setupMockAgentWithClientAuth(true)
          mockGetDueDates(Right(overdueDueDates))
          mockSingleBusinessIncomeSource()

          val result: Future[Result] = controller.showAgent()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#updates-tile p:nth-child(2)").text() shouldBe "OVERDUE 1 January 2018"
        }

        "there are no updates to display" in new Setup {
          setupMockAgentWithClientAuth(true)
          mockGetDueDates(Right(Seq()))
          mockSingleBusinessIncomeSource()

          val result: Future[Result] = controller.showAgent()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#updates-tile").text() shouldBe messages("home.updates.heading")
        }
      }

      "render the home without the Next Updates tile" when {
        "the user has no updates due" in new Setup {
          setupMockAgentWithClientAuth(true)
          mockSingleBusinessIncomeSource()
          mockGetDueDates(Right(Seq()))

          val result: Future[Result] = controller.showAgent()(fakeRequest)
          status(result) shouldBe Status.OK

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#updates-tile").text shouldBe messages("home.updates.heading")
        }
      }

      "render the home page with the Income Sources tile" that {
        "has `Cease an income source`" when {
          "the user has non-ceased businesses or property and income sources is enabled" in new Setup {
            setupMockAgentWithClientAuth(isSupportingAgent)
            enable(IncomeSources)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
            mockGetDueDates(Right(futureDueDates))

            val result: Future[Result] = controller.showAgent()(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe homePageTitle
            document.select("#income-sources-tile h2:nth-child(1)").text() shouldBe messages("home.incomeSources.heading")
            document.select("#income-sources-tile > div > p:nth-child(2) > a").text() shouldBe messages("home.incomeSources.addIncomeSource.view")
            document.select("#income-sources-tile > div > p:nth-child(2) > a").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
            document.select("#income-sources-tile > div > p:nth-child(3) > a").text() shouldBe messages("home.incomeSources.manageIncomeSource.view")
            document.select("#income-sources-tile > div > p:nth-child(3) > a").attr("href") shouldBe controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true).url
            document.select("#income-sources-tile > div > p:nth-child(4) > a").text() shouldBe messages("home.incomeSources.ceaseIncomeSource.view")
            document.select("#income-sources-tile > div > p:nth-child(4) > a").attr("href") shouldBe controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url
          }
        }

        "does not have a `Cease an income source`" when {
          "the user has ceased businesses or property and income sources is enabled" in new Setup {
            setupMockAgentWithClientAuth(isSupportingAgent)
            enable(IncomeSources)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncomeCeased)
            mockGetDueDates(Right(futureDueDates))

            val result: Future[Result] = controller.showAgent()(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe homePageTitle
            document.select("#income-sources-tile h2:nth-child(1)").text() shouldBe messages("home.incomeSources.heading")
            document.select("#income-sources-tile > div > p:nth-child(2) > a").text() shouldBe messages("home.incomeSources.addIncomeSource.view")
            document.select("#income-sources-tile > div > p:nth-child(2) > a").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
            document.select("#income-sources-tile > div > p:nth-child(3) > a").text() shouldBe messages("home.incomeSources.manageIncomeSource.view")
            document.select("#income-sources-tile > div > p:nth-child(3) > a").attr("href") shouldBe controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true).url
            document.select("#income-sources-tile > div > p:nth-child(4) > a").text() should not be messages("home.incomeSources.ceaseIncomeSource.view")
            document.select("#income-sources-tile > div > p:nth-child(4) > a").attr("href") should not be controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url
          }
        }
      }

      "render the home page with the Your Businesses tile with link" when {
        "the IncomeSourcesNewJourney is enabled" in new Setup {
          setupMockAgentWithClientAuth(isSupportingAgent)
          enable(IncomeSources)
          enable(IncomeSourcesNewJourney)
          mockGetDueDates(Right(futureDueDates))
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val result: Future[Result] = controller.showAgent()(fakeRequest)
          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#income-sources-tile h2:nth-child(1)").text() shouldBe messages("home.incomeSources.newJourneyHeading")
          document.select("#income-sources-tile > div > p:nth-child(2) > a").text() shouldBe messages("home.incomeSources.newJourney.view")
          document.select("#income-sources-tile > div > p:nth-child(2) > a").attr("href") shouldBe controllers.manageBusinesses.routes.ManageYourBusinessesController.show(true).url
        }
      }

      "render the home page without a Next Payments due tile" in new Setup {
        setupMockAgentWithClientAuth(true)
        mockGetDueDates(Right(overdueDueDates))
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = controller.showAgent()(fakeRequest)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("h1").text() shouldBe homePageHeading
        document.getElementsByClass("govuk-caption-xl").text() shouldBe homePageCaption
        document.getElementById("payments-tile") shouldBe null
      }

      "render the home page without a Payment history tile" in new Setup {
        setupMockAgentWithClientAuth(true)
        mockGetDueDates(Right(overdueDueDates))
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = controller.showAgent()(fakeRequest)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("h1").text() shouldBe homePageHeading
        document.getElementsByClass("govuk-caption-xl").text() shouldBe homePageCaption
        document.getElementById("payment-history-tile") shouldBe null
      }

      "render the home page without a returns tile" in new Setup {
        setupMockAgentWithClientAuth(true)
        mockGetDueDates(Right(overdueDueDates))
        mockSingleBusinessIncomeSource()

        val result: Future[Result] = controller.showAgent()(fakeRequest)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("h1").text() shouldBe homePageHeading
        document.getElementsByClass("govuk-caption-xl").text() shouldBe homePageCaption
        document.getElementById("returns-tile") shouldBe null
      }
    }

    testMTDAgentAuthFailures(testHomeController.showAgent(), true)
  }
}