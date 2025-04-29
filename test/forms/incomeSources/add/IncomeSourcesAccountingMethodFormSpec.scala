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
import authV2.AuthActionsTestData.getMinimalMTDITUser
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import implicits.ImplicitDateFormatter
import play.api.data.FormError
import services.DateService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.noIncomeDetails
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class IncomeSourcesAccountingMethodFormSpec extends TestSupport with ImplicitDateFormatter {

  val mockDateService: DateService = app.injector.instanceOf[DateService]
  val selfEmploymentAccountingMethod: String = "incomeSources.add." + SelfEmployment.key + ".AccountingMethod"
  val UKPropertyAccountingMethod: String = "incomeSources.add." + UkProperty.key + ".AccountingMethod"
  val foreignPropertyAccountingMethod: String = "incomeSources.add." + ForeignProperty.key + ".AccountingMethod"

  val cashAccountingMethod = "cash"
  val traditionalAccountingMethod = "traditional"
  val radioEmptyError: String = "incomeSources.add.AccountingMethod.no-selection"

  val testUser: MtdItUser[_] = getMinimalMTDITUser(Some(Individual), noIncomeDetails, false, fakeRequestNoSession)

  "IncomeSourcesAccountingMethodForm form" should {
    "bind with a valid response Self Employment - cash" in {
      val formData = Map(selfEmploymentAccountingMethod -> "cash")
      val completedForm = IncomeSourcesAccountingMethodForm(SelfEmployment).bind(formData)
      completedForm.data.get(selfEmploymentAccountingMethod) shouldBe Some(cashAccountingMethod)
      completedForm.errors shouldBe List.empty
    }
    "bind with a valid response UK Property - cash" in {
      val formData = Map(UKPropertyAccountingMethod -> "cash")
      val completedForm = IncomeSourcesAccountingMethodForm(UkProperty).bind(formData)
      completedForm.data.get(UKPropertyAccountingMethod) shouldBe Some(cashAccountingMethod)
      completedForm.errors shouldBe List.empty
    }
    "bind with a valid response Foreign Property - cash" in {
      val formData = Map(foreignPropertyAccountingMethod -> "cash")
      val completedForm = IncomeSourcesAccountingMethodForm(ForeignProperty).bind(formData)
      completedForm.data.get(foreignPropertyAccountingMethod) shouldBe Some(cashAccountingMethod)
      completedForm.errors shouldBe List.empty
    }
    "bind with a valid response Self Employment - traditional" in {
      val formData = Map(selfEmploymentAccountingMethod -> "traditional")
      val completedForm = IncomeSourcesAccountingMethodForm(SelfEmployment).bind(formData)
      completedForm.data.get(selfEmploymentAccountingMethod) shouldBe Some(traditionalAccountingMethod)
      completedForm.errors shouldBe List.empty
    }
    "bind with a valid response UK Property - traditional" in {
      val formData = Map(UKPropertyAccountingMethod -> "traditional")
      val completedForm = IncomeSourcesAccountingMethodForm(UkProperty).bind(formData)
      completedForm.data.get(UKPropertyAccountingMethod) shouldBe Some(traditionalAccountingMethod)
      completedForm.errors shouldBe List.empty
    }
    "bind with a valid response Foreign Property - traditional" in {
      val formData = Map(foreignPropertyAccountingMethod -> "traditional")
      val completedForm = IncomeSourcesAccountingMethodForm(ForeignProperty).bind(formData)
      completedForm.data.get(foreignPropertyAccountingMethod) shouldBe Some(traditionalAccountingMethod)
      completedForm.errors shouldBe List.empty
    }
    "bind with an invalid response Self Employment" in {
      val completedForm = IncomeSourcesAccountingMethodForm(SelfEmployment).bind(Map(selfEmploymentAccountingMethod -> ""))
      completedForm.data.get(selfEmploymentAccountingMethod) shouldBe Some("")
      completedForm.errors shouldBe List(FormError(selfEmploymentAccountingMethod, List(radioEmptyError), List()))
    }
    "bind with an invalid response UK Property" in {
      val completedForm = IncomeSourcesAccountingMethodForm(UkProperty).bind(Map(UKPropertyAccountingMethod -> ""))
      completedForm.data.get(UKPropertyAccountingMethod) shouldBe Some("")
      completedForm.errors shouldBe List(FormError(UKPropertyAccountingMethod, List(radioEmptyError), List()))
    }
    "bind with an invalid response Foreign Property" in {
      val completedForm = IncomeSourcesAccountingMethodForm(ForeignProperty).bind(Map(foreignPropertyAccountingMethod -> ""))
      completedForm.data.get(foreignPropertyAccountingMethod) shouldBe Some("")
      completedForm.errors shouldBe List(FormError(foreignPropertyAccountingMethod, List(radioEmptyError), List()))
    }
  }
}
