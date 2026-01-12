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

import enums.IncomeSourceJourney.UkProperty
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.MTDIndividual
import models.admin._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.test.Helpers._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{emptyUIJourneySessionData, notCompletedUIJourneySessionData}

class ManageIncomeSourceDetailsUkPropertySpec extends ManageIncomeSourceDetailsHelper {

  mtdAllRoles.foreach { mtdUserRole =>

    val isAgent = mtdUserRole != MTDIndividual

    Seq(
      false -> "show",
      true  -> "showChange"
    ).foreach { case (isChange, prefix) =>

      s"$prefix(role=$mtdUserRole, isAgent=$isAgent, $UkProperty)" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
        val action = if (isChange) {
          testController.showChange(UkProperty, isAgent)
        } else {
          testController.show(isAgent, UkProperty, None)
        }
        s"the user is authenticated as a $mtdUserRole" should {
          "render the appropriate IncomeSourceDetails page" when {
            "the user has a valid id parameter and no latency information" in {
              enable(DisplayBusinessStartDate, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(mockDateServiceInjected)(2024)
              setupMockLatencyYearsQuarterlyAndAnnualStatus(true, true)
              mockUkPlusForeignPlusSoleTraderNoLatency()
              setupMockCreateSession(true)
              setupMockSetSessionKeyMongo(Right(true))

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe title()
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe false
              hasChangeSecondYearReportingMethodLink(document) shouldBe false
              hasGracePeriodInfo(document) shouldBe false
              document.getElementById("reportingFrequency").text() shouldBe "View and change your reporting frequency for all your businesses"
              Option(document.getElementById("up-to-two-tax-years")) shouldBe None
            }

            "the user has a valid id parameter and latency period expired" in {
              enable(DisplayBusinessStartDate, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(mockDateServiceInjected)(2024)
              mockUkPlusForeignPlusSoleTraderWithLatencyExpired()
              setupMockCreateSession(true)
              setupMockSetSessionKeyMongo(Right(true))

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe title()
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe false
              hasChangeSecondYearReportingMethodLink(document) shouldBe false
              hasGracePeriodInfo(document) shouldBe false
              document.getElementById("reportingFrequency").text() shouldBe "View and change your reporting frequency for all your businesses"
            }

            "the user does not have reporting frequency related content" in {

              enable(DisplayBusinessStartDate)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(mockDateServiceInjected)(2024)
              setupMockLatencyYearsQuarterlyAndAnnualStatus(true, true)
              mockUkPlusForeignPlusSoleTraderNoLatency()
              setupMockCreateSession(true)
              setupMockSetSessionKeyMongo(Right(true))

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe title()
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe false
              hasChangeSecondYearReportingMethodLink(document) shouldBe false
              hasGracePeriodInfo(document) shouldBe false
              hasReportingFrequencyContent(document) shouldBe false
            }

            "the user has a valid id parameter and OptInOptOutContentUpdateR17 is enabled" in {

              enable(DisplayBusinessStartDate, OptInOptOutContentUpdateR17, ReportingFrequencyPage)

              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)
              setupMockGetCurrentTaxYearEnd(mockDateServiceInjected)(2023)
              setupMockLatencyYearsQuarterlyAndAnnualStatus(true, true)
              setupMockTaxYearNotCrystallised(2023)
              setupMockTaxYearNotCrystallised(2024)
              mockUkPlusForeignPlusSoleTraderWithLatency()

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))
              setupMockSetSessionKeyMongo(Right(true))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK

              val document: Document = Jsoup.parse(contentAsString(result))

              val latencyParagraph = document.getElementById("up-to-two-tax-years")
              val summaryKeys = getManageDetailsSummaryKeys(document)
              val summaryValues = getManageDetailsSummaryValues(document).eachText()
              val actions = document.select(".govuk-summary-list__actions a").eachText()

              latencyParagraph.text().nonEmpty shouldBe true

              document.title shouldBe title()
              getHeading(document) shouldBe heading
              hasInsetText(document) shouldBe true

              document.getElementById("reportingFrequency").text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting obligations for all your businesses."
              summaryKeys.eq(1).text() shouldBe "Using Making Tax Digital for Income Tax for 2022 to 2023"
              summaryKeys.eq(2).text() shouldBe "Using Making Tax Digital for Income Tax for 2023 to 2024"
              summaryValues should contain("Yes")
              summaryValues should contain("No")

              actions should contain("Opt out")
              actions should contain("Sign up")
            }

            "the user has a valid id parameter, valid latency information and two tax years not crystallised" in {

              enable(DisplayBusinessStartDate, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(mockDateServiceInjected)(2023)
              setupMockLatencyYearsQuarterlyAndAnnualStatus(true, true)
              mockUkPlusForeignPlusSoleTraderWithLatency()
              setupMockTaxYearNotCrystallised(2023)
              setupMockTaxYearNotCrystallised(2024)

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe title()
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe true
              hasChangeSecondYearReportingMethodLink(document) shouldBe true
              hasInsetText(document) shouldBe true

              val latencyParagraph = document.getElementById("up-to-two-tax-years")
              latencyParagraph.text().nonEmpty shouldBe true

              val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
              manageDetailsSummaryValues.get(1).text() shouldBe quarterlyGracePeriod
              manageDetailsSummaryValues.get(2).text() shouldBe annuallyGracePeriod
            }

            "valid latency information and two tax years not crystallised and ITSA status for TY2 is Annual" in {

              enable(DisplayBusinessStartDate, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(mockDateServiceInjected)(2023)
              setupMockLatencyYearsQuarterlyAndAnnualStatus(true, false)
              mockUkPlusForeignPlusSoleTraderWithLatency()
              setupMockTaxYearNotCrystallised(2023)
              setupMockTaxYearNotCrystallised(2024)

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe title()
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe true
              hasChangeSecondYearReportingMethodLink(document) shouldBe false
              hasInsetText(document) shouldBe true
              val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
              manageDetailsSummaryValues.eq(1).size() shouldBe 1
              manageDetailsSummaryValues.eq(2).size() shouldBe 1
            }
            "the user has a valid id parameter, valid latency information and tax year1 crystallised and tax year not crystallised" in {
              enable(DisplayBusinessStartDate, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(mockDateServiceInjected)(2023)
              setupMockLatencyYearsQuarterlyAndAnnualStatus(true, true)
              mockUkPlusForeignPlusSoleTraderWithLatency()
              setupMockTaxYearCrystallised(2023)
              setupMockTaxYearNotCrystallised(2024)

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe title()
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe false
              hasChangeSecondYearReportingMethodLink(document) shouldBe true
              hasGracePeriodInfo(document) shouldBe false
              val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
              manageDetailsSummaryValues.eq(1).size() shouldBe 1
              manageDetailsSummaryValues.eq(2).size() shouldBe 1
            }

            "the user has a valid id parameter, valid latency information and two tax years crystallised" in { //I think this scenario is not possible
              enable(DisplayBusinessStartDate, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(mockDateServiceInjected)(2023)
              setupMockLatencyYearsQuarterlyAndAnnualStatus(true, true)
              mockUkPlusForeignPlusSoleTraderWithLatency()
              setupMockTaxYearCrystallised(2023)
              setupMockTaxYearCrystallised(2024)

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe title()
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe false
              hasChangeSecondYearReportingMethodLink(document) shouldBe false
              hasGracePeriodInfo(document) shouldBe false
              val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
              manageDetailsSummaryValues.eq(1).size() shouldBe 1
              manageDetailsSummaryValues.eq(2).size() shouldBe 1
            }

            "the user has a valid id parameter, but non eligable itsa status" in {
              enable(DisplayBusinessStartDate, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(mockDateServiceInjected)(2023)
              setupMockLatencyYearsQuarterlyAndAnnualStatus(false, false)
              setupMockTaxYearCrystallised(2023)
              setupMockTaxYearCrystallised(2024)
              mockUkPlusForeignPlusSoleTrader2023WithLatencyAndUnknowns()

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)

              document.title shouldBe title()
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe false
              hasChangeSecondYearReportingMethodLink(document) shouldBe false
              hasGracePeriodInfo(document) shouldBe false
              manageDetailsSummaryValues.get(0).text() shouldBe unknown
            }

            "the user has a valid id parameter, latency expired" in {
              enable(DisplayBusinessStartDate, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(mockDateServiceInjected)(2025)
              setupMockLatencyYearsQuarterlyAndAnnualStatus(true, true)
              mockUkPlusForeignPlusSoleTraderWithLatency()
              setupMockTaxYearNotCrystallised(2023)
              setupMockTaxYearNotCrystallised(2024)

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe title()
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe false
              hasChangeSecondYearReportingMethodLink(document) shouldBe false
              val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
              manageDetailsSummaryValues.size() shouldBe 1
            }
          }

          "render the error page" when {
            "the user has no income source of the called type" in {
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)
              mockSingleBusinessIncomeSource()

              setupMockGetCurrentTaxYearEnd(mockDateServiceInjected)(2023)
              setupMockLatencyYearsQuarterlyAndAnnualStatus(false, false)

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
}
