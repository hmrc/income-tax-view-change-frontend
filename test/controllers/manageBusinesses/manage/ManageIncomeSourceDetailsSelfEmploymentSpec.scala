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

package controllers.manageBusinesses.manage

import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.MTDIndividual
import models.admin.{AccountingMethodJourney, DisplayBusinessStartDate, OptInOptOutContentUpdateR17, ReportingFrequencyPage}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.test.Helpers._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{emptyUIJourneySessionData, notCompletedUIJourneySessionData}

class ManageIncomeSourceDetailsSelfEmploymentSpec extends ManageIncomeSourceDetailsHelper {

  mtdAllRoles.foreach { mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    s"show($isAgent, $SelfEmployment)" when {
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
      val action = testController.show(isAgent, SelfEmployment, Some(incomeSourceIdHash))
      s"the user is authenticated as a $mtdUserRole" should {
        "render the appropriate IncomeSourceDetails page" when {
          "the user has a valid id parameter and no latency information" in {
            enable(DisplayBusinessStartDate, AccountingMethodJourney, ReportingFrequencyPage)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)

            setupMockGetCurrentTaxYearEnd(2024)
            mockUkPlusForeignPlusSoleTraderNoLatency()
            setupMockCreateSession(true)
            setupMockSetSessionKeyMongo(Right(true))

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment)))))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe title()
            getHeading(document) shouldBe heading
            hasChangeFirstYearReportingMethodLink(document) shouldBe false
            hasChangeSecondYearReportingMethodLink(document) shouldBe false
            hasGracePeriodInfo(document) shouldBe false
            val manageDetailsSummaryRows = getManageDetailsSummaryRows(document)

            manageDetailsSummaryRows.get(1).getElementsByTag("dt").text() shouldBe "Address"
            manageDetailsSummaryRows.get(1).getElementsByTag("dd").text() shouldBe businessWithLatencyAddress
            getManageDetailsSummaryValues(document).get(5).text() shouldBe calendar
            manageDetailsSummaryRows.eq(6).isEmpty
            manageDetailsSummaryRows.eq(7).isEmpty
            document.getElementById("reportingFrequency").text() shouldBe "View and change your reporting frequency for all your businesses"
            Option(document.getElementById("up-to-two-tax-years")) shouldBe None
          }

          "the user has a valid id parameter and latency information but user is not in latency period" in {
            enable(DisplayBusinessStartDate, AccountingMethodJourney, ReportingFrequencyPage)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)

            setupMockGetCurrentTaxYearEnd(2024)
            mockUkPlusForeignPlusSoleTraderWithLatencyExpired()
            setupMockCreateSession(true)
            setupMockSetSessionKeyMongo(Right(true))

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment)))))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe title()
            getHeading(document) shouldBe heading
            hasChangeFirstYearReportingMethodLink(document) shouldBe false
            hasChangeSecondYearReportingMethodLink(document) shouldBe false
            hasGracePeriodInfo(document) shouldBe false
            val manageDetailsSummaryRows = getManageDetailsSummaryRows(document)

            manageDetailsSummaryRows.get(1).getElementsByTag("dt").text() shouldBe "Address"
            manageDetailsSummaryRows.get(1).getElementsByTag("dd").text() shouldBe businessWithLatencyAddress2
            manageDetailsSummaryRows.eq(5).isEmpty
            manageDetailsSummaryRows.eq(6).isEmpty
            manageDetailsSummaryRows.eq(7).isEmpty
            document.getElementById("reportingFrequency").text() shouldBe "View and change your reporting frequency for all your businesses"
          }

          "the user has a valid id parameter, valid latency information and two tax years not crystallised" in {
            enable(DisplayBusinessStartDate, AccountingMethodJourney, ReportingFrequencyPage)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)

            setupMockGetCurrentTaxYearEnd(2023)
            setupMockLatencyYearsQuarterlyAndAnnualStatus(true, true)
            mockUkPlusForeignPlusSoleTraderWithLatency()
            setupMockTaxYearNotCrystallised(2023)
            setupMockTaxYearNotCrystallised(2024)

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment)))))
            setupMockSetSessionKeyMongo(Right(true))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe title()
            getHeading(document) shouldBe heading
            hasChangeFirstYearReportingMethodLink(document) shouldBe true
            hasChangeSecondYearReportingMethodLink(document) shouldBe true
            hasInsetText(document) shouldBe true
            val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
            val manageDetailsSummaryKeys = getManageDetailsSummaryKeys(document)
            manageDetailsSummaryKeys.get(1).text() shouldBe "Address"
            manageDetailsSummaryValues.get(1).text() shouldBe businessWithLatencyAddress
            manageDetailsSummaryValues.get(5).text() shouldBe annuallyGracePeriod
            manageDetailsSummaryValues.get(6).text() shouldBe quarterlyGracePeriod
            manageDetailsSummaryKeys.eq(6).text().contains(reportingMethod)
            manageDetailsSummaryKeys.eq(7).text().contains(reportingMethod)
          }

          "valid latency information and two tax years not crystallised and ITSA status for TY2 is Annual but Latency TY2 is Q" in {
            enable(DisplayBusinessStartDate, AccountingMethodJourney, ReportingFrequencyPage)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)

            setupMockGetCurrentTaxYearEnd(2023)
            setupMockLatencyYearsQuarterlyAndAnnualStatus(true, false)
            mockUkPlusForeignPlusSoleTraderWithLatency()
            setupMockTaxYearNotCrystallised(2023)
            setupMockTaxYearNotCrystallised(2024)

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment)))))
            setupMockSetSessionKeyMongo(Right(true))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe title()
            getHeading(document) shouldBe heading
            hasChangeFirstYearReportingMethodLink(document) shouldBe true
            hasChangeSecondYearReportingMethodLink(document) shouldBe true
            hasInsetText(document) shouldBe true
            val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
            val manageDetailsSummaryKeys = getManageDetailsSummaryKeys(document)
            manageDetailsSummaryKeys.get(1).text() shouldBe "Address"
            manageDetailsSummaryValues.get(1).text() shouldBe businessWithLatencyAddress
            manageDetailsSummaryValues.eq(5).isEmpty
            manageDetailsSummaryValues.eq(6).isEmpty
          }

          "valid latency information and two tax years not crystallised and ITSA status for TY2 is Annual but Latency TY2 is A" in {
            enable(DisplayBusinessStartDate, AccountingMethodJourney, ReportingFrequencyPage)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)

            setupMockGetCurrentTaxYearEnd(2023)
            setupMockLatencyYearsQuarterlyAndAnnualStatus(true, false)
            mockUkPlusForeignPlusSoleTraderWithLatencyAnnual()
            setupMockTaxYearNotCrystallised(2023)
            setupMockTaxYearNotCrystallised(2024)

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment)))))
            setupMockSetSessionKeyMongo(Right(true))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe title()
            getHeading(document) shouldBe heading
            hasChangeFirstYearReportingMethodLink(document) shouldBe true
            hasChangeSecondYearReportingMethodLink(document) shouldBe false
            hasInsetText(document) shouldBe true
            val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
            val manageDetailsSummaryKeys = getManageDetailsSummaryKeys(document)
            manageDetailsSummaryKeys.get(1).text() shouldBe "Address"
            manageDetailsSummaryValues.get(1).text() shouldBe businessWithLatencyAddress2
            manageDetailsSummaryValues.eq(5).isEmpty
            manageDetailsSummaryValues.eq(6).isEmpty
          }

          "the user has a valid id parameter, valid latency information and two tax years crystallised" in {
            enable(ReportingFrequencyPage)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)

            setupMockGetCurrentTaxYearEnd(2023)
            setupMockLatencyYearsQuarterlyAndAnnualStatus(true, true)
            mockUkPlusForeignPlusSoleTraderWithLatency()
            setupMockTaxYearCrystallised(2023)
            setupMockTaxYearCrystallised(2024)

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment)))))
            setupMockSetSessionKeyMongo(Right(true))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe title()
            getHeading(document) shouldBe heading
            hasChangeFirstYearReportingMethodLink(document) shouldBe false
            hasChangeSecondYearReportingMethodLink(document) shouldBe false
            hasInsetText(document) shouldBe true
            val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
            getManageDetailsSummaryKeys(document).get(1).text() shouldBe "Address"
            manageDetailsSummaryValues.get(1).text() shouldBe businessWithLatencyAddress
            manageDetailsSummaryValues.eq(5).isEmpty
            manageDetailsSummaryValues.eq(6).isEmpty
          }

          "the user has a valid id parameter, but non eligible itsa status" in {
            enable(DisplayBusinessStartDate, AccountingMethodJourney, ReportingFrequencyPage)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)

            setupMockGetCurrentTaxYearEnd(2023)
            setupMockLatencyYearsQuarterlyAndAnnualStatus(false, false)
            setupMockTaxYearCrystallised(2023)
            setupMockTaxYearCrystallised(2024)
            mockUkPlusForeignPlusSoleTrader2023WithLatencyAndUnknowns()

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment)))))
            setupMockSetSessionKeyMongo(Right(true))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            val manageDetailsSummaryKeys = getManageDetailsSummaryKeys(document)
            val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)

            document.title shouldBe title()
            getHeading(document) shouldBe heading
            hasChangeFirstYearReportingMethodLink(document) shouldBe false
            hasChangeSecondYearReportingMethodLink(document) shouldBe false
            hasInsetText(document) shouldBe true
            manageDetailsSummaryKeys.get(0).text() shouldBe "Business name"
            manageDetailsSummaryValues.get(0).text() shouldBe unknown
            manageDetailsSummaryKeys.get(1).text() shouldBe "Address"
            manageDetailsSummaryValues.get(1).text() shouldBe unknown
            manageDetailsSummaryKeys.get(2).text() shouldBe "Date started"
            manageDetailsSummaryValues.get(2).text() shouldBe unknown
            manageDetailsSummaryKeys.get(3).text() shouldBe "Type of trade"
            manageDetailsSummaryKeys.get(4).text() shouldBe "Accounting method"
            manageDetailsSummaryValues.get(4).text() shouldBe "Traditional accounting"
          }

          "the user has a valid id parameter, latency expired" in {
            enable(DisplayBusinessStartDate, AccountingMethodJourney, ReportingFrequencyPage)
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)

            setupMockGetCurrentTaxYearEnd(2025)
            setupMockLatencyYearsQuarterlyAndAnnualStatus(true, true)
            mockUkPlusForeignPlusSoleTraderWithLatency()
            setupMockTaxYearNotCrystallised(2023)
            setupMockTaxYearNotCrystallised(2024)

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment)))))
            setupMockSetSessionKeyMongo(Right(true))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe title()
            getHeading(document) shouldBe heading
            hasChangeFirstYearReportingMethodLink(document) shouldBe false
            hasChangeSecondYearReportingMethodLink(document) shouldBe false
            hasInsetText(document) shouldBe true
            val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
            getManageDetailsSummaryKeys(document).get(1).text() shouldBe "Address"
            manageDetailsSummaryValues.get(1).text() shouldBe businessWithLatencyAddress
            manageDetailsSummaryValues.get(5).text() shouldBe standard
          }

          "the user has a valid id parameter and AccountingMethodJourney is disabled" in {
            enable(DisplayBusinessStartDate, ReportingFrequencyPage)
            disable(AccountingMethodJourney)
            disable(OptInOptOutContentUpdateR17)

            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)

            setupMockGetCurrentTaxYearEnd(2024)
            setupMockLatencyYearsQuarterlyAndAnnualStatus(true, true)
            mockUkPlusForeignPlusSoleTraderWithLatency()
            setupMockTaxYearNotCrystallised(2023)
            setupMockTaxYearNotCrystallised(2024)

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment)))))
            setupMockSetSessionKeyMongo(Right(true))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))

            val keys = getManageDetailsSummaryKeys(document).eachText()
            keys should not contain messages("incomeSources.manage.business-manage-details.accounting-method")
          }


          "the user has a valid id parameter and OptInOptOutContentUpdateR17 is enabled" in {
            enable(DisplayBusinessStartDate, OptInOptOutContentUpdateR17, ReportingFrequencyPage)

            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)

            setupMockGetCurrentTaxYearEnd(2023)
            setupMockLatencyYearsQuarterlyAndAnnualStatus(true, true)
            setupMockTaxYearNotCrystallised(2023)
            setupMockTaxYearNotCrystallised(2024)

            mockUkPlusForeignPlusSoleTraderWithLatency()

            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment)))))
            setupMockSetSessionKeyMongo(Right(true))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK

            val document: Document = Jsoup.parse(contentAsString(result))

            document.title shouldBe title()
            getHeading(document) shouldBe heading

            hasInsetText(document) shouldBe true
            document.getElementById("reportingFrequency").text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting obligations for all your businesses."

            val summaryKeys = getManageDetailsSummaryKeys(document)

            summaryKeys.eq(4).text() shouldBe "Using Making Tax Digital for Income Tax for 2022 to 2023"
            summaryKeys.eq(5).text() shouldBe  "Using Making Tax Digital for Income Tax for 2023 to 2024"

            val summaryValues = getManageDetailsSummaryValues(document).eachText()
            summaryValues should contain("No")
            summaryValues should contain("Yes")

            val actions = document.select(".govuk-summary-list__actions a").eachText()
            actions should contain("Sign up")
            actions should contain("Opt out")
          }

        }

        "render the error page" when {
          "the user has no income source of the called type" in {
            setupMockSuccess(mtdUserRole)
            setupMockCreateSession(true)
            mockUKPropertyIncomeSource()

            setupMockGetCurrentTaxYearEnd(2023)
            setupMockLatencyYearsQuarterlyAndAnnualStatus(false, false)

            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment)))))

            val result = action(fakeRequest)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdUserRole)(fakeRequest)
    }
  }
}