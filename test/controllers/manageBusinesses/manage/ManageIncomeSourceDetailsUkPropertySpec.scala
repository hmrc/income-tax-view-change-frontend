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
    List(false, true).foreach { isChange =>
      s"show${if (isChange) "Change"}($isAgent, $UkProperty)" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
        val action = if (isChange) {
          testController.showChange(UkProperty, isAgent)
        } else {
          testController.show(isAgent, UkProperty, None)
        }
        s"the user is authenticated as a $mtdUserRole" should {
          "render the appropriate IncomeSourceDetails page" when {
            "the user has a valid id parameter and no latency information" in {
              enable(DisplayBusinessStartDate, AccountingMethodJourney, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(2024)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(true, true)
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
              getManageDetailsSummaryValues(document).get(2).text() shouldBe standard
              document.getElementById("reportingFrequency").text() shouldBe "View and change your reporting frequency for all your businesses"
            }

            "the user does not have reporting frequency related content when RF FS is off" in {
              enable(DisplayBusinessStartDate, AccountingMethodJourney)
              disable(ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(2024)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(true, true)
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
              getManageDetailsSummaryValues(document).get(2).text() shouldBe standard
              hasReportingFrequencyContent(document) shouldBe false
            }

            "the user has a valid id parameter and AccountingMethodJourney is disabled" in {
              enable(DisplayBusinessStartDate, ReportingFrequencyPage)
              disable(AccountingMethodJourney)

              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(2024)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(true, true)
              mockUkPlusForeignPlusSoleTraderNoLatency()
              setupMockCreateSession(true)
              setupMockSetSessionKeyMongo(Right(true))

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))

              val summaryKeys = getManageDetailsSummaryKeys(document).eachText()
              summaryKeys should not contain messages("incomeSources.manage.uk-property-manage-details.accounting-method")
            }
            "the user has a valid id parameter and OptInOptOutContentUpdateR17 is enabled" in {
              enable(DisplayBusinessStartDate,
                AccountingMethodJourney, OptInOptOutContentUpdateR17, ReportingFrequencyPage)

              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)
              setupMockGetCurrentTaxYearEnd(2023)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(true, true)
              setupMockTaxYearNotCrystallised(2023)
              setupMockTaxYearNotCrystallised(2024)
              mockUkPlusForeignPlusSoleTraderWithLatency()

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, UkProperty)))))
              setupMockSetSessionKeyMongo(Right(true))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK

              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe title()
              getHeading(document) shouldBe heading

              hasInsetText(document) shouldBe true
              document.getElementById("reportingFrequency").text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting obligations for all your businesses."

              val summaryKeys = getManageDetailsSummaryKeys(document)

              summaryKeys.eq(3).text() shouldBe "Using Making Tax Digital for Income Tax for 2022 to 2023"
              summaryKeys.eq(4).text() shouldBe "Using Making Tax Digital for Income Tax for 2023 to 2024"

              val summaryValues = getManageDetailsSummaryValues(document).eachText()
              summaryValues should contain("Yes")
              summaryValues should contain("No")

              val actions = document.select(".govuk-summary-list__actions a").eachText()
              actions should contain("Opt out")
              actions should contain("Sign up")
            }

            "the user has a valid id parameter, valid latency information and two tax years not crystallised" in {
              enable(DisplayBusinessStartDate,
                AccountingMethodJourney, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(2023)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(true, true)
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
              val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
              manageDetailsSummaryValues.get(2).text() shouldBe calendar
              manageDetailsSummaryValues.get(3).text() shouldBe quarterlyGracePeriod
              manageDetailsSummaryValues.get(4).text() shouldBe annuallyGracePeriod
            }

            "valid latency information and two tax years not crystallised and ITSA status for TY2 is Annual" in {
              enable(DisplayBusinessStartDate,
                AccountingMethodJourney, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(2023)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(true, false)
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
              hasInsetText(document) shouldBe true
              val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
              manageDetailsSummaryValues.get(2).text() shouldBe calendar
              manageDetailsSummaryValues.eq(3).size() shouldBe 0
              manageDetailsSummaryValues.eq(4).size() shouldBe 0
            }

            "the user has a valid id parameter, valid latency information and two tax years crystallised" in {
              enable(DisplayBusinessStartDate, AccountingMethodJourney, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(2023)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(true, true)
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
              manageDetailsSummaryValues.get(2).text() shouldBe calendar
              manageDetailsSummaryValues.eq(3).size() shouldBe 0
              manageDetailsSummaryValues.eq(4).size() shouldBe 0
            }

            "the user has a valid id parameter, but non eligable itsa status" in {
              enable(DisplayBusinessStartDate, AccountingMethodJourney, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(2023)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(false, false)
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
              manageDetailsSummaryValues.get(1).text() shouldBe "Traditional accounting"
            }

            "the user has a valid id parameter, latency expired" in {
              enable(DisplayBusinessStartDate, AccountingMethodJourney, ReportingFrequencyPage)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(2025)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(true, true)
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
              val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
              manageDetailsSummaryValues.get(2).text() shouldBe calendar
              manageDetailsSummaryValues.get(3).text() shouldBe quarterlyGracePeriod
              manageDetailsSummaryValues.get(4).text() shouldBe annuallyGracePeriod
            }
          }

          "render the error page" when {
            "the user has no income source of the called type" in {
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)
              mockSingleBusinessIncomeSource()

              setupMockGetCurrentTaxYearEnd(2023)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(false, false)

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
