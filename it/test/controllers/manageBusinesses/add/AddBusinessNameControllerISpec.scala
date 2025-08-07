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
import forms.manageBusinesses.add.BusinessNameForm
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.NavBarFs
import models.core.{CheckMode, Mode, NormalMode}
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
  val continueButtonText: String = messagesAPI("base.continue")
  val testBusinessName: String = "Test Business"
  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  override implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val journeyType: IncomeSourceJourneyType = IncomeSourceJourneyType(Add, SelfEmployment)

  def backUrl(mode: Mode, isAgent: Boolean): String = {
    if (mode == CheckMode) {
      val incomeSourceController = controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController
      if(isAgent) incomeSourceController.showAgent(SelfEmployment).url else incomeSourceController.show(SelfEmployment).url
    } else {
      val manageBusinessController = controllers.manageBusinesses.routes.ManageYourBusinessesController
      if(isAgent) manageBusinessController.showAgent().url else manageBusinessController.show().url
    }
  }

  def getPath(mtdRole: MTDUserRole, mode: Mode): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    val pathEnd = if(mode == CheckMode) "/change-business-name" else "/business-name"
    pathStart + "/manage-your-businesses/add-sole-trader" + pathEnd
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole, mode = NormalMode)
    val changePath = getPath(mtdUserRole, mode = CheckMode)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Add Business Name page" when {
            "using the manage businesses journey" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              lazy val document: Document = Jsoup.parse(result.body)
              document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl(mode = NormalMode, mtdUserRole != MTDIndividual)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "add-business-name.heading1"),
                elementTextByID("continue-button")(continueButtonText)
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
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val result = buildGETMTDClient(changePath, additionalCookies).futureValue

              lazy val document: Document = Jsoup.parse(result.body)
              document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl(mode = CheckMode, mtdUserRole != MTDIndividual)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "add-business-name.heading1"),
                elementTextByID("continue-button")(continueButtonText)
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
            "using the manage businesses journey" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val formData: Map[String, Seq[String]] = {
                Map(
                  BusinessNameForm.businessName -> Seq("Test Business")
                )
              }

              val result = buildPOSTMTDPostClient(path, additionalCookies, formData).futureValue
              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = mtdUserRole != MTDIndividual, mode = NormalMode).url)
              )

              sessionService.getMongoKeyTyped[String](businessNameField, journeyType).futureValue shouldBe Right(Some(testBusinessName))
            }
          }
          "show error when form is filled incorrectly" in {
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
              elementTextByID("business-name-error")(messagesAPI("base.error-prefix") + " " +
                messagesAPI("add-business-name.form.error.invalidNameFormat"))
            )
          }
        }
        testAuthFailures(path, mtdUserRole, optBody = Some(Map(
          BusinessNameForm.businessName -> Seq("Test Business")
        )))
      }
    }

    s"POST $changePath" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          val expectedRedirectUrl = {
            val controllerRoute = controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController
              if(mtdUserRole == MTDIndividual) controllerRoute.show(SelfEmployment).url else controllerRoute.showAgent(SelfEmployment).url
          }
          s"303 SEE_OTHER and redirect to $expectedRedirectUrl" when {
            "the income sources is enabled" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

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

              sessionService.getMongoKeyTyped[String](businessNameField, journeyType).futureValue shouldBe Right(Some(testBusinessName))
            }
          }
          "show error when form is filled incorrectly" in {
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
              elementTextByID("business-name-error")(messagesAPI("base.error-prefix") + " " +
                messagesAPI("add-business-name.form.error.invalidNameFormat"))
            )
          }
        }
        testAuthFailures(changePath, mtdUserRole, optBody = Some(Map(
          BusinessNameForm.businessName -> Seq("Test Business")
        )))
      }
    }
  }

}
