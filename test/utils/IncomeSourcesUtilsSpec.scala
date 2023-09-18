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

package utils

import auth.MtdItUser
import forms.utils.SessionKeys
import forms.utils.SessionKeys._
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckUKPropertyViewModel}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import services.SessionService
import testUtils.TestSupport

import java.time.LocalDate
import scala.language.postfixOps

class IncomeSourcesUtilsSpec extends TestSupport with IncomeSourcesUtils {

  val viewModelMax: CheckBusinessDetailsViewModel = CheckBusinessDetailsViewModel(
    businessName = Some("Test Business"),
    businessStartDate = Some(LocalDate.of(2022, 1, 1)),
    businessTrade = "Test Trade",
    businessAddressLine1 = "Test Business Address Line 1",
    businessPostalCode = Some("Test Business Postal Code"),
    incomeSourcesAccountingMethod = Some("Cash"),
    accountingPeriodEndDate = LocalDate.of(2022, 1, 1),
    businessAddressLine2 = None,
    businessAddressLine3 = None,
    businessAddressLine4 = None,
    businessCountryCode = None,
    cashOrAccrualsFlag = "Cash",
    skippedAccountingMethod = false
  )

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val checkUKPropertyViewModel: CheckUKPropertyViewModel = CheckUKPropertyViewModel(
    tradingStartDate = LocalDate.of(2023, 5, 1),
    cashOrAccrualsFlag = "Cash")

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = fakeRequestWithActiveSession.withSession(
    businessName -> viewModelMax.businessName.get,
    businessStartDate -> viewModelMax.businessStartDate.get.toString,
    businessTrade -> viewModelMax.businessTrade,
    addBusinessAddressLine1 -> viewModelMax.businessAddressLine1,
    addBusinessPostalCode -> viewModelMax.businessPostalCode.get,
    addIncomeSourcesAccountingMethod -> viewModelMax.incomeSourcesAccountingMethod.get,
    addBusinessAccountingPeriodEndDate -> viewModelMax.accountingPeriodEndDate.toString,
    addUkPropertyStartDate -> checkUKPropertyViewModel.tradingStartDate.toString,
    addIncomeSourcesAccountingMethod -> checkUKPropertyViewModel.cashOrAccrualsFlag
  )

  "getUKPropertyDetailsFromSession" when {
    "user has uk property details in session" should {
      "return CheckBusinessDetailsViewModel" in {
        implicit val user: MtdItUser[AnyContentAsEmpty.type] = individualUser.copy()(fakeRequest)
        val result = IncomeSourcesUtils.getUKPropertyDetailsFromSession(sessionService)
        result.futureValue shouldBe Right(checkUKPropertyViewModel)
      }
    }

    "user is missing uk property details in session" should {
      "returns an exception" in {
        val result = IncomeSourcesUtils.getUKPropertyDetailsFromSession(sessionService)
        result.futureValue.isLeft shouldBe true
      }
    }

  }

  "removeIncomeSourceDetailsFromSession" when {
    "user has session data" should {
      "remove session data" in {

        implicit val user: MtdItUser[AnyContentAsEmpty.type] = individualUser.copy()(fakeRequest)

        val redirect = withIncomeSourcesRemovedFromSession {
          Redirect("nowhere")
        }(individualUser, sessionService, ec)

        SessionKeys.incomeSourcesSessionKeys.forall(
          redirect.futureValue.session.get(_).isDefined == false
        ) shouldBe true
      }
    }
  }
}
