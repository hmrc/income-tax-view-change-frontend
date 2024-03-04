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

package utils

import org.scalatest.prop.TableDrivenPropertyChecks._
import testUtils.UnitSpec

class OptOutSpec extends UnitSpec {

  case class OptOutParameters(crystallised : String,
                              cyM1         : String,
                              cy           : String,
                              cyP1         : String)

  private val optOuts =
    Table(
      ("Crystallised", "CY-1", "CY", "CY+1", "Outcome"),  // First tuple defines column names
      ("Y", "M", "M", "M", "No Opt out"),
      ("Y", "M", "V", "M", "Can Opt out for: CY"),
      ("Y", "M", "M", "A", "No Opt out"),
      (" ", "M", "M", " ", "No Opt out"),
      ("Y", "M", "V", "A", "Can Opt out for: CY"),
      (" ", "V", "M", " ", "Can Opt out for: CY-1"),
      (" ", "V", "A", " ", "Can Opt out for: CY-1"),
      (" ", "M", "V", " ", "Can Opt out for: CY, CY+1"),
      ("Y", "M", "A", "M", "No Opt out"),
      ("Y", "M", "A", "V", "Can Opt out for: CY+1"),
      ("Y", "M", "A", "A", "No Opt out"),
      (" ", "M", "A", " ", "No Opt out"),
      (" ", "M", " ", "M", "Invalid"),
      (" ", "M", " ", "V", "Invalid"),
      (" ", "M", " ", "A", "Invalid"),
      (" ", "M", " ", " ", "Invalid"),
      ("Y", "V", "M", "M", "No Opt out"),
      ("Y", "V", "M", "V", "Can Opt out for: CY+1"),
      ("Y", "V", "M", "A", "No Opt out"),
      (" ", "V", "V", " ", "Can Opt out for: CY-1, CY, CY+1"),
      ("Y", "V", "V", "M", "Can Opt out for: CY"),
      ("Y", "V", "V", "V", "Can Opt out for: CY, CY+1"),
      ("Y", "V", "V", "A", "Can Opt out for: CY"),
      ("Y", "M", "V", "V", "Can Opt out for: CY, CY+1"),
      ("Y", "V", "A", "M", "No Opt out"),
      ("Y", "V", "A", "V", "Can Opt out for: CY+1"),
      ("Y", "V", "A", "A", "No Opt out"),
      ("Y", "M", "M", "V", "Can Opt out for: CY+1"),
      (" ", "V", " ", "M", "Invalid"),
      (" ", "V", " ", "V", "Invalid"),
      (" ", "V", " ", "A", "Invalid"),
      (" ", "V", " ", " ", "Invalid"),
      ("Y", "A", "M", "M", "No Opt out"),
      ("Y", "A", "M", "V", "Invalid"),
      ("Y", "A", "M", "A", "No Opt out"),
      (" ", "A", "M", " ", "No Opt out"),
      ("Y", "A", "V", "M", "Can Opt out for: CY"),
      ("Y", "A", "V", "V", "Can Opt out for: CY, CY+1"),
      ("Y", "A", "V", "A", "Can Opt out for: CY"),
      (" ", "A", "V", " ", "Can Opt out for: CY, CY+1"),
      ("Y", "A", "A", "M", "No Opt out"),
      ("Y", "A", "A", "V", "Can Opt out for: CY+1"),
      ("Y", "A", "A", "A", "No Opt out"),
      (" ", "A", "A", " ", "No Opt out"),
      (" ", "A", " ", "M", "Invalid"),
      (" ", "A", " ", "V", "Invalid"),
      (" ", "A", " ", "A", "Invalid"),
      (" ", "A", " ", " ", "Invalid"),
      (" ", " ", "M", "M", "No Opt out"),
      (" ", " ", "M", "V", "Can Opt out for: CY+1"),
      (" ", " ", "M", "A", "No Opt out"),
      (" ", " ", "M", " ", "No Opt out"),
      (" ", " ", "V", "M", "Can Opt out for: CY"),
      (" ", " ", "V", "V", "Can Opt out for: CY, CY+1"),
      (" ", " ", "V", "A", "Can Opt out for: CY"),
      (" ", " ", "V", " ", "Can Opt out for: CY, CY+1"),
      (" ", " ", "A", "M", "No Opt out"),
      (" ", " ", "A", "V", "Can Opt out for: CY+1"),
      (" ", " ", "A", "A", "No Opt out"),
      (" ", " ", "A", " ", "No Opt out"),
      (" ", " ", " ", "M", "No Opt out"),
      (" ", " ", " ", "V", "Can Opt out for: CY+1"),
      (" ", " ", " ", "A", "No Opt out"),
      (" ", " ", " ", " ", "Invalid"),
      // Carl's new blue cases for un-crystallised CY-1
      (" ", "M", "V", "A", "Can Opt out for: CY"),
      (" ", "V", "V", "A", "Can Opt out for: CY-1, CY"),
      (" ", "A", "V", "A", "Can Opt out for: CY"),
      (" ", " ", "V", "A", "Can Opt out for: CY")
    )

  forAll(optOuts) { (crystallised : String,
                     cyM1         : String,
                     cy           : String,
                     cyP1         : String,
                     outcome      : String) =>
    optOut(OptOutParameters(crystallised, cyM1, cy, cyP1)) shouldEqual outcome
  }

  private def optOut(optOutParams : OptOutParameters): String = {
    if (invalid(optOutParams))
      "Invalid"
    else
      validOptOut(optOutParams)
  }

  private def invalid(oop: OptOutParameters): Boolean = {
    // Might be good to add explanation of why various of these are invalid when known!
    (oop.cyM1, oop.cy, oop.cyP1) match {
      case (  "A", "M", "V")                => true   // Pending confirmation from comment in sheet!!!
      case (cyM1 , " ", _  ) if cyM1 != " " => true
      case (  " ", " ", " ")                => true
      case (   _ ,  _ ,  _ )                => false
    }
  }

  private def validOptOut(oop: OptOutParameters): String = {
    val newCyM1 = voluntaryCannotBeOptedOutOfIfCrystallised(oop)
    val newCyP1 = unknownFollowingVoluntaryCanBeOptedOutOf(oop)

    if (newCyM1 != "V" && oop.cy != "V" && newCyP1 != "V")
      "No Opt out"
    else {
      val outcomes = Seq(
        if (newCyM1 == "V") Some("CY-1") else None,
        if ( oop.cy == "V") Some("CY"  ) else None,
        if (newCyP1 == "V") Some("CY+1") else None,
      ).flatten.mkString(", ")

      s"Can Opt out for: $outcomes"
    }
  }

  private def voluntaryCannotBeOptedOutOfIfCrystallised(oop: OptOutParameters): String =
    if (oop.cyM1 == "V" && oop.crystallised == "Y") "VC" else oop.cyM1

  private def unknownFollowingVoluntaryCanBeOptedOutOf(oop: OptOutParameters): String =
    if (oop.cy == "V" && oop.cyP1 == " ") "V" else oop.cyP1

}
