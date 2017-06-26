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
import models.ObligationsModel
import play.api.http.Status

object SelfAssessmentStub {

  val businessDetailsUrl: String => String = nino => s"/self-assessment/ni/$nino/self-employments"
  val obligationsDataUrl: (String, String) => String = (nino, selfEmploymentId) => s"/self-assessment/ni/$nino/self-employments/$selfEmploymentId"
  val propertyObligationsUrl: String => String = nino => s"/self-assessment/ni/$nino/uk-properties/obligations"


  //TODO change the stubbed response to return a business details model

  def stubGetObligations(nino: String, selfEmploymentId: String, business: ObligationsModel, property: ObligationsModel): Unit = {
    val businessDetailsResponse = IntegrationTestConstants.GetBusinessDetails.successResponse(selfEmploymentId).toString()
    WiremockHelper.stubGet(businessDetailsUrl(nino), Status.OK, businessDetailsResponse)

    val businessObligationResponse = IntegrationTestConstants.GetObligationsData.successResponse(business).toString()
    WiremockHelper.stubGet(obligationsDataUrl(nino, selfEmploymentId), Status.OK, businessObligationResponse)

    val propertyObligationResponse = IntegrationTestConstants.GetObligationsData.successResponse(property).toString()
    WiremockHelper.stubGet(propertyObligationsUrl(nino), Status.OK, propertyObligationResponse)
  }

  def stubGetOnlyBizObs(nino: String, selfEmploymentId: String, business: ObligationsModel): Unit = {
    val businessDetailsResponse = IntegrationTestConstants.GetBusinessDetails.successResponse(selfEmploymentId).toString()
    WiremockHelper.stubGet(businessDetailsUrl(nino), Status.OK, businessDetailsResponse)

    val businessObligationResponse = IntegrationTestConstants.GetObligationsData.successResponse(business).toString()
    WiremockHelper.stubGet(obligationsDataUrl(nino, selfEmploymentId), Status.OK, businessObligationResponse)

    val propertyObligationResponse = IntegrationTestConstants.GetObligationsData.emptyResponse().toString()
    WiremockHelper.stubGet(propertyObligationsUrl(nino), Status.OK, propertyObligationResponse)
  }

  def stubGetOnlyPropObs(nino: String, selfEmploymentId: String, property: ObligationsModel): Unit = {

    val businessDetailsResponse = IntegrationTestConstants.GetBusinessDetails.successResponse(selfEmploymentId).toString()
    WiremockHelper.stubGet(businessDetailsUrl(nino), Status.OK, businessDetailsResponse)

    val businessObligationResponse = IntegrationTestConstants.GetObligationsData.emptyResponse().toString()
    WiremockHelper.stubGet(obligationsDataUrl(nino, selfEmploymentId), Status.OK, businessObligationResponse)

    val propertyObligationResponse = IntegrationTestConstants.GetObligationsData.successResponse(property).toString()
    WiremockHelper.stubGet(propertyObligationsUrl(nino), Status.OK, propertyObligationResponse)
  }

  def verifyGetObligations(nino: String, selfEmploymentId: String): Unit = {
    WiremockHelper.verifyGet(businessDetailsUrl(nino))
    WiremockHelper.verifyGet(obligationsDataUrl(nino, selfEmploymentId))
  }



}

