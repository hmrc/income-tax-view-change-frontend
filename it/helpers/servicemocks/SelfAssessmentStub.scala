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

package helpers.servicemocks

import helpers.{IntegrationTestConstants, WiremockHelper}
import models.{BusinessDetailsModel, BusinessModel, ObligationsModel}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}

object SelfAssessmentStub {

  val businessDetailsUrl: String => String = nino => s"/ni/$nino/self-employments"
  val propertyDetailsUrl: String => String = nino => s"/ni/$nino/uk-properties"
  val businessObligationsUrl: (String, String) => String = (nino, selfEmploymentId) => s"/ni/$nino/self-employments/$selfEmploymentId/obligations"
  val propertyObligationsUrl: String => String = nino => s"/ni/$nino/uk-properties/obligations"

  //Income Source Details
  def stubGetBusinessDetails(nino: String, businessDetails: JsValue) : Unit =
    WiremockHelper.stubGet(businessDetailsUrl(nino), Status.OK, businessDetails.toString())

  def stubGetNoBusinessDetails(nino: String) : Unit =
    WiremockHelper.stubGet(businessDetailsUrl(nino), Status.OK, "[]")

  def stubGetPropertyDetails(nino: String, propertyDetails: JsValue) : Unit =
    WiremockHelper.stubGet(propertyDetailsUrl(nino), Status.OK, propertyDetails.toString())

  def stubGetNoPropertyDetails(nino: String) : Unit =
    WiremockHelper.stubGet(propertyDetailsUrl(nino), Status.NOT_FOUND, "{}")

  //Obligations
  def stubGetBusinessObligations(nino: String, selfEmploymentId: String, business: ObligationsModel): Unit =
    WiremockHelper.stubGet(businessObligationsUrl(nino, selfEmploymentId), Status.OK, Json.toJson(business).toString())

  def stubGetPropertyObligations(nino: String, property: ObligationsModel): Unit =
    WiremockHelper.stubGet(propertyObligationsUrl(nino), Status.OK, Json.toJson(property).toString())

  def stubPropertyObligationsError(nino: String): Unit =
    WiremockHelper.stubGet(propertyObligationsUrl(nino), Status.INTERNAL_SERVER_ERROR, "ISE")

  def stubBusinessObligationsError(nino: String, selfEmploymentId: String): Unit =
    WiremockHelper.stubGet(businessObligationsUrl(nino, selfEmploymentId), Status.INTERNAL_SERVER_ERROR, "ISE")


  // Verifications
  def verifyGetBusinessObligations(nino: String, selfEmploymentId: String): Unit =
    WiremockHelper.verifyGetWithHeader(businessObligationsUrl(nino, selfEmploymentId), "Accept", "application/vnd.hmrc.1.0+json")

  def verifyGetPropertyObligations(nino: String): Unit =
    WiremockHelper.verifyGetWithHeader(propertyObligationsUrl(nino), "Accept", "application/vnd.hmrc.1.0+json")

  def verifyGetBusinessDetails(nino: String): Unit =
    WiremockHelper.verifyGetWithHeader(businessDetailsUrl(nino), "Accept", "application/vnd.hmrc.1.0+json")

  def verifyGetPropertyDetails(nino: String): Unit =
    WiremockHelper.verifyGetWithHeader(propertyDetailsUrl(nino), "Accept", "application/vnd.hmrc.1.0+json")
}

