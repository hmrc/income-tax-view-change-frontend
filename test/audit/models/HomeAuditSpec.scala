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

import testConstants.BaseTestConstants._
import auth.MtdItUser
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class HomeAuditSpec extends AnyWordSpecLike with Matchers {

  val transactionName: String = "itsa-home-page"
  val auditType: String = "ItsaHomePage"

  def homeAuditFull(userType: Option[AffinityGroup] = Some(Agent), agentReferenceNumber: Option[String] = Some("agentReferenceNumber"),
                    nextPaymentOrOverdue: Either[(LocalDate, Boolean), Int],
                    nextUpdateOrOverdue: Either[(LocalDate, Boolean), Int]): HomeAudit = HomeAudit(
    mtdItUser = MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = Some(Name(Some("firstName"), Some("lastName"))),
      incomeSources = IncomeSourceDetailsModel("nino", "mtditid", None, Nil, Nil),
      btaNavPartial = None,
      saUtr = Some("saUtr"),
      credId = Some("credId"),
      userType = userType,
      arn = agentReferenceNumber
    ),
    nextPaymentOrOverdue = Some(nextPaymentOrOverdue),
    nextUpdateOrOverdue = nextUpdateOrOverdue
  )

  val homeAuditMin: HomeAudit = HomeAudit(
    mtdItUser = MtdItUser(
      mtditid = "mtditid",
      nino = "nino",
      userName = None,
      incomeSources = IncomeSourceDetailsModel("nino", "mtditid", None, Nil, Nil),
      btaNavPartial = None,
      saUtr = None,
      credId = None,
      userType = None,
      arn = None
    ),
    nextPaymentOrOverdue = None,
    nextUpdateOrOverdue = Right(2)
  )

  "HomeAudit(mtdItUser, nextPaymentOrOverdue, nextUpdateOrOverdue, agentReferenceNumber)" should {

    s"have the correct transaction name of '$transactionName'" in {
      homeAuditFull(
        nextPaymentOrOverdue = Right(2),
        nextUpdateOrOverdue = Right(2)
      ).transactionName mustBe transactionName
    }

    s"have the correct audit event type of '$auditType'" in {
      homeAuditFull(
        nextPaymentOrOverdue = Right(2),
        nextUpdateOrOverdue = Right(2)
      ).auditType mustBe auditType
    }

    "have the correct details for the audit event" when {
      "the home audit has all detail" when {
        "there are multiple overdue payments and updates" in {
          homeAuditFull(
            userType = Some(Agent),
            nextPaymentOrOverdue = Right(2),
            nextUpdateOrOverdue = Right(2)
          ).detail mustBe Json.obj(
            "agentReferenceNumber" -> "agentReferenceNumber",
            "nino" -> "nino",
            "saUtr" -> "saUtr",
            "userType" -> "Agent",
            "credId" -> "credId",
            "mtditid" -> "mtditid",
            "overduePayments" -> 2,
            "overdueUpdates" -> 2,
            "agentReferenceNumber" -> "agentReferenceNumber"
          )
        }
        "there is are payments and updates due which are not overdue" in {
          homeAuditFull(
            userType = Some(Individual),
            nextPaymentOrOverdue = Left(LocalDate.now -> false),
            nextUpdateOrOverdue = Left(LocalDate.now -> false)
          ).detail mustBe Json.obj(
            "agentReferenceNumber" -> "agentReferenceNumber",
            "nino" -> "nino",
            "saUtr" -> "saUtr",
            "userType" -> "Individual",
            "credId" -> "credId",
            "mtditid" -> "mtditid",
            "nextPaymentDeadline" -> LocalDate.now.toString,
            "nextUpdateDeadline" -> LocalDate.now.toString,
            "agentReferenceNumber" -> "agentReferenceNumber"
          )
        }
      }
      "the home audit has minimal details" in {
        homeAuditMin.detail mustBe Json.obj(
          "nino" -> "nino",
          "mtditid" -> "mtditid",
          "overdueUpdates" -> 2
        )
      }
    }
  }

}
