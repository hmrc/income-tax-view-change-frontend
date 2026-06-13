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

package obligations.services.reportingObligations.optOut.quarterlyupdates

import common.config.FrontendAppConfig
import common.mocks.services.{MockDateService, MockITSAStatusService}
import common.mocks.{MockAuditingService, MockHttpV2}
import common.services.DateService
import mocks.services.MockCalculationListService
import obligations.connectors.ObligationsConnector
import obligations.repositories.OptOutSessionDataRepository
import obligations.services.NextUpdatesService
import obligations.services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import obligations.services.reportingObligations.ReportingFrequency.QuarterlyUpdatesCountForTaxYearModel
import obligations.services.reportingObligations.optOut.{OptOutService, OptOutTestSupport}
import obligations.testConstants.NextUpdatesTestConstants.obligationsDataFromJson
import org.mockito.Mockito.mock
import org.scalatest.BeforeAndAfter
import play.api.Configuration
import play.mvc.Http.Status
import common.testConstants.BaseTestConstants.testNino
import testUtils.UnitSpec
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate

class QuarterlyUpdatesCountSpec extends UnitSpec
  with BeforeAndAfter
  with MockITSAStatusService
  with MockCalculationListService
  with MockDateService
  with MockHttpV2
  with MockAuditingService {

  implicit override val dateService: DateService = mockDateService

  override val appConfig: FrontendAppConfig = new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
    override lazy val incomeTaxObligationsService: String = "http://localhost:9999"
  }

  val obligationsConnector: ObligationsConnector = new ObligationsConnector(mockHttpClientV2, mockAuditingService, appConfig)
  val nextUpdatesService: NextUpdatesService = new NextUpdatesService(obligationsConnector)
  val repository: OptOutSessionDataRepository = mock(classOf[OptOutSessionDataRepository])

  val service: OptOutService = new OptOutService(mockITSAStatusService, mockCalculationListService,
    nextUpdatesService, mockDateService, repository)

  def buildUrl(fromDate: LocalDate, toDate: LocalDate): String = {
    s"http://localhost:9999/income-tax-obligations/$testNino/obligations/from/$fromDate/to/$toDate"
  }

  def buildSuccessResponse(): HttpResponse = {
    HttpResponse(status = Status.OK, json = obligationsDataFromJson, headers = Map.empty)
  }

  "QuarterlyUpdatesCount" when {
    "one year available for opt-out; end-year 2023" should {
      "return successful response" in {

        val proposition = OptOutTestSupport.buildOneYearOptOutPropositionForPreviousYear()
        val optOutTaxYear = proposition.availableTaxYearsForOptOut.head

        val requestUrl = buildUrl(optOutTaxYear.toFinancialYearStart, optOutTaxYear.toFinancialYearEnd)
        val successResponse = buildSuccessResponse()

        setupMockHttpV2Get(requestUrl)(successResponse)

        val result = service.getQuarterlyUpdatesCountForOfferedYears(proposition)

        val expected = QuarterlyUpdatesCountForTaxYearModel(List(QuarterlyUpdatesCountForTaxYear(optOutTaxYear, 2)))

        result.futureValue shouldBe expected
      }
    }
  }

}