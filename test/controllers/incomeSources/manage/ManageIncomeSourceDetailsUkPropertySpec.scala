/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.incomeSources.manage

import enums.IncomeSourceJourney.UkProperty
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.MTDIndividual
import models.admin.IncomeSourcesFs
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.test.Helpers._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{emptyUIJourneySessionData, notCompletedUIJourneySessionData}

class ManageIncomeSourceDetailsUkPropertyISpec extends ManageIncomeSourceDetailsHelper {

  mtdAllRoles.foreach { mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    s"show($isAgent, $UkProperty)" when {
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
      val action = testController.show(isAgent, UkProperty, None)
      s"the user is authenticated as a $mtdUserRole" should {
        "render the appropriate IncomeSourceDetails page" when {
          "the user has a valid id parameter and no latency information" in {
            enable(IncomeSourcesFs)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)
            setupMockGetCurrentTaxYearEnd(2024)
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            mockUkPlusForeignPlusSoleTraderNoLatency()
            setupMockCreateSession(true)
            setupMockSetSessionKeyMongo(Right(true))

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe title(mtdUserRole)
            getHeading(document) shouldBe heading
            hasChangeFirstYearReportingMethodLink(document) shouldBe false
            hasChangeSecondYearReportingMethodLink(document) shouldBe false
            getManageDetailsSummaryValues(document).get(2).text() shouldBe standard
          }

          "the user has a valid id parameter, valid latency information and two tax years not crystallised" in {
            enable(IncomeSourcesFs)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)
            setupMockGetCurrentTaxYearEnd(2023)
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            mockUkPlusForeignPlusSoleTraderWithLatency()
            setupMockTaxYearNotCrystallised(2023)
            setupMockTaxYearNotCrystallised(2024)

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe title(mtdUserRole)
            getHeading(document) shouldBe heading
            hasChangeFirstYearReportingMethodLink(document) shouldBe true
            hasChangeSecondYearReportingMethodLink(document) shouldBe true
            val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
            manageDetailsSummaryValues.get(2).text() shouldBe calendar
            manageDetailsSummaryValues.get(3).text() shouldBe quarterly
            manageDetailsSummaryValues.get(4).text() shouldBe annually
          }

          "the user has a valid id parameter, valid latency information and two tax years crystallised" in {
            enable(IncomeSourcesFs)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)
            setupMockGetCurrentTaxYearEnd(2023)
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            mockUkPlusForeignPlusSoleTraderWithLatency()
            setupMockTaxYearCrystallised(2023)
            setupMockTaxYearCrystallised(2024)

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe title(mtdUserRole)
            getHeading(document) shouldBe heading
            hasChangeFirstYearReportingMethodLink(document) shouldBe false
            hasChangeSecondYearReportingMethodLink(document) shouldBe false
            val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
            manageDetailsSummaryValues.get(2).text() shouldBe calendar
            manageDetailsSummaryValues.get(3).text() shouldBe quarterly
            manageDetailsSummaryValues.get(4).text() shouldBe annually
          }

          "the user has a valid id parameter, but non eligable itsa status" in {
            enable(IncomeSourcesFs)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)

            setupMockGetCurrentTaxYearEnd(2023)
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(false)
            mockUkPlusForeignPlusSoleTrader2023WithLatencyAndUnknowns()

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)

            document.title shouldBe title(mtdUserRole)
            getHeading(document) shouldBe heading
            hasChangeFirstYearReportingMethodLink(document) shouldBe false
            hasChangeSecondYearReportingMethodLink(document) shouldBe false
            manageDetailsSummaryValues.get(0).text() shouldBe unknown
            manageDetailsSummaryValues.get(1).text() shouldBe "Traditional accounting"
          }

          "the user has a valid id parameter, latency expired" in {
            enable(IncomeSourcesFs)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)

            setupMockGetCurrentTaxYearEnd(2025)
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            mockUkPlusForeignPlusSoleTraderWithLatency()
            setupMockTaxYearNotCrystallised(2023)
            setupMockTaxYearNotCrystallised(2024)

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe title(mtdUserRole)
            getHeading(document) shouldBe heading
            hasChangeFirstYearReportingMethodLink(document) shouldBe true
            hasChangeSecondYearReportingMethodLink(document) shouldBe true
            val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
            manageDetailsSummaryValues.get(2).text() shouldBe calendar
            manageDetailsSummaryValues.get(3).text() shouldBe quarterly
            manageDetailsSummaryValues.get(4).text() shouldBe annually
          }
        }

        "redirect to the home page" when {
          "incomeSources FS is disabled" in {
            disable(IncomeSourcesFs)
            setupMockSuccess(mtdUserRole)
            mockBothPropertyBothBusiness()
            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))
            setupMockSetSessionKeyMongo(Right(true))

            val result = action(fakeRequest)

            status(result) shouldBe Status.SEE_OTHER
            val homeUrl = if (isAgent) {
              controllers.routes.HomeController.showAgent().url
            } else {
              controllers.routes.HomeController.show().url
            }
            redirectLocation(result) shouldBe Some(homeUrl)
          }
        }

        "render the error page" when {
          "the user has no income source of the called type" in {
            enable(IncomeSourcesFs)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)
            mockSingleBusinessIncomeSource()

            setupMockGetCurrentTaxYearEnd(2023)
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(false)

            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

            val result = action(fakeRequest)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdUserRole)(fakeRequest)
    }
  }
}
