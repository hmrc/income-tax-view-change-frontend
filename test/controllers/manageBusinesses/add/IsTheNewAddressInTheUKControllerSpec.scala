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

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import forms.manageBusinesses.add.IsTheNewAddressInTheUKForm
import forms.manageBusinesses.add.*
import forms.manageBusinesses.add.IsTheNewAddressInTheUKForm.{responseForeign, responseUK}
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.OverseasBusinessAddress
import models.core.{CheckMode, Mode, NormalMode}
import models.incomeSourceDetails.AddIncomeSourceData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{DateServiceInterface, SessionService}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, emptyUIJourneySessionData}

class IsTheNewAddressInTheUKControllerSpec extends MockAuthActions with MockSessionService {

  val validBusinessName: String = "Test Business Name"

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController: IsTheNewAddressInTheUKController = app.injector.instanceOf[IsTheNewAddressInTheUKController]

  def getAction(mtdRole: MTDUserRole, mode: Mode, isPost: Boolean = false): Action[AnyContent] = mtdRole match {
    case MTDIndividual if isPost => testController.submit(mode, false)
    case MTDIndividual => testController.show(mode, false)
    case _ if isPost => testController.submitAgent(mode, false)
    case _ => testController.showAgent(mode, false)
  }

  def getRequest(isAgent: Boolean): FakeRequest[AnyContentAsEmpty.type] = {
    if (isAgent) fakeRequestConfirmedClient()
    else fakeRequestWithActiveSession
  }

  def postRequest(isAgent: Boolean): FakeRequest[AnyContentAsEmpty.type] = {
    if (isAgent) fakePostRequestConfirmedClient()
    else fakePostRequestWithActiveSession
  }

  def getValidationErrorTabTitle: String = {
    s"${messages("htmlTitle.invalidInput", messages("add-business-is.the.new.address.in.the.uk.heading"))}"
  }

  Seq(CheckMode, NormalMode).foreach { mode =>
    mtdAllRoles.foreach { mtdRole =>
      s"show${if (mtdRole != MTDIndividual) "Agent": Unit} (mode = $mode)" when {
        val action = getAction(mtdRole, mode)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "display the is the new address in the uk page" when {
            "using the manage businesses journey" in {
              enable(OverseasBusinessAddress)
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              val result = action(fakeRequest)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName)))))))

              val document: Document = Jsoup.parse(contentAsString(result))
              document.title should include(messages("add-business-is.the.new.address.in.the.uk.heading"))
              status(result) shouldBe OK
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }

      s"submit${if (mtdRole != MTDIndividual) "Agent"}(mode = $mode)" when {
        val action = getAction(mtdRole, mode, true)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
        s"the user is authenticated as a $mtdRole" should {
// TODO this should be implemented as a part of the https://jira.tools.tax.service.gov.uk/browse/MISUV-10722 Jira ticket
          s"return ${Status.SEE_OTHER}: redirect to the correct  Page" when {
            "foreign property selected" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName)))))))


              val result = action(fakeRequest.withFormUrlEncodedBody(
                IsTheNewAddressInTheUKForm.response -> responseForeign
              ))

              status(result) shouldBe SEE_OTHER
              val redirectUrl = controllers.manageBusinesses.add.routes.IsTheNewAddressInTheUKController.show().url
              redirectLocation(result) shouldBe Some(redirectUrl)
            }
            "uk business property selected" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Add, SelfEmployment))
                .copy(addIncomeSourceData = Some(AddIncomeSourceData(businessName = Some(validBusinessName)))))))

              val result = action(fakeRequest.withFormUrlEncodedBody(
                IsTheNewAddressInTheUKForm.response -> responseUK
              ))

              status(result) shouldBe SEE_OTHER
              val redirectUrl = controllers.manageBusinesses.add.routes.IsTheNewAddressInTheUKController.show().url
              redirectLocation(result) shouldBe Some(redirectUrl)
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}