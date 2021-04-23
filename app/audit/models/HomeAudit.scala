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

package audit.models

import audit.Utilities._
import auth.MtdItUser
import play.api.libs.json.{JsObject, JsValue, Json}

import java.time.LocalDate

case class HomeAudit(mtdItUser: MtdItUser[_],
                     nextPaymentOrOverdue: Option[Either[(LocalDate, Boolean), Int]],
                     nextUpdateOrOverdue: Either[(LocalDate, Boolean), Int],
                     agentReferenceNumber: Option[String]) extends ExtendedAuditModel {

  private val paymentsInformation: JsObject = nextPaymentOrOverdue match {
    case Some(Right(count)) => Json.obj("overduePayments" -> count)
    case Some(Left((date, _))) => Json.obj("nextPaymentDeadline" -> date.toString)
    case None => Json.obj()
  }

  private val updatesInformation: JsObject = nextUpdateOrOverdue match {
    case Right(count) => Json.obj("overdueUpdates" -> count)
    case Left((date, _)) => Json.obj("nextUpdateDeadline" -> date.toString)
  }

  override val transactionName: String = "itsa-home-page"
  override val detail: JsValue = {
    agentReferenceNumber.fold(Json.obj())(arn => Json.obj("agentReferenceNumber" -> arn)) ++
      Json.obj("nationalInsuranceNumber" -> mtdItUser.nino) ++
      mtdItUser.saUtr.fold(Json.obj())(sautr => Json.obj("saUtr" -> sautr)) ++
      userType(mtdItUser.userType) ++
      mtdItUser.credId.fold(Json.obj())(credId => Json.obj("credId" -> credId)) ++
      Json.obj("mtditid" -> mtdItUser.mtditid) ++
      paymentsInformation ++
      updatesInformation
  }
  override val auditType: String = "ItsaHomePage"

}

object HomeAudit {
  def apply(mtdItUser: MtdItUser[_],
            nextPaymentDueDate: Option[LocalDate],
            nextUpdateDueDate: LocalDate,
            overduePaymentsCount: Int,
            overdueUpdatesCount: Int): HomeAudit = {

    val nextPaymentOrOverdue: Option[Either[(LocalDate, Boolean), Int]] = nextPaymentDueDate.map { date =>
      if (overduePaymentsCount == 0) Left(date -> false)
      else if (overduePaymentsCount == 1) Left(date -> true)
      else Right(overduePaymentsCount)
    }

    val nextUpdateOrOverdue: Either[(LocalDate, Boolean), Int] = {
      if (overdueUpdatesCount == 0) Left(nextUpdateDueDate -> false)
      else if (overdueUpdatesCount == 1) Left(nextUpdateDueDate -> true)
      else Right(overdueUpdatesCount)
    }

    HomeAudit(
      mtdItUser,
      nextPaymentOrOverdue = nextPaymentOrOverdue,
      nextUpdateOrOverdue = nextUpdateOrOverdue,
      agentReferenceNumber = None
    )
  }
}
