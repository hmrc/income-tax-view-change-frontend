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

import businessDetails.controllers.manageBusinesses.add.routes as addBusinessRoutes
import businessDetails.enums.IncomeSourceJourney.SelfEmployment
import businessDetails.mocks.services.MockSessionService
import businessDetails.models.incomeSourceDetails.AddIncomeSourceData
import businessDetails.services.SessionService
import businessDetails.testConstants.UpdateIncomeSourceTestConstants.*
import common.connectors.ITSAStatusConnector
import common.enums.{MTDIndividual, MTDUserRole}
import common.mocks.auth.MockAuthActions
import common.models.admin.OverseasBusinessAddress
import common.models.core.{CheckMode, Mode, NormalMode}
import common.services.DateServiceInterface
import common.testConstants.BaseTestConstants.*
import common.testConstants.BusinessDetailsTestConstants.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api
import play.api.Application
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import shared.enums.JourneyType.{IncomeSourceJourneyType, Manage}
import shared.models.UIJourneySessionData

import scala.concurrent.Future

class IsTheNewAddressInTheUKControllerSpec extends MockAuthActions with MockSessionService {

  private val addBusinessIsTheNewAddressInTheUKHeading = "add-business-is.the.new.address.in.the.uk.heading"
  private val addBusinessIsTheAddressOfYourSoleTraderBusinessInTheUKHeading = "add-business-is.the.address.of.your.sole.trader.business.in.the.uk.heading"
  private val testAddIncomeSourceSessionData: Option[AddIncomeSourceData] = Some(AddIncomeSourceData(address = None, addressId = None, addressLookupId = None))

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSessionService.setMongoData(any()))
      .thenReturn(Future(true))
  }

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController: IsTheNewAddressInTheUKController = app.injector.instanceOf[IsTheNewAddressInTheUKController]

  private def getAction(mtdRole: MTDUserRole, mode: Mode, isPost: Boolean = false): Action[AnyContent] = mtdRole match {
    case MTDIndividual if isPost => testController.submit(false, mode, false)
    case MTDIndividual => testController.show(false, mode, false)
    case _ if isPost => testController.submit(true, mode, false)
    case _ => testController.show(true, mode, false)
  }

  private def verifySetMongoData(): Unit = {
    val argument: ArgumentCaptor[UIJourneySessionData] = ArgumentCaptor.forClass(classOf[UIJourneySessionData])
    verify(mockSessionService).setMongoData(argument.capture())
    argument.getValue.addIncomeSourceData shouldBe testAddIncomeSourceSessionData
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
                setupMockSuccess(mtdRole, false, List(OverseasBusinessAddress))
                mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
                setupMockCreateSession(true)
                val result = action(fakeRequest)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData())))))

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title should include(messages(addBusinessIsTheNewAddressInTheUKHeading))
                document.select(".govuk-back-link").attr("href") shouldBe addBusinessRoutes.ChooseSoleTraderAddressController.show(mtdRole != MTDIndividual).url
                status(result) shouldBe OK
                verifySetMongoData()
              }
            }
            "display the is the address of your sole trader business in the UK page" when {
              "when user has no addresses on file using the manage businesses journey" in {
                setupMockSuccess(mtdRole, false, List(OverseasBusinessAddress))
                mockItsaStatusRetrievalAction(businessesAndPropertyIncome.copy(businesses = List(business1.copy(address = None))))
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome.copy(businesses = List(business1.copy(address = None))))
                setupMockCreateSession(true)
                val result = action(fakeRequest)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData())))))

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title should include(messages(addBusinessIsTheAddressOfYourSoleTraderBusinessInTheUKHeading))
                status(result) shouldBe OK
                verifySetMongoData()
              }
              "when user has invalid UK addresses without post code on file using the manage businesses journey" in {
                setupMockSuccess(mtdRole, false, List(OverseasBusinessAddress))
                mockItsaStatusRetrievalAction(businessesAndPropertyIncome.copy(businesses = List(business1.copy(address = Some(invalidUKAddressNoPostCode)))))
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome.copy(businesses = List(business1.copy(address = Some(invalidUKAddressNoPostCode)))))
                setupMockCreateSession(true)
                val result = action(fakeRequest)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData())))))

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title should include(messages(addBusinessIsTheAddressOfYourSoleTraderBusinessInTheUKHeading))
                status(result) shouldBe OK
                verifySetMongoData()
              }
              "when user has no UK addresses on file using the manage businesses journey" in {
                setupMockSuccess(mtdRole, false, List(OverseasBusinessAddress))
                mockItsaStatusRetrievalAction(businessesAndPropertyIncome.copy(businesses = List(business1.copy(address = Some(foreignAddress)))))
                setupMockGetIncomeSourceDetails(businessesAndPropertyIncome.copy(businesses = List(business1.copy(address = Some(foreignAddress)))))
                setupMockCreateSession(true)
                val result = action(fakeRequest)
                setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, SelfEmployment))
                  .copy(addIncomeSourceData = Some(AddIncomeSourceData())))))

                val document: Document = Jsoup.parse(contentAsString(result))
                document.title should include(messages(addBusinessIsTheAddressOfYourSoleTraderBusinessInTheUKHeading))
                status(result) shouldBe OK
                verifySetMongoData()
              }
            }
          }
          "redirect to the home page page" when {
            "fs is disables using the manage businesses journey" in {
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