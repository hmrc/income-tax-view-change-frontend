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

package controllers.optIn

import controllers.optIn.OptInErrorControllerISpec.headingText
import helpers.ComponentSpecBase
import helpers.servicemocks.ITSAStatusDetailsStub.ITSAYearStatus
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class OptInErrorControllerISpec extends ComponentSpecBase {

  val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  val nextTaxYear = currentTaxYear.nextYear
  val previousTaxYear = currentTaxYear.previousYear

  val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary)

  def testShowErrorPage(isAgent: Boolean): Unit = {

    val optInErrorControllerPageUrl = controllers.optIn.routes.OptInErrorController.show(isAgent).url

    s"calling GET $optInErrorControllerPageUrl" should {
      s"render page for show error view $optInErrorControllerPageUrl" when {
        "User is authorised" in {

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          val result = IncomeTaxViewChangeFrontendManageBusinesses.renderOptInErrorPage()
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            elementTextBySelector(".govuk-heading-l")(headingText),
          )
        }
      }
    }
  }


  "OptInErrorController - Individual" when {
    testShowErrorPage(isAgent = false)
  }

  "OptInErrorController - Agent" when {
    testShowErrorPage(isAgent = true)
  }

}

object OptInErrorControllerISpec {
  val headingText = "Sorry, there is a problem with the service"
}