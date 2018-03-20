/*
 * Copyright 2018 HM Revenue & Customs
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

package services

import assets.TestConstants.BusinessDetails._
import assets.TestConstants.NewBizDeets._
import assets.TestConstants.NewPropDeets._
import assets.TestConstants.Estimates._
import assets.TestConstants.IncomeSources._
import assets.TestConstants._
import mocks.services.{MockCalculationService, MockReportDeadlinesService}
import models.core.AccountingPeriodModel
import models.incomeSourcesWithDeadlines.{BusinessIncomeWithDeadlinesModel, IncomeSourcesWithDeadlinesModel, PropertyIncomeWithDeadlinesModel}
import models.reportDeadlines.{ReportDeadlineModel, ReportDeadlinesErrorModel, ReportDeadlinesModel}
import utils.TestSupport

class BTAPartialServiceSpec extends TestSupport with MockCalculationService with MockReportDeadlinesService {

  object TestBTAPartialService extends BTAPartialService(mockCalculationService)

  "The BTAPartialService getNextObligation method" when {

    "both property and business obligations are returned - business due before property" should {
      val returnedObligation = ReportDeadlineModel(
        start = "2017-04-01",
        end = "2017-06-30",
        due = "2017-07-30",
        met = false
      )
      val bothIncomeSourcesBusinessReportDueNext = IncomeSourcesWithDeadlinesModel(
        List(BusinessIncomeWithDeadlinesModel(
          business1,
          reportDeadlines = ReportDeadlinesModel(
            obligations = List(returnedObligation)
          )
        )),
        Some(PropertyIncomeWithDeadlinesModel(
          propertyDetails,
          reportDeadlines = ReportDeadlinesModel(
            obligations = List(
              ReportDeadlineModel(
                start = "2017-04-01",
                end = "2017-06-30",
                due = "2017-07-31",
                met = false
              )
            )
          )
        ))
      )

      "return an ReportDeadlineModel" in {
        await(TestBTAPartialService.getNextObligation(bothIncomeSourcesBusinessReportDueNext)) shouldBe returnedObligation
      }
    }

    "both property and business obligations are returned - property due before business" should {

      val returnedObligation = ReportDeadlineModel(
        start = "2017-04-01",
        end = "2017-06-30",
        due = "2017-07-30",
        met = false
      )
      val bothIncomeSourcesPropertyReportDueNext = IncomeSourcesWithDeadlinesModel(
        List(BusinessIncomeWithDeadlinesModel(
          business1,
          reportDeadlines = ReportDeadlinesModel(
            obligations = List(
              ReportDeadlineModel(
                start = "2017-04-01",
                end = "2017-06-30",
                due = "2017-07-31",
                met = false
              )
            )
          )
        )),
        Some(PropertyIncomeWithDeadlinesModel(
          propertyDetails,
          reportDeadlines = ReportDeadlinesModel(
            obligations = List(returnedObligation)
          )
        ))
      )
      "return an ReportDeadlineModel" in {
        await(TestBTAPartialService.getNextObligation(bothIncomeSourcesPropertyReportDueNext)) shouldBe returnedObligation
      }
    }

    "both property and business obligations are returned - business due before property but is received, therefore property obligation returned" should {
      val returnedObligation = ReportDeadlineModel(
        start = "2017-04-01",
        end = "2017-06-30",
        due = "2017-07-31",
        met = false
      )
      val bothIncomeSourcesPropertyReportDueNext = IncomeSourcesWithDeadlinesModel(
        List(BusinessIncomeWithDeadlinesModel(
          business1,
          reportDeadlines = ReportDeadlinesModel(
            obligations = List(ReportDeadlineModel(
              start = "2017-04-01",
              end = "2017-06-30",
              due = "2017-07-30",
              met = true
            ))
          )
        )),
        Some(PropertyIncomeWithDeadlinesModel(
          propertyDetails,
          reportDeadlines = ReportDeadlinesModel(
            obligations = List(returnedObligation)
          )
        ))
      )

      "return an ReportDeadlineModel" in {
        await(TestBTAPartialService.getNextObligation(bothIncomeSourcesPropertyReportDueNext)) shouldBe returnedObligation
      }
    }

    "both property and business obligations are returned - property due before business but is received, therefore businesses obligation returned" should {
      val returnedObligation = ReportDeadlineModel(
        start = "2017-04-01",
        end = "2017-06-30",
        due = "2017-07-31",
        met = false
      )
      val bothIncomeSourcesBusinessReportDueNext = IncomeSourcesWithDeadlinesModel(
        List(BusinessIncomeWithDeadlinesModel(
          business1,
          reportDeadlines = ReportDeadlinesModel(
            obligations = List(returnedObligation)
          )
        )),
        Some(PropertyIncomeWithDeadlinesModel(
          propertyDetails,
          reportDeadlines = ReportDeadlinesModel(
            obligations = List(ReportDeadlineModel(
              start = "2017-04-01",
              end = "2017-06-30",
              due = "2017-07-30",
              met = true
            ))
          )
        ))
      )

      "return an ReportDeadlineModel" in {
        await(TestBTAPartialService.getNextObligation(bothIncomeSourcesBusinessReportDueNext)) shouldBe returnedObligation
      }
    }

    "both property and business obligations are returned - both are received, therefore most recent obligation returned" should {
      val returnedObligation = ReportDeadlineModel(
        start = "2017-04-01",
        end = "2017-06-30",
        due = "2017-07-31",
        met = true
      )
      val bothIncomeSourcesBusinessReportDueNext = IncomeSourcesWithDeadlinesModel(
        List(BusinessIncomeWithDeadlinesModel(
          business1,
          reportDeadlines = ReportDeadlinesModel(
            obligations = List(returnedObligation)
          )
        )),
        Some(PropertyIncomeWithDeadlinesModel(
          propertyDetails,
          reportDeadlines = ReportDeadlinesModel(
            obligations = List(ReportDeadlineModel(
              start = "2017-04-01",
              end = "2017-06-30",
              due = "2017-07-30",
              met = true
            ))
          )
        ))
      )

      "return an ReportDeadlineModel" in {
        await(TestBTAPartialService.getNextObligation(bothIncomeSourcesBusinessReportDueNext)) shouldBe returnedObligation
      }
    }

    "only business obligations are returned" should {
      val otherObligation = ReportDeadlineModel(
        start = "2017-07-01",
        end = "2017-09-30",
        due = "2017-10-31",
        met = false
      )
      val returnedObligation = ReportDeadlineModel(
        start = "2017-04-01",
        end = "2017-06-30",
        due = "2017-07-31",
        met = false
      )
      val businessIncomeSourceReportDueNext = IncomeSourcesWithDeadlinesModel(
        List(BusinessIncomeWithDeadlinesModel(
          business1,
          reportDeadlines = ReportDeadlinesModel(
            obligations = List(otherObligation, returnedObligation)
          )
        )),
        None
      )
      "return an ReportDeadlineModel" in {
        await(TestBTAPartialService.getNextObligation(businessIncomeSourceReportDueNext)) shouldBe returnedObligation
      }
    }

    "only property obligations are returned, and both are met" should {
      val returnedObligation = ReportDeadlineModel(
        start = "2017-07-01",
        end = "2017-09-30",
        due = "2017-10-31",
        met = true
      )
      val otherObligation = ReportDeadlineModel(
        start = "2017-04-01",
        end = "2017-06-30",
        due = "2017-07-31",
        met = true
      )
      val propertyIncomeSourceReportDueNext = IncomeSourcesWithDeadlinesModel(
        List(),
        Some(PropertyIncomeWithDeadlinesModel(
          propertyDetails,
          reportDeadlines = ReportDeadlinesModel(
            obligations = List(otherObligation, returnedObligation)
          )
        ))
      )
      "return an ReportDeadlineModel" in {
        await(TestBTAPartialService.getNextObligation(propertyIncomeSourceReportDueNext)) shouldBe returnedObligation
      }
    }

    "no obligations are returned" in {
      IncomeSourcesWithDeadlinesModel(
        List(),
        None
      )
      await(TestBTAPartialService.getNextObligation(noIncomeSourceSuccess)) shouldBe ReportDeadlinesErrorModel(500,"Could not retrieve obligations")
    }
  }

  "The BTAPartialService getEstimate method" when {
    "a valid LastTaxCalculation is returned from the FinancialDataService" should {
      "return LastTaxCalc model" in {
        setupMockGetLastEstimatedTaxCalculation(testNino, testYear)(lastTaxCalcSuccess)
        await(TestBTAPartialService.getEstimate(testNino, testYear)) shouldBe lastTaxCalcSuccess
      }
    }
    "NoLastTaxCalculation is returned from the FinancialDataService" should {
      "return NoLastTaxCalc" in {
        setupMockGetLastEstimatedTaxCalculation(testNino, testYear)(lastTaxCalcNotFound)
        await(TestBTAPartialService.getEstimate(testNino, testYear)) shouldBe lastTaxCalcNotFound
      }
    }
    "LastTaxCalculationError is returned from the FinancialDataService" should {
      "return LastTaxCalcError" in {
        setupMockGetLastEstimatedTaxCalculation(testNino, testYear)(lastTaxCalcError)
        await(TestBTAPartialService.getEstimate(testNino, testYear)) shouldBe lastTaxCalcError
      }
    }
  }

}
