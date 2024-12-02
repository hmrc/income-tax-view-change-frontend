/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.incomeSources.manage

import auth.MtdItUser
import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import forms.incomeSources.manage.ConfirmReportingMethodForm
import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDIndividualAuthStub}
import models.admin.{IncomeSourcesFs, NavBarFs}
import models.incomeSourceDetails.{LatencyDetails, ManageIncomeSourceData, UIJourneySessionData}
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.mvc.Http.Status
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate
import java.time.Month.APRIL

class ConfirmReportingMethodSharedControllerISpec extends ControllerISpecHelper {

  val annual = "Annual"
  val quarterly = "Quarterly"
  val quarterlyIndicator: String = "Q"
  val annuallyIndicator: String = "A"
  val taxYear = "2023-2024"
  val timestamp = "2023-01-31T09:26:17Z"
  val currentTaxYear: Int = dateService.getCurrentTaxYearEnd
  val taxYear1: Int = currentTaxYear
  val taxYear2: Int = currentTaxYear + 1
  val taxYear1YYtoYY: String = s"${(taxYear1 - 1).toString.takeRight(2)}-${taxYear1.toString.takeRight(2)}"
  val taxYear1YYYYtoYY: String = "20" + taxYear1YYtoYY
  val taxYearYYYYtoYYYY = s"${taxYear1 - 1}-$taxYear1"
  val lastDayOfCurrentTaxYear: LocalDate = LocalDate.of(currentTaxYear, APRIL, 5)
  val latencyDetails: LatencyDetails =
    LatencyDetails(
      latencyEndDate = lastDayOfCurrentTaxYear.plusYears(2),
      taxYear1 = taxYear1.toString,
      latencyIndicator1 = quarterlyIndicator,
      taxYear2 = taxYear2.toString,
      latencyIndicator2 = annuallyIndicator
    )

  private lazy val manageObligationsController = controllers.incomeSources.manage.routes
    .ManageObligationsController

  val pathSE = "/income-sources/manage/confirm-you-want-to-report" + s"?changeTo=$annual&taxYear=$taxYear"
  val pathUK = "/income-sources/manage/confirm-you-want-to-report-uk-property" + s"?taxYear=$taxYear&changeTo=$annual"
  val pathOV = "/income-sources/manage/confirm-you-want-to-report-foreign-property" + s"?taxYear=$taxYear&changeTo=$annual"


  val manageObligationsShowUKPropertyUrl: String = manageObligationsController
    .showUKProperty(changeTo = annual, taxYear = taxYear).url
  val manageObligationsShowForeignPropertyUrl: String = manageObligationsController
    .showForeignProperty(changeTo = annual, taxYear = taxYear).url
  val manageObligationsShowSelfEmploymentUrl: String = manageObligationsController
    .showSelfEmployment(changeTo = annual, taxYear = taxYear).url

  val prefix: String = "incomeSources.manage.propertyReportingMethod"

  val continueButtonText: String = messagesAPI("base.confirm-this-change")

