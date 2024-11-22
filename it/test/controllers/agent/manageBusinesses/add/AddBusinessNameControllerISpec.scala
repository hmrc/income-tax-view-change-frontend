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

package controllers.agent.manageBusinesses.add

import controllers.agent.ControllerISpecHelper
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, IncomeSources}
import enums.{MTDPrimaryAgent, MTDSupportingAgent}
import forms.incomeSources.add.BusinessNameForm
import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDAgentAuthStub}
import models.admin.{IncomeSourcesFs, NavBarFs}
import models.incomeSourceDetails.AddIncomeSourceData.businessNameField
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{getAgentClientDetailsForCookie, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

import scala.concurrent.ExecutionContext

class AddBusinessNameControllerISpec extends ControllerISpecHelper {

  val addBusinessStartDateUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false).url
  val incomeSourcesUrl: String = controllers.routes.HomeController.showAgent.url

  val formHint: String = messagesAPI("add-business-name.p1") + " " +
    messagesAPI("add-business-name.p2", "'")
  val continueButtonText: String = messagesAPI("base.continue")
  val testBusinessName: String = "Test Business"
  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  override implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val journeyType: IncomeSources = IncomeSources(Add, SelfEmployment)

  def backUrl(isChange: Boolean): String = {
    if (isChange) {
        controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url
    } else {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
    }
  }

  val path = "/agents/manage-your-businesses/add-sole-trader/business-name"
  val changePath = "/agents/manage-your-businesses/add-sole-trader/change-business-name"

  List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          "render the Add Business Name page" when {
            "income sources feature is enabled" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              lazy val document: Document = Jsoup.parse(result.body)
              document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl(isChange = false)

              result should have(
                httpStatus(OK),
                pageTitleAgent("add-business-name.heading"),
                elementTextByID("business-name-hint > p")(formHint),
                elementTextByID("continue-button")(continueButtonText)
              )
            }
          }
          "303 SEE_OTHER - redirect to home page" when {
            "Income Sources FS disabled" in {
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              disable(IncomeSourcesFs)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(incomeSourcesUrl)
              )
            }
          }
        }
        testAuthFailuresForMTDAgent(path, isSupportingAgent)
      }
    }

    s"GET $changePath" when {
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          "render the Add Business Name page" when {
            "User is authorised" in {
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              enable(IncomeSourcesFs)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val result = buildGETMTDClient(changePath, additionalCookies).futureValue

              lazy val document: Document = Jsoup.parse(result.body)
              document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl(isChange = true)

              result should have(
                httpStatus(OK),
                pageTitleAgent("add-business-name.heading"),
                elementTextByID("business-name-hint > p")(formHint),
                elementTextByID("continue-button")(continueButtonText)
              )
            }
          }
          "303 SEE_OTHER - redirect to home page" when {
            "Income Sources FS disabled" in {
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              disable(IncomeSourcesFs)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val result = buildGETMTDClient(changePath, additionalCookies).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(incomeSourcesUrl)
              )
            }
          }
        }
        testAuthFailuresForMTDAgent(changePath, isSupportingAgent)
      }
    }

    s"POST $path" when {
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          s"303 SEE_OTHER and redirect to $addBusinessStartDateUrl" when {
            "the income sources is enabled" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val formData: Map[String, Seq[String]] = {
                Map(
                  BusinessNameForm.businessName -> Seq("Test Business")
                )
              }

              val result = buildPOSTMTDPostClient(path, additionalCookies, formData).futureValue
              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(addBusinessStartDateUrl)
              )

              sessionService.getMongoKeyTyped[String](businessNameField, journeyType).futureValue shouldBe Right(Some(testBusinessName))
            }
          }
          "show error when form is filled incorrectly" in {
            enable(IncomeSourcesFs)
            disable(NavBarFs)
            MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

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
        testAuthFailuresForMTDAgent(path, isSupportingAgent, optBody = Some(Map(
          BusinessNameForm.businessName -> Seq("Test Business")
        )))
      }
    }

    s"POST $changePath" when {
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          val expectedRedirectUrl = controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url
          s"303 SEE_OTHER and redirect to $expectedRedirectUrl" when {
            "the income sources is enabled" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
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
            enable(IncomeSourcesFs)
            disable(NavBarFs)
            MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

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
        testAuthFailuresForMTDAgent(changePath, isSupportingAgent, optBody = Some(Map(
          BusinessNameForm.businessName -> Seq("Test Business")
        )))
      }
    }
  }
}
