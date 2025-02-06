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

package controllers.manageBusinesses.add

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import forms.incomeSources.add.BusinessNameForm
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSourcesFs, NavBarFs}
import models.incomeSourceDetails.AddIncomeSourceData.businessNameField
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

import scala.concurrent.ExecutionContext

class AddBusinessNameControllerISpec extends ControllerISpecHelper {

  val formHint: String = messagesAPI("add-business-name.p1") + " " +
    messagesAPI("add-business-name.p2", "'")
  val continueButtonText:   String                  = messagesAPI("base.continue")
  val testBusinessName:     String                  = "Test Business"
  val sessionService:       SessionService          = app.injector.instanceOf[SessionService]
  override implicit val ec: ExecutionContext        = app.injector.instanceOf[ExecutionContext]
  val journeyType:          IncomeSourceJourneyType = IncomeSourceJourneyType(Add, SelfEmployment)

  def backUrl(isChange: Boolean, isAgent: Boolean): String = {
    if (isChange) {
      val incomeSourceController = controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController
      if (isAgent) incomeSourceController.showAgent(SelfEmployment).url
      else incomeSourceController.show(SelfEmployment).url
    } else {
      val manageBusinessController = controllers.manageBusinesses.routes.ManageYourBusinessesController
      if (isAgent) manageBusinessController.showAgent().url else manageBusinessController.show().url
    }
  }

  def getPath(mtdRole: MTDUserRole, isChange: Boolean): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    val pathEnd   = if (isChange) "/change-business-name" else "/business-name"
    pathStart + "/manage-your-businesses/add-sole-trader" + pathEnd
  }

  mtdAllRoles.foreach {
    case mtdUserRole =>
      val path              = getPath(mtdUserRole, isChange = false)
      val changePath        = getPath(mtdUserRole, isChange = true)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the Add Business Name page" when {
              "income sources feature is enabled" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  noPropertyOrBusinessResponse
                )

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                lazy val document: Document = Jsoup.parse(result.body)
                document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl(
                  isChange = false,
                  mtdUserRole != MTDIndividual
                )

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "add-business-name.heading"),
                  elementTextByID("business-name-hint > p")(formHint),
                  elementTextByID("continue-button")(continueButtonText)
                )
              }
            }
            "303 SEE_OTHER - redirect to home page" when {
              "Income Sources FS disabled" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                disable(IncomeSourcesFs)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  noPropertyOrBusinessResponse
                )

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }

      s"GET $changePath" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the Add Business Name page" when {
              "User is authorised" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                enable(IncomeSourcesFs)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  noPropertyOrBusinessResponse
                )

                val result = buildGETMTDClient(changePath, additionalCookies).futureValue

                lazy val document: Document = Jsoup.parse(result.body)
                document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl(
                  isChange = true,
                  mtdUserRole != MTDIndividual
                )

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "add-business-name.heading"),
                  elementTextByID("business-name-hint > p")(formHint),
                  elementTextByID("continue-button")(continueButtonText)
                )
              }
            }
            "303 SEE_OTHER - redirect to home page" when {
              "Income Sources FS disabled" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                disable(IncomeSourcesFs)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  noPropertyOrBusinessResponse
                )

                val result = buildGETMTDClient(changePath, additionalCookies).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
            }
          }
          testAuthFailures(changePath, mtdUserRole)
        }
      }

      s"POST $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            s"303 SEE_OTHER and redirect to add business start date" when {
              "the income sources is enabled" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  noPropertyOrBusinessResponse
                )

                val formData: Map[String, Seq[String]] = {
                  Map(
                    BusinessNameForm.businessName -> Seq("Test Business")
                  )
                }

                val result = buildPOSTMTDPostClient(path, additionalCookies, formData).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(
                    controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController
                      .show(incomeSourceType = SelfEmployment, isAgent = mtdUserRole != MTDIndividual, isChange = false)
                      .url
                  )
                )

                sessionService.getMongoKeyTyped[String](businessNameField, journeyType).futureValue shouldBe Right(
                  Some(testBusinessName)
                )
              }
            }
            "show error when form is filled incorrectly" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val formData: Map[String, Seq[String]] = {
                Map(
                  BusinessNameForm.businessName -> Seq("£££")
                )
              }

              val result = buildPOSTMTDPostClient(path, additionalCookies, formData).futureValue

              result should have(
                httpStatus(BAD_REQUEST),
                elementTextByID("business-name-error")(
                  messagesAPI("base.error-prefix") + " " +
                    messagesAPI("add-business-name.form.error.invalidNameFormat")
                )
              )
            }
          }
          testAuthFailures(
            path,
            mtdUserRole,
            optBody = Some(
              Map(
                BusinessNameForm.businessName -> Seq("Test Business")
              )
            )
          )
        }
      }

      s"POST $changePath" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            val expectedRedirectUrl = {
              val controllerRoute = controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController
              if (mtdUserRole == MTDIndividual) controllerRoute.show(SelfEmployment).url
              else controllerRoute.showAgent(SelfEmployment).url
            }
            s"303 SEE_OTHER and redirect to $expectedRedirectUrl" when {
              "the income sources is enabled" in {
                enable(IncomeSourcesFs)
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK,
                  noPropertyOrBusinessResponse
                )

                val formData: Map[String, Seq[String]] = {
                  Map(
                    BusinessNameForm.businessName -> Seq("Test Business")
                  )
                }

                val result = buildPOSTMTDPostClient(changePath, additionalCookies, formData).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(expectedRedirectUrl)
                )

                sessionService.getMongoKeyTyped[String](businessNameField, journeyType).futureValue shouldBe Right(
                  Some(testBusinessName)
                )
              }
            }
            "show error when form is filled incorrectly" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val formData: Map[String, Seq[String]] = {
                Map(
                  BusinessNameForm.businessName -> Seq("£££")
                )
              }

              val result = buildPOSTMTDPostClient(changePath, additionalCookies, formData).futureValue

              result should have(
                httpStatus(BAD_REQUEST),
                elementTextByID("business-name-error")(
                  messagesAPI("base.error-prefix") + " " +
                    messagesAPI("add-business-name.form.error.invalidNameFormat")
                )
              )
            }
          }
          testAuthFailures(
            changePath,
            mtdUserRole,
            optBody = Some(
              Map(
                BusinessNameForm.businessName -> Seq("Test Business")
              )
            )
          )
        }
      }
  }

}
