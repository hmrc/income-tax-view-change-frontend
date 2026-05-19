/*
 * Copyright 2026 HM Revenue & Customs
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

package businessDetails.controllers.manageBusinesses.add

import businessDetails.enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment}
import businessDetails.forms.manageBusinesses.add.IsTheNewAddressInTheUKForm
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import businessDetails.helpers.IncomeSourceCheckDetailsConstants.{testBusinessName, testBusinessStartDate, testBusinessTrade}
import common.controllers.ControllerISpecHelper
import common.services.SessionService
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.UIJourneySessionData
import models.admin.OverseasBusinessAddress
import models.core.NormalMode
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class IsTheNewAddressInTheUKControllerISpec extends ControllerISpecHelper {

  val continueButtonText: String = messagesAPI("base.continue")
  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  private def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/manage-your-businesses/add-sole-trader/is-the-new-address-in-the-uk"
  }

  private def getAddBusinessAddressControllerUrlByType(isAgent: Boolean) =
    if isAgent then routes.AddBusinessAddressController.showAgent(NormalMode).url
    else routes.AddBusinessAddressController.show(NormalMode).url

  private def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = IncomeSourceJourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(AddIncomeSourceData(
      businessName = Some(testBusinessName),
      businessTrade = Some(testBusinessTrade),
      dateStarted = Some(testBusinessStartDate),
      incomeSourceId = Some("ID")
    )))

  List(MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent).foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)

    s"GET $path" when {
      s"user is ${if (mtdUserRole != MTDSupportingAgent) "an" else "a"} $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the IsTheNewAddressInTheUK page" when {
            "OverseasBusinessAddress FS is enabled" in {
              stubAuthorised(mtdUserRole, List(OverseasBusinessAddress))
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "add-business-is.the.new.address.in.the.uk.heading")
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
    s"POST $path" when {
      s"user is ${if (mtdUserRole != MTDSupportingAgent) "an" else "a"} $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "form response is Yes" in {
            stubAuthorised(mtdUserRole, List(OverseasBusinessAddress))
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

            await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

            val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map(IsTheNewAddressInTheUKForm.response -> Seq("Yes"))).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(getAddBusinessAddressControllerUrlByType(mtdUserRole != MTDIndividual))
            )
          }
          "form response is No" in {
            val isAgent: Boolean = mtdUserRole != MTDIndividual
            stubAuthorised(mtdUserRole, List(OverseasBusinessAddress))
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

            await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

            val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map(IsTheNewAddressInTheUKForm.response -> Seq("No"))).futureValue

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.AddInternationalBusinessAddressController.show(isAgent).url)
            )
          }
          "return a BAD_REQUEST" when {
            "form is empty" in {
              stubAuthorised(mtdUserRole, List(OverseasBusinessAddress))
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

              val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map(IsTheNewAddressInTheUKForm.response -> Seq())).futureValue

              result should have(
                httpStatus(BAD_REQUEST)
              )
            }
            "form is invalid" in {
              stubAuthorised(mtdUserRole, List(OverseasBusinessAddress))
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

              val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map(IsTheNewAddressInTheUKForm.response -> Seq("Invalid"))).futureValue

              result should have(
                httpStatus(BAD_REQUEST)
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole, optBody = Some(Map.empty))
      }
    }
  }
}
