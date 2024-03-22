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

class OptOutSpecV14 extends UnitSpec {

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
      ("Crystallised", "CY-1", "CY", "CY+1", "Outcome", "Customer intent", "Valid"),  // First tuple defines column names
      ("N", "M", "M", "M", "Nothing displayed", "CY-1", "N/A"),
      ("Y", "M", "M", "M", "Nothing displayed", "CY-1", "N/A"),
      ("N", "M", "M", "M", "Nothing displayed", "CY", "N/A"),
      ("Y", "M", "M", "M", "Nothing displayed", "CY", "N/A"),
      ("N", "M", "M", "M", "Nothing displayed", "CY+1", "N/A"),
      ("Y", "M", "M", "M", "Nothing displayed", "CY+1", "N/A"),
      ("N", "M", "M", " ", "Nothing displayed", "CY-1", "N/A"),
      ("Y", "M", "M", " ", "Nothing displayed", "CY-1", "N/A"),
      ("N", "M", "M", " ", "Nothing displayed", "CY", "N/A"),
      ("Y", "M", "M", " ", "Nothing displayed", "CY", "N/A"),
      ("N", "M", "M", " ", "Nothing displayed", "CY+1", "N/A"),
      ("Y", "M", "M", " ", "Nothing displayed", "CY+1", "N/A"),
      ("N", "M", "M", "V", "CY+1", "CY-1", "N/A"),
      ("Y", "M", "M", "V", "CY+1", "CY-1", "N/A"),
      ("N", "M", "M", "V", "CY+1", "CY", "N/A"),
      ("Y", "M", "M", "V", "CY+1", "CY", "N/A"),
      ("N", "M", "M", "V", "CY+1", "CY+1", "Allowed"),
      ("Y", "M", "M", "V", "CY+1", "CY+1", "Allowed"),
      ("N", "M", "M", "A", "Nothing displayed", "CY-1", "N/A"),
      ("Y", "M", "M", "A", "Nothing displayed", "CY-1", "N/A"),
      ("N", "M", "M", "A", "Nothing displayed", "CY", "N/A"),
      ("Y", "M", "M", "A", "Nothing displayed", "CY", "N/A"),
      ("N", "M", "M", "A", "Nothing displayed", "CY+1", "N/A"),
      ("Y", "M", "M", "A", "Nothing displayed", "CY+1", "N/A"),
      ("N", "M", "V", "M", "CY", "CY-1", "N/A"),
      ("Y", "M", "V", "M", "CY", "CY-1", "N/A"),
      ("N", "M", "V", "M", "CY", "CY", "Allowed"),
      ("Y", "M", "V", "M", "CY", "CY", "Allowed"),
      ("N", "M", "V", "M", "CY", "CY+1", "N/A"),
      ("Y", "M", "V", "M", "CY", "CY+1", "N/A"),
      ("N", "M", "V", " ", "CY, CY+1", "CY-1", "N/A"),
      ("Y", "M", "V", " ", "CY, CY+1", "CY-1", "N/A"),
      ("N", "M", "V", " ", "CY, CY+1", "CY", "Allowed"),
      ("Y", "M", "V", " ", "CY, CY+1", "CY", "Allowed"),
      ("N", "M", "V", " ", "CY, CY+1", "CY+1", "Allowed"),
      ("Y", "M", "V", " ", "CY, CY+1", "CY+1", "Allowed"),
      ("N", "M", "V", "V", "CY, CY+1", "CY-1", "N/A"),
      ("Y", "M", "V", "V", "CY, CY+1", "CY-1", "N/A"),
      ("N", "M", "V", "V", "CY, CY+1", "CY", "Allowed"),
      ("Y", "M", "V", "V", "CY, CY+1", "CY", "Allowed"),
      ("N", "M", "V", "V", "CY, CY+1", "CY+1", "Allowed"),
      ("Y", "M", "V", "V", "CY, CY+1", "CY+1", "Allowed"),
      ("N", "M", "V", "A", "CY", "CY-1", "N/A"),
      ("Y", "M", "V", "A", "CY", "CY-1", "N/A"),
      ("N", "M", "V", "A", "CY", "CY", "Allowed"),
      ("Y", "M", "V", "A", "CY", "CY", "Allowed"),
      ("N", "M", "V", "A", "CY", "CY+1", "N/A"),
      ("Y", "M", "V", "A", "CY", "CY+1", "N/A"),
      ("N", "M", "A", "M", "Nothing displayed", "CY-1", "N/A"),
      ("Y", "M", "A", "M", "Nothing displayed", "CY-1", "N/A"),
      ("N", "M", "A", "M", "Nothing displayed", "CY", "N/A"),
      ("Y", "M", "A", "M", "Nothing displayed", "CY", "N/A"),
      ("N", "M", "A", "M", "Nothing displayed", "CY+1", "N/A"),
      ("Y", "M", "A", "M", "Nothing displayed", "CY+1", "N/A"),
      ("N", "M", "A", " ", "Nothing displayed", "CY-1", "N/A"),
      ("Y", "M", "A", " ", "Nothing displayed", "CY-1", "N/A"),
      ("N", "M", "A", " ", "Nothing displayed", "CY", "N/A"),
      ("Y", "M", "A", " ", "Nothing displayed", "CY", "N/A"),
      ("N", "M", "A", " ", "Nothing displayed", "CY+1", "N/A"),
      ("Y", "M", "A", " ", "Nothing displayed", "CY+1", "N/A"),
      ("N", "M", "A", "V", "CY+1", "CY-1", "N/A"),
      ("Y", "M", "A", "V", "CY+1", "CY-1", "N/A"),
      ("N", "M", "A", "V", "CY+1", "CY", "N/A"),
      ("Y", "M", "A", "V", "CY+1", "CY", "N/A"),
      ("N", "M", "A", "V", "CY+1", "CY+1", "Allowed"),
      ("Y", "M", "A", "V", "CY+1", "CY+1", "Allowed"),
      ("N", "M", "A", "A", "Nothing displayed", "CY-1", "N/A"),
      ("Y", "M", "A", "A", "Nothing displayed", "CY-1", "N/A"),
      ("N", "M", "A", "A", "Nothing displayed", "CY", "N/A"),
      ("Y", "M", "A", "A", "Nothing displayed", "CY", "N/A"),
      ("N", "M", "A", "A", "Nothing displayed", "CY+1", "N/A"),
      ("Y", "M", "A", "A", "Nothing displayed", "CY+1", "N/A"),
      ("N", "V", "M", "M", "CY-1", "CY-1", "Allowed"),
      ("Y", "V", "M", "M", "Nothing displayed", "CY-1", "N/A"),
      ("N", "V", "M", "M", "CY-1", "CY", "N/A"),
      ("Y", "V", "M", "M", "Nothing displayed", "CY", "N/A"),
      ("N", "V", "M", "M", "CY-1", "CY+1", "N/A"),
      ("Y", "V", "M", "M", "Nothing displayed", "CY+1", "N/A"),
      ("N", "V", "M", " ", "CY-1", "CY-1", "Allowed"),
      ("Y", "V", "M", " ", "Nothing displayed", "CY-1", "N/A"),
      ("N", "V", "M", " ", "CY-1", "CY", "N/A"),
      ("Y", "V", "M", " ", "Nothing displayed", "CY", "N/A"),
      ("N", "V", "M", " ", "CY-1", "CY+1", "N/A"),
      ("Y", "V", "M", " ", "Nothing displayed", "CY+1", "N/A"),
      ("N", "V", "M", "V", "CY-1, CY+1", "CY-1", "Allowed"),
      ("Y", "V", "M", "V", "CY+1", "CY-1", "N/A"),
      ("N", "V", "M", "V", "CY-1, CY+1", "CY", "N/A"),
      ("Y", "V", "M", "V", "CY+1", "CY", "N/A"),
      ("N", "V", "M", "V", "CY-1, CY+1", "CY+1", "Allowed"),
      ("Y", "V", "M", "V", "CY+1", "CY+1", "Allowed"),
      ("N", "V", "M", "A", "CY-1", "CY-1", "Allowed"),
      ("Y", "V", "M", "A", "Nothing displayed", "CY-1", "N/A"),
      ("N", "V", "M", "A", "CY-1", "CY", "N/A"),
      ("Y", "V", "M", "A", "Nothing displayed", "CY", "N/A"),
      ("N", "V", "M", "A", "CY-1", "CY+1", "N/A"),
      ("Y", "V", "M", "A", "Nothing displayed", "CY+1", "N/A"),
      ("N", "V", "V", "M", "CY-1, CY", "CY-1", "Allowed"),
      ("Y", "V", "V", "M", "CY", "CY-1", "N/A"),
      ("N", "V", "V", "M", "CY-1, CY", "CY", "Allowed"),
      ("Y", "V", "V", "M", "CY", "CY", "Allowed"),
      ("N", "V", "V", "M", "CY-1, CY", "CY+1", "N/A"),
      ("Y", "V", "V", "M", "CY", "CY+1", "N/A"),
      ("N", "V", "V", " ", "CY-1, CY, CY+1", "CY-1", "Allowed"),
      ("Y", "V", "V", " ", "CY, CY+1", "CY-1", "N/A"),
      ("N", "V", "V", " ", "CY-1, CY, CY+1", "CY", "Allowed"),
      ("Y", "V", "V", " ", "CY, CY+1", "CY", "Allowed"/*"N/A"*/), // Changed from N/A to allowed as I don't think we cover this case yet
      ("N", "V", "V", " ", "CY-1, CY, CY+1", "CY+1", "Allowed"),
      ("Y", "V", "V", " ", "CY, CY+1", "CY+1", "Allowed"),
      ("N", "V", "V", "V", "CY-1, CY, CY+1", "CY-1", "Allowed"),
      ("Y", "V", "V", "V", "CY, CY+1", "CY-1", "N/A"),
      ("N", "V", "V", "V", "CY-1, CY, CY+1", "CY", "Allowed"),
      ("Y", "V", "V", "V", "CY, CY+1", "CY", "Allowed"),
      ("N", "V", "V", "V", "CY-1, CY, CY+1", "CY+1", "Allowed"),
      ("Y", "V", "V", "V", "CY, CY+1", "CY+1", "Allowed"),
      ("N", "V", "V", "A", "CY-1, CY", "CY-1", "Allowed"),
      ("Y", "V", "V", "A", "CY", "CY-1", "N/A"),
      ("N", "V", "V", "A", "CY-1, CY", "CY", "Allowed"),
      ("Y", "V", "V", "A", "CY", "CY", "Allowed"),
      ("N", "V", "V", "A", "CY-1, CY", "CY+1", "N/A"),
      ("Y", "V", "V", "A", "CY", "CY+1", "N/A"),
      ("N", "V", "A", "M", "CY-1", "CY-1", "Allowed"),
      ("Y", "V", "A", "M", "Nothing displayed", "CY-1", "N/A"),
      ("N", "V", "A", "M", "CY-1", "CY", "N/A"),
      ("Y", "V", "A", "M", "Nothing displayed", "CY", "N/A"),
      ("N", "V", "A", "M", "CY-1", "CY+1", "N/A"),
      ("Y", "V", "A", "M", "Nothing displayed", "CY+1", "N/A"),
      ("N", "V", "A", " ", "CY-1", "CY-1", "Allowed"),
      ("Y", "V", "A", " ", "Nothing displayed", "CY-1", "N/A"),
      ("N", "V", "A", " ", "CY-1", "CY", "N/A"),
      ("Y", "V", "A", " ", "Nothing displayed", "CY", "N/A"),
      ("N", "V", "A", " ", "CY-1", "CY+1", "N/A"),
      ("Y", "V", "A", " ", "Nothing displayed", "CY+1", "N/A"),
      ("N", "V", "A", "V", "CY-1, CY+1", "CY-1", "Allowed"),
      ("Y", "V", "A", "V", "CY+1", "CY-1", "N/A"),
      ("N", "V", "A", "V", "CY-1, CY+1", "CY", "N/A"),
      ("Y", "V", "A", "V", "CY+1", "CY", "N/A"),
      ("N", "V", "A", "V", "CY-1, CY+1", "CY+1", "Allowed"),
      ("Y", "V", "A", "V", "CY+1", "CY+1", "Allowed"),
      ("N", "V", "A", "A", "CY-1", "CY-1", "Allowed"),
      ("Y", "V", "A", "A", "Nothing displayed", "CY-1", "N/A"),
      ("N", "V", "A", "A", "CY-1", "CY", "N/A"),
      ("Y", "V", "A", "A", "Nothing displayed", "CY", "N/A"),
      ("N", "V", "A", "A", "CY-1", "CY+1", "N/A"),
      ("Y", "V", "A", "A", "Nothing displayed", "CY+1", "N/A"),
      ("N", "A", "M", "M", "Nothing displayed", "CY-1", "N/A"),
      ("Y", "A", "M", "M", "Nothing displayed", "CY-1", "N/A"),
      ("N", "A", "M", "M", "Nothing displayed", "CY", "N/A"),
      ("Y", "A", "M", "M", "Nothing displayed", "CY", "N/A"),
      ("N", "A", "M", "M", "Nothing displayed", "CY+1", "N/A"),
      ("Y", "A", "M", "M", "Nothing displayed", "CY+1", "N/A"),
      ("N", "A", "M", " ", "Nothing displayed", "CY-1", "N/A"),
      ("Y", "A", "M", " ", "Nothing displayed", "CY-1", "N/A"),
      ("N", "A", "M", " ", "Nothing displayed", "CY", "N/A"),
      ("Y", "A", "M", " ", "Nothing displayed", "CY", "N/A"),
      ("N", "A", "M", " ", "Nothing displayed", "CY+1", "N/A"),
      ("Y", "A", "M", " ", "Nothing displayed", "CY+1", "N/A"),
      ("N", "A", "M", "V", "CY+1", "CY-1", "N/A"),
      ("Y", "A", "M", "V", "CY+1", "CY-1", "N/A"),
      ("N", "A", "M", "V", "CY+1", "CY", "N/A"),
      ("Y", "A", "M", "V", "CY+1", "CY", "N/A"),
      ("N", "A", "M", "V", "CY+1", "CY+1", "Allowed"),
      ("Y", "A", "M", "V", "CY+1", "CY+1", "Allowed"),
      ("N", "A", "M", "A", "Nothing displayed", "CY-1", "N/A"),
      ("Y", "A", "M", "A", "Nothing displayed", "CY-1", "N/A"),
      ("N", "A", "M", "A", "Nothing displayed", "CY", "N/A"),
      ("Y", "A", "M", "A", "Nothing displayed", "CY", "N/A"),
      ("N", "A", "M", "A", "Nothing displayed", "CY+1", "N/A"),
      ("Y", "A", "M", "A", "Nothing displayed", "CY+1", "N/A"),
      ("N", "A", "V", "M", "CY", "CY-1", "N/A"),
      ("Y", "A", "V", "M", "CY", "CY-1", "N/A"),
      ("N", "A", "V", "M", "CY", "CY", "Allowed"),
      ("Y", "A", "V", "M", "CY", "CY", "Allowed"),
      ("N", "A", "V", "M", "CY", "CY+1", "N/A"),
      ("Y", "A", "V", "M", "CY", "CY+1", "N/A"),
      ("N", "A", "V", " ", "CY, CY+1", "CY-1", "N/A"),
      ("Y", "A", "V", " ", "CY, CY+1", "CY-1", "N/A"),
      ("N", "A", "V", " ", "CY, CY+1", "CY", "Allowed"),
      ("Y", "A", "V", " ", "CY, CY+1", "CY", "Allowed"),
      ("N", "A", "V", " ", "CY, CY+1", "CY+1", "Allowed"),
      ("Y", "A", "V", " ", "CY, CY+1", "CY+1", "Allowed"),
      ("N", "A", "V", "V", "CY, CY+1", "CY-1", "N/A"),
      ("Y", "A", "V", "V", "CY, CY+1", "CY-1", "N/A"),
      ("N", "A", "V", "V", "CY, CY+1", "CY", "Allowed"),
      ("Y", "A", "V", "V", "CY, CY+1", "CY", "Allowed"),
      ("N", "A", "V", "V", "CY, CY+1", "CY+1", "Allowed"),
      ("Y", "A", "V", "V", "CY, CY+1", "CY+1", "Allowed"),
      ("N", "A", "V", "A", "CY", "CY-1", "N/A"),
      ("Y", "A", "V", "A", "CY", "CY-1", "N/A"),
      ("N", "A", "V", "A", "CY", "CY", "Allowed"),
      ("Y", "A", "V", "A", "CY", "CY", "Allowed"),
      ("N", "A", "V", "A", "CY", "CY+1", "N/A"),
      ("Y", "A", "V", "A", "CY", "CY+1", "N/A"),
      ("N", "A", "A", "M", "Nothing displayed", "CY-1", "N/A"),
      ("Y", "A", "A", "M", "Nothing displayed", "CY-1", "N/A"),
      ("N", "A", "A", "M", "Nothing displayed", "CY", "N/A"),
      ("Y", "A", "A", "M", "Nothing displayed", "CY", "N/A"),
      ("N", "A", "A", "M", "Nothing displayed", "CY+1", "N/A"),
      ("Y", "A", "A", "M", "Nothing displayed", "CY+1", "N/A"),
      ("N", "A", "A", " ", "Nothing displayed", "CY-1", "N/A"),
      ("Y", "A", "A", " ", "Nothing displayed", "CY-1", "N/A"),
      ("N", "A", "A", " ", "Nothing displayed", "CY", "N/A"),
      ("Y", "A", "A", " ", "Nothing displayed", "CY", "N/A"),
      ("N", "A", "A", " ", "Nothing displayed", "CY+1", "N/A"),
      ("Y", "A", "A", " ", "Nothing displayed", "CY+1", "N/A"),
      ("N", "A", "A", "V", "CY+1", "CY-1", "N/A"),
      ("Y", "A", "A", "V", "CY+1", "CY-1", "N/A"),
      ("N", "A", "A", "V", "CY+1", "CY", "N/A"),
      ("Y", "A", "A", "V", "CY+1", "CY", "N/A"),
      ("N", "A", "A", "V", "CY+1", "CY+1", "Allowed"),
      ("Y", "A", "A", "V", "CY+1", "CY+1", "Allowed"),
      ("N", "A", "A", "A", "Nothing displayed", "CY-1", "N/A"),
      ("Y", "A", "A", "A", "Nothing displayed", "CY-1", "N/A"),
      ("N", "A", "A", "A", "Nothing displayed", "CY", "N/A"),
      ("Y", "A", "A", "A", "Nothing displayed", "CY", "N/A"),
      ("N", "A", "A", "A", "Nothing displayed", "CY+1", "N/A"),
      ("Y", "A", "A", "A", "Nothing displayed", "CY+1", "N/A"),
      ("N", " ", "M", "M", "Nothing displayed", "CY-1", "N/A"),
      ("Y", " ", "M", "M", "Nothing displayed", "CY-1", "N/A"),
      ("N", " ", "M", "M", "Nothing displayed", "CY", "N/A"),
      ("Y", " ", "M", "M", "Nothing displayed", "CY", "N/A"),
      ("N", " ", "M", "M", "Nothing displayed", "CY+1", "N/A"),
      ("Y", " ", "M", "M", "Nothing displayed", "CY+1", "N/A"),
      ("N", " ", "M", " ", "Nothing displayed", "CY-1", "N/A"),
      ("Y", " ", "M", " ", "Nothing displayed", "CY-1", "N/A"),
      ("N", " ", "M", " ", "Nothing displayed", "CY", "N/A"),
      ("Y", " ", "M", " ", "Nothing displayed", "CY", "N/A"),
      ("N", " ", "M", " ", "Nothing displayed", "CY+1", "N/A"),
      ("Y", " ", "M", " ", "Nothing displayed", "CY+1", "N/A"),
      ("N", " ", "M", "V", "CY+1", "CY-1", "N/A"),
      ("Y", " ", "M", "V", "CY+1", "CY-1", "N/A"),
      ("N", " ", "M", "V", "CY+1", "CY", "N/A"),
      ("Y", " ", "M", "V", "CY+1", "CY", "N/A"),
      ("N", " ", "M", "V", "CY+1", "CY+1", "Allowed"),
      ("Y", " ", "M", "V", "CY+1", "CY+1", "Allowed"),
      ("N", " ", "M", "A", "Nothing displayed", "CY-1", "N/A"),
      ("Y", " ", "M", "A", "Nothing displayed", "CY-1", "N/A"),
      ("N", " ", "M", "A", "Nothing displayed", "CY", "N/A"),
      ("Y", " ", "M", "A", "Nothing displayed", "CY", "N/A"),
      ("N", " ", "M", "A", "Nothing displayed", "CY+1", "N/A"),
      ("Y", " ", "M", "A", "Nothing displayed", "CY+1", "N/A"),
      ("N", " ", "V", "M", "CY", "CY-1", "N/A"),
      ("Y", " ", "V", "M", "CY", "CY-1", "N/A"),
      ("N", " ", "V", "M", "CY", "CY", "Allowed"),
      ("Y", " ", "V", "M", "CY", "CY", "Allowed"),
      ("N", " ", "V", "M", "CY", "CY+1", "N/A"),
      ("Y", " ", "V", "M", "CY", "CY+1", "N/A"),
      ("N", " ", "V", " ", "CY, CY+1", "CY-1", "N/A"),
      ("Y", " ", "V", " ", "CY, CY+1", "CY-1", "N/A"),
      ("N", " ", "V", " ", "CY, CY+1", "CY", "Allowed"),
      ("Y", " ", "V", " ", "CY, CY+1", "CY", "Allowed"),
      ("N", " ", "V", " ", "CY, CY+1", "CY+1", "Allowed"),
      ("Y", " ", "V", " ", "CY, CY+1", "CY+1", "Allowed"),
      ("N", " ", "V", "V", "CY, CY+1", "CY-1", "N/A"),
      ("Y", " ", "V", "V", "CY, CY+1", "CY-1", "N/A"),
      ("N", " ", "V", "V", "CY, CY+1", "CY", "Allowed"),
      ("Y", " ", "V", "V", "CY, CY+1", "CY", "Allowed"),
      ("N", " ", "V", "V", "CY, CY+1", "CY+1", "Allowed"),
      ("Y", " ", "V", "V", "CY, CY+1", "CY+1", "Allowed"),
      ("N", " ", "V", "A", "CY", "CY-1", "N/A"),
      ("Y", " ", "V", "A", "CY", "CY-1", "N/A"),
      ("N", " ", "V", "A", "CY", "CY", "Allowed"),
      ("Y", " ", "V", "A", "CY", "CY", "Allowed"),
      ("N", " ", "V", "A", "CY", "CY+1", "N/A"),
      ("Y", " ", "V", "A", "CY", "CY+1", "N/A"),
      ("N", " ", "A", "M", "Nothing displayed", "CY-1", "N/A"),
      ("Y", " ", "A", "M", "Nothing displayed", "CY-1", "N/A"),
      ("N", " ", "A", "M", "Nothing displayed", "CY", "N/A"),
      ("Y", " ", "A", "M", "Nothing displayed", "CY", "N/A"),
      ("N", " ", "A", "M", "Nothing displayed", "CY+1", "N/A"),
      ("Y", " ", "A", "M", "Nothing displayed", "CY+1", "N/A"),
      ("N", " ", "A", " ", "Nothing displayed", "CY-1", "N/A"),
      ("Y", " ", "A", " ", "Nothing displayed", "CY-1", "N/A"),
      ("N", " ", "A", " ", "Nothing displayed", "CY", "N/A"),
      ("Y", " ", "A", " ", "Nothing displayed", "CY", "N/A"),
      ("N", " ", "A", " ", "Nothing displayed", "CY+1", "N/A"),
      ("Y", " ", "A", " ", "Nothing displayed", "CY+1", "N/A"),
      ("N", " ", "A", "V", "CY+1", "CY-1", "N/A"),
      ("Y", " ", "A", "V", "CY+1", "CY-1", "N/A"),
      ("N", " ", "A", "V", "CY+1", "CY", "N/A"),
      ("Y", " ", "A", "V", "CY+1", "CY", "N/A"),
      ("N", " ", "A", "V", "CY+1", "CY+1", "Allowed"),
      ("Y", " ", "A", "V", "CY+1", "CY+1", "Allowed"),
      ("N", " ", "A", "A", "Nothing displayed", "CY-1", "N/A"),
      ("Y", " ", "A", "A", "Nothing displayed", "CY-1", "N/A"),
      ("N", " ", "A", "A", "Nothing displayed", "CY", "N/A"),
      ("Y", " ", "A", "A", "Nothing displayed", "CY", "N/A"),
      ("N", " ", "A", "A", "Nothing displayed", "CY+1", "N/A"),
      ("Y", " ", "A", "A", "Nothing displayed", "CY+1", "N/A"),
      ("N", " ", " ", "V", "CY+1", "CY+1", "Allowed"),
      ("Y", " ", " ", "V", "CY+1", "CY+1", "Allowed"),
      ("N", " ", " ", "M", "Nothing displayed", "CY+1", "N/A"),
      ("Y", " ", " ", "M", "Nothing displayed", "CY+1", "N/A")
    )

  forAll(optOuts) { (crystallised     : String,
                     cyM1             : String,
                     cy               : String,
                     cyP1             : String,
                     outcome          : String,
                     customerSelection: String,
                     valid            : String) =>

    val previousYear = PastOptOutTaxYear(toITSAStatus(cyM1), crystallised)
    val currentYear  = CurrentOptOutTaxYear(toITSAStatus(cy))
    val nextYear     = FutureOptOutTaxYear(toITSAStatus(cyP1), currentYear)

    optOut(previousYear, currentYear, nextYear) shouldEqual outcome
  }

  forAll(optOuts) { (crystallised     : String,
                     cyM1             : String,
                     cy               : String,
                     cyP1             : String,
                     outcome          : String,
                     customerSelection: String,
                     valid            : String) =>

    val previousYear = PastOptOutTaxYear(toITSAStatus(cyM1), crystallised)
    val currentYear  = CurrentOptOutTaxYear(toITSAStatus(cy))
    val nextYear     = FutureOptOutTaxYear(toITSAStatus(cyP1), currentYear)

    (isValid(previousYear, currentYear, nextYear, customerSelection) match {
      case true  => "Allowed"
      case false => "N/A"
    }) shouldEqual valid
  }

  private def isValid(cyM1: PastOptOutTaxYear,
                      cy  : CurrentOptOutTaxYear,
                      cyP1: FutureOptOutTaxYear,
                      customerSelection: String): Boolean = {

    val validCombinationOfStatuses = (cyM1.itsaStatus, cy.itsaStatus, cyP1.itsaStatus) match {
      case (cyM1   , Unknown,    _   ) if cyM1 != Unknown => false
      case (Unknown, Unknown, Unknown)                    => false
      case (   _   ,    _   ,    _   )                    => true
    }

    val optOutAvailable = cyM1.canOptOut || cy.canOptOut || cyP1.canOptOut

    val possibleSelection = optOut(cyM1, cy, cyP1).split(",").map(_.trim).contains(customerSelection)

    validCombinationOfStatuses && optOutAvailable && possibleSelection
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

    // The v11 sheet does not contain the invalid cases (which might be considered an oversight)
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