  val pageTitle = messagesAPI(s"$prefix.heading.annual")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = IncomeSourceJourneyType(Manage, incomeSourceType).toString,
    manageIncomeSourceData = Some(ManageIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), reportingMethod = Some(annual), taxYear = Some(2024))))

  s"GET $pathSE" when{
    "the user is authenticated, with a valid MTD enrolment" should {
      "render the Confirm Reporting Method page" when {
        "all query parameters are valid" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
            manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod(latencyDetails))

          IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

          val result = buildGETMTDClient(pathSE).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            pageTitleIndividual(pageTitle),
            elementTextByID("confirm-button")(continueButtonText)
          )
        }
      }
      "redirect to home page" when {
        "Income Sources FS is Disabled" in {
          disable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
            manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

          IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

          val result = buildGETMTDClient(pathSE).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)
          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.HomeController.show().url)
          )
        }
      }
    }
    testAuthFailuresForMTDIndividual(pathSE)
  }

  s"GET $pathUK" when {
    " is authenticated, with a valid MTD enrolment" should {
      "render the Confirm Reporting Method page" when {
        "all query parameters are valid" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

          IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

          await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

          val result = buildGETMTDClient(pathUK).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            pageTitleIndividual(pageTitle),
            elementTextByID("confirm-button")(continueButtonText)
          )
        }
      }

      "redirect to home page" when {
        "Income Sources FS is Disabled" in {
          disable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

          IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

          await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

          val result = buildGETMTDClient(pathUK).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)
          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.HomeController.show().url)
          )
        }
      }

    }
    testAuthFailuresForMTDIndividual(pathUK)
  }

  s"GET $pathOV" when {
    " is authenticated, with a valid MTD enrolment" should {
      "render the Confirm Reporting Method page" when {
        "all query parameters are valid" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

          IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

          await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

          val result = buildGETMTDClient(pathOV).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            pageTitleIndividual(pageTitle),
            elementTextByID("confirm-button")(continueButtonText)
          )
        }
      }

      "redirect to home page" when {
        "Income Sources FS is Disabled" in {
          disable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

          IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

          await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

          val result = buildGETMTDClient(pathOV).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)
          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.HomeController.show().url)
          )
        }
      }
    }
    testAuthFailuresForMTDIndividual(pathOV)
  }


  s"POST $pathSE" when {
    " is authenticated, with a valid MTD enrolment" should {
      s"redirect to $manageObligationsShowSelfEmploymentUrl" when {
        "called with a valid form" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
            manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

          IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

          await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

          val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

          val result = buildPOSTMTDPostClient(pathSE, body = formData).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(manageObligationsShowSelfEmploymentUrl)
          )
        }
      }

      s"return ${Status.BAD_REQUEST}" when {
        "called with a invalid form" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
            manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

          IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

          val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))

          val result = buildPOSTMTDPostClient(pathSE, body = formData).futureValue

          result should have(
            httpStatus(BAD_REQUEST)
          )

        }
      }

      "redirect to home page" when {
        "Income Sources FS is disabled" in {
          disable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

          IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

          val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))

          val result = buildPOSTMTDPostClient(pathSE, body = formData).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.HomeController.show().url)
          )
        }
      }

    }

    testAuthFailuresForMTDIndividual(pathSE, optBody = Some(Map
    (ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
    )))
  }

  s"POST $pathUK" when {
    " is authenticated, with a valid MTD enrolment" should {
      s"redirect to $manageObligationsShowUKPropertyUrl" when {
        "called with a valid form" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

          await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

          val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))


          val result = buildPOSTMTDPostClient(pathUK, body = formData).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(manageObligationsShowUKPropertyUrl)
          )

        }
      }

      s"return ${Status.BAD_REQUEST}" when {
        "called with a invalid form" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

          await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

          val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))

          val result = buildPOSTMTDPostClient(pathUK, body = formData).futureValue

          result should have(
            httpStatus(BAD_REQUEST)
          )

        }
      }

      "redirect to home page" when {
        "Income Sources FS is disabled" in {
          disable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

          await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

          val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

          val result = buildPOSTMTDPostClient(pathUK, body = formData).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.HomeController.show().url)
          )
        }
      }

      testAuthFailuresForMTDIndividual(pathUK, optBody = Some(Map
      (ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
      )))
    }
  }

  s"POST $pathOV" when {
    " is authenticated, with a valid MTD enrolment" should {
      s"redirect to $manageObligationsShowForeignPropertyUrl" when {
        "called with a valid form" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

          await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

          val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

          val result = buildPOSTMTDPostClient(pathOV, body = formData).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(manageObligationsShowForeignPropertyUrl)
          )
        }
      }

      s"return ${Status.BAD_REQUEST}" when {
        "called with a invalid form" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

          await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

          val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))

          val result = buildPOSTMTDPostClient(pathOV, body = formData).futureValue

          result should have(
            httpStatus(BAD_REQUEST)
          )
        }
      }

      "redirect to home page" when {
        "Income Sources FS is disabled" in {
          disable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

          await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

          val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

          val result = buildPOSTMTDPostClient(pathOV, body = formData).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.HomeController.show().url)
          )
        }
      }
    }
  }
}
