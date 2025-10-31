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
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.UIJourneySessionData
import models.admin.NavBarFs
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.ExecutionContext

class IncomeSourcesAccountingMethodControllerISpec extends ControllerISpecHelper {

  def accountingMethodKey(incomeSourceType: IncomeSourceType): String = "incomeSources.add." + incomeSourceType.key + ".AccountingMethod"
  val continueButtonText: String = messagesAPI("base.continue")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  implicit override val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit override val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType, accountingMethod: Option[String]): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = IncomeSourceJourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(
      AddIncomeSourceData(
        businessName  = if (incomeSourceType.equals(SelfEmployment)) Some("testBusinessName")  else None,
        businessTrade = if (incomeSourceType.equals(SelfEmployment)) Some("testBusinessTrade") else None,
        incomeSourcesAccountingMethod = accountingMethod
      )
    )
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
    await(sessionService.createSession(IncomeSourceJourneyType(Add, SelfEmployment)))
    await(sessionService.createSession(IncomeSourceJourneyType(Add, UkProperty)))
    await(sessionService.createSession(IncomeSourceJourneyType(Add, ForeignProperty)))
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "/manage-your-businesses/add" else "/agents/manage-your-businesses/add"
    incomeSourceType match {
      case SelfEmployment => s"$pathStart/business-accounting-method"
      case UkProperty => s"$pathStart/uk-property-accounting-method"
      case ForeignProperty => s"$pathStart/foreign-property-accounting-method"
    }
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the Business Accounting Method page" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "incomeSources.add." + incomeSourceType.key + ".AccountingMethod.heading"),
                elementTextByID("continue-button")(continueButtonText)
              )
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }
      s"POST $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            s"add 'cash' to session storage and redirect to check business details" when {
              "user selects 'cash basis accounting'" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

                val formData: Map[String, Seq[String]] = Map(accountingMethodKey(incomeSourceType) -> Seq("cash"))
                val result = buildPOSTMTDPostClient(path, additionalCookies, body = formData).futureValue

                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType, Some("cash"))))

                val session = sessionService.getMongo(IncomeSourceJourneyType(Add, incomeSourceType))(hc, ec).futureValue

                val resultAccountingMethod = session match {
                  case Right(Some(uiJourneySessionData)) =>
                    uiJourneySessionData.addIncomeSourceData.get.incomeSourcesAccountingMethod
                  case _ => None
                }

                val expectedUrl = if(mtdUserRole == MTDIndividual) {
                  controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(incomeSourceType).url
                } else {
                  controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType).url
                }

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(expectedUrl)
                )
                resultAccountingMethod shouldBe Some("cash")
              }
            }

            s"add 'accruals' to session storage and redirect to check business details" when {
              "user selects 'traditional accounting'" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

                val formData: Map[String, Seq[String]] = Map(accountingMethodKey(incomeSourceType) -> Seq("traditional"))
                val result = buildPOSTMTDPostClient(path, additionalCookies, body = formData).futureValue

                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType, Some("accruals"))))

                val session = sessionService.getMongo(IncomeSourceJourneyType(Add, incomeSourceType))(hc, ec).futureValue

                val resultAccountingMethod = session match {
                  case Right(Some(uiJourneySessionData)) =>
                    uiJourneySessionData.addIncomeSourceData.get.incomeSourcesAccountingMethod
                  case _ => None
                }

                val expectedUrl = if(mtdUserRole == MTDIndividual) {
                  controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(incomeSourceType).url
                } else {
                  controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType).url
                }

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(expectedUrl)
                )
                resultAccountingMethod shouldBe Some("accruals")
              }
            }

            s"return BAD_REQUEST" when {
              "user does not select anything" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

                val formData: Map[String, Seq[String]] = Map(accountingMethodKey(incomeSourceType) -> Seq(""))
                val result = buildPOSTMTDPostClient(path, additionalCookies, body = formData).futureValue

                result should have(
                  httpStatus(BAD_REQUEST),
                  elementTextByClass("govuk-error-summary__title")(messagesAPI("base.error_summary.heading"))
                )
              }
            }
          }
          testAuthFailures(path, mtdUserRole, optBody = Some(Map(accountingMethodKey(incomeSourceType) -> Seq("traditional"))))
        }
      }
    }
  }
}
