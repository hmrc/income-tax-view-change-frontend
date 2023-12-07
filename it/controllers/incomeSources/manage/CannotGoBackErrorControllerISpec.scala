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
package controllers.incomeSources.manage

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{JourneyType, Manage}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.{ManageIncomeSourceData, UIJourneySessionData}
import org.scalatest.Assertion
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSelfEmploymentId, testSelfEmploymentIdHashed, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class CannotGoBackErrorControllerISpec extends ComponentSpecBase {
  val title: String = messagesAPI("cannotGoBack.heading")
  val annualReporting: String = "annual"
  val taxYear = "2022-2023"

  val url: IncomeSourceType => String = (incomeSourceType: IncomeSourceType) =>
    if (incomeSourceType == SelfEmployment) {
      routes.CannotGoBackErrorController.show(isAgent = false, incomeSourceType, annualReporting, taxYear, Some(testSelfEmploymentIdHashed)).url
    }
    else {
      routes.CannotGoBackErrorController.show(isAgent = false, incomeSourceType, annualReporting, taxYear, None).url
    }

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  def runOKTest(incomeSourceType: IncomeSourceType): Assertion = {
    enable(IncomeSources)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

    await(sessionService.setMongoData(UIJourneySessionData(testSessionId, JourneyType(Manage, incomeSourceType).toString,
      manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

    val result: WSResponse = incomeSourceType match {
      case SelfEmployment => IncomeTaxViewChangeFrontend.getManageSECannotGoBack
      case UkProperty => IncomeTaxViewChangeFrontend.getManageForeignPropertyCannotGoBack
      case ForeignProperty => IncomeTaxViewChangeFrontend.getManageUKPropertyCannotGoBack
    }

    result should have(
      httpStatus(OK),
      pageTitleIndividual("cannotGoBack.heading")
    )
  }

  def runRedirectTest(incomeSourceType: IncomeSourceType): Assertion = {
    disable(IncomeSources)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

    val result: WSResponse = incomeSourceType match {
      case SelfEmployment => IncomeTaxViewChangeFrontend.getManageSECannotGoBack
      case UkProperty => IncomeTaxViewChangeFrontend.getManageForeignPropertyCannotGoBack
      case ForeignProperty => IncomeTaxViewChangeFrontend.getManageUKPropertyCannotGoBack
    }

    result should have(
      httpStatus(SEE_OTHER),
      pageTitleIndividual("cannotGoBack.heading")
    )
  }

  s"calling GET ${url(UkProperty)}" should {
    "return 200 OK" when {
      "FS enabled - UK Property" in {
        runOKTest(UkProperty)
      }
    }
    "return 303 SEE_OTHER" when {
      "FS disabled - UK Property" in {
        runRedirectTest(UkProperty)
      }
    }
  }
  s"calling GET ${url(ForeignProperty)}" should {
    "return 200 OK" when {
      "FS enabled - Foreign Property" in {
        runOKTest(ForeignProperty)
      }
    }
    "return 303 SEE_OTHER" when {
      "FS disabled - Foreign Property" in {
        runRedirectTest(ForeignProperty)
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
  }
}

