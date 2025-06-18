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

import implicits.ImplicitDateFormatter
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json._

import java.time.LocalDate
import java.time.format.DateTimeFormatter


sealed trait LiabilityCalculationResponseModel

case class LiabilityCalculationError(status: Int, message: String) extends LiabilityCalculationResponseModel

object LiabilityCalculationError {
  implicit val format: OFormat[LiabilityCalculationError] = Json.format[LiabilityCalculationError]
}

case class LiabilityCalculationResponse(
                                         inputs: Inputs,
                                         metadata: Metadata,
                                         messages: Option[Messages],
                                         calculation: Option[Calculation]
                                       ) extends LiabilityCalculationResponseModel

object LiabilityCalculationResponse {
  implicit val format: OFormat[LiabilityCalculationResponse] = Json.format[LiabilityCalculationResponse]
}

case class Metadata(calculationTimestamp: Option[String],
                    calculationType: String,
                    calculationReason: Option[String] = None,
                    periodFrom: Option[LocalDate] = None,
                    periodTo: Option[LocalDate] = None) {

  def isCalculationCrystallised: Boolean = {
    calculationType match {
      case "DF" | "crystallisation" => true
      case _ => false
    }
  }
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

  def getErrorMessageVariables(messagesProperty: MessagesApi, isAgent: Boolean): Seq[Message] = {
    val lang = Lang("GB")
    val errMessages = errorMessages.map(msg => {
      val key = if(isAgent) "tax-year-summary.agent.message." + msg.id else "tax-year-summary.message." + msg.id
      if (messagesProperty.isDefinedAt(key)(lang)) {
        val regex = "\\d{2}/\\d{2}/\\d{4}|£\\d+,\\d+|\\d+|£\\d+".r
        val variable: String = regex.findFirstIn(msg.text).getOrElse("")
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

    val pattern = DateTimeFormatter.ofPattern("d/MM/yyyy")
    val errorMessagesDateFormat: Seq[String] = Seq("C15014", "C55014", "C55008", "C55011", "C55012", "C55013")
    // lang conversion for date (GB,CY)
    val errorMessages = messages.map(msg => {
      errorMessagesDateFormat.contains(msg.id) match {
        case true =>
          val date = LocalDate.parse(msg.text, pattern)
          val dateText = implicitDateFormatter.longDate(date).toLongDate
          Message(id = msg.id, text = dateText)
        case false =>
          Message(id = msg.id, text = msg.text)
      }
    })

    errorMessages
  }
}
