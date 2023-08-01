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

package views.incomeSources.cease

import auth.MtdItUser
import models.incomeSourceDetails.viewmodels.CeaseBusinessDetailsViewModel
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testArn, testCredId, testMtditid, testNino, testRetrievedUserName, testSaUtr, testSelfEmploymentId, testUserTypeAgent, testUserTypeIndividual}
import testConstants.BusinessDetailsTestConstants.{address, businessIncomeSourceId, testStartDate, testTradeName}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessAndPropertyAligned, businessesAndPropertyIncome}
import testUtils.{TestSupport, ViewSpec}
import uk.gov.hmrc.auth.core.retrieve.Name
import views.html.incomeSources.cease.CheckCeaseBusinessDetails

class CheckCeaseBusinessDetailsViewSpec extends TestSupport with ViewSpec{
  val checkCeaseBusinessDetailsView = app.injector.instanceOf[CheckCeaseBusinessDetails]
  val businessEndDate = "2022-04-23"
  val businessEndShortLongDate = "23 Apr 2022"
  val individualUseWithSession: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = businessAndPropertyAligned,
    btaNavPartial = None,
    saUtr = Some(testSaUtr),
    credId = Some(testCredId),
    userType = Some(testUserTypeIndividual),
    arn = None
  )(FakeRequest().withSession(forms.utils.SessionKeys.ceaseBusinessEndDate -> businessEndDate)
    .withSession(forms.utils.SessionKeys.ceaseBusinessIncomeSourceId -> businessIncomeSourceId))

 def agentUserConfirmedClientWithSession(clientNino: String = "AA111111A"): MtdItUser[_] = MtdItUser(
    mtditid = "XAIT00000000015",
    nino = clientNino,
    userName = Some(Name(Some("Test"), Some("User"))),
    incomeSources = businessesAndPropertyIncome,
    btaNavPartial = None,
    saUtr = Some("1234567890"),
    credId = Some(testCredId),
    userType = Some(testUserTypeAgent),
    arn = Some(testArn)
  )(FakeRequest().withSession(forms.utils.SessionKeys.ceaseBusinessEndDate -> businessEndDate)
   .withSession(forms.utils.SessionKeys.ceaseBusinessIncomeSourceId -> businessIncomeSourceId))

  val ceaseBusinessDetailsViewModel = CeaseBusinessDetailsViewModel(
    incomeSourceId = testSelfEmploymentId,
    tradingName = Some(testTradeName),
    tradingStartDate = Some(testStartDate),
    address = Some(address)
  )

  lazy val viewAgent: HtmlFormat.Appendable = checkCeaseBusinessDetailsView(ceaseBusinessDetailsViewModel, true)(agentUserConfirmedClientWithSession(), implicitly)
  lazy val view: HtmlFormat.Appendable = checkCeaseBusinessDetailsView(ceaseBusinessDetailsViewModel, false)(individualUseWithSession, implicitly)

  val caption = messages("incomeSources.ceaseBusiness.checkDetails.soloTrader")
  val heading = messages("incomeSources.ceaseBusiness.checkDetails.heading")
  val businessStopDateLabel = messages("incomeSources.ceaseBusiness.checkDetails.dateStopped")
  val businessNameLabel = messages("incomeSources.ceaseBusiness.checkDetails.businessName")
  val businessAddressLabel = messages("incomeSources.ceaseBusiness.checkDetails.businessAddress")
  val buttonLabel = messages("incomeSources.ceaseBusiness.checkDetails.confirm")
  val changeUrl = controllers.incomeSources.cease.routes.BusinessEndDateController.show(testSelfEmploymentId).url
  val changeUrlAgent = controllers.incomeSources.cease.routes.BusinessEndDateController.showAgent(testSelfEmploymentId).url
  val formAction = controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.submit().url
  val formActionAgent = controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.submitAgent().url

  def validateHtmlValues(isAgent: Boolean, document: Document): Unit = {
    document.hasPageHeading(heading)
    document.getElementById("caption").text() shouldBe caption
    document.getElementById("businessStopDateLabel").text() shouldBe businessStopDateLabel
    document.getElementById("businessStopDate").text() shouldBe businessEndShortLongDate
    document.getElementById("businessNameLabel").text() shouldBe businessNameLabel
    document.getElementById("businessName").text() shouldBe testTradeName
    document.getElementById("businessAddressLabel").text() shouldBe businessAddressLabel
    document.getElementById("businessAddress").text() shouldBe address.addressLine1+" "+
      address.addressLine2.get+" "+
      address.addressLine3.get+" "+
      address.addressLine4.get+" "+
      address.postCode.get+" "+
      address.countryCode
    document.getElementById("change").attr("href") shouldBe (if(isAgent) changeUrlAgent else changeUrl)
    document.getElementById("continue-button").text() shouldBe buttonLabel
    document.form.attr("action") shouldBe (if(isAgent) formActionAgent else formAction)
  }

  "CheckCeaseBusinessDetailsView - Individual" should {

    "render the page " in new Setup(view) {
      validateHtmlValues(false, document)
    }
  }

  "CheckCeaseBusinessDetailsView - Agent" should {

    "render the page " in new Setup(viewAgent) {
      validateHtmlValues(true, document)
    }
  }
}
