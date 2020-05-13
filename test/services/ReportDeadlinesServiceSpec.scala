/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate

import assets.BaseTestConstants._
import assets.BusinessDetailsTestConstants.{obligationsDataSuccessModel => _, _}
import assets.IncomeSourceDetailsTestConstants._
import assets.IncomeSourcesWithDeadlinesTestConstants._
import assets.PropertyDetailsTestConstants.{propertyDetails, propertyIncomeModel}
import assets.ReportDeadlinesTestConstants.{obligationsCrystallisedSuccessModel, _}
import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.incomeSourcesWithDeadlines._
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModelWithIncomeType, ReportDeadlinesErrorModel, ReportDeadlinesModel}
import testUtils.TestSupport

class ReportDeadlinesServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector {

  object TestReportDeadlinesService extends ReportDeadlinesService(mockIncomeTaxViewChangeConnector)

  class Setup extends ReportDeadlinesService(mockIncomeTaxViewChangeConnector)

  "getNextDeadlineDueDate" should {
    "return the next report deadline due date" when {
      "there are income sources from property, business with crystallisation" in new Setup {
        setupMockReportDeadlines(testNino)(obligationsAllDeadlinesSuccessModel)

        await(getNextDeadlineDueDate(businessAndPropertyAligned)) shouldBe LocalDate.of(2017, 10, 1)
      }
      "there is just one report deadline from an income source" in new Setup {
        setupMockReportDeadlines(testNino)(obligationsPropertyOnlySuccessModel)

        await(getNextDeadlineDueDate(propertyIncomeOnly)) shouldBe LocalDate.of(2017, 10, 1)
      }
      "there is just a crystallisation deadline" in new Setup {
        setupMockReportDeadlines(testNino)(obligationsCrystallisedOnlySuccessModel)

        await(getNextDeadlineDueDate(noIncomeDetails)) shouldBe LocalDate.of(2017, 10, 31)
      }

      "there are no deadlines available" in new Setup {
        setupMockReportDeadlines(testNino)(emptyObligationsSuccessModel)

        the[Exception] thrownBy await(getNextDeadlineDueDate(noIncomeDetails)) should have message "Unexpected Exception getting next deadline due"
      }

      "the report deadlines returned back an error model" in new Setup {
        setupMockReportDeadlines(testNino)(obligationsDataErrorModel)

        the[Exception] thrownBy await(getNextDeadlineDueDate(noIncomeDetails)) should have message "Dummy Error Message"
      }
    }
  }

  "previousObligationsWithIncomeType" should {
    "return all the deadlines for any property and business income sources with crystallisation ordered by date submitted (latest first)" in new Setup {
      setupMockPreviousObligations(testNino)(ObligationsModel(Seq(
        ReportDeadlinesModel(testSelfEmploymentId, previousObligationsDataSuccessModel.obligations),
        ReportDeadlinesModel(testPropertyIncomeId, previousObligationsEOPSDataSuccessModel.obligations),
        ReportDeadlinesModel(testMtditid, previousObligationsCrystallisedSuccessModel.obligations)
      )))

      await(previousObligationsWithIncomeType(businessAndPropertyAligned)) shouldBe List(
        ReportDeadlineModelWithIncomeType("Property", previousObligationFour),
        ReportDeadlineModelWithIncomeType("Crystallised", previousObligationFive),
        ReportDeadlineModelWithIncomeType("Property", previousObligationThree),
        ReportDeadlineModelWithIncomeType(testTradeName, previousObligationTwo),
        ReportDeadlineModelWithIncomeType(testTradeName, previousObligationOne)
      )
    }

    "return an error" when {
      "deadlines are errored" in new Setup {
        setupMockPreviousObligations(testNino)(obligationsDataErrorModel)

        await(previousObligationsWithIncomeType(businessAndPropertyAligned)) shouldBe List()
      }
    }
  }

