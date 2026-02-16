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

package controllers.manageBusinesses.add

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.{MTDIndividual, MTDUserRole}
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.OverseasBusinessAddress
import models.core.{AddressModel, CheckMode, Mode, NormalMode}
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
import testConstants.BusinessDetailsTestConstants.{business1, foreignAddress, invalidUKAddressNoPostCode}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, emptyUIJourneySessionData}

class IsTheNewAddressInTheUKControllerSpec extends MockAuthActions with MockSessionService {
  
  private val addBusinessIsTheNewAddressInTheUKHeading = "add-business-is.the.new.address.in.the.uk.heading"
  private val addBusinessIsTheAddressOfYourSoleTraderBusinessInTheUKHeading = "add-business-is.the.address.of.your.sole.trader.business.in.the.uk.heading"

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
    s"${messages("htmlTitle.invalidInput", messages(addBusinessIsTheNewAddressInTheUKHeading))}"
  }

  Seq(CheckMode, NormalMode).foreach { mode =>
    mtdAllRoles.foreach { mtdRole =>
      s"show${if (mtdRole != MTDIndividual) "Agent": Unit} (mode = $mode)" when {
        val action = getAction(mtdRole, mode)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "display the is the new address in the uk page" when {
            "fs is enabled" when {
              "using the manage businesses journey" in {
                enable(OverseasBusinessAddress)
                setupMockSuccess(mtdRole)
                mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
                setupMockCreateSession(true)
                val result = action(fakeRequest)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData())))))

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title should include(messages(addBusinessIsTheNewAddressInTheUKHeading))
                status(result) shouldBe OK
              }
            }
            "display the is the address of your sole trader business in the UK page" when {
              "when user has no addresses on file using the manage businesses journey" in {
                enable(OverseasBusinessAddress)
                setupMockSuccess(mtdRole)
                mockItsaStatusRetrievalAction(businessesAndPropertyIncome.copy(businesses = List(business1.copy(address = None))))
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome.copy(businesses = List(business1.copy(address = None))))
                setupMockCreateSession(true)
                val result = action(fakeRequest)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData())))))

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title should include(messages(addBusinessIsTheAddressOfYourSoleTraderBusinessInTheUKHeading))
                status(result) shouldBe OK
              }
              "when user has invalid UK addresses without post code on file using the manage businesses journey" in {
                enable(OverseasBusinessAddress)
                setupMockSuccess(mtdRole)
                mockItsaStatusRetrievalAction(businessesAndPropertyIncome.copy(businesses = List(business1.copy(address = Some(invalidUKAddressNoPostCode)))))
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome.copy(businesses = List(business1.copy(address = Some(invalidUKAddressNoPostCode)))))
                setupMockCreateSession(true)
                val result = action(fakeRequest)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData())))))

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title should include(messages(addBusinessIsTheAddressOfYourSoleTraderBusinessInTheUKHeading))
                status(result) shouldBe OK
              }
              "when user has no UK addresses on file using the manage businesses journey" in {
                enable(OverseasBusinessAddress)
                setupMockSuccess(mtdRole)
                mockItsaStatusRetrievalAction(businessesAndPropertyIncome.copy(businesses = List(business1.copy(address = Some(foreignAddress)))))
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome.copy(businesses = List(business1.copy(address = Some(foreignAddress)))))
                setupMockCreateSession(true)
                val result = action(fakeRequest)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData())))))

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title should include(messages(addBusinessIsTheAddressOfYourSoleTraderBusinessInTheUKHeading))
                status(result) shouldBe OK
              }
            }
          }
          "redirect to the home page page" when {
            "fs is disables using the manage businesses journey" in {
                disable(OverseasBusinessAddress)
                setupMockSuccess(mtdRole)
                mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
                setupMockCreateSession(true)
                val result = action(fakeRequest)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData())))))
                
                status(result) shouldBe SEE_OTHER
                redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view")
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}