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

package audit.models

import auth.MtdItUser
import enums.IncomeSourceJourney.{SelfEmployment, UkProperty}
import forms.incomeSources.manage.ConfirmReportingMethodForm
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import play.api.test.FakeRequest
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

class SwitchReportingMethodAuditModelSpec extends AnyWordSpecLike with TestSupport {

  val transactionName: String = "switch-reporting-method"
  val auditType: String = "SwitchReportingMethod"

  val taxYear = "2022-2023"

  def mtdItUser(userType: Option[AffinityGroup], agentReferenceNumber: Option[String]): MtdItUser[_] = {
    MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = None,
      incomeSources = IncomeSourceDetailsModel("mtditid", None, Nil, Nil),
      btaNavPartial = None,
      saUtr = Some("saUtr"),
      credId = Some("credId"),
      userType = userType,
      arn = agentReferenceNumber
    )(FakeRequest())
  }

  def switchToQuarterlyReportingMethodAudit(userType: Option[AffinityGroup],
                                            agentReferenceNumber: Option[String]
                                           ): SwitchReportingMethodAuditModel = {

    val changeTo = "quarterly"

    SwitchReportingMethodAuditModel(
      journeyType = UkProperty.journeyType,
      reportingMethodChangeTo = changeTo,
      taxYear = taxYear,
      errorMessage = None
    )(
      mtdItUser(userType, agentReferenceNumber)
    )
  }

  def switchToAnnualReportingMethodErrorAudit(userType: Option[AffinityGroup],
                                              agentReferenceNumber: Option[String]
                                             ): SwitchReportingMethodAuditModel = {

    val changeTo = "annual"

    SwitchReportingMethodAuditModel(
      journeyType = SelfEmployment.journeyType,
      reportingMethodChangeTo = changeTo,
      taxYear = taxYear,
      errorMessage = Some(messages(ConfirmReportingMethodForm.noSelectionError(changeTo)))
    )(
      mtdItUser(userType, agentReferenceNumber)
    )
  }

  "SwitchReportingMethodAuditModel" should {
    "present a full audit model with an error message" when {

      val changeTo = "annual"

      "the user is an agent" in {
        switchToAnnualReportingMethodErrorAudit(Some(Agent), agentReferenceNumber = Some("agentReferenceNumber"))
          .detail shouldBe Json.obj(
            ("saUtr", "saUtr"),
            ("nationalInsuranceNumber", "nino"),
            ("userType", "Agent"),
            ("credId", "credId"),
            ("mtditid", "mtditid"),
            ("agentReferenceNumber", "agentReferenceNumber"),
            ("journeyType", SelfEmployment.journeyType),
            ("reportingMethodChangeTo", changeTo.capitalize),
            ("taxYear", taxYear),
            ("errorMessage", messages(s"incomeSources.manage.propertyReportingMethod.error.$changeTo"))
          )
      }
      "the user is an individual" in {
        switchToAnnualReportingMethodErrorAudit(Some(Individual), agentReferenceNumber = None)
          .detail shouldBe Json.obj(
            ("saUtr", "saUtr"),
            ("nationalInsuranceNumber", "nino"),
            ("userType", "Individual"),
            ("credId", "credId"),
            ("mtditid", "mtditid"),
            ("journeyType", SelfEmployment.journeyType),
            ("reportingMethodChangeTo", changeTo.capitalize),
            ("taxYear", taxYear),
            ("errorMessage", messages(s"incomeSources.manage.propertyReportingMethod.error.$changeTo"))
          )
      }
    }

    "present a full audit model with no error message field present" when {

      val changeTo = "quarterly"

      "the user is an agent and there is no form error" in {
        switchToQuarterlyReportingMethodAudit(Some(Agent), agentReferenceNumber = Some("agentReferenceNumber"))
          .detail shouldBe Json.obj(
            ("saUtr", "saUtr"),
            ("nationalInsuranceNumber", "nino"),
            ("userType", "Agent"),
            ("credId", "credId"),
            ("mtditid", "mtditid"),
            ("agentReferenceNumber", "agentReferenceNumber"),
            ("journeyType", UkProperty.journeyType),
            ("reportingMethodChangeTo", changeTo.toLowerCase.capitalize),
            ("taxYear", taxYear)
        )
      }
      "the user is an individual and there is no form error" in {
        switchToQuarterlyReportingMethodAudit(Some(Individual), agentReferenceNumber = None)
          .detail shouldBe Json.obj(
            ("saUtr", "saUtr"),
            ("nationalInsuranceNumber", "nino"),
            ("userType", "Individual"),
            ("credId", "credId"),
            ("mtditid", "mtditid"),
            ("journeyType", UkProperty.journeyType),
            ("reportingMethodChangeTo", changeTo.toLowerCase.capitalize),
            ("taxYear", taxYear)
        )
      }
    }
  }
}
