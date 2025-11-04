/*
 * Copyright 2025 HM Revenue & Customs
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

import audit.Utilities
import auth.MtdItUser
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.ITSAStatus
import play.api.libs.json.{JsValue, Json}

case class SignUpAuditModel(signUpTaxYear: TaxYear,
                            signUpType: SignUpType,
                            currentYearItsaStatus: ITSAStatus,
                            nextYearItsaStatus: ITSAStatus)(implicit user: MtdItUser[_]) extends ExtendedAuditModel {
  override val transactionName: String = enums.TransactionName.SignUpTaxYearsPage
  override val auditType: String = enums.AuditType.SignUpTaxYearsPage

  override val detail: JsValue =
    Utilities.userAuditDetails(user) ++
      Json.obj(
        "signUpTaxYear" -> signUpTaxYear.toString,
        "signUpType" -> signUpType.toString,
        "currentTaxYearItsaStatus" -> currentYearItsaStatus,
        "nextTaxYearItsaStatus" -> nextYearItsaStatus
      )
}

sealed trait SignUpType

case object SignUpSingleYear extends SignUpType {
  override def toString: String = "SingleYear"
}

case object SignUpMultipleYears extends SignUpType {
  override def toString: String = "MultiYear"
}
