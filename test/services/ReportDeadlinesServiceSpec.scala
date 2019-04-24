/*
 * Copyright 2019 HM Revenue & Customs
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
import models.reportDeadlines.ReportDeadlineModelWithIncomeType
import testUtils.TestSupport

class ReportDeadlinesServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector {

  object TestReportDeadlinesService extends ReportDeadlinesService(mockIncomeTaxViewChangeConnector)

  class Setup extends ReportDeadlinesService(mockIncomeTaxViewChangeConnector)

  "getNextDeadlineDueDate" should {
    "return the next report deadline due date" when {
      "there are income sources from property, business with crystallisation" in new Setup {
        setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
        setupMockReportDeadlines(testPropertyIncomeId)(obligationsEOPSDataSuccessModel)
        setupMockReportDeadlines(testNino)(obligationsCrystallisedSuccessModel)

        await(getNextDeadlineDueDate(businessAndPropertyAligned)) shouldBe LocalDate.of(2017, 10, 1)
      }
      "there is just one report deadline from property" in new Setup {
        setupMockReportDeadlines(testPropertyIncomeId)(obligationsEOPSDataSuccessModel)
        setupMockReportDeadlines(testNino)(obligationsCrystallisedEmptySuccessModel)

        await(getNextDeadlineDueDate(propertyIncomeOnly)) shouldBe LocalDate.of(2017, 10, 1)
      }
      "there is just one report deadline from business" in new Setup {
        setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
        setupMockReportDeadlines(testNino)(obligationsCrystallisedEmptySuccessModel)

        await(getNextDeadlineDueDate(singleBusinessIncome)) shouldBe LocalDate.of(2017, 10, 30)
      }
      "there is just a crystallisation deadline" in new Setup {
        setupMockReportDeadlines(testNino)(obligationsCrystallisedSuccessModel)

        await(getNextDeadlineDueDate(noIncomeDetails)) shouldBe LocalDate.of(2017,10,31)
      }
      "one of the report deadlines returned back an error model" in new Setup {
        setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
        setupMockReportDeadlines(testSelfEmploymentId2)(obligationsDataSuccessModel)
        setupMockReportDeadlines(testPropertyIncomeId)(obligationsDataErrorModel)
        setupMockReportDeadlines(testNino)(obligationsCrystallisedEmptySuccessModel)

        await(getNextDeadlineDueDate(businessesAndPropertyIncome)) shouldBe LocalDate.of(2017, 10, 30)
      }
    }
  }

  "getAllDeadlines" should {
    "return all the deadlines for any property and business income sources with crystallisation" in new Setup {
      setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
      setupMockReportDeadlines(testPropertyIncomeId)(obligationsEOPSDataSuccessModel)
      setupMockReportDeadlines(testNino)(obligationsCrystallisedSuccessModel)

      await(getAllDeadlines(businessAndPropertyAligned)) shouldBe obligationsDataSuccessModel.obligations ++
        obligationsEOPSDataSuccessModel.obligations ++
        obligationsCrystallisedSuccessModel.obligations
    }
    "return only the non errored deadlines for any property and business income sources with crystallisation" when {
      "property is errored" in new Setup {
        setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
        setupMockReportDeadlines(testPropertyIncomeId)(obligationsDataErrorModel)
        setupMockReportDeadlines(testNino)(obligationsCrystallisedSuccessModel)

        await(getAllDeadlines(businessAndPropertyAligned)) shouldBe obligationsDataSuccessModel.obligations ++ obligationsCrystallisedSuccessModel.obligations
      }
      "business is errored" in new Setup {
        setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataErrorModel)
        setupMockReportDeadlines(testPropertyIncomeId)(obligationsDataSuccessModel)
        setupMockReportDeadlines(testNino)(obligationsCrystallisedSuccessModel)

        await(getAllDeadlines(businessAndPropertyAligned)) shouldBe obligationsDataSuccessModel.obligations ++ obligationsCrystallisedSuccessModel.obligations
      }
      "crystallisation is errored" in new Setup {
        setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
        setupMockReportDeadlines(testPropertyIncomeId)(obligationsEOPSDataSuccessModel)
        setupMockReportDeadlines(testNino)(obligationsDataErrorModel)

        await(getAllDeadlines(businessAndPropertyAligned)) shouldBe obligationsDataSuccessModel.obligations ++ obligationsEOPSDataSuccessModel.obligations
      }
    }
  }

  "previousObligationsWithIncomeType" should {
    "return all the deadlines for any property and business income sources with crystallisation ordered by date submitted (latest first)" in new Setup {
      setupMockPreviousObligations(testSelfEmploymentId)(previousObligationsDataSuccessModel)
      setupMockPreviousObligations(testPropertyIncomeId)(previousObligationsEOPSDataSuccessModel)
      setupMockPreviousObligations(testNino)(previousObligationsCrystallisedSuccessModel)

      await(previousObligationsWithIncomeType(businessAndPropertyAligned)) shouldBe List(
        ReportDeadlineModelWithIncomeType("Property", previousObligationFour),
        ReportDeadlineModelWithIncomeType("Crystallised", previousObligationFive),
        ReportDeadlineModelWithIncomeType("Property", previousObligationThree),
        ReportDeadlineModelWithIncomeType(testTradeName, previousObligationTwo),
        ReportDeadlineModelWithIncomeType(testTradeName, previousObligationOne)
      )
    }

    "return only the non errored deadlines for any property and business income sources with crystallisation" when {
      "property is errored" in new Setup {
        setupMockPreviousObligations(testSelfEmploymentId)(obligationsDataErrorModel)
        setupMockPreviousObligations(testPropertyIncomeId)(previousObligationsEOPSDataSuccessModel)
        setupMockPreviousObligations(testNino)(previousObligationsCrystallisedSuccessModel)

        await(previousObligationsWithIncomeType(businessAndPropertyAligned)) shouldBe List(
          ReportDeadlineModelWithIncomeType("Property", previousObligationFour),
          ReportDeadlineModelWithIncomeType("Crystallised", previousObligationFive),
          ReportDeadlineModelWithIncomeType("Property", previousObligationThree)
        )
      }
      "business is errored" in new Setup {
        setupMockPreviousObligations(testSelfEmploymentId)(previousObligationsDataSuccessModel)
        setupMockPreviousObligations(testPropertyIncomeId)(obligationsDataErrorModel)
        setupMockPreviousObligations(testNino)(previousObligationsCrystallisedSuccessModel)

        await(previousObligationsWithIncomeType(businessAndPropertyAligned)) shouldBe List(
          ReportDeadlineModelWithIncomeType("Crystallised", previousObligationFive),
          ReportDeadlineModelWithIncomeType(testTradeName, previousObligationTwo),
          ReportDeadlineModelWithIncomeType(testTradeName, previousObligationOne)
        )
      }
      "crystallisation is errored" in new Setup {
        setupMockPreviousObligations(testSelfEmploymentId)(previousObligationsDataSuccessModel)
        setupMockPreviousObligations(testPropertyIncomeId)(previousObligationsEOPSDataSuccessModel)
        setupMockPreviousObligations(testNino)(obligationsDataErrorModel)

        await(previousObligationsWithIncomeType(businessAndPropertyAligned)) shouldBe List(
          ReportDeadlineModelWithIncomeType("Property", previousObligationFour),
          ReportDeadlineModelWithIncomeType("Property", previousObligationThree),
          ReportDeadlineModelWithIncomeType(testTradeName, previousObligationTwo),
          ReportDeadlineModelWithIncomeType(testTradeName, previousObligationOne)
        )
      }
    }
  }

  "The ReportDeadlinesService.getReportDeadlines method" when {

    "a valid list of Report Deadlines is returned from the connector" should {

      "return a valid list of Report Deadlines" in {
        setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
        await(TestReportDeadlinesService.getReportDeadlines(testSelfEmploymentId)) shouldBe obligationsDataSuccessModel
      }
    }

    "an error is returned from the connector" should {

      "return the error" in {
        setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataErrorModel)
        await(TestReportDeadlinesService.getReportDeadlines(testSelfEmploymentId)) shouldBe obligationsDataErrorModel
      }
    }
  }

  "The ReportDeadlinesService.createIncomeSourcesWithDeadlinesModel method" when {

    "A user has multiple SE Business and Property Income Sources" when {

      "A successful set of report deadlines is retrieved for all income sources" should {

        "Return a IncomeSourcesWithDeadlines response which contains the expected report deadlines with the associated Income Sources" in {

          setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
          setupMockReportDeadlines(testSelfEmploymentId2)(obligationsDataSuccessModel)
          setupMockReportDeadlines(testPropertyIncomeId)(obligationsDataSuccessModel)
          setupMockReportDeadlines(testNino)(obligationsDataErrorModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(businessesAndPropertyIncome)) shouldBe
            businessAndPropertyIncomeWithDeadlines
        }

      }

      "A successful set of report deadlines is retrieved for all but the property income source" should {

        "Return a IncomeSourcesWithDeadlines response which contains the expected report deadlines with an errored Property response" in {

          setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
          setupMockReportDeadlines(testSelfEmploymentId2)(obligationsDataSuccessModel)
          setupMockReportDeadlines(testPropertyIncomeId)(obligationsDataErrorModel)
          setupMockReportDeadlines(testNino)(obligationsDataErrorModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(businessesAndPropertyIncome)) shouldBe
            IncomeSourcesWithDeadlinesModel(
              List(businessIncomeModel, businessIncomeModel2),
              Some(PropertyIncomeWithDeadlinesModel(
                propertyDetails,
                obligationsDataErrorModel
              )),
              None
            )
        }

      }

      "No successful report deadlines are retrieved" should {

        "Return a IncomeSourcesWithDeadlines response which all errored responses" in {

          setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataErrorModel)
          setupMockReportDeadlines(testSelfEmploymentId2)(obligationsDataErrorModel)
          setupMockReportDeadlines(testPropertyIncomeId)(obligationsDataErrorModel)
          setupMockReportDeadlines(testNino)(obligationsDataErrorModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(businessesAndPropertyIncome)) shouldBe
            IncomeSourcesWithDeadlinesModel(
              List(
                BusinessIncomeWithDeadlinesModel(
                  business1,
                  obligationsDataErrorModel
                ),
                BusinessIncomeWithDeadlinesModel(
                  business2,
                  obligationsDataErrorModel
                )
              ),
              Some(PropertyIncomeWithDeadlinesModel(
                propertyDetails,
                obligationsDataErrorModel
              )),
              None
            )
        }

      }

    }

    "A user has a single SE Business Income Source" when {

      "A successful set of report deadlines is retrieved for the SE Business" should {

        "Return a IncomeSourcesWithDeadlines response which contains the expected report deadlines" in {

          setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
          setupMockReportDeadlines(testNino)(obligationsDataErrorModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(singleBusinessIncome)) shouldBe
            singleBusinessIncomeWithDeadlines
        }

      }

      "No successful report deadlines are retrieved" should {

        "Return a IncomeSourcesWithDeadlines response with errored report deadlines" in {

          setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataErrorModel)
          setupMockReportDeadlines(testNino)(obligationsDataErrorModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(singleBusinessIncome)) shouldBe
            IncomeSourcesWithDeadlinesModel(
              List(
                BusinessIncomeWithDeadlinesModel(
                  business1,
                  obligationsDataErrorModel
                )
              ),
              None,
              None
            )
        }
      }
    }


    "A user has only has crystallised Obligation and both income sources" should {
      "Return a successful crystallised Obligation" in {
        setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
        setupMockReportDeadlines(testSelfEmploymentId2)(obligationsDataSuccessModel)
        setupMockReportDeadlines(testPropertyIncomeId)(obligationsDataSuccessModel)
        setupMockReportDeadlines(testNino)(crystallisedDeadlineSuccess)
        await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(businessesAndPropertyIncome)) shouldBe
          IncomeSourcesWithDeadlinesModel(
            List(businessIncomeModel,businessIncomeModel2),
            Some(propertyIncomeModel),
            Some(CrystallisedDeadlinesModel(crystallisedDeadlineSuccess))
          )
      }
    }


    "A user has only has Property Income Source" when {

      "A successful set of report deadlines is retrieved for the SE Business" should {

        "Return a IncomeSourcesWithDeadlines response which contains the expected report deadlines" in {

          setupMockReportDeadlines(testPropertyIncomeId)(obligationsDataSuccessModel)
          setupMockReportDeadlines(testNino)(obligationsDataErrorModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(propertyIncomeOnly)) shouldBe
            propertyIncomeOnlyWithDeadlines
        }

      }

      "No successful report deadlines are retrieved" should {

        "Return a IncomeSourcesWithDeadlines response with errored report deadlines" in {

          setupMockReportDeadlines(testPropertyIncomeId)(obligationsDataErrorModel)
          setupMockReportDeadlines(testNino)(obligationsDataErrorModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(propertyIncomeOnly)) shouldBe
            IncomeSourcesWithDeadlinesModel(
              List(),
              Some(PropertyIncomeWithDeadlinesModel(
                propertyDetails,
                obligationsDataErrorModel
              )),
              None
            )
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
