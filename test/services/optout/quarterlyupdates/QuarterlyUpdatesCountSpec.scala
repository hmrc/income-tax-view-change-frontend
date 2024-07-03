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

package services.optout.quarterlyupdates

import audit.mocks.MockAuditingService
import config.FrontendAppConfig
import connectors.ObligationsConnector
import connectors.optout.ITSAStatusUpdateConnector
import mocks.MockHttp
import mocks.services.{MockCalculationListService, MockDateService, MockITSAStatusService}
import org.mockito.Mockito.mock
import org.scalatest.BeforeAndAfter
import play.api.Configuration
import play.mvc.Http.Status
import repositories.UIJourneySessionDataRepository
import services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import services.optout.OptOutService.QuarterlyUpdatesCountForTaxYearModel
import services.optout.{OptOutService, OptOutTestSupport}
import services.{DateService, NextUpdatesService}
import testConstants.BaseTestConstants.testNino
import testUtils.UnitSpec
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import services.optout.quarterlyupdates.BackendServiceJsonSamples.obligationsDataFromJson

import java.time.LocalDate

class QuarterlyUpdatesCountSpec extends UnitSpec
  with BeforeAndAfter
  with MockITSAStatusService
  with MockCalculationListService
  with MockDateService
  with MockHttp
  with MockAuditingService {

  implicit override val dateService: DateService = mockDateService

  override val appConfig: FrontendAppConfig = new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
    override lazy val itvcProtectedService: String = "http://localhost:9999"
  }

  val optOutConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])
  val obligationsConnector: ObligationsConnector = new ObligationsConnector(httpClientMock, mockAuditingService, appConfig)
  val nextUpdatesService: NextUpdatesService = new NextUpdatesService(obligationsConnector)
  val repository: UIJourneySessionDataRepository = mock(classOf[UIJourneySessionDataRepository])

  val service: OptOutService = new OptOutService(optOutConnector, mockITSAStatusService, mockCalculationListService,
    nextUpdatesService, mockDateService, repository)

  def buildUrl(fromDate: LocalDate, toDate: LocalDate): String = {
    s"http://localhost:9999/income-tax-view-change/$testNino/report-deadlines/from/$fromDate/to/$toDate"
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

        setupMockHttpGet(requestUrl)(successResponse)

        val result = service.getQuarterlyUpdatesCountForOfferedYears(proposition)

        val onlyQuarterlyAndFulfilled = QuarterlyUpdatesCountForTaxYearModel(List(QuarterlyUpdatesCountForTaxYear(optOutTaxYear, 2)))

        result.futureValue shouldBe onlyQuarterlyAndFulfilled
      }
    }
  }

}