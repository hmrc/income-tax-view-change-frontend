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

import controllers.ControllerISpecHelper
import controllers.optIn.BeforeYouStartControllerISpec._
import enums.JourneyType.{Opt, OptInJourney}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.NavBarFs
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus
import models.optin.{OptInContextData, OptInSessionData}
import play.api.http.Status.OK
import repositories.ITSAStatusRepositorySupport.statusToString
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class BeforeYouStartControllerISpec extends ControllerISpecHelper {

  val forYearEnd = 2023
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(forYearEnd)

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/opt-in/start"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the before you start page with a button" that {
            "Redirects to choose tax year page" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              setupOptInSessionData(currentTaxYear, ITSAStatus.Annual, ITSAStatus.Annual)

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "optIn.beforeYouStart.heading"),
                elementTextByID("heading")(headingText),
                elementTextByID("desc1")(desc1),
                elementTextByID("desc2")(desc2),
                elementTextByID("reportQuarterly")(reportQuarterlyText),
                elementTextByID("voluntaryStatus")(voluntaryStatus),
                elementTextByID("voluntaryStatus-text")(voluntaryStatusText),
                elementAttributeBySelector("#start-button", "href")(routes.ChooseYearController.show(isAgent).url)

              )
            }

            "Redirects to confirm tax year page" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

              setupOptInSessionData(currentTaxYear, ITSAStatus.Annual, ITSAStatus.Voluntary)

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "optIn.beforeYouStart.heading"),
                elementTextByID("heading")(headingText),
                elementTextByID("desc1")(desc1),
                elementTextByID("desc2")(desc2),
                elementTextByID("reportQuarterly")(reportQuarterlyText),
                elementTextByID("voluntaryStatus")(voluntaryStatus),
                elementTextByID("voluntaryStatus-text")(voluntaryStatusText),
                elementAttributeBySelector("#start-button", "href")(routes.ConfirmTaxYearController.show(isAgent).url)
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }



  private def setupOptInSessionData(currentTaxYear: TaxYear, currentYearStatus: ITSAStatus.Value, nextYearStatus: ITSAStatus.Value): Unit = {
    repository.set(
      UIJourneySessionData(testSessionId,
        Opt(OptInJourney).toString,
        optInSessionData =
          Some(OptInSessionData(
            Some(OptInContextData(
              currentTaxYear.toString, statusToString(currentYearStatus), statusToString(nextYearStatus))), None))))
  }
}

object BeforeYouStartControllerISpec {
  val headingText = "Before you start"
  val desc1 = "Reporting quarterly allows HMRC to give you a more precise forecast of how much tax you owe to help you budget more accurately."
  val desc2 = "To report quarterly you will need compatible software. There are both paid and free options for you or your agent to choose from."
  val reportQuarterlyText = "Reporting quarterly"
  val voluntaryStatus = "Your voluntary status"
  val voluntaryStatusText = "As you would be voluntarily opting in to reporting quarterly, you can decide to opt out and return to reporting annually at any time."
}
