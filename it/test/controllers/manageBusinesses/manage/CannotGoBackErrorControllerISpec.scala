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

package controllers.manageBusinesses.manage

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{JourneyType, Manage}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import org.scalatest.Assertion
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, completedUIJourneySessionData, emptyUIJourneySessionData}

class CannotGoBackErrorControllerISpec extends ComponentSpecBase {
  val title: String = messagesAPI("cannotGoBack.heading")
  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val url: IncomeSourceType => String = (incomeSourceType: IncomeSourceType) =>
    routes.CannotGoBackErrorController.show(isAgent = false, incomeSourceType).url

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Manage))
  }

  def runOKTest(incomeSourceType: IncomeSourceType): Assertion = {
    enable(IncomeSources)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

    await(sessionService.setMongoData(completedUIJourneySessionData(JourneyType(Manage, incomeSourceType))))

    lazy val result: WSResponse = incomeSourceType match {
      case SelfEmployment => IncomeTaxViewChangeFrontendManageBusinesses.getManageSECannotGoBack
      case UkProperty => IncomeTaxViewChangeFrontendManageBusinesses.getManageForeignPropertyCannotGoBack
      case ForeignProperty => IncomeTaxViewChangeFrontendManageBusinesses.getManageUKPropertyCannotGoBack
    }

    result should have(
      httpStatus(OK),
      pageTitleIndividual("cannotGoBack.heading")
    )
  }

  def runRedirectTest(incomeSourceType: IncomeSourceType): Assertion = {
    disable(IncomeSources)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

    lazy val result: WSResponse = incomeSourceType match {
      case SelfEmployment => IncomeTaxViewChangeFrontendManageBusinesses.getManageSECannotGoBack
      case UkProperty => IncomeTaxViewChangeFrontendManageBusinesses.getManageForeignPropertyCannotGoBack
      case ForeignProperty => IncomeTaxViewChangeFrontendManageBusinesses.getManageUKPropertyCannotGoBack
    }

    val expectedRedirect: String = controllers.routes.HomeController.show().url

    result should have(
      httpStatus(SEE_OTHER),
      redirectURI(expectedRedirect)
    )
  }

  def runISETest(incomeSourceType: IncomeSourceType): Assertion = {
    enable(IncomeSources)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

    await(sessionService.setMongoData(emptyUIJourneySessionData(JourneyType(Manage, incomeSourceType))))

    val result: WSResponse = incomeSourceType match {
      case SelfEmployment => IncomeTaxViewChangeFrontendManageBusinesses.getManageSECannotGoBack
      case UkProperty => IncomeTaxViewChangeFrontendManageBusinesses.getManageUKPropertyCannotGoBack
      case ForeignProperty => IncomeTaxViewChangeFrontendManageBusinesses.getManageForeignPropertyCannotGoBack
    }

    result should have(
      httpStatus(INTERNAL_SERVER_ERROR)
    )
  }

  s"calling GET ${url(UkProperty)}" should {
    "return 200 OK" when {
      "FS enabled - UK Property" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
        await(sessionService.setMongoData(completedUIJourneySessionData(JourneyType(Manage, UkProperty))))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getManageUKPropertyCannotGoBack

        result should have(
          httpStatus(OK)
        )
      }
    }
    "return 303 SEE_OTHER" when {
      "FS disabled - UK Property" in {
        runRedirectTest(UkProperty)
      }
    }
    "return 500 INTERNAL_SERVER_ERROR" when {
      "Mongo empty - UK Property" in {
        runISETest(UkProperty)
      }
    }
  }
  s"calling GET ${url(ForeignProperty)}" should {
    "return 200 OK" when {
      "FS enabled - Foreign Property" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
        await(sessionService.setMongoData(completedUIJourneySessionData(JourneyType(Manage, ForeignProperty))))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getManageForeignPropertyCannotGoBack

        result should have(
          httpStatus(OK)
        )
      }
    }
    "return 303 SEE_OTHER" when {
      "FS disabled - Foreign Property" in {
        runRedirectTest(ForeignProperty)
      }

      "return 500 INTERNAL_SERVER_ERROR" when {
        "Mongo empty - Foreign Property" in {
          runISETest(ForeignProperty)
        }
      }
    }
  }
  s"calling GET ${url(SelfEmployment)}" should {
    "return 200 OK" when {
      "FS enabled - Self Employment" in {
        runOKTest(SelfEmployment)
      }
    }
    "return 303 SEE_OTHER" when {
      "FS disabled - Self Employment" in {
        runRedirectTest(SelfEmployment)
      }
    }
    "return 500 INTERNAL_SERVER_ERROR" when {
      "Mongo empty - Self Employment" in {
        runISETest(SelfEmployment)
      }
    }
  }
}

