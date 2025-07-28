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

package forms.manageBusinesses.add

import auth.MtdItUser
import authV2.AuthActionsTestData.getMinimalMTDITUser
import enums.IncomeSourceJourney.SelfEmployment
import implicits.ImplicitDateFormatter
import play.api.data.Form
import services.DateService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.noIncomeDetails
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class AddIncomeSourceStartDateCheckFormSpec extends TestSupport with ImplicitDateFormatter {

  val mockDateService: DateService = app.injector.instanceOf[DateService]

  val testUser: MtdItUser[_] = getMinimalMTDITUser(Some(Individual), noIncomeDetails, false, fakeRequestNoSession)

  lazy val form: Form[AddIncomeSourceStartDateCheckForm] = AddIncomeSourceStartDateCheckForm(SelfEmployment.addStartDateCheckMessagesPrefix)
  "ForeignPropertyStartDateCheck form" should {
    "bind with a valid response - yes" in {
      val formData = AddIncomeSourceStartDateCheckForm(Some("Yes"))
      val completedForm = form.fill(formData)
      completedForm.data.get(AddIncomeSourceStartDateCheckForm.response) shouldBe Some(AddIncomeSourceStartDateCheckForm.responseYes)
      completedForm.errors shouldBe List.empty
    }

    "bind with a valid response - No" in {
      val formData = AddIncomeSourceStartDateCheckForm(Some("No"))
      val completedForm = form.fill(formData)
      completedForm.data.get(AddIncomeSourceStartDateCheckForm.response) shouldBe Some(AddIncomeSourceStartDateCheckForm.responseNo)
      completedForm.errors shouldBe List.empty
    }

    "bind with an invalid response" in {
      val completedForm = form.bind(Map(AddIncomeSourceStartDateCheckForm.response -> "N/A"))
      completedForm.data.get(AddIncomeSourceStartDateCheckForm.response) shouldBe Some("N/A")
    }

  }
}


