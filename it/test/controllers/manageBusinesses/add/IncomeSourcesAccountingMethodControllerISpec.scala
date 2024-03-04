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

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.ExecutionContext

class IncomeSourcesAccountingMethodControllerISpec extends ComponentSpecBase {

  val addIncomeSourcesAccountingMethodShowUrlSoleTrader: String = controllers.manageBusinesses.add.routes.IncomeSourcesAccountingMethodController.show(SelfEmployment, isAgent = false).url
  val addIncomeSourcesAccountingMethodShowUrlUK: String = controllers.manageBusinesses.add.routes.IncomeSourcesAccountingMethodController.show(UkProperty, isAgent = false).url
  val addIncomeSourcesAccountingMethodShowUrlForeign: String = controllers.manageBusinesses.add.routes.IncomeSourcesAccountingMethodController.show(ForeignProperty, isAgent = false).url

  val checkBusinessDetailsShowUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url
  val checkUKPropertyDetailsShowUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(UkProperty).url
  val foreignPropertyCheckDetailsShowUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(ForeignProperty).url

  val selfEmploymentAccountingMethod: String = "incomeSources.add." + SelfEmployment.key + ".AccountingMethod"
  val UKPropertyAccountingMethod: String = "incomeSources.add." + UkProperty.key + ".AccountingMethod"
  val foreignPropertyAccountingMethod: String = "incomeSources.add." + ForeignProperty.key + ".AccountingMethod"

