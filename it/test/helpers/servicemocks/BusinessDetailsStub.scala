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

package helpers.servicemocks

import helpers.WiremockHelper
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.http.Status.OK
import play.api.libs.json.JsValue
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}

object BusinessDetailsStub {

  def businessDetailsUrl(nino: String): String = s"/income-tax-view-change/get-business-details/nino/$nino"

  def stubGetBusinessDetails(nino: String = testNino)(status: Int = OK, response: JsValue = businessResponse(nino).toJson): Unit = {
    WiremockHelper.stubGet(businessDetailsUrl(testNino), status, response.toString())
  }

  def businessResponse(nino: String): IncomeSourceDetailsModel =
    IncomeSourceDetailsModel(nino = nino, mtdbsa = testMtditid, yearOfMigration = None, businesses = List(), properties = List())

}
