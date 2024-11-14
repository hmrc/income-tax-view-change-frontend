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

package controllers.optOut

import com.github.tomakehurst.wiremock.client.WireMock
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import controllers.ControllerISpecBase
import helpers.servicemocks.{AuthStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.FeatureSwitchName.allFeatureSwitches
import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.businessAndPropertyResponseWoMigration
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}

class OptOutCancelledControllerISpec extends ControllerISpecBase with FeatureSwitching {

  override val appConfig: FrontendAppConfig = testAppConfig

  lazy val httpClient: HttpClientV2 = app.injector.instanceOf(classOf[HttpClientV2])

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    WireMock.reset()
    cache.removeAll()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  "GET - /report-quarterly/income-and-expenses/view/optout/cancelled" when {

    "the user is authorised" should {

      "return the OptOutCancelled page, OK - 200" in {

        allFeatureSwitches.foreach(switch => disable(switch))

        AuthStub.stubAuthorised()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(dateService.getCurrentTaxYearEnd)

        val res =
          httpClient
            .get(url"http://localhost:$port/report-quarterly/income-and-expenses/view/optout/cancelled")
            .setHeader(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ clientDetailsWithConfirmation), "X-Session-ID" -> testSessionId)
            .execute[HttpResponse]

        res.futureValue.status shouldBe OK
        Jsoup.parse(res.futureValue.body).title shouldBe "Opt out cancelled - Manage your Income Tax updates - GOV.UK"
      }
    }
  }
}
