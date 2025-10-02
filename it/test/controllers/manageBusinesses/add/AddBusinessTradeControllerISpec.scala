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

package controllers.manageBusinesses.add

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import forms.manageBusinesses.add.BusinessTradeForm
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.UIJourneySessionData
import models.admin.NavBarFs
import models.core.NormalMode
import models.incomeSourceDetails.AddIncomeSourceData.businessTradeField
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesResponse, noPropertyOrBusinessResponse}

class AddBusinessTradeControllerISpec extends ControllerISpecHelper {

  val pageTitleMsgKey: String = messagesAPI("add-trade.heading")
  val pageBody: String = messagesAPI("add-trade.trade-info-1")
  val pageHint: String = messagesAPI("add-trade.trade-info-2")
  val button: String = messagesAPI("base.continue")
  val testBusinessName: String = "Test Business Name"
  val testBusinessTrade: String = "Test Business Trade"

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  def getPath(mtdRole: MTDUserRole, isChange: Boolean): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    val pathEnd = if(isChange) "/change-business-trade" else "/business-trade"
    pathStart + "/manage-your-businesses/add-sole-trader" + pathEnd
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole, isChange = false)
    val changePath = getPath(mtdUserRole, isChange = true)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Add Business trade page for an Agent" when {
            "using the manage businesses journey" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
                addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

              val res = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              res should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextByID("business-trade-description")(pageBody),
                elementTextByID("business-trade-hint")(pageHint),
                elementTextByID("continue-button")(button)
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
          "render the Add Business trade page for an Agent" when {
            "using the manage businesses journey" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
                addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))
              val res = buildGETMTDClient(changePath, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              res should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, pageTitleMsgKey),
                elementTextByID("business-trade-hint")(pageHint),
                elementTextByID("continue-button")(button)
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
          s"303 SEE_OTHER and redirect to add business address" when {
            "User is authorised and business trade is valid" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val formData: Map[String, Seq[String]] = {
                Map(
                  BusinessTradeForm.businessTrade -> Seq(testBusinessTrade)
                )
              }

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
                addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

              val result = buildPOSTMTDPostClient(path, additionalCookies, formData).futureValue

              sessionService.getMongoKeyTyped[String](businessTradeField, IncomeSourceJourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(testBusinessTrade))

              val expectedUrl = if(mtdUserRole == MTDIndividual) {
                controllers.manageBusinesses.add.routes.AddBusinessAddressController.show(mode = NormalMode).url
              } else {
                controllers.manageBusinesses.add.routes.AddBusinessAddressController.showAgent(mode = NormalMode).url
              }
              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(expectedUrl)
              )
            }
          }
          "show error when form is filled incorrectly" in {
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

            await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
              addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

            val formData: Map[String, Seq[String]] = {
              Map(
                BusinessTradeForm.businessTrade -> Seq("")
              )
            }

            val result = buildPOSTMTDPostClient(path, additionalCookies, formData).futureValue

            result should have(
              httpStatus(BAD_REQUEST),
              elementTextByID("business-trade-error")(messagesAPI("base.error-prefix") + " " +
                messagesAPI("add-business-trade.form.error.empty"))
            )
          }
        }

        testAuthFailures(path, mtdUserRole, optBody = Some(Map(
          BusinessTradeForm.businessTrade -> Seq(testBusinessTrade)
        )))
      }
    }

    s"POST $changePath" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          s"303 SEE_OTHER and redirect to Check details" when {
            "User is authorised and business trade is valid" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val changedTrade = "Updated Business Trade"
              val formData: Map[String, Seq[String]] = {
                Map(
                  BusinessTradeForm.businessTrade -> Seq(changedTrade)
                )
              }

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
                addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName), Some(testBusinessTrade))))))

              val result = buildPOSTMTDPostClient(changePath, additionalCookies, formData).futureValue

              sessionService.getMongoKeyTyped[String](businessTradeField, IncomeSourceJourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(changedTrade))

              val expectedUrl = if(mtdUserRole == MTDIndividual) {
                controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url
              } else {
                controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url
              }
              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(expectedUrl)
              )
            }
          }
          "show error when form is filled incorrectly" in {
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

            await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
              addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName), Some(testBusinessTrade))))))

            val formData: Map[String, Seq[String]] = {
              Map(
                BusinessTradeForm.businessTrade -> Seq("")
              )
            }

            val result = buildPOSTMTDPostClient(changePath, additionalCookies, formData).futureValue

            result should have(
              httpStatus(BAD_REQUEST),
              elementTextByID("business-trade-error")(messagesAPI("base.error-prefix") + " " +
                messagesAPI("add-business-trade.form.error.empty"))
            )
          }
        }

        testAuthFailures(changePath, mtdUserRole, optBody = Some(Map(
          BusinessTradeForm.businessTrade -> Seq(testBusinessTrade)
        )))
      }
    }
  }

}
