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

import assets.BaseTestConstants._
import assets.BusinessDetailsTestConstants.{obligationsDataSuccessModel => _, _}
import assets.IncomeSourceDetailsTestConstants._
import assets.IncomeSourcesWithDeadlinesTestConstants._
import assets.PropertyDetailsTestConstants.propertyDetails
import assets.ReportDeadlinesTestConstants._
import mocks.connectors.MockReportDeadlinesConnector
import models.incomeSourcesWithDeadlines.{BusinessIncomeWithDeadlinesModel, IncomeSourcesWithDeadlinesError, IncomeSourcesWithDeadlinesModel, PropertyIncomeWithDeadlinesModel}
import utils.TestSupport

class ReportDeadlinesServiceSpec extends TestSupport with MockReportDeadlinesConnector {

  object TestReportDeadlinesService extends ReportDeadlinesService(mockReportDeadlinesConnector)

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
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(businessesAndPropertyIncome)) shouldBe
            businessAndPropertyIncomeWithDeadlines
        }

      }

      "A successful set of report deadlines is retrieved for all but the property income source" should {

        "Return a IncomeSourcesWithDeadlines response which contains the expected report deadlines with an errored Property response" in {

          setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
          setupMockReportDeadlines(testSelfEmploymentId2)(obligationsDataSuccessModel)
          setupMockReportDeadlines(testPropertyIncomeId)(obligationsDataErrorModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(businessesAndPropertyIncome)) shouldBe
            IncomeSourcesWithDeadlinesModel(
              List(businessIncomeModel, businessIncomeModel2),
              Some(PropertyIncomeWithDeadlinesModel(
                propertyDetails,
                obligationsDataErrorModel
              ))
            )
        }

      }

      "No successful report deadlines are retrieved" should {

        "Return a IncomeSourcesWithDeadlines response which all errored responses" in {

          setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataErrorModel)
          setupMockReportDeadlines(testSelfEmploymentId2)(obligationsDataErrorModel)
          setupMockReportDeadlines(testPropertyIncomeId)(obligationsDataErrorModel)
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
              ))
            )
        }

      }

    }

    "A user has a single SE Business Income Source" when {

      "A successful set of report deadlines is retrieved for the SE Business" should {

        "Return a IncomeSourcesWithDeadlines response which contains the expected report deadlines" in {

          setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(singleBusinessIncome)) shouldBe
            singleBusinessIncomeWithDeadlines
        }

      }

      "No successful report deadlines are retrieved" should {

        "Return a IncomeSourcesWithDeadlines response with errored report deadlines" in {

          setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataErrorModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(singleBusinessIncome)) shouldBe
            IncomeSourcesWithDeadlinesModel(
              List(
                BusinessIncomeWithDeadlinesModel(
                  business1,
                  obligationsDataErrorModel
                )
              ),
              None
            )
        }
      }
    }

    "A user has only has Property Income Source" when {

      "A successful set of report deadlines is retrieved for the SE Business" should {

        "Return a IncomeSourcesWithDeadlines response which contains the expected report deadlines" in {

          setupMockReportDeadlines(testPropertyIncomeId)(obligationsDataSuccessModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(propertyIncomeOnly)) shouldBe
            propertyIncomeOnlyWithDeadlines
        }

      }

      "No successful report deadlines are retrieved" should {

        "Return a IncomeSourcesWithDeadlines response with errored report deadlines" in {

          setupMockReportDeadlines(testPropertyIncomeId)(obligationsDataErrorModel)
          await(TestReportDeadlinesService.createIncomeSourcesWithDeadlinesModel(propertyIncomeOnly)) shouldBe
            IncomeSourcesWithDeadlinesModel(
              List(),
              Some(PropertyIncomeWithDeadlinesModel(
                propertyDetails,
                obligationsDataErrorModel
              ))
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