  "The ReportDeadlinesService.getReportDeadlines method" when {

    "a valid list of Report Deadlines is returned from the connector" should {

      "return a valid list of Report Deadlines" in {
        setupMockReportDeadlines(testNino)(ObligationsModel(Seq(reportDeadlinesDataSelfEmploymentSuccessModel)))
        await(TestReportDeadlinesService.getReportDeadlines()) shouldBe ObligationsModel(Seq(reportDeadlinesDataSelfEmploymentSuccessModel))
      }

      "return a valid list of previous Report Deadlines" in {
        setupMockPreviousObligations(testNino)(ObligationsModel(Seq(reportDeadlinesDataSelfEmploymentSuccessModel)))
        await(TestReportDeadlinesService.getReportDeadlines(previous = true)) shouldBe ObligationsModel(Seq(reportDeadlinesDataSelfEmploymentSuccessModel))
      }
    }

    "an error is returned from the connector" should {

      "return the error" in {
        setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataErrorModel)
        await(TestReportDeadlinesService.getReportDeadlines()) shouldBe obligationsDataErrorModel
      }

      "return the error for previous deadlines" in {
        setupMockPreviousObligations(testSelfEmploymentId)(obligationsDataErrorModel)
        await(TestReportDeadlinesService.getReportDeadlines(previous = true)) shouldBe obligationsDataErrorModel
      }
    }
  }

  "The ReportDeadlinesService.createIncomeSourcesWithDeadlinesModel method" when {

    "A user has multiple SE Business and Property Income Sources" when {

      "A successful set of report deadlines is retrieved for all income sources" should {

        "Return a IncomeSourcesWithDeadlines response which contains the expected report deadlines with the associated Income Sources" in {
          setupMockReportDeadlines(testNino)(obligationsDataAllMinusCrystallisedSuccessModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(businessesAndPropertyIncome)) shouldBe
            businessAndPropertyIncomeWithDeadlines
        }

      }

      "No successful report deadlines are retrieved" should {

        "Return a IncomeSourcesWithDeadlines response which all errored responses" in {
          setupMockReportDeadlines(testNino)(obligationsDataErrorModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(businessesAndPropertyIncome)) shouldBe
            IncomeSourcesWithDeadlinesError
        }

      }

    }

    "A user has a single SE Business Income Source" when {

      "A successful set of report deadlines is retrieved for the SE Business" should {

        "Return a IncomeSourcesWithDeadlines response which contains the expected report deadlines" in {

          setupMockReportDeadlines(testNino)(obligationsDataSelfEmploymentOnlySuccessModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(singleBusinessIncome)) shouldBe
            singleBusinessIncomeWithDeadlines
        }

      }

      "No successful report deadlines are retrieved" should {

        "Return a IncomeSourcesWithDeadlines response with errored report deadlines" in {

          setupMockReportDeadlines(testNino)(obligationsDataErrorModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(singleBusinessIncome)) shouldBe
            IncomeSourcesWithDeadlinesError
        }
      }
    }


    "A user has only has crystallised Obligation and both income sources" should {
      "Return a successful crystallised Obligation" in {
        setupMockReportDeadlines(testNino)(obligationsDataAllDataSuccessModel)
        await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(businessesAndPropertyIncome)) shouldBe
          IncomeSourcesWithDeadlinesModel(
            List(businessIncomeModel, businessIncomeModel2),
            Some(propertyIncomeModel),
            Some(CrystallisedDeadlinesModel(crystallisedDeadlineSuccess))
          )
      }
    }


    "A user has only has Property Income Source" when {

      "A successful set of report deadlines is retrieved for the SE Business" should {

        "Return a IncomeSourcesWithDeadlines response which contains the expected report deadlines" in {

          setupMockReportDeadlines(testNino)(obligationsDataPropertyOnlySuccessModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(propertyIncomeOnly)) shouldBe
            propertyIncomeOnlyWithDeadlines
        }

      }

      "No successful report deadlines are retrieved" should {

        "Return a IncomeSourcesWithDeadlines response with errored report deadlines" in {

          setupMockReportDeadlines(testNino)(obligationsDataErrorModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(propertyIncomeOnly)) shouldBe IncomeSourcesWithDeadlinesError
        }
      }
    }


    "The Income Source Details are Errored" should {

      "Return an IncomeSourcesWithDeadlinesError" in {
        await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(errorResponse)) shouldBe IncomeSourcesWithDeadlinesError
      }
    }
  }
}
