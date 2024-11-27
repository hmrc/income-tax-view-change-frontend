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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import helpers.servicemocks.{AuthStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.FeatureSwitchName.allFeatureSwitches
import models.admin.{OptOut, ReportingFrequencyPage}
import models.itsaStatus.ITSAStatus.{Annual, Mandated, Voluntary}
import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.businessAndPropertyResponseWoMigration
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}

class ReportingFrequencyControllerISpec extends ControllerISpecBase with FeatureSwitching {

  override val appConfig: FrontendAppConfig = testAppConfig

  lazy val httpClient: HttpClientV2 = app.injector.instanceOf(classOf[HttpClientV2])
  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  def bullet(i: Int): String = s"#main-content > div > div > div > ul > li:nth-child($i) > a"

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    WireMock.reset()
    cache.removeAll()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  "GET - /report-quarterly/income-and-expenses/view/reporting-frequency" when {

    "ReportingFrequencyPage feature switch is enabled" when {

      "the user is authorised" should {

        "CY is Quaterly and CY+1 is Quaterly" when {

          "return page with OK - 200 with just generic link for opt out" in {

            allFeatureSwitches.foreach(switch => disable(switch))
            enable(ReportingFrequencyPage)
            enable(OptOut)

          AuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            dateService.getCurrentTaxYear,
            Mandated,
            Voluntary,
            Voluntary
          )

            val res =
              httpClient
                .get(url"http://localhost:$port/report-quarterly/income-and-expenses/view/reporting-frequency")
                .setHeader(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ clientDetailsWithConfirmation), "X-Session-ID" -> testSessionId)
                .execute[HttpResponse]

            res.futureValue.status shouldBe OK
            Jsoup.parse(res.futureValue.body).title shouldBe "Your reporting frequency - Manage your Income Tax updates - GOV.UK"
            Jsoup.parse(res.futureValue.body).body().select(bullet(1)).text() shouldBe "Opt out of quarterly reporting and report annually"
          }
        }

        "CY is Annual and CY+1 is Annual" when {

          "return page with OK - 200 with just generic link for opt in" in {

            allFeatureSwitches.foreach(switch => disable(switch))
            enable(ReportingFrequencyPage)
            enable(OptOut)

            AuthStub.stubAuthorised()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
              dateService.getCurrentTaxYear,
              Mandated,
              Annual,
              Annual
            )

            val res =
              httpClient
                .get(url"http://localhost:$port/report-quarterly/income-and-expenses/view/reporting-frequency")
                .setHeader(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ clientDetailsWithConfirmation), "X-Session-ID" -> testSessionId)
                .execute[HttpResponse]

            res.futureValue.status shouldBe OK
            Jsoup.parse(res.futureValue.body).title shouldBe "Your reporting frequency - Manage your Income Tax updates - GOV.UK"
            Jsoup.parse(res.futureValue.body).body().select(bullet(1)).text() shouldBe "Opt in to quarterly reporting"
          }
        }

        "CY is Quaterly and CY+1 is Annual" when {

          "return page with OK - 200 with tax year link for opt out and onwards link for opt in" in {

            allFeatureSwitches.foreach(switch => disable(switch))
            enable(ReportingFrequencyPage)
            enable(OptOut)

            AuthStub.stubAuthorised()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
              dateService.getCurrentTaxYear,
              Mandated,
              Voluntary,
              Annual
            )

            val res =
              httpClient
                .get(url"http://localhost:$port/report-quarterly/income-and-expenses/view/reporting-frequency")
                .setHeader(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ clientDetailsWithConfirmation), "X-Session-ID" -> testSessionId)
                .execute[HttpResponse]

            res.futureValue.status shouldBe OK
            Jsoup.parse(res.futureValue.body).title shouldBe "Your reporting frequency - Manage your Income Tax updates - GOV.UK"
            Jsoup.parse(res.futureValue.body).body().select(bullet(1)).text() shouldBe s"Opt in to quarterly reporting from the ${dateService.getCurrentTaxYear.nextYear.startYear} to ${dateService.getCurrentTaxYear.nextYear.endYear} tax year onwards"
            Jsoup.parse(res.futureValue.body).body().select(bullet(2)).text() shouldBe s"Opt out of quarterly reporting and report annually for the ${dateService.getCurrentTaxYear.startYear} to ${dateService.getCurrentTaxYear.endYear} tax year"
          }
        }

        "CY is Annual and CY+1 is Quaterly" when {

          "return page with OK - 200 with tax year link for opt in and onwards link for opt out" in {

            allFeatureSwitches.foreach(switch => disable(switch))
            enable(ReportingFrequencyPage)
            enable(OptOut)

            AuthStub.stubAuthorised()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
              dateService.getCurrentTaxYear,
              Mandated,
              Annual,
              Voluntary
            )

            val res =
              httpClient
                .get(url"http://localhost:$port/report-quarterly/income-and-expenses/view/reporting-frequency")
                .setHeader(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ clientDetailsWithConfirmation), "X-Session-ID" -> testSessionId)
                .execute[HttpResponse]

            res.futureValue.status shouldBe OK
            Jsoup.parse(res.futureValue.body).title shouldBe "Your reporting frequency - Manage your Income Tax updates - GOV.UK"
            Jsoup.parse(res.futureValue.body).body().select(bullet(1)).text() shouldBe s"Opt in to quarterly reporting for the ${dateService.getCurrentTaxYear.startYear} to ${dateService.getCurrentTaxYear.endYear} tax year"
            Jsoup.parse(res.futureValue.body).body().select(bullet(2)).text() shouldBe s"Opt out of quarterly reporting and report annually from the ${dateService.getCurrentTaxYear.nextYear.startYear} to ${dateService.getCurrentTaxYear.nextYear.endYear} tax year onwards"
          }
        }

        "CY is Annual and CY+1 is Quaterly Mandated" when {

          "return page with OK - 200 with just tax year link for opt in" in {

            allFeatureSwitches.foreach(switch => disable(switch))
            enable(ReportingFrequencyPage)
            enable(OptOut)

            AuthStub.stubAuthorised()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
              dateService.getCurrentTaxYear,
              Mandated,
              Annual,
              Mandated
            )

            val res =
              httpClient
                .get(url"http://localhost:$port/report-quarterly/income-and-expenses/view/reporting-frequency")
                .setHeader(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ clientDetailsWithConfirmation), "X-Session-ID" -> testSessionId)
                .execute[HttpResponse]

            res.futureValue.status shouldBe OK
            Jsoup.parse(res.futureValue.body).title shouldBe "Your reporting frequency - Manage your Income Tax updates - GOV.UK"
            Jsoup.parse(res.futureValue.body).body().select(bullet(1)).text() shouldBe s"Opt in to quarterly reporting for the ${dateService.getCurrentTaxYear.startYear} to ${dateService.getCurrentTaxYear.endYear} tax year"
          }
        }

        "CY is Quaterly Mandated and CY+1 is Annual" when {

          "return page with OK - 200 with just tax year onwards link for opt in" in {

            allFeatureSwitches.foreach(switch => disable(switch))
            enable(ReportingFrequencyPage)
            enable(OptOut)

            AuthStub.stubAuthorised()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
              dateService.getCurrentTaxYear,
              Mandated,
              Mandated,
              Annual
            )

            val res =
              httpClient
                .get(url"http://localhost:$port/report-quarterly/income-and-expenses/view/reporting-frequency")
                .setHeader(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ clientDetailsWithConfirmation), "X-Session-ID" -> testSessionId)
                .execute[HttpResponse]

            res.futureValue.status shouldBe OK
            Jsoup.parse(res.futureValue.body).title shouldBe "Your reporting frequency - Manage your Income Tax updates - GOV.UK"
            Jsoup.parse(res.futureValue.body).body().select(bullet(1)).text() shouldBe s"Opt in to quarterly reporting from the ${dateService.getCurrentTaxYear.nextYear.startYear} to ${dateService.getCurrentTaxYear.nextYear.endYear} tax year onwards"
          }
        }

        "CY is Quaterly Mandated and CY+1 is Quaterly" when {

          "return page with OK - 200 with just tax year onwards link for opt out" in {

            allFeatureSwitches.foreach(switch => disable(switch))
            enable(ReportingFrequencyPage)
            enable(OptOut)

            AuthStub.stubAuthorised()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
              dateService.getCurrentTaxYear,
              Mandated,
              Mandated,
              Voluntary
            )

            val res =
              httpClient
                .get(url"http://localhost:$port/report-quarterly/income-and-expenses/view/reporting-frequency")
                .setHeader(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ clientDetailsWithConfirmation), "X-Session-ID" -> testSessionId)
                .execute[HttpResponse]

            res.futureValue.status shouldBe OK
            Jsoup.parse(res.futureValue.body).title shouldBe "Your reporting frequency - Manage your Income Tax updates - GOV.UK"
            Jsoup.parse(res.futureValue.body).body().select(bullet(1)).text() shouldBe s"Opt out of quarterly reporting and report annually from the ${dateService.getCurrentTaxYear.nextYear.startYear} to ${dateService.getCurrentTaxYear.nextYear.endYear} tax year onwards"
          }
        }

        "CY-1 is Quaterly, CY is Quaterly and CY+1 is Annual" when {

          "return page with OK - 200 with generic link for opt out and tax year onward link for opt in" in {

            allFeatureSwitches.foreach(switch => disable(switch))
            enable(ReportingFrequencyPage)
            enable(OptOut)

            AuthStub.stubAuthorised()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
              dateService.getCurrentTaxYear,
              Voluntary,
              Voluntary,
              Annual
            )

            val res =
              httpClient
                .get(url"http://localhost:$port/report-quarterly/income-and-expenses/view/reporting-frequency")
                .setHeader(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ clientDetailsWithConfirmation), "X-Session-ID" -> testSessionId)
                .execute[HttpResponse]

            res.futureValue.status shouldBe OK
            Jsoup.parse(res.futureValue.body).title shouldBe "Your reporting frequency - Manage your Income Tax updates - GOV.UK"
            Jsoup.parse(res.futureValue.body).body().select(bullet(1)).text() shouldBe s"Opt in to quarterly reporting from the ${dateService.getCurrentTaxYear.nextYear.startYear} to ${dateService.getCurrentTaxYear.nextYear.endYear} tax year onwards"
            Jsoup.parse(res.futureValue.body).body().select(bullet(2)).text() shouldBe s"Opt out of quarterly reporting and report annually"
          }
        }

        "CY-1 is Quaterly, CY is Annual and CY+1 is Annual" when {

          "return page with OK - 200 with tax year link for opt out and generic link for opt in" in {

            allFeatureSwitches.foreach(switch => disable(switch))
            enable(ReportingFrequencyPage)
            enable(OptOut)

            AuthStub.stubAuthorised()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
              dateService.getCurrentTaxYear,
              Voluntary,
              Annual,
              Annual
            )

            val res =
              httpClient
                .get(url"http://localhost:$port/report-quarterly/income-and-expenses/view/reporting-frequency")
                .setHeader(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ clientDetailsWithConfirmation), "X-Session-ID" -> testSessionId)
                .execute[HttpResponse]

            res.futureValue.status shouldBe OK
            Jsoup.parse(res.futureValue.body).title shouldBe "Your reporting frequency - Manage your Income Tax updates - GOV.UK"
            Jsoup.parse(res.futureValue.body).body().select(bullet(1)).text() shouldBe s"Opt in to quarterly reporting"
            Jsoup.parse(res.futureValue.body).body().select(bullet(2)).text() shouldBe s"Opt out of quarterly reporting and report annually for the ${dateService.getCurrentTaxYear.previousYear.startYear} to ${dateService.getCurrentTaxYear.previousYear.endYear} tax year"
          }
        }
      }
    }

    "ReportingFrequencyPage feature switch is disabled" when {

      "the user is authorised" should {

        "return error page, INTERNAL_SERVER_ERROR - 500" in {

          AuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

          disable(ReportingFrequencyPage)

          val res =
            httpClient
              .get(url"http://localhost:$port/report-quarterly/income-and-expenses/view/reporting-frequency")
              .setHeader(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ clientDetailsWithConfirmation), "X-Session-ID" -> testSessionId)
              .execute[HttpResponse]

          res.futureValue.status shouldBe INTERNAL_SERVER_ERROR
          Jsoup.parse(res.futureValue.body).title shouldBe "Sorry, there is a problem with the service - GOV.UK"
        }
      }
    }
  }
}
