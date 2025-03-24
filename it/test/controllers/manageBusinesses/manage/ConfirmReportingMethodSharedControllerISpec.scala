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

package controllers.manageBusinesses.manage


import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.{MTDIndividual, MTDUserRole}
import forms.incomeSources.manage.ConfirmReportingMethodForm
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSourcesFs, IncomeSourcesNewJourney, NavBarFs}
import models.incomeSourceDetails.{LatencyDetails, ManageIncomeSourceData, UIJourneySessionData}
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._

import java.time.LocalDate
import java.time.Month.APRIL

class ConfirmReportingMethodSharedControllerISpec extends ControllerISpecHelper {

  val annual = "Annual"
  val quarterlyIndicator: String = "Q"
  val annuallyIndicator: String = "A"
  val taxYear = "2023-2024"
  val timestamp = "2023-01-31T09:26:17Z"
  val currentTaxYear: Int = dateService.getCurrentTaxYearEnd
  val taxYear1: Int = currentTaxYear
  val taxYear2: Int = currentTaxYear + 1
  val lastDayOfCurrentTaxYear: LocalDate = LocalDate.of(currentTaxYear, APRIL, 5)
  val latencyDetails: LatencyDetails =
    LatencyDetails(
      latencyEndDate = lastDayOfCurrentTaxYear.plusYears(2),
      taxYear1 = taxYear1.toString,
      latencyIndicator1 = quarterlyIndicator,
      taxYear2 = taxYear2.toString,
      latencyIndicator2 = annuallyIndicator
    )

  def getPath(mtdUserRole: MTDUserRole, incomeSourceType: IncomeSourceType, newReportingMethod: String): String = {
    val pathStart = if (mtdUserRole == MTDIndividual) "" else "/agents"
    val pathEnd = incomeSourceType match {
      case SelfEmployment => "/confirm-you-want-to-report"
      case UkProperty => "/confirm-you-want-to-report-uk-property"
      case _ => "/confirm-you-want-to-report-foreign-property"
    }
    pathStart + "/manage-your-businesses/manage" + pathEnd + s"?taxYear=$taxYear&changeTo=$newReportingMethod"
  }

  private lazy val checkYourAnswersController = controllers.manageBusinesses.manage.routes
    .CheckYourAnswersController

  val prefix: String = "manageBusinesses.manage.propertyReportingMethod"

  val continueButtonText: String = "Confirm and save"

  def mainPageTitle(newReportingMethod: String) = messagesAPI(s"$prefix.heading.$newReportingMethod")

