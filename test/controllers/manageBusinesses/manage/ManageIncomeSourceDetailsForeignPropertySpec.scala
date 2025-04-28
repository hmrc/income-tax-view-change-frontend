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

import enums.IncomeSourceJourney.ForeignProperty
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.MTDIndividual
import models.admin.{DisplayBusinessStartDate, IncomeSourcesFs}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.test.Helpers._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{emptyUIJourneySessionData, notCompletedUIJourneySessionData}

class ManageIncomeSourceDetailsForeignPropertyISpec extends ManageIncomeSourceDetailsHelper {

  mtdAllRoles.foreach { mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    List(false, true).foreach { isChange =>
      s"show${if (isChange) "Change"}($isAgent, $ForeignProperty)" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
        val action = if(isChange) {
          testController.showChange(ForeignProperty, isAgent)
        } else {
          testController.show(isAgent, ForeignProperty, None)
        }
        s"the user is authenticated as a $mtdUserRole" should {
          "render the appropriate IncomeSourceDetails page" when {
            "the user has a valid id parameter and no latency information" in {
              enable(IncomeSourcesFs)
              enable(DisplayBusinessStartDate)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(2024)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(true, true)
              mockUkPlusForeignPlusSoleTraderNoLatency()
              setupMockCreateSession(true)
              setupMockSetSessionKeyMongo(Right(true))

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, ForeignProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe title(mtdUserRole)
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe false
              hasChangeSecondYearReportingMethodLink(document) shouldBe false
              hasGracePeriodInfo(document) shouldBe false
              getManageDetailsSummaryValues(document).get(2).text() shouldBe calendar
            }

            "the user has a valid id parameter, valid latency information and two tax years not crystallised" in {
              enable(IncomeSourcesFs)
              enable(DisplayBusinessStartDate)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(2023)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(true, true)
              mockUkPlusForeignPlusSoleTraderWithLatency()
              setupMockTaxYearNotCrystallised(2023)
              setupMockTaxYearNotCrystallised(2024)

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, ForeignProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe title(mtdUserRole)
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe true
              hasChangeSecondYearReportingMethodLink(document) shouldBe true
              hasInsetText(document) shouldBe true
              val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
              manageDetailsSummaryValues.get(2).text() shouldBe annuallyGracePeriod
              manageDetailsSummaryValues.get(3).text() shouldBe annuallyGracePeriod
            }

            "the user has a valid id parameter, valid latency information and two tax years crystallised" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(2023)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(true, true)
              mockUkPlusForeignPlusSoleTraderWithLatency()
              setupMockTaxYearCrystallised(2023)
              setupMockTaxYearCrystallised(2024)

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, ForeignProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe title(mtdUserRole)
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe false
              hasChangeSecondYearReportingMethodLink(document) shouldBe false
              hasGracePeriodInfo(document) shouldBe false
              val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
              manageDetailsSummaryValues.eq(2).size() shouldBe 0
              manageDetailsSummaryValues.eq(3).size() shouldBe 0
            }

            "the user has a valid id parameter, but non eligable itsa status" in {
              enable(IncomeSourcesFs)
              enable(DisplayBusinessStartDate)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(2023)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(false, false)
              setupMockTaxYearCrystallised(2023)
              setupMockTaxYearCrystallised(2024)
              mockUkPlusForeignPlusSoleTrader2023WithLatencyAndUnknowns()

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, ForeignProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)

              document.title shouldBe title(mtdUserRole)
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe false
              hasChangeSecondYearReportingMethodLink(document) shouldBe false
              hasGracePeriodInfo(document) shouldBe false
              manageDetailsSummaryValues.get(0).text() shouldBe unknown
              manageDetailsSummaryValues.get(1).text() shouldBe "Traditional accounting"
            }

            "the user has a valid id parameter, latency expired" in {
              enable(IncomeSourcesFs)
              enable(DisplayBusinessStartDate)
              setupMockSuccess(mtdUserRole)
              setupMockCreateSession(true)

              setupMockGetCurrentTaxYearEnd(2025)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(true, true)
              mockUkPlusForeignPlusSoleTraderWithLatency()
              setupMockTaxYearNotCrystallised(2023)
              setupMockTaxYearNotCrystallised(2024)

              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, ForeignProperty)))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe title(mtdUserRole)
              getHeading(document) shouldBe heading
              hasChangeFirstYearReportingMethodLink(document) shouldBe true
              hasChangeSecondYearReportingMethodLink(document) shouldBe true
              hasInsetText(document) shouldBe true
              val manageDetailsSummaryValues = getManageDetailsSummaryValues(document)
              manageDetailsSummaryValues.get(2).text() shouldBe standard
              manageDetailsSummaryValues.get(3).text() shouldBe annuallyGracePeriod
              manageDetailsSummaryValues.get(4).text() shouldBe annuallyGracePeriod
            }
          }

          "redirect to the home page" when {
            "incomeSources FS is disabled" in {
              disable(IncomeSourcesFs)
              setupMockSuccess(mtdUserRole)
              mockBothPropertyBothBusiness()
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, ForeignProperty)))))
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
              mockBusinessIncomeSource()

              setupMockGetCurrentTaxYearEnd(2023)
              setupMockHasMandatedOrVoluntaryStatusForLatencyYears(false, false)

              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, ForeignProperty)))))

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
