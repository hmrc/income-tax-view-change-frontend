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

package audit.models

import auth.MtdItUser
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.Annual
import play.api.libs.json.Json
import services.optIn.core.{CurrentOptInTaxYear, NextOptInTaxYear, OptInProposition}
import testUtils.TestSupport

class OptOutNewAuditModelSpec extends TestSupport {

  val currentYear: TaxYear = TaxYear.forYearEnd(2024)
  val nextYear: TaxYear = currentYear.nextYear

  val currentTaxYearOptIn: CurrentOptInTaxYear = CurrentOptInTaxYear(Annual, currentYear)
  val nextTaxYearOptIn: NextOptInTaxYear = NextOptInTaxYear(Annual, nextYear, currentTaxYearOptIn)

  val optInProposition: OptInProposition = OptInProposition(
    currentTaxYearOptIn,
    nextTaxYearOptIn
  )

  implicit val user: MtdItUser[_] = tsTestUser

  "OptOutNewAuditModel" when {
    "user opt out for quarterly reporting is submitted" should {
      "create an audit model - single year" in {
        val intentTaxYear = "2023-2024"
        val auditModel = OptOutNewAuditModel(Seq(intentTaxYear))

        auditModel.auditType shouldBe "OptOutTaxYearsPage"
        auditModel.transactionName shouldBe "opt-out-tax-years-page"
        auditModel.detail shouldBe Json.obj(
          "nino" -> tsTestUser.nino,
          "mtditid" -> tsTestUser.mtditid,
          "saUtr" -> tsTestUser.saUtr,
          "credId" -> tsTestUser.credId,
          "userType" -> tsTestUser.userType,
          "optOutTaxYears" -> Json.arr("2023-2024")
        )
      }
      "create an audit model - multi year" in {
        val intentTaxYear = "2023-2024"
        val nextTaxYear = "2024-2025"
        val auditModel = OptOutNewAuditModel(Seq(intentTaxYear, nextTaxYear))

        auditModel.auditType shouldBe "OptOutTaxYearsPage"
        auditModel.transactionName shouldBe "opt-out-tax-years-page"
        auditModel.detail shouldBe Json.obj(
          "nino" -> tsTestUser.nino,
          "mtditid" -> tsTestUser.mtditid,
          "saUtr" -> tsTestUser.saUtr,
          "credId" -> tsTestUser.credId,
          "userType" -> tsTestUser.userType,
          "optOutTaxYears" -> Json.arr("2023-2024", "2024-2025")
        )
      }
    }
  }
}