  val allReportingMethods = List("annual", "quarterly")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = IncomeSourceJourneyType(Manage, incomeSourceType).toString,
    manageIncomeSourceData = Some(ManageIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), reportingMethod = Some(annual), taxYear = Some(2024))))

  mtdAllRoles.foreach { mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    val pathSE = getPath(mtdUserRole, SelfEmployment)
    s"GET $pathSE" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Confirm Reporting Method page" when {
            "all query parameters are valid" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                  manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseInLatencyPeriod(latencyDetails))

                IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                val result = buildGETMTDClient(pathSE, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, mainPageTitle(reportingMethod)),
                  elementTextByID("confirm-button")(continueButtonText)

                )
              }
            }

          "redirect to home page" when {
            "Income Sources FS is Disabled" in {
              disable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                  manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

                IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                val result = buildGETMTDClient(pathSE, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
              "Income Sources New Journey FS is Disabled" in {
                disable(IncomeSourcesNewJourney)
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                  manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

                IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                val result = buildGETMTDClient(pathSE, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
            }

          }
          testAuthFailures(pathSE, mtdUserRole)
        }
      }

    val pathUK = getPath(mtdUserRole, UkProperty)
    s"GET $pathUK" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Confirm Reporting Method page" when {
            "all query parameters are valid" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

                IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

                val result = buildGETMTDClient(pathUK, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, mainPageTitle(reportingMethod)),
                  elementTextByID("confirm-button")(continueButtonText)
                )
              }
            }

          "redirect to home page" when {
            "Income Sources FS is Disabled" in {
              disable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

                IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

                val result = buildGETMTDClient(pathUK, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
              "Income Sources New Journey FS is Disabled" in {
                enable(IncomeSourcesFs)
                disable(IncomeSourcesNewJourney)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKPropertyResponseInLatencyPeriod(latencyDetails))

                IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

                val result = buildGETMTDClient(pathUK, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
            }

          }
          testAuthFailures(pathUK, mtdUserRole)
        }
      }

    val pathFP = getPath(mtdUserRole, ForeignProperty)
    s"GET $pathFP" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Confirm Reporting Method page" when {
            "all query parameters are valid" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

                IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

                val result = buildGETMTDClient(pathFP, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, mainPageTitle(reportingMethod)),
                  elementTextByID("confirm-button")(continueButtonText)
                )
              }
            }

          "redirect to home page" when {
            "Income Sources FS is Disabled" in {
              disable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

                IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

                val result = buildGETMTDClient(pathFP, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
              "Income Sources New Journey FS is Disabled" in {
                enable(IncomeSourcesFs)
                disable(IncomeSourcesNewJourney)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleForeignPropertyResponseInLatencyPeriod(latencyDetails))

                IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

                val result = buildGETMTDClient(pathFP, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
            }

          }
          testAuthFailures(pathFP, mtdUserRole)
        }
      }

    s"POST $pathSE" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          s"redirect to Check your answers" when {
            "called with a valid form" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                  manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

                await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

                val result = buildPOSTMTDPostClient(pathSE, additionalCookies, body = Map()).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(checkYourAnswersController.show(isAgent, SelfEmployment).url)
                )
              }
            }

            "redirect to home page" when {
              "Income Sources FS is disabled" in {
                disable(IncomeSourcesFs)
                enable(IncomeSourcesNewJourney)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

                IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))

                val result = buildPOSTMTDPostClient(pathSE, additionalCookies, body = formData).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
              "Income Sources New JourneyFS is disabled" in {
                enable(IncomeSourcesFs)
                disable(IncomeSourcesNewJourney)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

                IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

                val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))

                val result = buildPOSTMTDPostClient(pathSE, additionalCookies, body = formData).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
            }

          }

          s"return ${Status.BAD_REQUEST}" when {
            "called with a invalid form" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
                manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

              val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))

              val result = buildPOSTMTDPostClient(pathSE, additionalCookies, body = formData).futureValue

              result should have(
                httpStatus(BAD_REQUEST)
              )
            }
          }

          "redirect to home page" when {
            "Income Sources FS is disabled" in {
              disable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

              val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))

              val result = buildPOSTMTDPostClient(pathSE, additionalCookies, body = formData).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(homeUrl(mtdUserRole))
              )
            }
          }

        }

        testAuthFailures(pathSE, mtdUserRole, optBody = Some(Map
        (ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
        )))
      }
    }

    s"POST $pathUK" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          s"redirect to Check your answers" when {
            "called with a valid form" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

              val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

              val result = buildPOSTMTDPostClient(pathUK, additionalCookies, body = formData).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(checkYourAnswersController.show(isAgent, UkProperty).url)
              )
            }
          }

          s"return ${Status.BAD_REQUEST}" when {
            "called with a invalid form" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

              val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("RANDOM"))

              val result = buildPOSTMTDPostClient(pathUK, additionalCookies, body = formData).futureValue

              result should have(
                httpStatus(BAD_REQUEST)
              )
            }
          }

          "redirect to home page" when {
            "Income Sources FS is disabled" in {
              disable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

              val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

              val result = buildPOSTMTDPostClient(pathUK, additionalCookies, body = formData).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(homeUrl(mtdUserRole))
              )
            }
          }

          testAuthFailures(pathUK, mtdUserRole, optBody = Some(Map
          (ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
          )))
        }
      }

    s"POST $pathFP" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          s"redirect to check your answers" when {
            "called with a valid form" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

                await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

                val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

                val result = buildPOSTMTDPostClient(pathUK, additionalCookies, body = formData).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(checkYourAnswersController.show(isAgent, UkProperty).url)
                )
              }
            }

          s"return ${Status.BAD_REQUEST}" when {
            "called with a invalid form" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

                await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

                val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

                val result = buildPOSTMTDPostClient(pathUK, additionalCookies, body = formData).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
              "Income Sources New Journey FS is disabled" in {
                enable(IncomeSourcesFs)
                disable(IncomeSourcesNewJourney)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

                await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

                val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

                val result = buildPOSTMTDPostClient(pathUK, additionalCookies, body = formData).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
            }

          "redirect to home page" when {
            "Income Sources FS is disabled" in {
              disable(IncomeSourcesNewJourney)
              disable(NavBarFs)
               stubAuthorised(mtdUserRole)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

              val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

              val result = buildPOSTMTDPostClient(pathFP, additionalCookies, body = formData).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(homeUrl(mtdUserRole))
              )
            }
          }

          testAuthFailures(pathFP, mtdUserRole,
            Some(Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
            )))
          }
        }
      }

      s"POST $pathFP" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            s"redirect to check your answers" when {
              "called with a valid form" in {
                enable(IncomeSourcesFs)
                enable(IncomeSourcesNewJourney)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

                await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

                val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

                val result = buildPOSTMTDPostClient(pathFP, additionalCookies, body = formData).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(checkYourAnswersController.show(isAgent, ForeignProperty).url)
                )
              }
            }

            "redirect to home page" when {
              "Income Sources FS is disabled" in {
                disable(IncomeSourcesFs)
                enable(IncomeSourcesNewJourney)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

                await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

                val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

                val result = buildPOSTMTDPostClient(pathFP, additionalCookies, body = formData).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }

              "Income Sources New Journey FS is disabled" in {
                enable(IncomeSourcesFs)
                disable(IncomeSourcesNewJourney)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

                await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

                val formData = Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("true"))

                val result = buildPOSTMTDPostClient(pathFP, additionalCookies, body = formData).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
            }

            testAuthFailures(pathFP, mtdUserRole,
              Some(Map(ConfirmReportingMethodForm.confirmReportingMethod -> Seq("Test Business")
              )))
          }
        }
      }
    }
  }
}
