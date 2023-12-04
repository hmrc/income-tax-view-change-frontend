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

package models.incomeSourceDetails

import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Instant, LocalDate}

case class UIJourneySessionData(
                                 sessionId: String,
                                 journeyType: String,
                                 addIncomeSourceData: Option[AddIncomeSourceData] = None,
                                 manageIncomeSourceData: Option[ManageIncomeSourceData] = None,
                                 ceaseIncomeSourceData: Option[CeaseIncomeSourceData] = None,
                                 lastUpdated: Instant = Instant.now)

object UIJourneySessionData {

  val reads: Reads[UIJourneySessionData] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "sessionId").read[String] and
        (__ \ "journeyType").read[String] and
        (__ \ "addIncomeSourceData").readNullable[AddIncomeSourceData] and
        (__ \ "manageIncomeSourceData").readNullable[ManageIncomeSourceData] and
        (__ \ "ceaseIncomeSourceData").readNullable[CeaseIncomeSourceData] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      )(UIJourneySessionData.apply _)
  }

  val writes: OWrites[UIJourneySessionData] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "sessionId").write[String] and
        (__ \ "journeyType").write[String] and
        (__ \ "addIncomeSourceData").writeNullable[AddIncomeSourceData] and
        (__ \ "manageIncomeSourceData").writeNullable[ManageIncomeSourceData] and
        (__ \ "ceaseIncomeSourceData").writeNullable[CeaseIncomeSourceData] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
      )(unlift(UIJourneySessionData.unapply))
  }

  implicit val format: OFormat[UIJourneySessionData] = OFormat(reads, writes)
}

case class AddIncomeSourceData(
                                businessName: Option[String] = None,
                                businessTrade: Option[String] = None,
                                dateStarted: Option[LocalDate] = None,
                                accountingPeriodStartDate: Option[LocalDate] = None,
                                accountingPeriodEndDate: Option[LocalDate] = None,
                                incomeSourceId: Option[String] = None,
                                address: Option[Address] = None,
                                countryCode: Option[String] = None,
                                incomeSourcesAccountingMethod: Option[String] = None,
                                incomeSourceAdded: Option[Boolean] = None,
                                reportingMethodSet: Option[Boolean] = None
                              )

object AddIncomeSourceData {
  val businessNameField = "businessName"
  val businessTradeField = "businessTrade"
  val dateStartedField = "dateStarted"
  val accountingPeriodStartDateField = "accountingPeriodStartDate"
  val accountingPeriodEndDateField = "accountingPeriodEndDate"
  val incomeSourceIdField: String = "incomeSourceId"
  val addressField: String = "address"
  val countryCodeField: String = "countryCode"
  val incomeSourcesAccountingMethodField: String = "incomeSourcesAccountingMethod"
  val reportingMethodSetField: String = "reportingMethodSet"
  val incomeSourceAddedField: String = "incomeSourceAdded"

  def getJSONKeyPath(name: String): String = s"addIncomeSourceData.$name"

  implicit val format: OFormat[AddIncomeSourceData] = Json.format[AddIncomeSourceData]
}

case class ManageIncomeSourceData(
                                   incomeSourceId: Option[String] = None
                                 )

object ManageIncomeSourceData {

  val incomeSourceIdField = "incomeSourceId"

  def getJSONKeyPath(name: String): String = s"manageIncomeSourceData.$name"

  implicit val format: OFormat[ManageIncomeSourceData] = Json.format[ManageIncomeSourceData]
}

case class CeaseIncomeSourceData(
                                  incomeSourceId: Option[String],
                                  endDate: Option[String],
                                  ceasePropertyDeclare: Option[String]
                                )

object CeaseIncomeSourceData {

  val incomeSourceIdField: String = "incomeSourceId"
  val dateCeasedField: String = "endDate"
  val ceasePropertyDeclare: String = "ceasePropertyDeclare"

  def getJSONKeyPath(name: String): String = s"ceaseIncomeSourceData.$name"

  implicit val format: OFormat[CeaseIncomeSourceData] = Json.format[CeaseIncomeSourceData]
}