/*
 * Copyright 2017 HM Revenue & Customs
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

package models

import play.api.libs.json.Json

sealed trait EstimatedTaxLiabilityResponseModel

case class EstimatedTaxLiability(total: BigDecimal,
                                 nic2: BigDecimal,
                                 nic4: BigDecimal,
                                 incomeTax: BigDecimal) extends EstimatedTaxLiabilityResponseModel

case class EstimatedTaxLiabilityError(status: Int, message: String) extends EstimatedTaxLiabilityResponseModel


object EstimatedTaxLiability {
  implicit val format = Json.format[EstimatedTaxLiability]
}

object EstimatedTaxLiabilityError {
  implicit val format = Json.format[EstimatedTaxLiabilityError]
}