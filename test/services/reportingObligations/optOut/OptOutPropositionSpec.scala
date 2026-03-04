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

package services.reportingObligations.optOut

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, NoStatus, Voluntary}
import org.scalatest.prop.TableDrivenPropertyChecks.*
import services.reportingObligations.optOut.{CurrentOptOutTaxYear, NextOptOutTaxYear, OptOutProposition, PreviousOptOutTaxYear}
import testUtils.UnitSpec

import scala.io.Source

class OptOutPropositionSpec extends UnitSpec {

  "Parse opt out scenarios from tsv file" ignore {
    /*  The programme has specified all required Opt Out scenarios in a large spreadsheet.
        This code can ingest the data and convert to parameters for table based test found below.
        This generator is currently ignored but can be reactivated to regenerate the test data if needed.
          - To reactivate generator, replace "ignore" with "in"

      Select first 9 columns of Opt Out Scenarios Spreadsheet (currently v15).
        - to avoid cells that contain carriage returns as to make parsing easier.
      Paste into a fresh spreadsheet and save as OptOutScenarios.tsv in project root directory.
        - csv has issues with commas within cells, therefore use tsv to allow simple parsing.
      Run the test to generate the formatted test data in the console, based on the required scenarios.
        - Copy the scenarios output from the console and paste them into the data table "scenarios" to update the following test in this file.
   */

    val validTaxYears = Seq("CY-1", "CY", "CY+1")

    def invalid(taxYear: String): Boolean = {
      !validTaxYears.contains(taxYear)
    }

    def parseTaxYears(taxYears: String): Seq[String] = {
      (taxYears match {
        case "Nothing displayed" => Seq()
        case "N/A" => Seq()
        case _ => taxYears.replaceAll("\"", "").split("( [aA]nd |, )").map(_.trim).toSeq
      }).map(taxYear => {
        if (invalid(taxYear)) throw new RuntimeException(s"Unexpected tax year - $taxYear")
        taxYear
      })
    }

    def formatTaxYears(taxYears: Seq[String]): String = {
      "Seq(" + taxYears.map(option => s"\"$option\"").mkString(", ") + ")"
    }

    def parseCrystallisedStatus(input: String): String =
      input match {
        case "Y" => "Y"
        case "N" => "N"
        case _ => throw new RuntimeException(s"Unexpected Crystallised status data $input")
      }

    def parseItsaStatus(input: String): String =
      input match {
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
        case _ => throw new RuntimeException("Unexpected entry in Valid column")
      }

    def parseCustomerIntent(intent: String): String = {
      intent.replaceAll("Customer wants to opt out from ", "")
    }

    def quote(input: String): String = {
      "\"" + input + "\""
    }

    println("""("Crystallised", "CY-1", "CY", "CY+1", "Expected Opt Out years offered", "Customer intent", "Valid", "Expected Opt Out years", "CY-1", "CY", "CY+1"),""")

    val tsvOptOut = Source.fromFile("OptOutScenarios.tsv")
    val rowsWithoutHeaders = tsvOptOut.getLines().drop(2)
    val noOfColumns = 12

    rowsWithoutHeaders.foreach(line => {
      val cells = line.split("\t").take(noOfColumns)
      println(s"(${
        Seq(
          quote(parseCrystallisedStatus(cells(0))),
          quote(parseItsaStatus(cells(1))),
          quote(parseItsaStatus(cells(2))),
          quote(parseItsaStatus(cells(3))),
          formatTaxYears(parseTaxYears(cells(4))),
          quote(parseCustomerIntent(cells(5))),
          parseIsValid(cells(6)).toString,
          formatTaxYears(parseTaxYears(cells(8))),
          quote(parseItsaStatus(cells(9))),
          quote(parseItsaStatus(cells(10))),
          quote(parseItsaStatus(cells(11))),
        ).mkString(", ")
      }),")
    })

