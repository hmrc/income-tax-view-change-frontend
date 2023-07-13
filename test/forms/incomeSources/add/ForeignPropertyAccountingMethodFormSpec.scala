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

package forms.incomeSources.add

import auth.MtdItUser
import implicits.ImplicitDateFormatter
import play.api.data.{Form, FormError}
import services.DateService
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceDetailsTestConstants.noIncomeDetails
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class ForeignPropertyAccountingMethodFormSpec extends TestSupport with ImplicitDateFormatter {

  val testUser: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = None,
    btaNavPartial = None,
    saUtr = None,
    credId = Some("12345-credId"),
    userType = Some(Individual),
    arn = None,
    incomeSources = noIncomeDetails
  )(fakeRequestNoSession)

  lazy val form: Form[_] = ForeignPropertyAccountingMethodForm.form

  "ForeignPropertyAccountingMethodForm form" should {
    "bind with a valid response - cash" in {
      val formData = Map("incomeSources.add.foreignPropertyAccountingMethod" -> "cash")
      val completedForm = form.bind(formData)
      completedForm.data.get(ForeignPropertyAccountingMethodForm.response) shouldBe Some(ForeignPropertyAccountingMethodForm.cashAccountingMethod)
      completedForm.errors shouldBe List.empty
    }
    "bind with a valid response - traditional" in {
      val formData = Map("incomeSources.add.foreignPropertyAccountingMethod" -> "traditional")
      val completedForm = form.bind(formData)
      completedForm.data.get(ForeignPropertyAccountingMethodForm.response) shouldBe Some(ForeignPropertyAccountingMethodForm.traditionalAccountingMethod)
      completedForm.errors shouldBe List.empty
    }
    "bind with an invalid response" in {
      val completedForm = form.bind(Map(ForeignPropertyAccountingMethodForm.response -> ""))
      completedForm.data.get(ForeignPropertyAccountingMethodForm.response) shouldBe Some("")
      completedForm.errors shouldBe List(FormError(ForeignPropertyAccountingMethodForm.response, List(ForeignPropertyAccountingMethodForm.radioEmptyError), List()))
    }
  }
}
