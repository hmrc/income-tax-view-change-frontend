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
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.noIncomeDetails
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class ForeignPropertyStartDateCheckFormSpec extends TestSupport with ImplicitDateFormatter {

  val mockDateService: DateService = app.injector.instanceOf[DateService]

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

  lazy val form: Form[ForeignPropertyStartDateCheckForm] = ForeignPropertyStartDateCheckForm.form

  "ForeignPropertyStartDateCheck form" should {
    "bind with a valid response - yes" in {
      val formData = ForeignPropertyStartDateCheckForm(Some("Yes"))
      val completedForm = form.fill(formData)
      completedForm.data.get(ForeignPropertyStartDateCheckForm.response) shouldBe Some(ForeignPropertyStartDateCheckForm.responseYes)
      completedForm.errors shouldBe List.empty
    }

    "bind with a valid response - No" in {
      val formData = ForeignPropertyStartDateCheckForm(Some("No"))
      val completedForm = form.fill(formData)
      completedForm.data.get(ForeignPropertyStartDateCheckForm.response) shouldBe Some(ForeignPropertyStartDateCheckForm.responseNo)
      completedForm.errors shouldBe List.empty
    }

    "bind with an invalid response" in {
      val completedForm = form.bind(Map(ForeignPropertyStartDateCheckForm.response -> "N/A"))
      completedForm.data.get(ForeignPropertyStartDateCheckForm.response) shouldBe Some("N/A")
      completedForm.errors shouldBe List(FormError(ForeignPropertyStartDateCheckForm.response, List(ForeignPropertyStartDateCheckForm.radiosEmptyError), List()))
    }

  }
}


