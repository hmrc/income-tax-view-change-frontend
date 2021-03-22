/*
 * Copyright 2021 HM Revenue & Customs
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

package models.financialDetails

import auth.MtdItUser
import play.api.libs.json.{Format, Json}

sealed trait FinancialDetailsResponseModel

case class FinancialDetailsModel(financialDetails: List[Charge]) extends FinancialDetailsResponseModel {
  def withYears(): Seq[ChargeModelWithYear] = financialDetails.map(fd => ChargeModelWithYear(fd, fd.taxYear.toInt))

  def findChargeForTaxYear(taxYear: Int): Option[Charge] = financialDetails.find(_.taxYear.toInt == taxYear)

  def isAllPaid()(implicit user: MtdItUser[_]): Boolean = financialDetails.forall(_.isPaid)
}


object FinancialDetailsModel {
  implicit val format: Format[FinancialDetailsModel] = Json.format[FinancialDetailsModel]
}

case class FinancialDetailsErrorModel(code: Int, message: String) extends FinancialDetailsResponseModel

object FinancialDetailsErrorModel {
  implicit val format: Format[FinancialDetailsErrorModel] = Json.format[FinancialDetailsErrorModel]
}