  val continueButtonText: String = messagesAPI("base.continue")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  implicit override val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit override val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType, accountingMethod: Option[String]): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = JourneyType(Add, incomeSourceType).toString,
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
    await(sessionService.createSession(JourneyType(Add, SelfEmployment).toString))
    await(sessionService.createSession(JourneyType(Add, UkProperty).toString))
    await(sessionService.createSession(JourneyType(Add, ForeignProperty).toString))
  }

  def runGetTest(addIncomeSourcesAccountingMethodShowUrl: String, url: String, messageKey: String): Unit = {
    "User is authorised" in {
      Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
      When(s"I call GET $addIncomeSourcesAccountingMethodShowUrl")
      val result = IncomeTaxViewChangeFrontendManageBusinesses.get(url, clientDetailsWithConfirmation)
      verifyIncomeSourceDetailsCall(testMtditid)
      result should have(
        httpStatus(OK),
        pageTitleIndividual(messageKey),
        elementTextByID("continue-button")(continueButtonText)
      )
    }
  }

  def runPostTest(checkDetailsShowUrl: String, url: String, formData: Map[String, Seq[String]], incomeSourceType: IncomeSourceType, accountingMethod: Option[String]): Unit = {

    enable(IncomeSources)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

    val result = IncomeTaxViewChangeFrontendManageBusinesses.post(url, clientDetailsWithConfirmation)(formData)

    await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType, accountingMethod)))

    val session = sessionService.getMongo(JourneyType(Add, incomeSourceType).toString)(hc, ec).futureValue

    val resultAccountingMethod = session match {
      case Right(Some(uiJourneySessionData)) =>
        uiJourneySessionData.addIncomeSourceData.get.incomeSourcesAccountingMethod
      case _ => None
    }

    result should have(
      httpStatus(SEE_OTHER),
      redirectURI(checkDetailsShowUrl))
    resultAccountingMethod shouldBe accountingMethod

  }

  s"calling GET $addIncomeSourcesAccountingMethodShowUrlSoleTrader" should {
    "render the Business Accounting Method page" when {
      runGetTest(addIncomeSourcesAccountingMethodShowUrlSoleTrader, "/manage-your-businesses/add/business-accounting-method", "incomeSources.add.SE.AccountingMethod.heading")
    }
  }
  s"calling GET $addIncomeSourcesAccountingMethodShowUrlUK" should {
    "render the Business Accounting Method page" when {
      runGetTest(addIncomeSourcesAccountingMethodShowUrlUK, "/manage-your-businesses/add/uk-property-accounting-method", "incomeSources.add.UK.AccountingMethod.heading")
    }
  }
  s"calling GET $addIncomeSourcesAccountingMethodShowUrlForeign" should {
    "render the Business Accounting Method page" when {
      runGetTest(addIncomeSourcesAccountingMethodShowUrlForeign, "/manage-your-businesses/add/foreign-property-business-accounting-method", "incomeSources.add.FP.AccountingMethod.heading")
    }
  }
  s"calling POST $addIncomeSourcesAccountingMethodShowUrlSoleTrader" should {
    s"redirect to $checkBusinessDetailsShowUrl" when {
      "user selects 'cash basis accounting', 'cash' should be added to session storage" in {
        val formData: Map[String, Seq[String]] = Map(selfEmploymentAccountingMethod -> Seq("cash"))
        runPostTest(checkBusinessDetailsShowUrl, "/manage-your-businesses/add/business-accounting-method", formData, SelfEmployment, Some("cash"))
      }
      s"redirect to $checkBusinessDetailsShowUrl" when {
        "user selects 'traditional accounting', 'accruals' should be added to session storage" in {
          val formData: Map[String, Seq[String]] = Map(selfEmploymentAccountingMethod -> Seq("traditional"))
          runPostTest(checkBusinessDetailsShowUrl, "/manage-your-businesses/add/business-accounting-method", formData, SelfEmployment, Some("accruals"))
        }
      }
      s"return BAD_REQUEST $checkBusinessDetailsShowUrl" when {
        "user does not select anything" in {
          val formData: Map[String, Seq[String]] = Map(selfEmploymentAccountingMethod -> Seq(""))
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/add/business-accounting-method", clientDetailsWithConfirmation)(formData)

          result should have(
            httpStatus(BAD_REQUEST),
            elementTextByID("error-summary-heading")(messagesAPI("base.error_summary.heading"))
          )
        }
      }
    }
  }
  s"calling POST $addIncomeSourcesAccountingMethodShowUrlUK" should {
    s"redirect to $checkUKPropertyDetailsShowUrl" when {
      "user selects 'cash basis accounting', 'cash' should be added to session storage" in {
        val formData: Map[String, Seq[String]] = Map(UKPropertyAccountingMethod -> Seq("cash"))
        runPostTest(checkUKPropertyDetailsShowUrl, "/manage-your-businesses/add/uk-property-accounting-method", formData, UkProperty, Some("cash"))
      }
      s"redirect to $checkUKPropertyDetailsShowUrl" when {
        "user selects 'traditional accounting', 'accruals' should be added to session storage" in {
          val formData: Map[String, Seq[String]] = Map(UKPropertyAccountingMethod -> Seq("traditional"))
          runPostTest(checkUKPropertyDetailsShowUrl, "/manage-your-businesses/add/uk-property-accounting-method", formData, UkProperty, Some("accruals"))
        }
      }
      s"return BAD_REQUEST $checkUKPropertyDetailsShowUrl" when {
        "user does not select anything" in {
          val formData: Map[String, Seq[String]] = Map(UKPropertyAccountingMethod -> Seq(""))
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/add/uk-property-accounting-method", clientDetailsWithConfirmation)(formData)

          result should have(
            httpStatus(BAD_REQUEST),
            elementTextByID("error-summary-heading")(messagesAPI("base.error_summary.heading"))
          )
        }
      }
    }
  }
  s"calling POST $addIncomeSourcesAccountingMethodShowUrlForeign" should {
    s"redirect to $foreignPropertyCheckDetailsShowUrl" when {
      "user selects 'cash basis accounting', 'cash' should be added to session storage" in {
        val formData: Map[String, Seq[String]] = Map(foreignPropertyAccountingMethod -> Seq("cash"))
        runPostTest(foreignPropertyCheckDetailsShowUrl, "/manage-your-businesses/add/foreign-property-business-accounting-method", formData, ForeignProperty, Some("cash"))
      }
      s"redirect to $foreignPropertyCheckDetailsShowUrl" when {
        "user selects 'traditional accounting', 'accruals' should be added to session storage" in {
          val formData: Map[String, Seq[String]] = Map(foreignPropertyAccountingMethod -> Seq("traditional"))
          runPostTest(foreignPropertyCheckDetailsShowUrl, "/manage-your-businesses/add/foreign-property-business-accounting-method", formData, ForeignProperty, Some("accruals"))
        }
      }
      s"return BAD_REQUEST $foreignPropertyCheckDetailsShowUrl" when {
        "user does not select anything" in {
          val formData: Map[String, Seq[String]] = Map(foreignPropertyAccountingMethod -> Seq(""))
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/add/foreign-property-business-accounting-method", clientDetailsWithConfirmation)(formData)

          result should have(
            httpStatus(BAD_REQUEST),
            elementTextByID("error-summary-heading")(messagesAPI("base.error_summary.heading"))
          )
        }
      }
    }
  }
}
