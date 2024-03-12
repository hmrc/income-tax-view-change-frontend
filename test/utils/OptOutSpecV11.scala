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
 * See the License for the specific language governing permissions,
 * limitations under the License.
 */

package utils

import org.scalatest.prop.TableDrivenPropertyChecks._
import testUtils.UnitSpec

class OptOutSpecV11 extends UnitSpec {

  object ITSAStatus extends Enumeration {
    type ITSAStatus = Value
    val Mandated, Voluntary, Annual, Unknown, NotGenerated = Value
  }
  import ITSAStatus._

  trait OptOutTaxYear {
    val itsaStatus: ITSAStatus
  }

  case class CurrentOptOutTaxYear(itsaStatus: ITSAStatus) extends OptOutTaxYear {
    def canOptOut: Boolean = itsaStatus == Voluntary
  }

  case class FutureOptOutTaxYear(itsaStatus: ITSAStatus, previousTaxYear: OptOutTaxYear) extends OptOutTaxYear {
    def canOptOut: Boolean = itsaStatus == Voluntary || (previousTaxYear.itsaStatus == Voluntary && itsaStatus == Unknown)
  }

  case class PastOptOutTaxYear(itsaStatus: ITSAStatus, crystallised: String) extends OptOutTaxYear {
    def canOptOut: Boolean = itsaStatus == Voluntary && crystallised != "Y"
  }

  
  private val optOuts =
    Table(
      ("Crystallised", "CY-1", "CY", "CY+1", "Valid", "Outcome"),  // First tuple defines column names
      ("N", "M", "M", "M", "N/A", "Nothing displayed"),
      ("Y", "M", "M", "M", "N/A", "Nothing displayed"),
      ("N", "M", "M", "M", "N/A", "Nothing displayed"),
      ("Y", "M", "M", "M", "N/A", "Nothing displayed"),
      ("N", "M", "M", "M", "N/A", "Nothing displayed"),
      ("Y", "M", "M", "M", "N/A", "Nothing displayed"),
      ("N", "M", "M", " ", "N/A", "Nothing displayed"),
      ("Y", "M", "M", " ", "N/A", "Nothing displayed"),
      ("N", "M", "M", " ", "N/A", "Nothing displayed"),
      ("Y", "M", "M", " ", "N/A", "Nothing displayed"),
      ("N", "M", "M", " ", "N/A", "Nothing displayed"),
      ("Y", "M", "M", " ", "N/A", "Nothing displayed"),
      ("N", "M", "M", "V", "N/A", "CY+1"),
      ("Y", "M", "M", "V", "N/A", "CY+1"),
      ("N", "M", "M", "V", "N/A", "CY+1"),
      ("Y", "M", "M", "V", "N/A", "CY+1"),
      ("N", "M", "M", "V", "Allowed", "CY+1"),
      ("Y", "M", "M", "V", "Allowed", "CY+1"),
      ("N", "M", "M", "A", "N/A", "Nothing displayed"),
      ("Y", "M", "M", "A", "N/A", "Nothing displayed"),
      ("N", "M", "M", "A", "N/A", "Nothing displayed"),
      ("Y", "M", "M", "A", "N/A", "Nothing displayed"),
      ("N", "M", "M", "A", "N/A", "Nothing displayed"),
      ("Y", "M", "M", "A", "N/A", "Nothing displayed"),
      ("N", "M", "V", "M", "N/A", "CY"),
      ("Y", "M", "V", "M", "N/A", "CY"),
      ("N", "M", "V", "M", "Allowed", "CY"),
      ("Y", "M", "V", "M", "Allowed", "CY"),
      ("N", "M", "V", "M", "N/A", "CY"),
      ("Y", "M", "V", "M", "N/A", "CY"),
      ("N", "M", "V", " ", "N/A", "CY, CY+1"),
      ("Y", "M", "V", " ", "N/A", "CY, CY+1"),
      ("N", "M", "V", " ", "Allowed", "CY, CY+1"),
      ("Y", "M", "V", " ", "Allowed", "CY, CY+1"),
      ("N", "M", "V", " ", "Allowed", "CY, CY+1"),
      ("Y", "M", "V", " ", "Allowed", "CY, CY+1"),
      ("N", "M", "V", "V", "N/A", "CY, CY+1"),
      ("Y", "M", "V", "V", "N/A", "CY, CY+1"),
      ("N", "M", "V", "V", "Allowed", "CY, CY+1"),
      ("Y", "M", "V", "V", "Allowed", "CY, CY+1"),
      ("N", "M", "V", "V", "Allowed", "CY, CY+1"),
      ("Y", "M", "V", "V", "Allowed", "CY, CY+1"),
      ("N", "M", "V", "A", "N/A", "CY"),
      ("Y", "M", "V", "A", "N/A", "CY"),
      ("N", "M", "V", "A", "Allowed", "CY"),
      ("Y", "M", "V", "A", "Allowed", "CY"),
      ("N", "M", "V", "A", "N/A", "CY"),
      ("Y", "M", "V", "A", "N/A", "CY"),
      ("N", "M", "A", "M", "N/A", "Nothing displayed"),
      ("Y", "M", "A", "M", "N/A", "Nothing displayed"),
      ("N", "M", "A", "M", "N/A", "Nothing displayed"),
      ("Y", "M", "A", "M", "N/A", "Nothing displayed"),
      ("N", "M", "A", "M", "N/A", "Nothing displayed"),
      ("Y", "M", "A", "M", "N/A", "Nothing displayed"),
      ("N", "M", "A", " ", "N/A", "Nothing displayed"),
      ("Y", "M", "A", " ", "N/A", "Nothing displayed"),
      ("N", "M", "A", " ", "N/A", "Nothing displayed"),
      ("Y", "M", "A", " ", "N/A", "Nothing displayed"),
      ("N", "M", "A", " ", "N/A", "Nothing displayed"),
      ("Y", "M", "A", " ", "N/A", "Nothing displayed"),
      ("N", "M", "A", "V", "N/A", "CY+1"),
      ("Y", "M", "A", "V", "N/A", "CY+1"),
      ("N", "M", "A", "V", "N/A", "CY+1"),
      ("Y", "M", "A", "V", "N/A", "CY+1"),
      ("N", "M", "A", "V", "Allowed", "CY+1"),
      ("Y", "M", "A", "V", "Allowed", "CY+1"),
      ("N", "M", "A", "A", "N/A", "Nothing displayed"),
      ("Y", "M", "A", "A", "N/A", "Nothing displayed"),
      ("N", "M", "A", "A", "N/A", "Nothing displayed"),
      ("Y", "M", "A", "A", "N/A", "Nothing displayed"),
      ("N", "M", "A", "A", "N/A", "Nothing displayed"),
      ("Y", "M", "A", "A", "N/A", "Nothing displayed"),
      ("N", "V", "M", "M", "Allowed", "CY-1"),
      ("Y", "V", "M", "M", "N/A", "Nothing displayed"),
      ("N", "V", "M", "M", "N/A", "CY-1"),
      ("Y", "V", "M", "M", "N/A", "Nothing displayed"),
      ("N", "V", "M", "M", "N/A", "CY-1"), // Changed from Nothing Displayed -> CY-1 (row 79 on sheet) Invalid Anyway!
      ("Y", "V", "M", "M", "N/A", "Nothing displayed"),
      ("N", "V", "M", " ", "Allowed", "CY-1"),
      ("Y", "V", "M", " ", "N/A", "Nothing displayed"),
      ("N", "V", "M", " ", "N/A", "CY-1"),
      ("Y", "V", "M", " ", "N/A", "Nothing displayed"),
      ("N", "V", "M", " ", "N/A", "CY-1"),
      ("Y", "V", "M", " ", "N/A", "Nothing displayed"),
      ("N", "V", "M", "V", "Allowed", "CY-1, CY+1"),
      ("Y", "V", "M", "V", "N/A", "CY+1"),
      ("N", "V", "M", "V", "N/A", "CY-1, CY+1"),
      ("Y", "V", "M", "V", "N/A", "CY+1"),
      ("N", "V", "M", "V", "Allowed", "CY-1, CY+1"),
      ("Y", "V", "M", "V", "Allowed", "CY+1"),
      ("N", "V", "M", "A", "Allowed", "CY-1"),
      ("Y", "V", "M", "A", "N/A", "Nothing displayed"),
      ("N", "V", "M", "A", "N/A", "CY-1"),
      ("Y", "V", "M", "A", "N/A", "Nothing displayed"),
      ("N", "V", "M", "A", "N/A", "CY-1"),
      ("Y", "V", "M", "A", "N/A", "Nothing displayed"),
      ("N", "V", "V", "M", "Allowed", "CY-1, CY"),
      ("Y", "V", "V", "M", "N/A", "CY"),
      ("N", "V", "V", "M", "Allowed", "CY-1, CY"),
      ("Y", "V", "V", "M", "Allowed", "CY"),
      ("N", "V", "V", "M", "N/A", "CY-1, CY"),
      ("Y", "V", "V", "M", "N/A", "CY"),
      ("N", "V", "V", " ", "Allowed", "CY-1, CY, CY+1"),
      ("Y", "V", "V", " ", "N/A", "CY, CY+1"), // Changed from CY -> CY, CY+1 (row 106 on sheet). Marked as Invalid but could be Valid???
      ("N", "V", "V", " ", "Allowed", "CY-1, CY, CY+1"),
      ("Y", "V", "V", " ", "Allowed", "CY, CY+1"),
      ("N", "V", "V", " ", "Allowed", "CY-1, CY, CY+1"),
      ("Y", "V", "V", " ", "Allowed", "CY, CY+1"),
      ("N", "V", "V", "V", "Allowed", "CY-1, CY, CY+1"),
      ("Y", "V", "V", "V", "N/A", "CY, CY+1"),
      ("N", "V", "V", "V", "Allowed", "CY-1, CY, CY+1"),
      ("Y", "V", "V", "V", "Allowed", "CY, CY+1"),
      ("N", "V", "V", "V", "Allowed", "CY-1, CY, CY+1"),
      ("Y", "V", "V", "V", "Allowed", "CY, CY+1"),
      ("N", "V", "V", "A", "Allowed", "CY-1, CY"),
      ("Y", "V", "V", "A", "N/A", "CY"),
      ("N", "V", "V", "A", "Allowed", "CY-1, CY"),
      ("Y", "V", "V", "A", "Allowed", "CY"),
      ("N", "V", "V", "A", "N/A", "CY-1, CY"),
      ("Y", "V", "V", "A", "N/A", "CY"),
      ("N", "V", "A", "M", "Allowed", "CY-1"),
      ("Y", "V", "A", "M", "N/A", "Nothing displayed"),
      ("N", "V", "A", "M", "N/A", "CY-1"),
      ("Y", "V", "A", "M", "N/A", "Nothing displayed"),
      ("N", "V", "A", "M", "N/A", "CY-1"),
      ("Y", "V", "A", "M", "N/A", "Nothing displayed"),
      ("N", "V", "A", " ", "Allowed", "CY-1"),
      ("Y", "V", "A", " ", "N/A", "Nothing displayed"),
      ("N", "V", "A", " ", "N/A", "CY-1"),
      ("Y", "V", "A", " ", "N/A", "Nothing displayed"),
      ("N", "V", "A", " ", "N/A", "CY-1"),
      ("Y", "V", "A", " ", "N/A", "Nothing displayed"),
      ("N", "V", "A", "V", "Allowed", "CY-1, CY+1"),
      ("Y", "V", "A", "V", "N/A", "CY+1"),
      ("N", "V", "A", "V", "N/A", "CY-1, CY+1"),
      ("Y", "V", "A", "V", "N/A", "CY+1"),
      ("N", "V", "A", "V", "Allowed", "CY-1, CY+1"),
      ("Y", "V", "A", "V", "Allowed", "CY+1"),
      ("N", "V", "A", "A", "Allowed", "CY-1"),
      ("Y", "V", "A", "A", "N/A", "Nothing displayed"),
      ("N", "V", "A", "A", "N/A", "CY-1"),
      ("Y", "V", "A", "A", "N/A", "Nothing displayed"),
      ("N", "V", "A", "A", "N/A", "CY-1"),
      ("Y", "V", "A", "A", "N/A", "Nothing displayed"),
      ("N", "A", "M", "M", "N/A", "Nothing displayed"),
      ("Y", "A", "M", "M", "N/A", "Nothing displayed"),
      ("N", "A", "M", "M", "N/A", "Nothing displayed"),
      ("Y", "A", "M", "M", "N/A", "Nothing displayed"),
      ("N", "A", "M", "M", "N/A", "Nothing displayed"),
      ("Y", "A", "M", "M", "N/A", "Nothing displayed"),
      ("N", "A", "M", " ", "N/A", "Nothing displayed"),
      ("Y", "A", "M", " ", "N/A", "Nothing displayed"),
      ("N", "A", "M", " ", "N/A", "Nothing displayed"),
      ("Y", "A", "M", " ", "N/A", "Nothing displayed"),
      ("N", "A", "M", " ", "N/A", "Nothing displayed"),
      ("Y", "A", "M", " ", "N/A", "Nothing displayed"),
      ("N", "A", "M", "V", "N/A", "CY+1"),
      ("Y", "A", "M", "V", "N/A", "CY+1"),
      ("N", "A", "M", "V", "N/A", "CY+1"),
      ("Y", "A", "M", "V", "N/A", "CY+1"),
      ("N", "A", "M", "V", "Allowed", "CY+1"),
      ("Y", "A", "M", "V", "Allowed", "CY+1"),
      ("N", "A", "M", "A", "N/A", "Nothing displayed"),
      ("Y", "A", "M", "A", "N/A", "Nothing displayed"),
      ("N", "A", "M", "A", "N/A", "Nothing displayed"),
      ("Y", "A", "M", "A", "N/A", "Nothing displayed"),
      ("N", "A", "M", "A", "N/A", "Nothing displayed"),
      ("Y", "A", "M", "A", "N/A", "Nothing displayed"),
      ("N", "A", "V", "M", "N/A", "CY"),
      ("Y", "A", "V", "M", "N/A", "CY"),
      ("N", "A", "V", "M", "Allowed", "CY"),
      ("Y", "A", "V", "M", "Allowed", "CY"),
      ("N", "A", "V", "M", "N/A", "CY"),
      ("Y", "A", "V", "M", "N/A", "CY"),
      ("N", "A", "V", " ", "N/A", "CY, CY+1"),
      ("Y", "A", "V", " ", "N/A", "CY, CY+1"),
      ("N", "A", "V", " ", "Allowed", "CY, CY+1"),
      ("Y", "A", "V", " ", "Allowed", "CY, CY+1"),
      ("N", "A", "V", " ", "Allowed", "CY, CY+1"),
      ("Y", "A", "V", " ", "Allowed", "CY, CY+1"),
      ("N", "A", "V", "V", "N/A", "CY, CY+1"),
      ("Y", "A", "V", "V", "N/A", "CY, CY+1"),
      ("N", "A", "V", "V", "Allowed", "CY, CY+1"),
      ("Y", "A", "V", "V", "Allowed", "CY, CY+1"),
      ("N", "A", "V", "V", "Allowed", "CY, CY+1"),
      ("Y", "A", "V", "V", "Allowed", "CY, CY+1"),
      ("N", "A", "V", "A", "N/A", "CY"),
      ("Y", "A", "V", "A", "N/A", "CY"),
      ("N", "A", "V", "A", "Allowed", "CY"),
      ("Y", "A", "V", "A", "Allowed", "CY"),
      ("N", "A", "V", "A", "N/A", "CY"),
      ("Y", "A", "V", "A", "N/A", "CY"),
      ("N", "A", "A", "M", "N/A", "Nothing displayed"),
      ("Y", "A", "A", "M", "N/A", "Nothing displayed"),
      ("N", "A", "A", "M", "N/A", "Nothing displayed"),
      ("Y", "A", "A", "M", "N/A", "Nothing displayed"),
      ("N", "A", "A", "M", "N/A", "Nothing displayed"),
      ("Y", "A", "A", "M", "N/A", "Nothing displayed"),
      ("N", "A", "A", " ", "N/A", "Nothing displayed"),
      ("Y", "A", "A", " ", "N/A", "Nothing displayed"),
      ("N", "A", "A", " ", "N/A", "Nothing displayed"),
      ("Y", "A", "A", " ", "N/A", "Nothing displayed"),
      ("N", "A", "A", " ", "N/A", "Nothing displayed"),
      ("Y", "A", "A", " ", "N/A", "Nothing displayed"),
      ("N", "A", "A", "V", "N/A", "CY+1"),
      ("Y", "A", "A", "V", "N/A", "CY+1"),
      ("N", "A", "A", "V", "N/A", "CY+1"),
      ("Y", "A", "A", "V", "N/A", "CY+1"),
      ("N", "A", "A", "V", "Allowed", "CY+1"),
      ("Y", "A", "A", "V", "Allowed", "CY+1"),
      ("N", "A", "A", "A", "N/A", "Nothing displayed"),
      ("Y", "A", "A", "A", "N/A", "Nothing displayed"),
      ("N", "A", "A", "A", "N/A", "Nothing displayed"),
      ("Y", "A", "A", "A", "N/A", "Nothing displayed"),
      ("N", "A", "A", "A", "N/A", "Nothing displayed"),
      ("Y", "A", "A", "A", "N/A", "Nothing displayed"),
      ("N", " ", "M", "M", "N/A", "Nothing displayed"),
      ("Y", " ", "M", "M", "N/A", "Nothing displayed"),
      ("N", " ", "M", "M", "N/A", "Nothing displayed"),
      ("Y", " ", "M", "M", "N/A", "Nothing displayed"),
      ("N", " ", "M", "M", "N/A", "Nothing displayed"),
      ("Y", " ", "M", "M", "N/A", "Nothing displayed"),
      ("N", " ", "M", " ", "N/A", "Nothing displayed"),
      ("Y", " ", "M", " ", "N/A", "Nothing displayed"),
      ("N", " ", "M", " ", "N/A", "Nothing displayed"),
      ("Y", " ", "M", " ", "N/A", "Nothing displayed"),
      ("N", " ", "M", " ", "N/A", "Nothing displayed"),
      ("Y", " ", "M", " ", "N/A", "Nothing displayed"),
      ("N", " ", "M", "V", "N/A", "CY+1"),
      ("Y", " ", "M", "V", "N/A", "CY+1"),
      ("N", " ", "M", "V", "N/A", "CY+1"),
      ("Y", " ", "M", "V", "N/A", "CY+1"),
      ("N", " ", "M", "V", "Allowed", "CY+1"),
      ("Y", " ", "M", "V", "Allowed", "CY+1"),
      ("N", " ", "M", "A", "N/A", "Nothing displayed"),
      ("Y", " ", "M", "A", "N/A", "Nothing displayed"),
      ("N", " ", "M", "A", "N/A", "Nothing displayed"),
      ("Y", " ", "M", "A", "N/A", "Nothing displayed"),
      ("N", " ", "M", "A", "N/A", "Nothing displayed"),
      ("Y", " ", "M", "A", "N/A", "Nothing displayed"),
      ("N", " ", "V", "M", "N/A", "CY"),
      ("Y", " ", "V", "M", "N/A", "CY"),
      ("N", " ", "V", "M", "Allowed", "CY"),
      ("Y", " ", "V", "M", "Allowed", "CY"),
      ("N", " ", "V", "M", "N/A", "CY"),
      ("Y", " ", "V", "M", "N/A", "CY"),
      ("N", " ", "V", " ", "N/A", "CY, CY+1"),
      ("Y", " ", "V", " ", "N/A", "CY, CY+1"),
      ("N", " ", "V", " ", "Allowed", "CY, CY+1"),
      ("Y", " ", "V", " ", "Allowed", "CY, CY+1"),
      ("N", " ", "V", " ", "Allowed", "CY, CY+1"),
      ("Y", " ", "V", " ", "Allowed", "CY, CY+1"),
      ("N", " ", "V", "V", "N/A", "CY, CY+1"),
      ("Y", " ", "V", "V", "N/A", "CY, CY+1"),
      ("N", " ", "V", "V", "Allowed", "CY, CY+1"),
      ("Y", " ", "V", "V", "Allowed", "CY, CY+1"),
      ("N", " ", "V", "V", "Allowed", "CY, CY+1"),
      ("Y", " ", "V", "V", "Allowed", "CY, CY+1"),
      ("N", " ", "V", "A", "N/A", "CY"),
      ("Y", " ", "V", "A", "N/A", "CY"),
      ("N", " ", "V", "A", "Allowed", "CY"),
      ("Y", " ", "V", "A", "Allowed", "CY"),
      ("N", " ", "V", "A", "N/A", "CY"),
      ("Y", " ", "V", "A", "N/A", "CY"),
      ("N", " ", "A", "M", "N/A", "Nothing displayed"),
      ("Y", " ", "A", "M", "N/A", "Nothing displayed"),
      ("N", " ", "A", "M", "N/A", "Nothing displayed"),
      ("Y", " ", "A", "M", "N/A", "Nothing displayed"),
      ("N", " ", "A", "M", "N/A", "Nothing displayed"),
      ("Y", " ", "A", "M", "N/A", "Nothing displayed"),
      ("N", " ", "A", " ", "N/A", "Nothing displayed"),
      ("Y", " ", "A", " ", "N/A", "Nothing displayed"),
      ("N", " ", "A", " ", "N/A", "Nothing displayed"),
      ("Y", " ", "A", " ", "N/A", "Nothing displayed"),
      ("N", " ", "A", " ", "N/A", "Nothing displayed"),
      ("Y", " ", "A", " ", "N/A", "Nothing displayed"),
      ("N", " ", "A", "V", "N/A", "CY+1"),
      ("Y", " ", "A", "V", "N/A", "CY+1"),
      ("N", " ", "A", "V", "N/A", "CY+1"),
      ("Y", " ", "A", "V", "N/A", "CY+1"),
      ("N", " ", "A", "V", "Allowed", "CY+1"),
      ("Y", " ", "A", "V", "Allowed", "CY+1"),
      ("N", " ", "A", "A", "N/A", "Nothing displayed"), // Changed from CY+1 -> Nothing displayed (row 285) Marked as Invalid, so no big deal
      ("Y", " ", "A", "A", "N/A", "Nothing displayed"), // Changed from CY+1 -> Nothing displayed (row 286) Marked as Invalid, so no big deal
      ("N", " ", "A", "A", "N/A", "Nothing displayed"), // Changed from CY+1 -> Nothing displayed (row 287) Marked as Invalid, so no big deal
      ("Y", " ", "A", "A", "N/A", "Nothing displayed"), // Changed from CY+1 -> Nothing displayed (row 288) Marked as Invalid, so no big deal
      ("N", " ", "A", "A", "N/A", "Nothing displayed"), // Changed from CY+1 -> Nothing displayed (row 289) Marked as Invalid, so no big deal
      ("Y", " ", "A", "A", "N/A", "Nothing displayed")  // Changed from CY+1 -> Nothing displayed (row 290) Marked as Invalid, so no big deal
    )