    tsvOptOut.close()
  }

  val currentTaxYear: TaxYear = TaxYear.forYearEnd(2021)
  val previousTaxYear: TaxYear = currentTaxYear.previousYear
  val nextTaxYear: TaxYear = currentTaxYear.nextYear

  private val scenarios =
    Table(
      ("Crystallised", "CY-1", "CY", "CY+1", "Expected Opt Out years offered", "Customer intent", "Valid", "Expected Opt Out years", "CY-1", "CY", "CY+1"),
      ("N", "M", "M", "M", Seq(), "CY-1", false, Seq(), "M", "M", "M"),
      ("Y", "M", "M", "M", Seq(), "CY-1", false, Seq(), "M", "M", "M"),
      ("N", "M", "M", "M", Seq(), "CY", false, Seq(), "M", "M", "M"),
      ("Y", "M", "M", "M", Seq(), "CY", false, Seq(), "M", "M", "M"),
      ("N", "M", "M", "M", Seq(), "CY+1", false, Seq(), "M", "M", "M"),
      ("Y", "M", "M", "M", Seq(), "CY+1", false, Seq(), "M", "M", "M"),
      ("N", "M", "M", " ", Seq(), "CY-1", false, Seq(), "M", "M", " "),
      ("Y", "M", "M", " ", Seq(), "CY-1", false, Seq(), "M", "M", " "),
      ("N", "M", "M", " ", Seq(), "CY", false, Seq(), "M", "M", " "),
      ("Y", "M", "M", " ", Seq(), "CY", false, Seq(), "M", "M", " "),
      ("N", "M", "M", " ", Seq(), "CY+1", false, Seq(), "M", "M", " "),
      ("Y", "M", "M", " ", Seq(), "CY+1", false, Seq(), "M", "M", " "),
      ("N", "M", "M", "V", Seq("CY+1"), "CY-1", false, Seq(), "M", "M", "V"),
      ("Y", "M", "M", "V", Seq("CY+1"), "CY-1", false, Seq(), "M", "M", "V"),
      ("N", "M", "M", "V", Seq("CY+1"), "CY", false, Seq(), "M", "M", "V"),
      ("Y", "M", "M", "V", Seq("CY+1"), "CY", false, Seq(), "M", "M", "V"),
      ("N", "M", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), "M", "M", "A"),
      ("Y", "M", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), "M", "M", "A"),
      ("N", "M", "M", "A", Seq(), "CY-1", false, Seq(), "M", "M", "A"),
      ("Y", "M", "M", "A", Seq(), "CY-1", false, Seq(), "M", "M", "A"),
      ("N", "M", "M", "A", Seq(), "CY", false, Seq(), "M", "M", "A"),
      ("Y", "M", "M", "A", Seq(), "CY", false, Seq(), "M", "M", "A"),
      ("N", "M", "M", "A", Seq(), "CY+1", false, Seq(), "M", "M", "A"),
      ("Y", "M", "M", "A", Seq(), "CY+1", false, Seq(), "M", "M", "A"),
      ("N", "M", "V", "M", Seq("CY"), "CY-1", false, Seq(), "M", "V", "M"),
      ("Y", "M", "V", "M", Seq("CY"), "CY-1", false, Seq(), "M", "V", "M"),
      ("N", "M", "V", "M", Seq("CY"), "CY", true, Seq("CY"), "M", "A", "M"),
      ("Y", "M", "V", "M", Seq("CY"), "CY", true, Seq("CY"), "M", "A", "M"),
      ("N", "M", "V", "M", Seq("CY"), "CY+1", false, Seq(), "M", "V", "M"),
      ("Y", "M", "V", "M", Seq("CY"), "CY+1", false, Seq(), "M", "V", "M"),
      ("N", "M", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq(), "M", "V", " "),
      ("Y", "M", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq(), "M", "V", " "),
      ("N", "M", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY"), "M", "A", " "),
      ("Y", "M", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY"), "M", "A", " "),
      ("N", "M", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), "M", "V", "A"),
      ("Y", "M", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), "M", "V", "A"),
      ("N", "M", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq(), "M", "V", "V"),
      ("Y", "M", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq(), "M", "V", "V"),
      ("N", "M", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1"), "M", "A", "A"),
      ("Y", "M", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1"), "M", "A", "A"),
      ("N", "M", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), "M", "V", "A"),
      ("Y", "M", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), "M", "V", "A"),
      ("N", "M", "V", "A", Seq("CY"), "CY-1", false, Seq(), "M", "V", "A"),
      ("Y", "M", "V", "A", Seq("CY"), "CY-1", false, Seq(), "M", "V", "A"),
      ("N", "M", "V", "A", Seq("CY"), "CY", true, Seq("CY"), "M", "A", "A"),
      ("Y", "M", "V", "A", Seq("CY"), "CY", true, Seq("CY"), "M", "A", "A"),
      ("N", "M", "V", "A", Seq("CY"), "CY+1", false, Seq(), "M", "V", "A"),
      ("Y", "M", "V", "A", Seq("CY"), "CY+1", false, Seq(), "M", "V", "A"),
      ("N", "M", "A", "M", Seq(), "CY-1", false, Seq(), "M", "A", "M"),
      ("Y", "M", "A", "M", Seq(), "CY-1", false, Seq(), "M", "A", "M"),
      ("N", "M", "A", "M", Seq(), "CY", false, Seq(), "M", "A", "M"),
      ("Y", "M", "A", "M", Seq(), "CY", false, Seq(), "M", "A", "M"),
      ("N", "M", "A", "M", Seq(), "CY+1", false, Seq(), "M", "A", "M"),
      ("Y", "M", "A", "M", Seq(), "CY+1", false, Seq(), "M", "A", "M"),
      ("N", "M", "A", " ", Seq(), "CY-1", false, Seq(), "M", "A", " "),
      ("Y", "M", "A", " ", Seq(), "CY-1", false, Seq(), "M", "A", " "),
      ("N", "M", "A", " ", Seq(), "CY", false, Seq(), "M", "A", " "),
      ("Y", "M", "A", " ", Seq(), "CY", false, Seq(), "M", "A", " "),
      ("N", "M", "A", " ", Seq(), "CY+1", false, Seq(), "M", "A", " "),
      ("Y", "M", "A", " ", Seq(), "CY+1", false, Seq(), "M", "A", " "),
      ("N", "M", "A", "V", Seq("CY+1"), "CY-1", false, Seq(), "M", "A", "V"),
      ("Y", "M", "A", "V", Seq("CY+1"), "CY-1", false, Seq(), "M", "A", "V"),
      ("N", "M", "A", "V", Seq("CY+1"), "CY", false, Seq(), "M", "A", "V"),
      ("Y", "M", "A", "V", Seq("CY+1"), "CY", false, Seq(), "M", "A", "V"),
      ("N", "M", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), "M", "A", "A"),
      ("Y", "M", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), "M", "A", "A"),
      ("N", "M", "A", "A", Seq(), "CY-1", false, Seq(), "M", "A", "A"),
      ("Y", "M", "A", "A", Seq(), "CY-1", false, Seq(), "M", "A", "A"),
      ("N", "M", "A", "A", Seq(), "CY", false, Seq(), "M", "A", "A"),
      ("Y", "M", "A", "A", Seq(), "CY", false, Seq(), "M", "A", "A"),
      ("N", "M", "A", "A", Seq(), "CY+1", false, Seq(), "M", "A", "A"),
      ("Y", "M", "A", "A", Seq(), "CY+1", false, Seq(), "M", "A", "A"),
      ("N", "V", "M", "M", Seq("CY-1"), "CY-1", true, Seq("CY-1"), "A", "M", "M"),
      ("Y", "V", "M", "M", Seq(), "CY-1", false, Seq(), "A", "M", "M"),
      ("N", "V", "M", "M", Seq("CY-1"), "CY", false, Seq(), "V", "M", "M"),
      ("Y", "V", "M", "M", Seq(), "CY", false, Seq(), "V", "M", "M"),
      ("N", "V", "M", "M", Seq("CY-1"), "CY+1", false, Seq(), "V", "M", "M"),
      ("Y", "V", "M", "M", Seq(), "CY+1", false, Seq(), "V", "M", "M"),
      ("N", "V", "M", " ", Seq("CY-1"), "CY-1", true, Seq("CY-1"), "A", "M", " "),
      ("Y", "V", "M", " ", Seq(), "CY-1", false, Seq(), "A", "M", " "),
      ("N", "V", "M", " ", Seq("CY-1"), "CY", false, Seq(), "V", "M", " "),
      ("Y", "V", "M", " ", Seq(), "CY", false, Seq(), "V", "M", " "),
      ("N", "V", "M", " ", Seq("CY-1"), "CY+1", false, Seq(), "V", "M", " "),
      ("Y", "V", "M", " ", Seq(), "CY+1", false, Seq(), "V", "M", " "),
      ("N", "V", "M", "V", Seq("CY-1", "CY+1"), "CY-1", true, Seq("CY-1", "CY+1"), "A", "M", "A"),
      ("Y", "V", "M", "V", Seq("CY+1"), "CY-1", false, Seq(), "A", "M", "V"),
      ("N", "V", "M", "V", Seq("CY-1", "CY+1"), "CY", false, Seq(), "V", "M", "V"),
      ("Y", "V", "M", "V", Seq("CY+1"), "CY", false, Seq(), "V", "M", "V"),
      ("N", "V", "M", "V", Seq("CY-1", "CY+1"), "CY+1", true, Seq("CY+1"), "V", "M", "A"),
      ("Y", "V", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), "V", "M", "A"),
      ("N", "V", "M", "A", Seq("CY-1"), "CY-1", true, Seq("CY-1"), "A", "M", "A"),
      ("Y", "V", "M", "A", Seq(), "CY-1", false, Seq(), "A", "M", "A"),
      ("N", "V", "M", "A", Seq("CY-1"), "CY", false, Seq(), "V", "M", "A"),
      ("Y", "V", "M", "A", Seq(), "CY", false, Seq(), "V", "M", "A"),
      ("N", "V", "M", "A", Seq("CY-1"), "CY+1", false, Seq(), "V", "M", "A"),
      ("Y", "V", "M", "A", Seq(), "CY+1", false, Seq(), "V", "M", "A"),
      ("N", "V", "V", "M", Seq("CY-1", "CY"), "CY-1", true, Seq("CY-1", "CY"), "A", "A", "M"),
      ("Y", "V", "V", "M", Seq("CY"), "CY-1", false, Seq(), "A", "A", "M"),
      ("N", "V", "V", "M", Seq("CY-1", "CY"), "CY", true, Seq("CY"), "V", "A", "M"),
      ("Y", "V", "V", "M", Seq("CY"), "CY", true, Seq("CY"), "V", "A", "M"),
      ("N", "V", "V", "M", Seq("CY-1", "CY"), "CY+1", false, Seq(), "V", "V", "M"),
      ("Y", "V", "V", "M", Seq("CY"), "CY+1", false, Seq(), "V", "V", "M"),
      ("N", "V", "V", " ", Seq("CY-1", "CY", "CY+1"), "CY-1", true, Seq("CY-1", "CY"), "A", "A", " "),
      ("Y", "V", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq(), "A", "A", " "),
      ("N", "V", "V", " ", Seq("CY-1", "CY", "CY+1"), "CY", true, Seq("CY"), "V", "A", " "),
      ("Y", "V", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY"), "V", "A", " "),
      ("N", "V", "V", " ", Seq("CY-1", "CY", "CY+1"), "CY+1", true, Seq("CY+1"), "V", "V", "A"),
      ("Y", "V", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), "V", "V", "A"),
      ("N", "V", "V", "V", Seq("CY-1", "CY", "CY+1"), "CY-1", true, Seq("CY-1", "CY", "CY+1"), "A", "A", "A"),
      ("Y", "V", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq(), "A", "A", "A"),
      ("N", "V", "V", "V", Seq("CY-1", "CY", "CY+1"), "CY", true, Seq("CY", "CY+1"), "V", "A", "A"),
      ("Y", "V", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1"), "V", "A", "A"),
      ("N", "V", "V", "V", Seq("CY-1", "CY", "CY+1"), "CY+1", true, Seq("CY+1"), "V", "V", "A"),
      ("Y", "V", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), "V", "V", "A"),
      ("N", "V", "V", "A", Seq("CY-1", "CY"), "CY-1", true, Seq("CY-1", "CY"), "A", "A", "A"),
      ("Y", "V", "V", "A", Seq("CY"), "CY-1", false, Seq(), "A", "A", "A"),
      ("N", "V", "V", "A", Seq("CY-1", "CY"), "CY", true, Seq("CY"), "V", "A", "A"),
      ("Y", "V", "V", "A", Seq("CY"), "CY", true, Seq("CY"), "V", "A", "A"),
      ("N", "V", "V", "A", Seq("CY-1", "CY"), "CY+1", false, Seq(), "V", "V", "A"),
      ("Y", "V", "V", "A", Seq("CY"), "CY+1", false, Seq(), "V", "V", "A"),
      ("N", "V", "A", "M", Seq("CY-1"), "CY-1", true, Seq("CY-1"), "A", "A", "M"),
      ("Y", "V", "A", "M", Seq(), "CY-1", false, Seq(), "A", "A", "M"),
      ("N", "V", "A", "M", Seq("CY-1"), "CY", false, Seq(), "V", "A", "M"),
      ("Y", "V", "A", "M", Seq(), "CY", false, Seq(), "V", "A", "M"),
      ("N", "V", "A", "M", Seq("CY-1"), "CY+1", false, Seq(), "V", "A", "M"),
      ("Y", "V", "A", "M", Seq(), "CY+1", false, Seq(), "V", "A", "M"),
      ("N", "V", "A", " ", Seq("CY-1"), "CY-1", true, Seq("CY-1"), "A", "A", " "),
      ("Y", "V", "A", " ", Seq(), "CY-1", false, Seq(), "A", "A", " "),
      ("N", "V", "A", " ", Seq("CY-1"), "CY", false, Seq(), "V", "A", " "),
      ("Y", "V", "A", " ", Seq(), "CY", false, Seq(), "V", "A", " "),
      ("N", "V", "A", " ", Seq("CY-1"), "CY+1", false, Seq(), "V", "A", " "),
      ("Y", "V", "A", " ", Seq(), "CY+1", false, Seq(), "V", "A", " "),
      ("N", "V", "A", "V", Seq("CY-1", "CY+1"), "CY-1", true, Seq("CY-1", "CY+1"), "A", "A", "A"),
      ("Y", "V", "A", "V", Seq("CY+1"), "CY-1", false, Seq(), "A", "A", "V"),
      ("N", "V", "A", "V", Seq("CY-1", "CY+1"), "CY", false, Seq(), "V", "A", "V"),
      ("Y", "V", "A", "V", Seq("CY+1"), "CY", false, Seq(), "V", "A", "V"),
      ("N", "V", "A", "V", Seq("CY-1", "CY+1"), "CY+1", true, Seq("CY+1"), "V", "A", "A"),
      ("Y", "V", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), "V", "A", "A"),
      ("N", "V", "A", "A", Seq("CY-1"), "CY-1", true, Seq("CY-1"), "A", "A", "A"),
      ("Y", "V", "A", "A", Seq(), "CY-1", false, Seq(), "A", "A", "A"),
      ("N", "V", "A", "A", Seq("CY-1"), "CY", false, Seq(), "V", "A", "A"),
      ("Y", "V", "A", "A", Seq(), "CY", false, Seq(), "V", "A", "A"),
      ("N", "V", "A", "A", Seq("CY-1"), "CY+1", false, Seq(), "V", "A", "A"),
      ("Y", "V", "A", "A", Seq(), "CY+1", false, Seq(), "V", "A", "A"),
      ("N", "A", "M", "M", Seq(), "CY-1", false, Seq(), "A", "M", "M"),
      ("Y", "A", "M", "M", Seq(), "CY-1", false, Seq(), "A", "M", "M"),
      ("N", "A", "M", "M", Seq(), "CY", false, Seq(), "A", "M", "M"),
      ("Y", "A", "M", "M", Seq(), "CY", false, Seq(), "A", "M", "M"),
      ("N", "A", "M", "M", Seq(), "CY+1", false, Seq(), "A", "M", "M"),
      ("Y", "A", "M", "M", Seq(), "CY+1", false, Seq(), "A", "M", "M"),
      ("N", "A", "M", " ", Seq(), "CY-1", false, Seq(), "A", "M", " "),
      ("Y", "A", "M", " ", Seq(), "CY-1", false, Seq(), "A", "M", " "),
      ("N", "A", "M", " ", Seq(), "CY", false, Seq(), "A", "M", " "),
      ("Y", "A", "M", " ", Seq(), "CY", false, Seq(), "A", "M", " "),
      ("N", "A", "M", " ", Seq(), "CY+1", false, Seq(), "A", "M", " "),
      ("Y", "A", "M", " ", Seq(), "CY+1", false, Seq(), "A", "M", " "),
      ("N", "A", "M", "V", Seq("CY+1"), "CY-1", false, Seq(), "A", "M", "V"),
      ("Y", "A", "M", "V", Seq("CY+1"), "CY-1", false, Seq(), "A", "M", "V"),
      ("N", "A", "M", "V", Seq("CY+1"), "CY", false, Seq(), "A", "M", "V"),
      ("Y", "A", "M", "V", Seq("CY+1"), "CY", false, Seq(), "A", "M", "V"),
      ("N", "A", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), "A", "M", "A"),
      ("Y", "A", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), "A", "M", "A"),
      ("N", "A", "M", "A", Seq(), "CY-1", false, Seq(), "A", "M", "A"),
      ("Y", "A", "M", "A", Seq(), "CY-1", false, Seq(), "A", "M", "A"),
      ("N", "A", "M", "A", Seq(), "CY", false, Seq(), "A", "M", "A"),
      ("Y", "A", "M", "A", Seq(), "CY", false, Seq(), "A", "M", "A"),
      ("N", "A", "M", "A", Seq(), "CY+1", false, Seq(), "A", "M", "A"),
      ("Y", "A", "M", "A", Seq(), "CY+1", false, Seq(), "A", "M", "A"),
      ("N", "A", "V", "M", Seq("CY"), "CY-1", false, Seq(), "A", "V", "M"),
      ("Y", "A", "V", "M", Seq("CY"), "CY-1", false, Seq(), "A", "V", "M"),
      ("N", "A", "V", "M", Seq("CY"), "CY", true, Seq("CY"), "A", "A", "M"),
      ("Y", "A", "V", "M", Seq("CY"), "CY", true, Seq("CY"), "A", "A", "M"),
      ("N", "A", "V", "M", Seq("CY"), "CY+1", false, Seq(), "A", "V", "M"),
      ("Y", "A", "V", "M", Seq("CY"), "CY+1", false, Seq(), "A", "V", "M"),
      ("N", "A", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq(), "A", "V", " "),
      ("Y", "A", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq(), "A", "V", " "),
      ("N", "A", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY"), "A", "A", " "),
      ("Y", "A", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY"), "A", "A", " "),
      ("N", "A", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), "A", "V", "A"),
      ("Y", "A", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), "A", "V", "A"),
      ("N", "A", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq(), "A", "V", "V"),
      ("Y", "A", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq(), "A", "V", "V"),
      ("N", "A", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1"), "A", "A", "A"),
      ("Y", "A", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1"), "A", "A", "A"),
      ("N", "A", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), "A", "V", "A"),
      ("Y", "A", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), "A", "V", "A"),
      ("N", "A", "V", "A", Seq("CY"), "CY-1", false, Seq(), "A", "V", "A"),
      ("Y", "A", "V", "A", Seq("CY"), "CY-1", false, Seq(), "A", "V", "A"),
      ("N", "A", "V", "A", Seq("CY"), "CY", true, Seq("CY"), "A", "A", "A"),
      ("Y", "A", "V", "A", Seq("CY"), "CY", true, Seq("CY"), "A", "A", "A"),
      ("N", "A", "V", "A", Seq("CY"), "CY+1", false, Seq(), "A", "V", "A"),
      ("Y", "A", "V", "A", Seq("CY"), "CY+1", false, Seq(), "A", "V", "A"),
      ("N", "A", "A", "M", Seq(), "CY-1", false, Seq(), "A", "A", "M"),
      ("Y", "A", "A", "M", Seq(), "CY-1", false, Seq(), "A", "A", "M"),
      ("N", "A", "A", "M", Seq(), "CY", false, Seq(), "A", "A", "M"),
      ("Y", "A", "A", "M", Seq(), "CY", false, Seq(), "A", "A", "M"),
      ("N", "A", "A", "M", Seq(), "CY+1", false, Seq(), "A", "A", "M"),
      ("Y", "A", "A", "M", Seq(), "CY+1", false, Seq(), "A", "A", "M"),
      ("N", "A", "A", " ", Seq(), "CY-1", false, Seq(), "A", "A", " "),
      ("Y", "A", "A", " ", Seq(), "CY-1", false, Seq(), "A", "A", " "),
      ("N", "A", "A", " ", Seq(), "CY", false, Seq(), "A", "A", " "),
      ("Y", "A", "A", " ", Seq(), "CY", false, Seq(), "A", "A", " "),
      ("N", "A", "A", " ", Seq(), "CY+1", false, Seq(), "A", "A", " "),
      ("Y", "A", "A", " ", Seq(), "CY+1", false, Seq(), "A", "A", " "),
      ("N", "A", "A", "V", Seq("CY+1"), "CY-1", false, Seq(), "A", "A", "V"),
      ("Y", "A", "A", "V", Seq("CY+1"), "CY-1", false, Seq(), "A", "A", "V"),
      ("N", "A", "A", "V", Seq("CY+1"), "CY", false, Seq(), "A", "A", "V"),
      ("Y", "A", "A", "V", Seq("CY+1"), "CY", false, Seq(), "A", "A", "V"),
      ("N", "A", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), "A", "A", "A"),
      ("Y", "A", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), "A", "A", "A"),
      ("N", "A", "A", "A", Seq(), "CY-1", false, Seq(), "A", "A", "A"),
      ("Y", "A", "A", "A", Seq(), "CY-1", false, Seq(), "A", "A", "A"),
      ("N", "A", "A", "A", Seq(), "CY", false, Seq(), "A", "A", "A"),
      ("Y", "A", "A", "A", Seq(), "CY", false, Seq(), "A", "A", "A"),
      ("N", "A", "A", "A", Seq(), "CY+1", false, Seq(), "A", "A", "A"),
      ("Y", "A", "A", "A", Seq(), "CY+1", false, Seq(), "A", "A", "A"),
      ("N", " ", "M", "M", Seq(), "CY-1", false, Seq(), " ", "M", "M"),
      ("Y", " ", "M", "M", Seq(), "CY-1", false, Seq(), " ", "M", "M"),
      ("N", " ", "M", "M", Seq(), "CY", false, Seq(), " ", "M", "M"),
      ("Y", " ", "M", "M", Seq(), "CY", false, Seq(), " ", "M", "M"),
      ("N", " ", "M", "M", Seq(), "CY+1", false, Seq(), " ", "M", "M"),
      ("Y", " ", "M", "M", Seq(), "CY+1", false, Seq(), " ", "M", "M"),
      ("N", " ", "M", " ", Seq(), "CY-1", false, Seq(), " ", "M", " "),
      ("Y", " ", "M", " ", Seq(), "CY-1", false, Seq(), " ", "M", " "),
      ("N", " ", "M", " ", Seq(), "CY", false, Seq(), " ", "M", " "),
      ("Y", " ", "M", " ", Seq(), "CY", false, Seq(), " ", "M", " "),
      ("N", " ", "M", " ", Seq(), "CY+1", false, Seq(), " ", "M", " "),
      ("Y", " ", "M", " ", Seq(), "CY+1", false, Seq(), " ", "M", " "),
      ("N", " ", "M", "V", Seq("CY+1"), "CY-1", false, Seq(), " ", "M", "V"),
      ("Y", " ", "M", "V", Seq("CY+1"), "CY-1", false, Seq(), " ", "M", "V"),
      ("N", " ", "M", "V", Seq("CY+1"), "CY", false, Seq(), " ", "M", "V"),
      ("Y", " ", "M", "V", Seq("CY+1"), "CY", false, Seq(), " ", "M", "V"),
      ("N", " ", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), " ", "M", "A"),
      ("Y", " ", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), " ", "M", "A"),
      ("N", " ", "M", "A", Seq(), "CY-1", false, Seq(), " ", "M", "A"),
      ("Y", " ", "M", "A", Seq(), "CY-1", false, Seq(), " ", "M", "A"),
      ("N", " ", "M", "A", Seq(), "CY", false, Seq(), " ", "M", "A"),
      ("Y", " ", "M", "A", Seq(), "CY", false, Seq(), " ", "M", "A"),
      ("N", " ", "M", "A", Seq(), "CY+1", false, Seq(), " ", "M", "A"),
      ("Y", " ", "M", "A", Seq(), "CY+1", false, Seq(), " ", "M", "A"),
      ("N", " ", "V", "M", Seq("CY"), "CY-1", false, Seq(), " ", "V", "M"),
      ("Y", " ", "V", "M", Seq("CY"), "CY-1", false, Seq(), " ", "V", "M"),
      ("N", " ", "V", "M", Seq("CY"), "CY", true, Seq("CY"), " ", "A", "M"),
      ("Y", " ", "V", "M", Seq("CY"), "CY", true, Seq("CY"), " ", "A", "M"),
      ("N", " ", "V", "M", Seq("CY"), "CY+1", false, Seq(), " ", "V", "M"),
      ("Y", " ", "V", "M", Seq("CY"), "CY+1", false, Seq(), " ", "V", "M"),
      ("N", " ", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq(), " ", "V", " "),
      ("Y", " ", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq(), " ", "V", " "),
      ("N", " ", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY"), " ", "A", " "),
      ("Y", " ", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY"), " ", "A", " "),
      ("N", " ", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), " ", "V", "A"),
      ("Y", " ", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), " ", "V", "A"),
      ("N", " ", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq(), " ", "V", "V"),
      ("Y", " ", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq(), " ", "V", "V"),
      ("N", " ", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1"), " ", "A", "A"),
      ("Y", " ", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1"), " ", "A", "A"),
      ("N", " ", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), " ", "V", "A"),
      ("Y", " ", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1"), " ", "V", "A"),
      ("N", " ", "V", "A", Seq("CY"), "CY-1", false, Seq(), " ", "V", "A"),
      ("Y", " ", "V", "A", Seq("CY"), "CY-1", false, Seq(), " ", "V", "A"),
      ("N", " ", "V", "A", Seq("CY"), "CY", true, Seq("CY"), " ", "A", "A"),
      ("Y", " ", "V", "A", Seq("CY"), "CY", true, Seq("CY"), " ", "A", "A"),
      ("N", " ", "V", "A", Seq("CY"), "CY+1", false, Seq(), " ", "V", "A"),
      ("Y", " ", "V", "A", Seq("CY"), "CY+1", false, Seq(), " ", "V", "A"),
      ("N", " ", "A", "M", Seq(), "CY-1", false, Seq(), " ", "A", "M"),
      ("Y", " ", "A", "M", Seq(), "CY-1", false, Seq(), " ", "A", "M"),
      ("N", " ", "A", "M", Seq(), "CY", false, Seq(), " ", "A", "M"),
      ("Y", " ", "A", "M", Seq(), "CY", false, Seq(), " ", "A", "M"),
      ("N", " ", "A", "M", Seq(), "CY+1", false, Seq(), " ", "A", "M"),
      ("Y", " ", "A", "M", Seq(), "CY+1", false, Seq(), " ", "A", "M"),
      ("N", " ", "A", " ", Seq(), "CY-1", false, Seq(), " ", "A", " "),
      ("Y", " ", "A", " ", Seq(), "CY-1", false, Seq(), " ", "A", " "),
      ("N", " ", "A", " ", Seq(), "CY", false, Seq(), " ", "A", " "),
      ("Y", " ", "A", " ", Seq(), "CY", false, Seq(), " ", "A", " "),
      ("N", " ", "A", " ", Seq(), "CY+1", false, Seq(), " ", "A", " "),
      ("Y", " ", "A", " ", Seq(), "CY+1", false, Seq(), " ", "A", " "),
      ("N", " ", "A", "V", Seq("CY+1"), "CY-1", false, Seq(), " ", "A", "V"),
      ("Y", " ", "A", "V", Seq("CY+1"), "CY-1", false, Seq(), " ", "A", "V"),
      ("N", " ", "A", "V", Seq("CY+1"), "CY", false, Seq(), " ", "A", "V"),
      ("Y", " ", "A", "V", Seq("CY+1"), "CY", false, Seq(), " ", "A", "V"),
      ("N", " ", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), " ", "A", "A"),
      ("Y", " ", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), " ", "A", "A"),
      ("N", " ", "A", "A", Seq(), "CY-1", false, Seq(), " ", "A", "A"),
      ("Y", " ", "A", "A", Seq(), "CY-1", false, Seq(), " ", "A", "A"),
      ("N", " ", "A", "A", Seq(), "CY", false, Seq(), " ", "A", "A"),
      ("Y", " ", "A", "A", Seq(), "CY", false, Seq(), " ", "A", "A"),
      ("N", " ", "A", "A", Seq(), "CY+1", false, Seq(), " ", "A", "A"),
      ("Y", " ", "A", "A", Seq(), "CY+1", false, Seq(), " ", "A", "A"),
      ("N", " ", " ", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), " ", " ", "A"),
      ("Y", " ", " ", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1"), " ", " ", "A"),
      ("N", " ", " ", "M", Seq(), "CY+1", false, Seq(), " ", " ", "M"),
      ("Y", " ", " ", "M", Seq(), "CY+1", false, Seq(), " ", " ", "M"),
    )

  "check all required scenarios" in {
    forAll(scenarios) { (isCrystallised: String,
                         pyStatus: String,
                         cyStatus: String,
                         nyStatus: String,
                         expectedTaxYearsOffered: Seq[String],
                         customerIntent: String,
                         valid: Boolean,
                         expectedTaxYearsOptedOut: Seq[String],
                         expectedOutcomePY: String,
                         expectedOutcomeCY: String,
                         expectedOutcomeNY: String,
                        ) =>

      val previousYear = PreviousOptOutTaxYear(toITSAStatus(pyStatus), taxYear = previousTaxYear, isCrystallised == "Y")
      val currentYear = CurrentOptOutTaxYear(toITSAStatus(cyStatus), taxYear = currentTaxYear)
      val nextYear = NextOptOutTaxYear(toITSAStatus(nyStatus), taxYear = nextTaxYear, currentTaxYear = currentYear)
      val optOutProposition = OptOutProposition(previousYear, currentYear, nextYear)

      testOptOutScenario(optOutProposition,
        expectedTaxYearsOffered.map(toTaxYear),
        valid,
        toTaxYear(customerIntent),
        expectedTaxYearsOptedOut.map(toTaxYear),
        Seq(toITSAStatus(expectedOutcomePY), toITSAStatus(expectedOutcomeCY), toITSAStatus(expectedOutcomeNY))
      )
    }
  }

  private def testOptOutScenario(optOutProposition: OptOutProposition,
                                 expectedTaxYearsOffered: Seq[TaxYear],
                                 valid: Boolean,
                                 customerIntent: TaxYear,
                                 expectedTaxYearsOptedOut: Seq[TaxYear],
                                 expectedItsaStatusesAfter: Seq[ITSAStatus]
                                ) = {

    assert(optOutProposition.availableTaxYearsForOptOut === expectedTaxYearsOffered)

    if (valid) {
      assert(optOutProposition.optOutYearsToUpdate(customerIntent) === expectedTaxYearsOptedOut)
      assert(optOutProposition.expectedItsaStatusesAfter(customerIntent) === expectedItsaStatusesAfter)
    }
  }

  def toTaxYear(option: String): TaxYear = {
    option match {
      case "CY-1" => previousTaxYear
      case "CY" => currentTaxYear
      case "CY+1" => nextTaxYear
    }
  }

  def toITSAStatus(status: String): ITSAStatus = status match {
    case "M" => Mandated
    case "V" => Voluntary
    case "A" => Annual
    case " " => NoStatus
  }
}
