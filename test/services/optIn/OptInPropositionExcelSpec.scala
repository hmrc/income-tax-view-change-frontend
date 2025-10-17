/*
 * Copyright 2024 HM Revenue & Customs
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

package services.optIn

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import org.scalatest.prop.TableDrivenPropertyChecks._
import services.optIn.core.OptInProposition
import services.optIn.core.OptInProposition.createOptInProposition
import testUtils.UnitSpec

import scala.io.Source

class OptInPropositionExcelSpec extends UnitSpec {

  "Parse opt out scenarios from tsv file" ignore {
    /*  The programme has specified all required Opt Out scenarios in a large spreadsheet.
        This code can ingest the data and convert to parameters for table based test found below.
        This generator is currently ignored but can be reactivated to regenerate the test data if needed.
          - To reactivate generator, replace "ignore" with "in"

      Select first 8 columns of Opt-In Scenarios Spreadsheet (currently v5).
        - to avoid cells that contain carriage returns as to make parsing easier.
      Paste into a fresh spreadsheet and save as test/resources/OptInScenarios.tsv in project root directory.
        - csv has issues with commas within cells, therefore use tsv to allow simple parsing.
      Run the test to generate the formatted test data in the console, based on the required scenarios.
        - Copy the scenarios output from the console and paste them into the data table "scenarios" to update the
        following test in this file.
   */

    def parseItsaStatus(input: String): String =
      input.trim match {
        case "A" => "A"
        case "V" => "V"
        case "M" => "M"
        case "" => " "
        case _ => throw new RuntimeException(s"Unexpected ITSA status data $input")
      }

    def parseIsValid(valid: String): Boolean =
      valid match {
        case "Allowed" => true
        case "N/A" => false
        case "Tbc" => true /* todo: check 'Tbc' line in excel sheet */
        case _ => throw new RuntimeException("Unexpected entry in Valid column")
      }

    def parseCustomerIntent(intent: String): String = {
      intent.replaceAll("Customer wants( to)? opt.* in from ", "")
    }

    def presentedInViewAndChange(input: String): Seq[String] =
      input.trim match {
        case "N/A" => Seq()
        case "CY" => Seq(quote("CY"))
        case "CY+1" => Seq(quote("CY+1"))
        case "CY and CY+1" => Seq(quote("CY"), quote("CY+1"))
        case "CY+1 and CY" => Seq(quote("CY"), quote("CY+1"))
        case "" => Seq()
        case _ => throw new RuntimeException(s"Unexpected presented in View and Change value: $input")
      }

    def sentFromViewAndChange(input: String): String =
      input.trim match {
        case "N/A" => quote(" ")
        case "CY" => quote("CY")
        case "CY+1" => quote("CY+1")
        case "" => quote(" ")
        case _ => throw new RuntimeException(s"Unexpected sent from View and Change value: $input")
      }

    def quote(input: String): String = {
      "\"" + input + "\""
    }

    println("""("Current Year", "Next Year", "Intent", "Valid", "Presented", "Sent", "Expected CY", "Expected NY"),""")

    val tsvOptIn = Source.fromFile("test/resources/OptInScenarios.tsv")
    val rowsWithoutHeaders = tsvOptIn.getLines().drop(2)

    val tab = "\t"

    rowsWithoutHeaders.foreach(line => {
      val cells = line.split(tab).take(8)

      val lineString = Seq(
        quote(parseItsaStatus(cells(0))),
        quote(parseItsaStatus(cells(1))),
        quote(parseCustomerIntent(cells(2))),
        parseIsValid(cells(3)).toString,
        presentedInViewAndChange(cells(4)).toString(),
        sentFromViewAndChange(cells(5)),
        quote(parseItsaStatus(cells(6))),
        quote(parseItsaStatus(cells(7))),
      ).mkString(", ")

      val s = s"($lineString),"

      println(s)
    })

    tsvOptIn.close()
  }

  val forYearEnd = 2021
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(forYearEnd)

  private val scenarios = Table(
    ("Current Year", "Next Year", "Intent", "Valid", "Presented", "Sent", "Expected CY", "Expected NY"),
    ("M", "M", "CY", false, List(), " ", "M", "M"),
    ("M", "M", "CY+1", false, List(), " ", "M", "M"),
    ("M", " ", "CY", false, List(), " ", "M", " "),
    ("M", " ", "CY+1", false, List(), " ", "M", " "),
    ("M", "V", "CY", false, List(), " ", "M", "V"),
    ("M", "V", "CY+1", false, List(), " ", "M", "V"),
    ("M", "A", "CY", false, List("CY+1"), " ", "M", "A"),
    ("M", "A", "CY+1", true, List("CY+1"), "CY+1", "M", "V"),
    ("V", "M", "CY", false, List(), " ", "V", "M"),
    ("V", "M", "CY+1", false, List(), " ", "V", "M"),
    ("V", " ", "CY", false, List(), " ", "V", " "),
    ("V", " ", "CY+1", false, List(), " ", "V", " "),
    ("V", "V", "CY", false, List(), " ", "V", "V"),
    ("V", "V", "CY+1", false, List(), " ", "V", "V"),
    ("V", "A", "CY", false, List("CY+1"), " ", "V", "A"),
    ("V", "A", "CY+1", true, List("CY+1"), "CY+1", "V", "V"),
    ("A", "M", "CY", true, List("CY"), "CY", "V", "M"),
    ("A", "M", "CY+1", false, List("CY"), " ", "A", "M"),
    ("A", " ", "CY", true, List("CY", "CY+1"), "CY", "V", " "),
    ("A", " ", "CY+1", true, List("CY", "CY+1"), "CY+1", "A", "V"),
    ("A", "V", "CY", true, List("CY"), "CY", "V", "V"),
    ("A", "V", "CY+1", false, List("CY"), " ", "A", "V"),
    ("A", "A", "CY", true, List("CY", "CY+1"), "CY", "V", "V"),
    ("A", "A", "CY+1", true, List("CY", "CY+1"), "CY+1", "A", "V"),
    (" ", "A", "CY+1", true, List("CY+1"), "CY+1", " ", "V"),
    (" ", "M", "CY+1", false, List(), " ", " ", "M"),
    (" ", "V", "CY+1", false, List(), " ", " ", "V"),
  )

  "check all required scenarios" in {

    forAll(scenarios) { (
                          cyStatus: String,
                          nyStatus: String,
                          intent: String,
                          valid: Boolean,
                          presented: List[String],
                          sent: String,
                          expectedCY: String,
                          expectedNY: String
                        ) =>

      val optInProposition = createOptInProposition(currentTaxYear,
        toITSAStatus(cyStatus),
        toITSAStatus(nyStatus))

      testOptIntScenario(optInProposition,
        valid,
        intent,
        presented,
        sent,
        expectedCY,
        expectedNY
      )
    }
  }

  private def testOptIntScenario(optInProposition: OptInProposition,
                                 valid: Boolean,
                                 intent: String,
                                 presented: List[String],
                                 sent: String,
                                 expectedCY: String,
                                 expectedNY: String) = {


    assert(optInProposition.availableTaxYearsForOptIn === presented.map(v => toTaxYear(v)))

    if (valid) {

      val intentTaxYear = toTaxYear(intent)
      assert(optInProposition.availableTaxYearsForOptIn.contains(intentTaxYear))

      assert(optInProposition.expectedItsaStatusesAfter(intentTaxYear) ===
        Seq(
          toITSAStatus(expectedCY),
          toITSAStatus(expectedNY))
      )
    }

  }

  def toTaxYear(option: String): TaxYear = {
    option match {
      case "CY" => currentTaxYear
      case "CY+1" => currentTaxYear.nextYear
    }
  }

  def toITSAStatus(status: String): ITSAStatus = status match {
    case "M" => Mandated
    case "V" => Voluntary
    case "A" => Annual
    case " " => NoStatus
  }
}