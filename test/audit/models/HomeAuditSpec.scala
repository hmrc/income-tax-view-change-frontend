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

import authV2.AuthActionsTestData.{defaultMTDITUser, getMinimalMTDITUser}
import forms.IncomeSourcesFormsSpec.commonAuditDetails
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.itsaStatus.ITSAStatus
import models.obligations.NextUpdatesTileViewModel
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsArray, JsNull, JsNumber, JsObject, JsValue, Json}
import testConstants.BaseTestConstants.*
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

import java.time.LocalDate

class HomeAuditSpec extends AnyWordSpecLike with Matchers {

  val transactionName: String = "itsa-home-page"
  val auditType: String = "ItsaHomePage"
  lazy val fixedDate : LocalDate = LocalDate.of(2022, 1, 7)

  def homeAuditFull(userType: Option[AffinityGroup] = Some(Agent),
                    nextPaymentOrOverdue: Either[(LocalDate, Boolean), Int],
                    nextUpdateOrOverdue: Either[(LocalDate, Boolean), Int]): HomeAudit = HomeAudit(
    defaultMTDITUser(userType, IncomeSourceDetailsModel("nino", "mtditid", None, Nil, Nil, "1")),
    nextPaymentOrOverdue = Some(nextPaymentOrOverdue),
    nextUpdateOrOverdue = nextUpdateOrOverdue
  )

  val homeAuditMin: HomeAudit = HomeAudit(
    getMinimalMTDITUser(None, IncomeSourceDetailsModel("nino", "mtditid", None, Nil, Nil, "1")),
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
          ).detail mustBe commonAuditDetails(Agent) ++ Json.obj(
            "overduePayments" -> 2,
            "overdueUpdates" -> 2
          )
        }
        "there is are payments and updates due which are not overdue" in {
          assertJsonEquals(homeAuditFull(
            userType = Some(Individual),
            nextPaymentOrOverdue = Left(fixedDate -> false),
            nextUpdateOrOverdue = Left(fixedDate -> false)
          ).detail, commonAuditDetails(Individual) ++ Json.obj(
            "nextPaymentDeadline" -> fixedDate.toString,
            "nextUpdateDeadline" -> fixedDate.toString
          ))
        }
      }
      "the home audit has minimal details" in {
        assertJsonEquals(homeAuditMin.detail, Json.obj(
          "nino" -> testNino,
          "mtditid" -> testMtditid,
          "overdueUpdates" -> 2
        ))
      }
    }
  }

  "applySupportingAgent" should {
    "render the expected audit event" when {
      val user = defaultMTDITUser(Some(Agent), IncomeSourceDetailsModel("nino", "mtditid", None, Nil, Nil, "1"), isSupportingAgent = true)
      "there are updates due" that {
        "are not overdue" in {
          val nextDetailsTile = NextUpdatesTileViewModel(dueDates = List(fixedDate),
            currentDate = fixedDate.minusDays(5),
            isReportingFrequencyEnabled = true,
            showOptInOptOutContentUpdateR17 = false,
            currentYearITSAStatus = ITSAStatus.NoStatus,
            nextQuarterlyUpdateDueDate = None,
            nextTaxReturnDueDate = None)
          HomeAudit.applySupportingAgent(user, nextDetailsTile).detail shouldBe commonAuditDetails(Agent, true) ++ Json.obj(
            "nextUpdateDeadline" -> fixedDate.toString
          )
        }

        "are overdue" in {
          val nextDetailsTile = NextUpdatesTileViewModel(dueDates = List(fixedDate),
            currentDate = fixedDate.plusDays(5),
            isReportingFrequencyEnabled = true,
            showOptInOptOutContentUpdateR17 = false,
            currentYearITSAStatus = ITSAStatus.NoStatus,
            nextQuarterlyUpdateDueDate = None,
            nextTaxReturnDueDate = None)
          HomeAudit.applySupportingAgent(user, nextDetailsTile).detail shouldBe commonAuditDetails(Agent, true) ++ Json.obj(
            "nextUpdateDeadline" -> fixedDate.toString
          )
        }
      }

      "there are multiple overdue updates" in {
        val nextDetailsTile = NextUpdatesTileViewModel(List(fixedDate.minusDays(5), fixedDate.minusDays(10)),
          currentDate = fixedDate,
          isReportingFrequencyEnabled = true,
          showOptInOptOutContentUpdateR17 = false,
          currentYearITSAStatus = ITSAStatus.NoStatus,
          nextQuarterlyUpdateDueDate = None,
          nextTaxReturnDueDate = None)
        HomeAudit.applySupportingAgent(user, nextDetailsTile).detail shouldBe commonAuditDetails(Agent, true) ++ Json.obj(
          "overdueUpdates" -> 2
        )
      }
    }
  }

  def normalise(js: JsValue): JsValue = js match {

    case JsObject(fields) =>
      val filteredFields = fields.collect {
        case (k, v) if v != JsNull =>
          normalise(v) match {
            case JsObject(inner) if inner.isEmpty => None // remove empty object
            case JsArray(inner) if inner.isEmpty => None // remove empty array
            case n => Some(k -> n)
          }
      }.flatten

      JsObject(filteredFields.toSeq.sortBy(_._1))

    case JsArray(values) =>
      val normValues = values.collect { case v if v != JsNull => normalise(v) }
      val filteredValues = normValues.filter {
        case JsObject(inner) if inner.isEmpty => false
        case JsArray(inner) if inner.isEmpty => false
        case _ => true
      }
      if (filteredValues.forall(_.isInstanceOf[JsObject])) JsArray(filteredValues.sortBy(_.toString))
      else JsArray(filteredValues)

    case JsNumber(n) => JsNumber(n.bigDecimal.stripTrailingZeros())
    case other => other
  }

  def assertJsonEquals(actual: JsValue, expected: JsValue): Assertion =
    normalise(actual) shouldEqual normalise(expected)

}