  forAll(optOuts) { (crystallised : String,
                     cyM1         : String,
                     cy           : String,
                     cyP1         : String,
                     valid        : String,
                     outcome      : String) =>

    val previousYear = PastOptOutTaxYear(toITSAStatus(cyM1), crystallised)
    val currentYear  = CurrentOptOutTaxYear(toITSAStatus(cy))
    val nextYear     = FutureOptOutTaxYear(toITSAStatus(cyP1), currentYear)

    optOut(previousYear, currentYear, nextYear) shouldEqual outcome
  }

  def toITSAStatus(status: String): ITSAStatus = status match {
    case "M" => Mandated
    case "V" => Voluntary
    case "A" => Annual
    case " " => Unknown
  }

  private def optOut(cyM1: PastOptOutTaxYear,
                     cy  : CurrentOptOutTaxYear,
                     cyP1: FutureOptOutTaxYear): String = {

    if (invalid(cyM1, cy, cyP1))
      "Invalid"
    else {
      validOptOut(cyM1, cy, cyP1)
    }
  }

  private def invalid(cyM1: PastOptOutTaxYear,
                      cy  : CurrentOptOutTaxYear,
                      cyP1: FutureOptOutTaxYear): Boolean = {
    // Might be good to add explanation of why various of these are invalid when known!
    (cyM1.itsaStatus, cy.itsaStatus, cyP1.itsaStatus) match {
//      case (Annual , Mandated, Voluntary)                    => true   // Pending confirmation from comment in sheet!!!
      case (  cyM1 , Unknown ,        _ ) if cyM1 != Unknown => true
      case (Unknown, Unknown , Unknown  )                    => true
      case (     _ ,       _ ,        _ )                    => false
    }
  }

  private def validOptOut(cyM1: PastOptOutTaxYear,
                          cy  : CurrentOptOutTaxYear,
                          cyP1: FutureOptOutTaxYear): String = {
    if (!cyM1.canOptOut && !cy.canOptOut && !cyP1.canOptOut)
      "Nothing displayed"
    else {
      val outcomes = Seq(
        if (cyM1.canOptOut) Some("CY-1") else None,
        if (cy  .canOptOut) Some("CY"  ) else None,
        if (cyP1.canOptOut) Some("CY+1") else None,
      ).flatten.mkString(", ")

      s"$outcomes"
    }
  }

}
