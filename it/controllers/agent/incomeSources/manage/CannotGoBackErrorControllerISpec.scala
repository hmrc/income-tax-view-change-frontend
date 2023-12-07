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
package controllers.agent.incomeSources.manage

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import org.scalatest.Assertion
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class CannotGoBackErrorControllerISpec extends ComponentSpecBase {
  val title: String = messagesAPI("cannotGoBack.heading")
  val annualReporting: String = "annual"
  val taxYear = "2022-2023"

  val url: IncomeSourceType => String = (incomeSourceType: IncomeSourceType) =>
    if (incomeSourceType == SelfEmployment) {
      controllers.incomeSources.manage.routes.CannotGoBackErrorController.show(isAgent = false, incomeSourceType, annualReporting, taxYear, Some(testSelfEmploymentId)).url
    }
    else {
      controllers.incomeSources.manage.routes.CannotGoBackErrorController.show(isAgent = false, incomeSourceType, annualReporting, taxYear, None).url
    }

  def runOKTest(incomeSourceType: IncomeSourceType): Assertion = {
    stubAuthorisedAgentUser(authorised = true)
    enable(IncomeSources)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

    val result: WSResponse = incomeSourceType match {
      case SelfEmployment => IncomeTaxViewChangeFrontend.getManageSECannotGoBack
      case UkProperty => IncomeTaxViewChangeFrontend.getManageForeignPropertyCannotGoBack
      case ForeignProperty => IncomeTaxViewChangeFrontend.getManageUKPropertyCannotGoBack
    }

    result should have(
      httpStatus(OK),
      pageTitleAgent("cannotGoBack.heading")
    )
  }

  def runRedirectTest(incomeSourceType: IncomeSourceType): Assertion = {
    stubAuthorisedAgentUser(authorised = true)
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
