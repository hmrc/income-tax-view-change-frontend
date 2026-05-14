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

package models.liabilitycalculation

import enums.TaxYearSummary.CalcType.{amendmentTypes, crystallisedTypes}
import implicits.ImplicitDateFormatter
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try
import scala.util.matching.Regex


sealed trait LiabilityCalculationResponseModel

case class LiabilityCalculationError(status: Int, message: String) extends LiabilityCalculationResponseModel

object LiabilityCalculationError {
  implicit val format: OFormat[LiabilityCalculationError] = Json.format[LiabilityCalculationError]
}

case class LiabilityCalculationResponse(
                                         inputs: Inputs,
                                         metadata: Metadata,
                                         messages: Option[Messages],
                                         calculation: Option[Calculation],
                                         submissionChannel: Option[SubmissionChannel]
                                       ) extends LiabilityCalculationResponseModel

object LiabilityCalculationResponse {
  implicit val format: OFormat[LiabilityCalculationResponse] = Json.format[LiabilityCalculationResponse]
}

case class Metadata(
                     calculationTimestamp: Option[String],
                     calculationType: String,
                     calculationReason: Option[String] = None,
                     periodFrom: Option[LocalDate] = None,
                     periodTo: Option[LocalDate] = None
                   ) {

  def isCalculationCrystallised: Boolean = crystallisedTypes.contains(calculationType)

  def hasAnAmendment: Boolean = amendmentTypes.contains(calculationType)
}

object Metadata {
  implicit val format: OFormat[Metadata] = Json.format[Metadata]
}

case class Inputs(personalInformation: PersonalInformation)

object Inputs {
  implicit val format: OFormat[Inputs] = Json.format[Inputs]
}

case class PersonalInformation(taxRegime: String, class2VoluntaryContributions: Option[Boolean] = None)

object PersonalInformation {
  implicit val format: OFormat[PersonalInformation] = Json.format[PersonalInformation]
}

case class Message(id: String, text: String)

object Message {
  implicit val format: OFormat[Message] = Json.format[Message]
}

case class Messages(info: Option[Seq[Message]] = None, warnings: Option[Seq[Message]] = None, errors: Option[Seq[Message]] = None) {
  // When updating the accepted messages also update the audit for the TaxCalculationDetailsResponseAuditModel
  private val acceptedMessages: Seq[String] = Seq("C22202", "C22203", "C22206", "C22207", "C22210", "C22211",
    "C22212", "C22213", "C22214", "C22215", "C22216", "C22217", "C22218", "C22223", "C22224", "C22225", "C22226", "C22225_Scottish", "C22226_Scottish")

  def formatMessagesScottishWelshTaxRegime(info: Seq[Message]): Seq[Message] = {
    val taxRegimeScottish: String = "Scottish Basic Rate"
    val taxRegimeScottishIds: Seq[String] = Seq("C22225", "C22226")

    info.map {
      case message if taxRegimeScottishIds.contains(message.id) && message.text.contains(taxRegimeScottish) => message.copy(id = message.id + "_Scottish")
      case otherMessage => otherMessage
    }
  }

  val allMessages: Seq[Message] = {
    formatMessagesScottishWelshTaxRegime(info.getOrElse(Seq.empty)) ++ warnings.getOrElse(Seq.empty) ++ errors.getOrElse(Seq.empty)
  }
  val errorMessages: Seq[Message] = errors.getOrElse(Seq.empty)

  val genericMessages: Seq[Message] = allMessages.filter(message => acceptedMessages.contains(message.id))

  private def messageVariablesRegex: Regex = {
    val dateDashPattern = "\\d{4}-\\d{2}-\\d{2}"
    val dateSlashPattern = "\\d{2}/\\d{2}/\\d{4}"
    val currencyDecimal = "£\\d+\\.\\d{2}"
    val currencyComma = "£\\d+,\\d+"
    val currencyWhole = "£\\d+"
    val numberPattern = "\\d+"

    Seq(
      dateDashPattern,
      dateSlashPattern,
      currencyDecimal,
      currencyComma,
      currencyWhole,
      numberPattern
    ).mkString("|").r
  }

  def getErrorMessageVariables(messagesProperty: MessagesApi, isAgent: Boolean): Seq[Message] = {
    val lang = Lang("GB")
    val errMessages = errorMessages.map(msg => {
      val key = if (isAgent) "tax-year-summary.agent.message." + msg.id else "tax-year-summary.message." + msg.id
      if (messagesProperty.isDefinedAt(key)(lang)) {
        val variable: String = messageVariablesRegex.findFirstIn(msg.text).getOrElse("")
        Message(id = msg.id, text = variable)
      } else {
        Message(id = msg.id, text = "")
      }
    })
    errMessages
  }

}

object Messages {
  implicit val format: OFormat[Messages] = Json.format[Messages]

  def translateMessageDateVariables(messages: Seq[Message])(implicit message: play.api.i18n.Messages, implicitDateFormatter: ImplicitDateFormatter): Seq[Message] = {

    val legacyPattern = DateTimeFormatter.ofPattern("d/MM/yyyy")
    val isoPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val errorMessagesDateFormat: Seq[String] = Seq("C15014", "C55014", "C55008", "C55011", "C55012", "C55013", "C55007")

    def parseDate(text: String): Option[LocalDate] =
      Iterator(isoPattern, legacyPattern)
        .flatMap(fmt => scala.util.Try(LocalDate.parse(text, fmt)).toOption)
        .nextOption()

    messages.map { msg =>
      if (errorMessagesDateFormat.contains(msg.id)) {
        parseDate(msg.text) match {
          case Some(date) => Message(id = msg.id, text = implicitDateFormatter.longDate(date).toLongDate)
          case None => msg
        }
      } else {
        msg
      }
    }
  }

  def translateMessageCurrencyVariables(messages: Seq[Message])(implicit message: play.api.i18n.Messages): Seq[Message] = {

    val errorMessagesCurrencyFormat: Seq[String] = Seq("C55109", "C55110", "C55602", "C55525")
    val currencyRegex = "£\\d+\\.\\d{2}".r

    messages.map { msg =>
      if (errorMessagesCurrencyFormat.contains(msg.id)) {
        val updatedText = currencyRegex.findFirstIn(msg.text)
          .flatMap(value => Try(BigDecimal(value.stripPrefix("£"))).toOption)
          .map { amount =>
            msg.text.replace(currencyRegex.findFirstIn(msg.text).get, amount.toString())
          }
          .getOrElse(msg.text)
        Message(id = msg.id, text = updatedText)
      } else {
        msg
      }
    }
  }
}
