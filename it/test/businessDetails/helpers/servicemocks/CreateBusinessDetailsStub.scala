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

package businessDetails.helpers.servicemocks

import businessDetails.models.createIncomeSource.{CreateIncomeSourceErrorResponse, CreateIncomeSourceResponse}
import common.helpers.WiremockHelper
import play.api.http.Status
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json

object  CreateBusinessDetailsStub { // scalastyle:off number.of.methods

  // Stub CreateBusinessDetails
  def stubCreateBusinessDetailsResponse()(status: Int, response: List[CreateIncomeSourceResponse]): Unit =
    WiremockHelper.stubPost(s"/income-tax-business-details/create-income-source/business", status, Json.toJson(response).toString)

  def stubCreateBusinessDetailsErrorResponse(): Unit =
    WiremockHelper.stubPost(s"/income-tax-business-details/create-income-source/business", INTERNAL_SERVER_ERROR, "")

  def stubCreateBusinessDetailsErrorResponseNew()(response: List[CreateIncomeSourceErrorResponse]): Unit =
    WiremockHelper.stubPost(s"/income-tax-business-details/create-income-source/business", INTERNAL_SERVER_ERROR, Json.toJson(response).toString)
  
}
