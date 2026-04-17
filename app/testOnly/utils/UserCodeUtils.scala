/*
 * Copyright 2026 HM Revenue & Customs
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

package testOnly.utils

import testOnly.forms.customLoginV2.{CustomUserBuilderForm, IncomeSourceBuilderForm, ObligationsBuilderForm}

object UserCodeUtils {

  def generateUserCode(data: CustomUserBuilderForm): String = {
    List(
      userTypeCode(data.agentType),
      userChannelCode(data.incomeSources.userChannel),
      soleTraderCode(data.incomeSources),
      ukPropertyCode(data.incomeSources),
      foreignPropertyCode(data.incomeSources),
      itsaStatusCode(
        data.itsaStatus.cyMinusOneCrystallisationStatus,
        data.itsaStatus.cyMinusOneItsaStatus,
        data.itsaStatus.cyItsaStatus,
        data.itsaStatus.cyPlusOneItsaStatus
      ),
      obligationsCode(data.obligations)
    ).mkString("|")
  }

  private def userTypeCode(userType: String): String =
    userType match {
      case "individual"      => "U1"
      case "primaryAgent"    => "U2"
      case "supportingAgent" => "U3"
      case other             => invalid("user type", other)
    }

  private def userChannelCode(userChannel: String): String =
    userChannel match {
      case "customer-led"            => "UC1"
      case "hmrc-led-unconfirmed"     => "UC2"
      case "hmrc-led-confirmed"       => "UC3"
      case other                     => invalid("user channel", other)
    }

  private def soleTraderCode(i: IncomeSourceBuilderForm): String =
    s"ST:${flags(
      'A' -> i.activeSoleTrader,
      'L' -> i.latentSoleTrader,
      'C' -> i.ceasedSoleTrader
    )}"

  private def ukPropertyCode(i: IncomeSourceBuilderForm): String =
    s"P:${flags(
      'A' -> i.activeUkProperty,
      'C' -> i.ceasedUkProperty
    )}"

  private def foreignPropertyCode(i: IncomeSourceBuilderForm): String =
    s"F:${flags(
      'A' -> i.activeForeignProperty,
      'C' -> i.ceasedForeignProperty
    )}"

  private def itsaStatusCode(
                              cyMinusOneCrystallisationStatus: String,
                              previousYear: String,
                              currentYear: String,
                              nextYear: String
                            ): String = {

    val crystallisation =
      cyMinusOneCrystallisationStatus match {
        case "Crystallised"     => "CR"
        case "NonCrystallised"  => "NC"
        case other              => invalid("crystallisation status", other)
      }

    val prev = itsaYearCode(previousYear)
    val curr = itsaYearCode(currentYear)
    val next = itsaYearCode(nextYear)

    s"ITSA:$crystallisation-$prev-$curr-$next"
  }

  private def itsaYearCode(status: String): String =
    status match {
      case "No Status"          => "0"
      case "MTD Mandated"       => "1"
      case "MTD Voluntary"      => "2"
      case "Annual"             => "3"
      case "Digitally Exempt"   => "4"
      case "Dormant"            => "5"
      case "MTD Exempt"         => "99"
      case other                => invalid("ITSA status", other)
    }

  private def obligationsCode(o: ObligationsBuilderForm): String =
    s"OB:${List(
      obligation(o.annualObligation),
      obligation(o.quarterlyUpdate1),
      obligation(o.quarterlyUpdate2),
      obligation(o.quarterlyUpdate3),
      obligation(o.quarterlyUpdate4)
    ).mkString("-")}"

  private def obligation(status: String): String =
    status match {
      case "Open"      => "O"
      case "Fulfilled" => "F"
      case "None"      => "N"
      case other       => invalid("obligation status", other)
    }

  private def flags(values: (Char, Boolean)*): String = {
    val code = values.collect { case (c, true) => c }.mkString
    if (code.isEmpty) "-" else code
  }

  private def invalid(context: String, value: String): Nothing =
    throw new IllegalArgumentException(s"Invalid $context: $value")
}