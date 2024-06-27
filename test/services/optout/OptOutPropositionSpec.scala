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

          Selected first 9 columns of Opt Out Scenarios Spreadsheet (currently v15).
            - to avoid cells with carriage returns in to make parsing easier.
          Paste into a fresh spreadsheet to save as OptOutScenarios.tsv in project root directory.
            - csv has issues with commas within cells, therefore use tsv to allow simple parsing
          Running test generates formatted test data in console, based on the required scenarios.
            - Used to update 'scenarios' in table based test below.
       */

      val tsvOptOut = Source.fromFile("OptOutScenarios.tsv")

      val lines = tsvOptOut.getLines().drop(2)

      def parseOptionsPresented(option: String): Seq[String] = {
        option match {
          case "Nothing displayed" => Seq()
          case "N/A" => Seq()
          case _ => option.replaceAll("\"", "").split("( [aA]nd |, )").map(_.trim).toSeq
        }
      }

      def formatOptionPresented(options: Seq[String]) = {
        "Seq(" ++ options.map(option => s"\"$option\"").mkString(", ") ++ ")"
      }

      def parseIsValid(valid: String): Boolean = {
        valid == "Allowed"
      }

      def parseCustomerIntent(intent: String): String = {
        intent.replaceAll("Customer wants to opt out from ", "")
      }

      def quote(input: String): String = {
        "\"" ++ input ++ "\""
      }

      def replaceEmptyWithSpace(input: String): String = {
        if (input.isEmpty) " " else input
      }

      // Print the extracted data
      lines.foreach(line => {
        val cells = line.split("\t").take(9).map(replaceEmptyWithSpace)
        //println(cells.mkString("  --  "))
        println(s"(${quote(cells(0))}, ${quote(cells(1))}, ${quote(cells(2))}, ${quote(cells(3))}, ${formatOptionPresented(parseOptionsPresented(cells(4)))}, ${quote(parseCustomerIntent(cells(5)))}, ${parseIsValid(cells(6))}, ${formatOptionPresented(parseOptionsPresented(cells(8)))}),")
      })

      // Close the CSV file
      tsvOptOut.close()
  }

  val currentTaxYear: TaxYear = TaxYear.forYearEnd(2021)
  val previousTaxYear: TaxYear = currentTaxYear.previousYear
  val nextTaxYear: TaxYear = currentTaxYear.nextYear

  private val scenarios =
    Table(
      ("Crystallised", "CY-1", "CY", "CY+1", "Expected Opt Out years offered", "Customer intent", "Valid", "Expected Opt Out years"), // First tuple defines column names
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

      val optionsPresented = expectedTaxYearsOffered.map(option => optionToTaxYear(option))
      val expectedOptOut = expectedTaxYearsOptedOut.map(year => optionToTaxYear(year))

      val optOutYearsOffered = optOutProposition.availableTaxYearsForOptOut

      assert(optOutYearsOffered === optionsPresented)
      if (valid) {
        assert(optOutProposition.optOutYearsToUpdate(optionToTaxYear(customerIntent)).map(year => year.taxYear) === expectedOptOut)
      }
    }
  }

  def optionToTaxYear(option: String): TaxYear = {
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
