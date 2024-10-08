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

package services.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, NoStatus, Voluntary}
import org.scalatest.prop.TableDrivenPropertyChecks._
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

    println("""("Crystallised", "CY-1", "CY", "CY+1", "Expected Opt Out years offered", "Customer intent", "Valid", "Expected Opt Out years"),""")

    val tsvOptOut = Source.fromFile("OptOutScenarios.tsv")
    val rowsWithoutHeaders = tsvOptOut.getLines().drop(2)

    rowsWithoutHeaders.foreach(line => {
      val cells = line.split("\t").take(9)
      //println(cells.mkString("  --  "))
      println(s"(${
        Seq(
          quote(parseCrystallisedStatus(cells(0))),
          quote(parseItsaStatus(cells(1))),
          quote(parseItsaStatus(cells(2))),
          quote(parseItsaStatus(cells(3))),
          formatTaxYears(parseTaxYears(cells(4))),
          quote(parseCustomerIntent(cells(5))),
          parseIsValid(cells(6)).toString,
          formatTaxYears(parseTaxYears(cells(8)))
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
      ("Crystallised", "CY-1", "CY", "CY+1", "Expected Opt Out years offered", "Customer intent", "Valid", "Expected Opt Out years"),
      ("N", "M", "M", "M", Seq(), "CY-1", false, Seq()),
      ("Y", "M", "M", "M", Seq(), "CY-1", false, Seq()),
      ("N", "M", "M", "M", Seq(), "CY", false, Seq()),
      ("Y", "M", "M", "M", Seq(), "CY", false, Seq()),
      ("N", "M", "M", "M", Seq(), "CY+1", false, Seq()),
      ("Y", "M", "M", "M", Seq(), "CY+1", false, Seq()),
      ("N", "M", "M", " ", Seq(), "CY-1", false, Seq()),
      ("Y", "M", "M", " ", Seq(), "CY-1", false, Seq()),
      ("N", "M", "M", " ", Seq(), "CY", false, Seq()),
      ("Y", "M", "M", " ", Seq(), "CY", false, Seq()),
      ("N", "M", "M", " ", Seq(), "CY+1", false, Seq()),
      ("Y", "M", "M", " ", Seq(), "CY+1", false, Seq()),
      ("N", "M", "M", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("Y", "M", "M", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("N", "M", "M", "V", Seq("CY+1"), "CY", false, Seq()),
      ("Y", "M", "M", "V", Seq("CY+1"), "CY", false, Seq()),
      ("N", "M", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", "M", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", "M", "M", "A", Seq(), "CY-1", false, Seq()),
      ("Y", "M", "M", "A", Seq(), "CY-1", false, Seq()),
      ("N", "M", "M", "A", Seq(), "CY", false, Seq()),
      ("Y", "M", "M", "A", Seq(), "CY", false, Seq()),
      ("N", "M", "M", "A", Seq(), "CY+1", false, Seq()),
      ("Y", "M", "M", "A", Seq(), "CY+1", false, Seq()),
      ("N", "M", "V", "M", Seq("CY"), "CY-1", false, Seq()),
      ("Y", "M", "V", "M", Seq("CY"), "CY-1", false, Seq()),
      ("N", "M", "V", "M", Seq("CY"), "CY", true, Seq("CY")),
      ("Y", "M", "V", "M", Seq("CY"), "CY", true, Seq("CY")),
      ("N", "M", "V", "M", Seq("CY"), "CY+1", false, Seq()),
      ("Y", "M", "V", "M", Seq("CY"), "CY+1", false, Seq()),
      ("N", "M", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("Y", "M", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("N", "M", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY")),
      ("Y", "M", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY")),
      ("N", "M", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", "M", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", "M", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("Y", "M", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("N", "M", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1")),
      ("Y", "M", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1")),
      ("N", "M", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", "M", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", "M", "V", "A", Seq("CY"), "CY-1", false, Seq()),
      ("Y", "M", "V", "A", Seq("CY"), "CY-1", false, Seq()),
      ("N", "M", "V", "A", Seq("CY"), "CY", true, Seq("CY")),
      ("Y", "M", "V", "A", Seq("CY"), "CY", true, Seq("CY")),
      ("N", "M", "V", "A", Seq("CY"), "CY+1", false, Seq()),
      ("Y", "M", "V", "A", Seq("CY"), "CY+1", false, Seq()),
      ("N", "M", "A", "M", Seq(), "CY-1", false, Seq()),
      ("Y", "M", "A", "M", Seq(), "CY-1", false, Seq()),
      ("N", "M", "A", "M", Seq(), "CY", false, Seq()),
      ("Y", "M", "A", "M", Seq(), "CY", false, Seq()),
      ("N", "M", "A", "M", Seq(), "CY+1", false, Seq()),
      ("Y", "M", "A", "M", Seq(), "CY+1", false, Seq()),
      ("N", "M", "A", " ", Seq(), "CY-1", false, Seq()),
      ("Y", "M", "A", " ", Seq(), "CY-1", false, Seq()),
      ("N", "M", "A", " ", Seq(), "CY", false, Seq()),
      ("Y", "M", "A", " ", Seq(), "CY", false, Seq()),
      ("N", "M", "A", " ", Seq(), "CY+1", false, Seq()),
      ("Y", "M", "A", " ", Seq(), "CY+1", false, Seq()),
      ("N", "M", "A", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("Y", "M", "A", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("N", "M", "A", "V", Seq("CY+1"), "CY", false, Seq()),
      ("Y", "M", "A", "V", Seq("CY+1"), "CY", false, Seq()),
      ("N", "M", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", "M", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", "M", "A", "A", Seq(), "CY-1", false, Seq()),
      ("Y", "M", "A", "A", Seq(), "CY-1", false, Seq()),
      ("N", "M", "A", "A", Seq(), "CY", false, Seq()),
      ("Y", "M", "A", "A", Seq(), "CY", false, Seq()),
      ("N", "M", "A", "A", Seq(), "CY+1", false, Seq()),
      ("Y", "M", "A", "A", Seq(), "CY+1", false, Seq()),
      ("N", "V", "M", "M", Seq("CY-1"), "CY-1", true, Seq("CY-1")),
      ("Y", "V", "M", "M", Seq(), "CY-1", false, Seq()),
      ("N", "V", "M", "M", Seq("CY-1"), "CY", false, Seq()),
      ("Y", "V", "M", "M", Seq(), "CY", false, Seq()),
      ("N", "V", "M", "M", Seq("CY-1"), "CY+1", false, Seq()),
      ("Y", "V", "M", "M", Seq(), "CY+1", false, Seq()),
      ("N", "V", "M", " ", Seq("CY-1"), "CY-1", true, Seq("CY-1")),
      ("Y", "V", "M", " ", Seq(), "CY-1", false, Seq()),
      ("N", "V", "M", " ", Seq("CY-1"), "CY", false, Seq()),
      ("Y", "V", "M", " ", Seq(), "CY", false, Seq()),
      ("N", "V", "M", " ", Seq("CY-1"), "CY+1", false, Seq()),
      ("Y", "V", "M", " ", Seq(), "CY+1", false, Seq()),
      ("N", "V", "M", "V", Seq("CY-1", "CY+1"), "CY-1", true, Seq("CY-1", "CY+1")),
      ("Y", "V", "M", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("N", "V", "M", "V", Seq("CY-1", "CY+1"), "CY", false, Seq()),
      ("Y", "V", "M", "V", Seq("CY+1"), "CY", false, Seq()),
      ("N", "V", "M", "V", Seq("CY-1", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", "V", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", "V", "M", "A", Seq("CY-1"), "CY-1", true, Seq("CY-1")),
      ("Y", "V", "M", "A", Seq(), "CY-1", false, Seq()),
      ("N", "V", "M", "A", Seq("CY-1"), "CY", false, Seq()),
      ("Y", "V", "M", "A", Seq(), "CY", false, Seq()),
      ("N", "V", "M", "A", Seq("CY-1"), "CY+1", false, Seq()),
      ("Y", "V", "M", "A", Seq(), "CY+1", false, Seq()),
      ("N", "V", "V", "M", Seq("CY-1", "CY"), "CY-1", true, Seq("CY-1", "CY")),
      ("Y", "V", "V", "M", Seq("CY"), "CY-1", false, Seq()),
      ("N", "V", "V", "M", Seq("CY-1", "CY"), "CY", true, Seq("CY")),
      ("Y", "V", "V", "M", Seq("CY"), "CY", true, Seq("CY")),
      ("N", "V", "V", "M", Seq("CY-1", "CY"), "CY+1", false, Seq()),
      ("Y", "V", "V", "M", Seq("CY"), "CY+1", false, Seq()),
      ("N", "V", "V", " ", Seq("CY-1", "CY", "CY+1"), "CY-1", true, Seq("CY-1", "CY")),
      ("Y", "V", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("N", "V", "V", " ", Seq("CY-1", "CY", "CY+1"), "CY", true, Seq("CY")),
      ("Y", "V", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY")),
      ("N", "V", "V", " ", Seq("CY-1", "CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", "V", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", "V", "V", "V", Seq("CY-1", "CY", "CY+1"), "CY-1", true, Seq("CY-1", "CY", "CY+1")),
      ("Y", "V", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("N", "V", "V", "V", Seq("CY-1", "CY", "CY+1"), "CY", true, Seq("CY", "CY+1")),
      ("Y", "V", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1")),
      ("N", "V", "V", "V", Seq("CY-1", "CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", "V", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", "V", "V", "A", Seq("CY-1", "CY"), "CY-1", true, Seq("CY-1", "CY")),
      ("Y", "V", "V", "A", Seq("CY"), "CY-1", false, Seq()),
      ("N", "V", "V", "A", Seq("CY-1", "CY"), "CY", true, Seq("CY")),
      ("Y", "V", "V", "A", Seq("CY"), "CY", true, Seq("CY")),
      ("N", "V", "V", "A", Seq("CY-1", "CY"), "CY+1", false, Seq()),
      ("Y", "V", "V", "A", Seq("CY"), "CY+1", false, Seq()),
      ("N", "V", "A", "M", Seq("CY-1"), "CY-1", true, Seq("CY-1")),
      ("Y", "V", "A", "M", Seq(), "CY-1", false, Seq()),
      ("N", "V", "A", "M", Seq("CY-1"), "CY", false, Seq()),
      ("Y", "V", "A", "M", Seq(), "CY", false, Seq()),
      ("N", "V", "A", "M", Seq("CY-1"), "CY+1", false, Seq()),
      ("Y", "V", "A", "M", Seq(), "CY+1", false, Seq()),
      ("N", "V", "A", " ", Seq("CY-1"), "CY-1", true, Seq("CY-1")),
      ("Y", "V", "A", " ", Seq(), "CY-1", false, Seq()),
      ("N", "V", "A", " ", Seq("CY-1"), "CY", false, Seq()),
      ("Y", "V", "A", " ", Seq(), "CY", false, Seq()),
      ("N", "V", "A", " ", Seq("CY-1"), "CY+1", false, Seq()),
      ("Y", "V", "A", " ", Seq(), "CY+1", false, Seq()),
      ("N", "V", "A", "V", Seq("CY-1", "CY+1"), "CY-1", true, Seq("CY-1", "CY+1")),
      ("Y", "V", "A", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("N", "V", "A", "V", Seq("CY-1", "CY+1"), "CY", false, Seq()),
      ("Y", "V", "A", "V", Seq("CY+1"), "CY", false, Seq()),
      ("N", "V", "A", "V", Seq("CY-1", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", "V", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", "V", "A", "A", Seq("CY-1"), "CY-1", true, Seq("CY-1")),
      ("Y", "V", "A", "A", Seq(), "CY-1", false, Seq()),
      ("N", "V", "A", "A", Seq("CY-1"), "CY", false, Seq()),
      ("Y", "V", "A", "A", Seq(), "CY", false, Seq()),
      ("N", "V", "A", "A", Seq("CY-1"), "CY+1", false, Seq()),
      ("Y", "V", "A", "A", Seq(), "CY+1", false, Seq()),
      ("N", "A", "M", "M", Seq(), "CY-1", false, Seq()),
      ("Y", "A", "M", "M", Seq(), "CY-1", false, Seq()),
      ("N", "A", "M", "M", Seq(), "CY", false, Seq()),
      ("Y", "A", "M", "M", Seq(), "CY", false, Seq()),
      ("N", "A", "M", "M", Seq(), "CY+1", false, Seq()),
      ("Y", "A", "M", "M", Seq(), "CY+1", false, Seq()),
      ("N", "A", "M", " ", Seq(), "CY-1", false, Seq()),
      ("Y", "A", "M", " ", Seq(), "CY-1", false, Seq()),
      ("N", "A", "M", " ", Seq(), "CY", false, Seq()),
      ("Y", "A", "M", " ", Seq(), "CY", false, Seq()),
      ("N", "A", "M", " ", Seq(), "CY+1", false, Seq()),
      ("Y", "A", "M", " ", Seq(), "CY+1", false, Seq()),
      ("N", "A", "M", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("Y", "A", "M", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("N", "A", "M", "V", Seq("CY+1"), "CY", false, Seq()),
      ("Y", "A", "M", "V", Seq("CY+1"), "CY", false, Seq()),
      ("N", "A", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", "A", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", "A", "M", "A", Seq(), "CY-1", false, Seq()),
      ("Y", "A", "M", "A", Seq(), "CY-1", false, Seq()),
      ("N", "A", "M", "A", Seq(), "CY", false, Seq()),
      ("Y", "A", "M", "A", Seq(), "CY", false, Seq()),
      ("N", "A", "M", "A", Seq(), "CY+1", false, Seq()),
      ("Y", "A", "M", "A", Seq(), "CY+1", false, Seq()),
      ("N", "A", "V", "M", Seq("CY"), "CY-1", false, Seq()),
      ("Y", "A", "V", "M", Seq("CY"), "CY-1", false, Seq()),
      ("N", "A", "V", "M", Seq("CY"), "CY", true, Seq("CY")),
      ("Y", "A", "V", "M", Seq("CY"), "CY", true, Seq("CY")),
      ("N", "A", "V", "M", Seq("CY"), "CY+1", false, Seq()),
      ("Y", "A", "V", "M", Seq("CY"), "CY+1", false, Seq()),
      ("N", "A", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("Y", "A", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("N", "A", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY")),
      ("Y", "A", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY")),
      ("N", "A", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", "A", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", "A", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("Y", "A", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("N", "A", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1")),
      ("Y", "A", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1")),
      ("N", "A", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", "A", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", "A", "V", "A", Seq("CY"), "CY-1", false, Seq()),
      ("Y", "A", "V", "A", Seq("CY"), "CY-1", false, Seq()),
      ("N", "A", "V", "A", Seq("CY"), "CY", true, Seq("CY")),
      ("Y", "A", "V", "A", Seq("CY"), "CY", true, Seq("CY")),
      ("N", "A", "V", "A", Seq("CY"), "CY+1", false, Seq()),
      ("Y", "A", "V", "A", Seq("CY"), "CY+1", false, Seq()),
      ("N", "A", "A", "M", Seq(), "CY-1", false, Seq()),
      ("Y", "A", "A", "M", Seq(), "CY-1", false, Seq()),
      ("N", "A", "A", "M", Seq(), "CY", false, Seq()),
      ("Y", "A", "A", "M", Seq(), "CY", false, Seq()),
      ("N", "A", "A", "M", Seq(), "CY+1", false, Seq()),
      ("Y", "A", "A", "M", Seq(), "CY+1", false, Seq()),
      ("N", "A", "A", " ", Seq(), "CY-1", false, Seq()),
      ("Y", "A", "A", " ", Seq(), "CY-1", false, Seq()),
      ("N", "A", "A", " ", Seq(), "CY", false, Seq()),
      ("Y", "A", "A", " ", Seq(), "CY", false, Seq()),
      ("N", "A", "A", " ", Seq(), "CY+1", false, Seq()),
      ("Y", "A", "A", " ", Seq(), "CY+1", false, Seq()),
      ("N", "A", "A", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("Y", "A", "A", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("N", "A", "A", "V", Seq("CY+1"), "CY", false, Seq()),
      ("Y", "A", "A", "V", Seq("CY+1"), "CY", false, Seq()),
      ("N", "A", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", "A", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", "A", "A", "A", Seq(), "CY-1", false, Seq()),
      ("Y", "A", "A", "A", Seq(), "CY-1", false, Seq()),
      ("N", "A", "A", "A", Seq(), "CY", false, Seq()),
      ("Y", "A", "A", "A", Seq(), "CY", false, Seq()),
      ("N", "A", "A", "A", Seq(), "CY+1", false, Seq()),
      ("Y", "A", "A", "A", Seq(), "CY+1", false, Seq()),
      ("N", " ", "M", "M", Seq(), "CY-1", false, Seq()),
      ("Y", " ", "M", "M", Seq(), "CY-1", false, Seq()),
      ("N", " ", "M", "M", Seq(), "CY", false, Seq()),
      ("Y", " ", "M", "M", Seq(), "CY", false, Seq()),
      ("N", " ", "M", "M", Seq(), "CY+1", false, Seq()),
      ("Y", " ", "M", "M", Seq(), "CY+1", false, Seq()),
      ("N", " ", "M", " ", Seq(), "CY-1", false, Seq()),
      ("Y", " ", "M", " ", Seq(), "CY-1", false, Seq()),
      ("N", " ", "M", " ", Seq(), "CY", false, Seq()),
      ("Y", " ", "M", " ", Seq(), "CY", false, Seq()),
      ("N", " ", "M", " ", Seq(), "CY+1", false, Seq()),
      ("Y", " ", "M", " ", Seq(), "CY+1", false, Seq()),
      ("N", " ", "M", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("Y", " ", "M", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("N", " ", "M", "V", Seq("CY+1"), "CY", false, Seq()),
      ("Y", " ", "M", "V", Seq("CY+1"), "CY", false, Seq()),
      ("N", " ", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", " ", "M", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", " ", "M", "A", Seq(), "CY-1", false, Seq()),
      ("Y", " ", "M", "A", Seq(), "CY-1", false, Seq()),
      ("N", " ", "M", "A", Seq(), "CY", false, Seq()),
      ("Y", " ", "M", "A", Seq(), "CY", false, Seq()),
      ("N", " ", "M", "A", Seq(), "CY+1", false, Seq()),
      ("Y", " ", "M", "A", Seq(), "CY+1", false, Seq()),
      ("N", " ", "V", "M", Seq("CY"), "CY-1", false, Seq()),
      ("Y", " ", "V", "M", Seq("CY"), "CY-1", false, Seq()),
      ("N", " ", "V", "M", Seq("CY"), "CY", true, Seq("CY")),
      ("Y", " ", "V", "M", Seq("CY"), "CY", true, Seq("CY")),
      ("N", " ", "V", "M", Seq("CY"), "CY+1", false, Seq()),
      ("Y", " ", "V", "M", Seq("CY"), "CY+1", false, Seq()),
      ("N", " ", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("Y", " ", "V", " ", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("N", " ", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY")),
      ("Y", " ", "V", " ", Seq("CY", "CY+1"), "CY", true, Seq("CY")),
      ("N", " ", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", " ", "V", " ", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", " ", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("Y", " ", "V", "V", Seq("CY", "CY+1"), "CY-1", false, Seq()),
      ("N", " ", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1")),
      ("Y", " ", "V", "V", Seq("CY", "CY+1"), "CY", true, Seq("CY", "CY+1")),
      ("N", " ", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", " ", "V", "V", Seq("CY", "CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", " ", "V", "A", Seq("CY"), "CY-1", false, Seq()),
      ("Y", " ", "V", "A", Seq("CY"), "CY-1", false, Seq()),
      ("N", " ", "V", "A", Seq("CY"), "CY", true, Seq("CY")),
      ("Y", " ", "V", "A", Seq("CY"), "CY", true, Seq("CY")),
      ("N", " ", "V", "A", Seq("CY"), "CY+1", false, Seq()),
      ("Y", " ", "V", "A", Seq("CY"), "CY+1", false, Seq()),
      ("N", " ", "A", "M", Seq(), "CY-1", false, Seq()),
      ("Y", " ", "A", "M", Seq(), "CY-1", false, Seq()),
      ("N", " ", "A", "M", Seq(), "CY", false, Seq()),
      ("Y", " ", "A", "M", Seq(), "CY", false, Seq()),
      ("N", " ", "A", "M", Seq(), "CY+1", false, Seq()),
      ("Y", " ", "A", "M", Seq(), "CY+1", false, Seq()),
      ("N", " ", "A", " ", Seq(), "CY-1", false, Seq()),
      ("Y", " ", "A", " ", Seq(), "CY-1", false, Seq()),
      ("N", " ", "A", " ", Seq(), "CY", false, Seq()),
      ("Y", " ", "A", " ", Seq(), "CY", false, Seq()),
      ("N", " ", "A", " ", Seq(), "CY+1", false, Seq()),
      ("Y", " ", "A", " ", Seq(), "CY+1", false, Seq()),
      ("N", " ", "A", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("Y", " ", "A", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("N", " ", "A", "V", Seq("CY+1"), "CY", false, Seq()),
      ("Y", " ", "A", "V", Seq("CY+1"), "CY", false, Seq()),
      ("N", " ", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", " ", "A", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", " ", "A", "A", Seq(), "CY-1", false, Seq()),
      ("Y", " ", "A", "A", Seq(), "CY-1", false, Seq()),
      ("N", " ", "A", "A", Seq(), "CY", false, Seq()),
      ("Y", " ", "A", "A", Seq(), "CY", false, Seq()),
      ("N", " ", "A", "A", Seq(), "CY+1", false, Seq()),
      ("Y", " ", "A", "A", Seq(), "CY+1", false, Seq()),
      ("N", " ", " ", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("Y", " ", " ", "V", Seq("CY+1"), "CY+1", true, Seq("CY+1")),
      ("N", " ", " ", "M", Seq(), "CY+1", false, Seq()),
      ("Y", " ", " ", "M", Seq(), "CY+1", false, Seq()),
      ("N", " ", " ", " ", Seq(), "CY", false, Seq()),
      ("N", " ", " ", " ", Seq(), "CY+1", false, Seq()),
      ("N", " ", " ", " ", Seq(), "CY-1", false, Seq()),
      ("N", " ", " ", "A", Seq(), "CY", false, Seq()),
      ("N", " ", " ", "A", Seq(), "CY+1", false, Seq()),
      ("N", " ", " ", "A", Seq(), "CY-1", false, Seq()),
      ("N", " ", " ", "M", Seq(), "CY", false, Seq()),
      ("N", " ", " ", "M", Seq(), "CY-1", false, Seq()),
      ("N", " ", " ", "V", Seq("CY+1"), "CY", false, Seq()),
      ("N", " ", " ", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("N", "A", " ", " ", Seq(), "CY", false, Seq()),
      ("N", "A", " ", " ", Seq(), "CY+1", false, Seq()),
      ("N", "A", " ", " ", Seq(), "CY-1", false, Seq()),
      ("N", "A", " ", "A", Seq(), "CY", false, Seq()),
      ("N", "A", " ", "A", Seq(), "CY+1", false, Seq()),
      ("N", "A", " ", "A", Seq(), "CY-1", false, Seq()),
      ("N", "A", " ", "M", Seq(), "CY", false, Seq()),
      ("N", "A", " ", "M", Seq(), "CY+1", false, Seq()),
      ("N", "A", " ", "M", Seq(), "CY-1", false, Seq()),
      ("N", "A", " ", "V", Seq("CY+1"), "CY", false, Seq()),
      ("N", "A", " ", "V", Seq("CY+1"), "CY+1", false, Seq("CY+1")),
      ("N", "A", " ", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("N", "M", " ", " ", Seq(), "CY", false, Seq()),
      ("N", "M", " ", " ", Seq(), "CY+1", false, Seq()),
      ("N", "M", " ", " ", Seq(), "CY-1", false, Seq()),
      ("N", "M", " ", "A", Seq(), "CY", false, Seq()),
      ("N", "M", " ", "A", Seq(), "CY+1", false, Seq()),
      ("N", "M", " ", "A", Seq(), "CY-1", false, Seq()),
      ("N", "M", " ", "M", Seq(), "CY", false, Seq()),
      ("N", "M", " ", "M", Seq(), "CY+1", false, Seq()),
      ("N", "M", " ", "M", Seq(), "CY-1", false, Seq()),
      ("N", "M", " ", "V", Seq("CY+1"), "CY", false, Seq()),
      ("N", "M", " ", "V", Seq("CY+1"), "CY+1", false, Seq("CY+1")),
      ("N", "M", " ", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("N", "V", " ", " ", Seq("CY-1"), "CY", false, Seq()),
      ("N", "V", " ", " ", Seq("CY-1"), "CY+1", false, Seq()),
      ("N", "V", " ", " ", Seq("CY-1"), "CY-1", false, Seq("CY-1")),
      ("N", "V", " ", "A", Seq("CY-1"), "CY", false, Seq()),
      ("N", "V", " ", "A", Seq("CY-1"), "CY+1", false, Seq()),
      ("N", "V", " ", "A", Seq("CY-1"), "CY-1", false, Seq("CY-1")),
      ("N", "V", " ", "M", Seq("CY-1"), "CY", false, Seq()),
      ("N", "V", " ", "M", Seq("CY-1"), "CY+1", false, Seq()),
      ("N", "V", " ", "M", Seq("CY-1"), "CY-1", false, Seq("CY-1")),
      ("N", "V", " ", "V", Seq("CY-1", "CY+1"), "CY", false, Seq()),
      ("N", "V", " ", "V", Seq("CY-1", "CY+1"), "CY+1", false, Seq("CY+1")),
      ("N", "V", " ", "V", Seq("CY-1", "CY+1"), "CY-1", false, Seq("CY-1", "CY+1")),
      ("Y", " ", " ", " ", Seq(), "CY", false, Seq()),
      ("Y", " ", " ", " ", Seq(), "CY+1", false, Seq()),
      ("Y", " ", " ", " ", Seq(), "CY-1", false, Seq()),
      ("Y", " ", " ", "A", Seq(), "CY", false, Seq()),
      ("Y", " ", " ", "A", Seq(), "CY+1", false, Seq()),
      ("Y", " ", " ", "A", Seq(), "CY-1", false, Seq()),
      ("Y", " ", " ", "M", Seq(), "CY", false, Seq()),
      ("Y", " ", " ", "M", Seq(), "CY-1", false, Seq()),
      ("Y", " ", " ", "V", Seq("CY+1"), "CY", false, Seq()),
      ("Y", " ", " ", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("Y", "A", " ", " ", Seq(), "CY", false, Seq()),
      ("Y", "A", " ", " ", Seq(), "CY+1", false, Seq()),
      ("Y", "A", " ", " ", Seq(), "CY-1", false, Seq()),
      ("Y", "A", " ", "A", Seq(), "CY", false, Seq()),
      ("Y", "A", " ", "A", Seq(), "CY+1", false, Seq()),
      ("Y", "A", " ", "A", Seq(), "CY-1", false, Seq()),
      ("Y", "A", " ", "M", Seq(), "CY", false, Seq()),
      ("Y", "A", " ", "M", Seq(), "CY+1", false, Seq()),
      ("Y", "A", " ", "M", Seq(), "CY-1", false, Seq()),
      ("Y", "A", " ", "V", Seq("CY+1"), "CY", false, Seq()),
      ("Y", "A", " ", "V", Seq("CY+1"), "CY+1", false, Seq("CY+1")),
      ("Y", "A", " ", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("Y", "M", " ", " ", Seq(), "CY", false, Seq()),
      ("Y", "M", " ", " ", Seq(), "CY+1", false, Seq()),
      ("Y", "M", " ", " ", Seq(), "CY-1", false, Seq()),
      ("Y", "M", " ", "A", Seq(), "CY", false, Seq()),
      ("Y", "M", " ", "A", Seq(), "CY+1", false, Seq()),
      ("Y", "M", " ", "A", Seq(), "CY-1", false, Seq()),
      ("Y", "M", " ", "M", Seq(), "CY", false, Seq()),
      ("Y", "M", " ", "M", Seq(), "CY+1", false, Seq()),
      ("Y", "M", " ", "M", Seq(), "CY-1", false, Seq()),
      ("Y", "M", " ", "V", Seq("CY+1"), "CY", false, Seq()),
      ("Y", "M", " ", "V", Seq("CY+1"), "CY+1", false, Seq("CY+1")),
      ("Y", "M", " ", "V", Seq("CY+1"), "CY-1", false, Seq()),
      ("Y", "V", " ", " ", Seq(), "CY", false, Seq()),
      ("Y", "V", " ", " ", Seq(), "CY+1", false, Seq()),
      ("Y", "V", " ", " ", Seq(), "CY-1", false, Seq()),
      ("Y", "V", " ", "A", Seq(), "CY", false, Seq()),
      ("Y", "V", " ", "A", Seq(), "CY+1", false, Seq()),
      ("Y", "V", " ", "A", Seq(), "CY-1", false, Seq()),
      ("Y", "V", " ", "M", Seq(), "CY", false, Seq()),
      ("Y", "V", " ", "M", Seq(), "CY+1", false, Seq()),
      ("Y", "V", " ", "M", Seq(), "CY-1", false, Seq()),
      ("Y", "V", " ", "V", Seq("CY+1"), "CY", false, Seq()),
      ("Y", "V", " ", "V", Seq("CY+1"), "CY+1", false, Seq("CY+1")),
      ("Y", "V", " ", "V", Seq("CY+1"), "CY-1", false, Seq()),
    )

  "check all required scenarios" in {
    forAll(scenarios) { (isCrystallised: String,
                         pyStatus: String,
                         cyStatus: String,
                         nyStatus: String,
                         expectedTaxYearsOffered: Seq[String],
                         customerIntent: String,
                         valid: Boolean,
                         expectedTaxYearsOptedOut: Seq[String]
                        ) =>

      val previousYear = PreviousOptOutTaxYear(toITSAStatus(pyStatus), taxYear = previousTaxYear, isCrystallised == "Y")
      val currentYear = CurrentOptOutTaxYear(toITSAStatus(cyStatus), taxYear = currentTaxYear)
      val nextYear = NextOptOutTaxYear(toITSAStatus(nyStatus), taxYear = nextTaxYear, currentTaxYear = currentYear)
      val optOutProposition = OptOutProposition(previousYear, currentYear, nextYear)

      testOptOutScenario(optOutProposition,
        expectedTaxYearsOffered.map(toTaxYear),
        valid,
        toTaxYear(customerIntent),
        expectedTaxYearsOptedOut.map(toTaxYear)
      )
    }
  }

  private def testOptOutScenario(optOutProposition: OptOutProposition,
                                 expectedTaxYearsOffered: Seq[TaxYear],
                                 valid: Boolean,
                                 customerIntent: TaxYear,
                                 expectedTaxYearsOptedOut: Seq[TaxYear]) = {

    assert(optOutProposition.availableTaxYearsForOptOut === expectedTaxYearsOffered)

    if (valid) {
      assert(optOutProposition.optOutYearsToUpdate(customerIntent) === expectedTaxYearsOptedOut)
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
