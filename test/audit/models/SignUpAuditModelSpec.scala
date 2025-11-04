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
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.Annual
import play.api.http.Status.OK
import play.api.libs.json.Json
import services.optIn.core.{CurrentOptInTaxYear, NextOptInTaxYear, OptInProposition}
import testUtils.TestSupport

class SignUpAuditModelSpec extends TestSupport {

  val currentYear: TaxYear = TaxYear.forYearEnd(2024)
  val nextYear: TaxYear = currentYear.nextYear

  val currentTaxYearOptIn: CurrentOptInTaxYear = CurrentOptInTaxYear(Annual, currentYear)
  val nextTaxYearOptIn: NextOptInTaxYear = NextOptInTaxYear(Annual, nextYear, currentTaxYearOptIn)

  val optInProposition: OptInProposition = OptInProposition(
    currentTaxYearOptIn,
    nextTaxYearOptIn
  )

  implicit val user: MtdItUser[_] = tsTestUser

  "SignUpAuditModel" when {
    "user sign up for quarterly reporting is submitted" should {
      "create an audit model for multi year" in {
        val intentTaxYear = TaxYear(2023, 2024)
        val auditModel = SignUpAuditModel(intentTaxYear, SignUpMultipleYears, Annual, Annual)

        auditModel.auditType shouldBe "SignUpTaxYearsPage"
        auditModel.transactionName shouldBe "sign-up-tax-years-page"
        auditModel.detail shouldBe Json.obj(
          "nino" -> tsTestUser.nino,
          "mtditid" -> tsTestUser.mtditid,
          "saUtr" -> tsTestUser.saUtr,
          "credId" -> tsTestUser.credId,
          "userType" -> tsTestUser.userType,
          "signUpTaxYear" -> "2023-2024",
          "signUpType" -> "MultiYear",
          "currentTaxYearItsaStatus" -> "Annual",
          "nextTaxYearItsaStatus" -> "Annual",
        )
      }

      "create an audit model for single year" in {
        val intentTaxYear = TaxYear(2023, 2024)
        val auditModel = SignUpAuditModel(intentTaxYear, SignUpSingleYear, Annual, ITSAStatus.Voluntary)

        auditModel.auditType shouldBe "SignUpTaxYearsPage"
        auditModel.transactionName shouldBe "sign-up-tax-years-page"
        auditModel.detail shouldBe Json.obj(
          "nino" -> tsTestUser.nino,
          "mtditid" -> tsTestUser.mtditid,
          "saUtr" -> tsTestUser.saUtr,
          "credId" -> tsTestUser.credId,
          "userType" -> tsTestUser.userType,
          "signUpTaxYear" -> "2023-2024",
          "signUpType" -> "SingleYear",
          "currentTaxYearItsaStatus" -> "Annual",
          "nextTaxYearItsaStatus" -> "MTD Voluntary",
        )
      }
    }
  }
}
