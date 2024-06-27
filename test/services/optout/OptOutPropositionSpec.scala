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
          case _ => option.replaceAll("\"", "").split("( and |, )").map(_.trim).toSeq
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
        val cells = line.split("\t").take(7).map(replaceEmptyWithSpace)
        //println(cells.mkString("  --  "))
        println(s"(${quote(cells(0))}, ${quote(cells(1))}, ${quote(cells(2))}, ${quote(cells(3))}, ${formatOptionPresented(parseOptionsPresented(cells(4)))}, ${quote(parseCustomerIntent(cells(5)))}, ${parseIsValid(cells(6))}),")
      })

      // Close the CSV file
      tsvOptOut.close()
  }

  val taxYear: TaxYear = TaxYear.forYearEnd(2021)
  val previousTaxYear: TaxYear = taxYear.previousYear
  val nextTaxYear: TaxYear = taxYear.nextYear

  private val scenarios =
    Table(
      ("Crystallised", "CY-1", "CY", "CY+1", "Outcome", "Customer intent", "Valid"), // First tuple defines column names
      ("N", "M", "M", "M", Seq(), "CY-1", false),
      ("Y", "M", "M", "M", Seq(), "CY-1", false),
      ("N", "M", "M", "M", Seq(), "CY", false),
      ("Y", "M", "M", "M", Seq(), "CY", false),
      ("N", "M", "M", "M", Seq(), "CY+1", false),
      ("Y", "M", "M", "M", Seq(), "CY+1", false),
      ("N", "M", "M", " ", Seq(), "CY-1", false),
      ("Y", "M", "M", " ", Seq(), "CY-1", false),
      ("N", "M", "M", " ", Seq(), "CY", false),
      ("Y", "M", "M", " ", Seq(), "CY", false),
      ("N", "M", "M", " ", Seq(), "CY+1", false),
      ("Y", "M", "M", " ", Seq(), "CY+1", false),
      ("N", "M", "M", "V", Seq("CY+1"), "CY-1", false),
      ("Y", "M", "M", "V", Seq("CY+1"), "CY-1", false),
      ("N", "M", "M", "V", Seq("CY+1"), "CY", false),
      ("Y", "M", "M", "V", Seq("CY+1"), "CY", false),
      ("N", "M", "M", "V", Seq("CY+1"), "CY+1", true),
      ("Y", "M", "M", "V", Seq("CY+1"), "CY+1", true),
      ("N", "M", "M", "A", Seq(), "CY-1", false),
      ("Y", "M", "M", "A", Seq(), "CY-1", false),
      ("N", "M", "M", "A", Seq(), "CY", false),
      ("Y", "M", "M", "A", Seq(), "CY", false),
      ("N", "M", "M", "A", Seq(), "CY+1", false),
      ("Y", "M", "M", "A", Seq(), "CY+1", false),
      ("N", "M", "V", "M", Seq("CY"), "CY-1", false),
      ("Y", "M", "V", "M", Seq("CY"), "CY-1", false),
      ("N", "M", "V", "M", Seq("CY"), "CY", true),
      ("Y", "M", "V", "M", Seq("CY"), "CY", true),
      ("N", "M", "V", "M", Seq("CY"), "CY+1", false),
      ("Y", "M", "V", "M", Seq("CY"), "CY+1", false),
      ("N", "M", "V", " ", Seq("CY", "CY+1"), "CY-1", false),
      ("Y", "M", "V", " ", Seq("CY", "CY+1"), "CY-1", false),
      ("N", "M", "V", " ", Seq("CY", "CY+1"), "CY", true),
      ("Y", "M", "V", " ", Seq("CY", "CY+1"), "CY", true),
      ("N", "M", "V", " ", Seq("CY", "CY+1"), "CY+1", true),
      ("Y", "M", "V", " ", Seq("CY", "CY+1"), "CY+1", true),
      ("N", "M", "V", "V", Seq("CY", "CY+1"), "CY-1", false),
      ("Y", "M", "V", "V", Seq("CY", "CY+1"), "CY-1", false),
      ("N", "M", "V", "V", Seq("CY", "CY+1"), "CY", true),
      ("Y", "M", "V", "V", Seq("CY", "CY+1"), "CY", true),
      ("N", "M", "V", "V", Seq("CY", "CY+1"), "CY+1", true),
      ("Y", "M", "V", "V", Seq("CY", "CY+1"), "CY+1", true),
      ("N", "M", "V", "A", Seq("CY"), "CY-1", false),
      ("Y", "M", "V", "A", Seq("CY"), "CY-1", false),
      ("N", "M", "V", "A", Seq("CY"), "CY", true),
      ("Y", "M", "V", "A", Seq("CY"), "CY", true),
      ("N", "M", "V", "A", Seq("CY"), "CY+1", false),
      ("Y", "M", "V", "A", Seq("CY"), "CY+1", false),
      ("N", "M", "A", "M", Seq(), "CY-1", false),
      ("Y", "M", "A", "M", Seq(), "CY-1", false),
      ("N", "M", "A", "M", Seq(), "CY", false),
      ("Y", "M", "A", "M", Seq(), "CY", false),
      ("N", "M", "A", "M", Seq(), "CY+1", false),
      ("Y", "M", "A", "M", Seq(), "CY+1", false),
      ("N", "M", "A", " ", Seq(), "CY-1", false),
      ("Y", "M", "A", " ", Seq(), "CY-1", false),
      ("N", "M", "A", " ", Seq(), "CY", false),
      ("Y", "M", "A", " ", Seq(), "CY", false),
      ("N", "M", "A", " ", Seq(), "CY+1", false),
      ("Y", "M", "A", " ", Seq(), "CY+1", false),
      ("N", "M", "A", "V", Seq("CY+1"), "CY-1", false),
      ("Y", "M", "A", "V", Seq("CY+1"), "CY-1", false),
      ("N", "M", "A", "V", Seq("CY+1"), "CY", false),
      ("Y", "M", "A", "V", Seq("CY+1"), "CY", false),
      ("N", "M", "A", "V", Seq("CY+1"), "CY+1", true),
      ("Y", "M", "A", "V", Seq("CY+1"), "CY+1", true),
      ("N", "M", "A", "A", Seq(), "CY-1", false),
      ("Y", "M", "A", "A", Seq(), "CY-1", false),
      ("N", "M", "A", "A", Seq(), "CY", false),
      ("Y", "M", "A", "A", Seq(), "CY", false),
      ("N", "M", "A", "A", Seq(), "CY+1", false),
      ("Y", "M", "A", "A", Seq(), "CY+1", false),
      ("N", "V", "M", "M", Seq("CY-1"), "CY-1", true),
      ("Y", "V", "M", "M", Seq(), "CY-1", false),
      ("N", "V", "M", "M", Seq("CY-1"), "CY", false),
      ("Y", "V", "M", "M", Seq(), "CY", false),
      ("N", "V", "M", "M", Seq("CY-1"), "CY+1", false),
      ("Y", "V", "M", "M", Seq(), "CY+1", false),
      ("N", "V", "M", " ", Seq("CY-1"), "CY-1", true),
      ("Y", "V", "M", " ", Seq(), "CY-1", false),
      ("N", "V", "M", " ", Seq("CY-1"), "CY", false),
      ("Y", "V", "M", " ", Seq(), "CY", false),
      ("N", "V", "M", " ", Seq("CY-1"), "CY+1", false),
      ("Y", "V", "M", " ", Seq(), "CY+1", false),
      ("N", "V", "M", "V", Seq("CY-1", "CY+1"), "CY-1", true),
      ("Y", "V", "M", "V", Seq("CY+1"), "CY-1", false),
      ("N", "V", "M", "V", Seq("CY-1", "CY+1"), "CY", false),
      ("Y", "V", "M", "V", Seq("CY+1"), "CY", false),
      ("N", "V", "M", "V", Seq("CY-1", "CY+1"), "CY+1", true),
      ("Y", "V", "M", "V", Seq("CY+1"), "CY+1", true),
      ("N", "V", "M", "A", Seq("CY-1"), "CY-1", true),
      ("Y", "V", "M", "A", Seq(), "CY-1", false),
      ("N", "V", "M", "A", Seq("CY-1"), "CY", false),
      ("Y", "V", "M", "A", Seq(), "CY", false),
      ("N", "V", "M", "A", Seq("CY-1"), "CY+1", false),
      ("Y", "V", "M", "A", Seq(), "CY+1", false),
      ("N", "V", "V", "M", Seq("CY-1", "CY"), "CY-1", true),
      ("Y", "V", "V", "M", Seq("CY"), "CY-1", false),
      ("N", "V", "V", "M", Seq("CY-1", "CY"), "CY", true),
      ("Y", "V", "V", "M", Seq("CY"), "CY", true),
      ("N", "V", "V", "M", Seq("CY-1", "CY"), "CY+1", false),
      ("Y", "V", "V", "M", Seq("CY"), "CY+1", false),
      ("N", "V", "V", " ", Seq("CY-1", "CY", "CY+1"), "CY-1", true),
      ("Y", "V", "V", " ", Seq("CY", "CY+1"), "CY-1", false),
      ("N", "V", "V", " ", Seq("CY-1", "CY", "CY+1"), "CY", true),
      ("Y", "V", "V", " ", Seq("CY", "CY+1"), "CY", true),
      ("N", "V", "V", " ", Seq("CY-1", "CY", "CY+1"), "CY+1", true),
      ("Y", "V", "V", " ", Seq("CY", "CY+1"), "CY+1", true),
      ("N", "V", "V", "V", Seq("CY-1", "CY", "CY+1"), "CY-1", true),
      ("Y", "V", "V", "V", Seq("CY", "CY+1"), "CY-1", false),
      ("N", "V", "V", "V", Seq("CY-1", "CY", "CY+1"), "CY", true),
      ("Y", "V", "V", "V", Seq("CY", "CY+1"), "CY", true),
      ("N", "V", "V", "V", Seq("CY-1", "CY", "CY+1"), "CY+1", true),
      ("Y", "V", "V", "V", Seq("CY", "CY+1"), "CY+1", true),
      ("N", "V", "V", "A", Seq("CY-1", "CY"), "CY-1", true),
      ("Y", "V", "V", "A", Seq("CY"), "CY-1", false),
      ("N", "V", "V", "A", Seq("CY-1", "CY"), "CY", true),
      ("Y", "V", "V", "A", Seq("CY"), "CY", true),
      ("N", "V", "V", "A", Seq("CY-1", "CY"), "CY+1", false),
      ("Y", "V", "V", "A", Seq("CY"), "CY+1", false),
      ("N", "V", "A", "M", Seq("CY-1"), "CY-1", true),
      ("Y", "V", "A", "M", Seq(), "CY-1", false),
      ("N", "V", "A", "M", Seq("CY-1"), "CY", false),
      ("Y", "V", "A", "M", Seq(), "CY", false),
      ("N", "V", "A", "M", Seq("CY-1"), "CY+1", false),
      ("Y", "V", "A", "M", Seq(), "CY+1", false),
      ("N", "V", "A", " ", Seq("CY-1"), "CY-1", true),
      ("Y", "V", "A", " ", Seq(), "CY-1", false),
      ("N", "V", "A", " ", Seq("CY-1"), "CY", false),
      ("Y", "V", "A", " ", Seq(), "CY", false),
      ("N", "V", "A", " ", Seq("CY-1"), "CY+1", false),
      ("Y", "V", "A", " ", Seq(), "CY+1", false),
      ("N", "V", "A", "V", Seq("CY-1", "CY+1"), "CY-1", true),
      ("Y", "V", "A", "V", Seq("CY+1"), "CY-1", false),
      ("N", "V", "A", "V", Seq("CY-1", "CY+1"), "CY", false),
      ("Y", "V", "A", "V", Seq("CY+1"), "CY", false),
      ("N", "V", "A", "V", Seq("CY-1", "CY+1"), "CY+1", true),
      ("Y", "V", "A", "V", Seq("CY+1"), "CY+1", true),
      ("N", "V", "A", "A", Seq("CY-1"), "CY-1", true),
      ("Y", "V", "A", "A", Seq(), "CY-1", false),
      ("N", "V", "A", "A", Seq("CY-1"), "CY", false),
      ("Y", "V", "A", "A", Seq(), "CY", false),
      ("N", "V", "A", "A", Seq("CY-1"), "CY+1", false),
      ("Y", "V", "A", "A", Seq(), "CY+1", false),
      ("N", "A", "M", "M", Seq(), "CY-1", false),
      ("Y", "A", "M", "M", Seq(), "CY-1", false),
      ("N", "A", "M", "M", Seq(), "CY", false),
      ("Y", "A", "M", "M", Seq(), "CY", false),
      ("N", "A", "M", "M", Seq(), "CY+1", false),
      ("Y", "A", "M", "M", Seq(), "CY+1", false),
      ("N", "A", "M", " ", Seq(), "CY-1", false),
      ("Y", "A", "M", " ", Seq(), "CY-1", false),
      ("N", "A", "M", " ", Seq(), "CY", false),
      ("Y", "A", "M", " ", Seq(), "CY", false),
      ("N", "A", "M", " ", Seq(), "CY+1", false),
      ("Y", "A", "M", " ", Seq(), "CY+1", false),
      ("N", "A", "M", "V", Seq("CY+1"), "CY-1", false),
      ("Y", "A", "M", "V", Seq("CY+1"), "CY-1", false),
      ("N", "A", "M", "V", Seq("CY+1"), "CY", false),
      ("Y", "A", "M", "V", Seq("CY+1"), "CY", false),
      ("N", "A", "M", "V", Seq("CY+1"), "CY+1", true),
      ("Y", "A", "M", "V", Seq("CY+1"), "CY+1", true),
      ("N", "A", "M", "A", Seq(), "CY-1", false),
      ("Y", "A", "M", "A", Seq(), "CY-1", false),
      ("N", "A", "M", "A", Seq(), "CY", false),
      ("Y", "A", "M", "A", Seq(), "CY", false),
      ("N", "A", "M", "A", Seq(), "CY+1", false),
      ("Y", "A", "M", "A", Seq(), "CY+1", false),
      ("N", "A", "V", "M", Seq("CY"), "CY-1", false),
      ("Y", "A", "V", "M", Seq("CY"), "CY-1", false),
      ("N", "A", "V", "M", Seq("CY"), "CY", true),
      ("Y", "A", "V", "M", Seq("CY"), "CY", true),
      ("N", "A", "V", "M", Seq("CY"), "CY+1", false),
      ("Y", "A", "V", "M", Seq("CY"), "CY+1", false),
      ("N", "A", "V", " ", Seq("CY", "CY+1"), "CY-1", false),
      ("Y", "A", "V", " ", Seq("CY", "CY+1"), "CY-1", false),
      ("N", "A", "V", " ", Seq("CY", "CY+1"), "CY", true),
      ("Y", "A", "V", " ", Seq("CY", "CY+1"), "CY", true),
      ("N", "A", "V", " ", Seq("CY", "CY+1"), "CY+1", true),
      ("Y", "A", "V", " ", Seq("CY", "CY+1"), "CY+1", true),
      ("N", "A", "V", "V", Seq("CY", "CY+1"), "CY-1", false),
      ("Y", "A", "V", "V", Seq("CY", "CY+1"), "CY-1", false),
      ("N", "A", "V", "V", Seq("CY", "CY+1"), "CY", true),
      ("Y", "A", "V", "V", Seq("CY", "CY+1"), "CY", true),
      ("N", "A", "V", "V", Seq("CY", "CY+1"), "CY+1", true),
      ("Y", "A", "V", "V", Seq("CY", "CY+1"), "CY+1", true),
      ("N", "A", "V", "A", Seq("CY"), "CY-1", false),
      ("Y", "A", "V", "A", Seq("CY"), "CY-1", false),
      ("N", "A", "V", "A", Seq("CY"), "CY", true),
      ("Y", "A", "V", "A", Seq("CY"), "CY", true),
      ("N", "A", "V", "A", Seq("CY"), "CY+1", false),
      ("Y", "A", "V", "A", Seq("CY"), "CY+1", false),
      ("N", "A", "A", "M", Seq(), "CY-1", false),
      ("Y", "A", "A", "M", Seq(), "CY-1", false),
      ("N", "A", "A", "M", Seq(), "CY", false),
      ("Y", "A", "A", "M", Seq(), "CY", false),
      ("N", "A", "A", "M", Seq(), "CY+1", false),
      ("Y", "A", "A", "M", Seq(), "CY+1", false),
      ("N", "A", "A", " ", Seq(), "CY-1", false),
      ("Y", "A", "A", " ", Seq(), "CY-1", false),
      ("N", "A", "A", " ", Seq(), "CY", false),
      ("Y", "A", "A", " ", Seq(), "CY", false),
      ("N", "A", "A", " ", Seq(), "CY+1", false),
      ("Y", "A", "A", " ", Seq(), "CY+1", false),
      ("N", "A", "A", "V", Seq("CY+1"), "CY-1", false),
      ("Y", "A", "A", "V", Seq("CY+1"), "CY-1", false),
      ("N", "A", "A", "V", Seq("CY+1"), "CY", false),
      ("Y", "A", "A", "V", Seq("CY+1"), "CY", false),
      ("N", "A", "A", "V", Seq("CY+1"), "CY+1", true),
      ("Y", "A", "A", "V", Seq("CY+1"), "CY+1", true),
      ("N", "A", "A", "A", Seq(), "CY-1", false),
      ("Y", "A", "A", "A", Seq(), "CY-1", false),
      ("N", "A", "A", "A", Seq(), "CY", false),
      ("Y", "A", "A", "A", Seq(), "CY", false),
      ("N", "A", "A", "A", Seq(), "CY+1", false),
      ("Y", "A", "A", "A", Seq(), "CY+1", false),
      ("N", " ", "M", "M", Seq(), "CY-1", false),
      ("Y", " ", "M", "M", Seq(), "CY-1", false),
      ("N", " ", "M", "M", Seq(), "CY", false),
      ("Y", " ", "M", "M", Seq(), "CY", false),
      ("N", " ", "M", "M", Seq(), "CY+1", false),
      ("Y", " ", "M", "M", Seq(), "CY+1", false),
      ("N", " ", "M", " ", Seq(), "CY-1", false),
      ("Y", " ", "M", " ", Seq(), "CY-1", false),
      ("N", " ", "M", " ", Seq(), "CY", false),
      ("Y", " ", "M", " ", Seq(), "CY", false),
      ("N", " ", "M", " ", Seq(), "CY+1", false),
      ("Y", " ", "M", " ", Seq(), "CY+1", false),
      ("N", " ", "M", "V", Seq("CY+1"), "CY-1", false),
      ("Y", " ", "M", "V", Seq("CY+1"), "CY-1", false),
      ("N", " ", "M", "V", Seq("CY+1"), "CY", false),
      ("Y", " ", "M", "V", Seq("CY+1"), "CY", false),
      ("N", " ", "M", "V", Seq("CY+1"), "CY+1", true),
      ("Y", " ", "M", "V", Seq("CY+1"), "CY+1", true),
      ("N", " ", "M", "A", Seq(), "CY-1", false),
      ("Y", " ", "M", "A", Seq(), "CY-1", false),
      ("N", " ", "M", "A", Seq(), "CY", false),
      ("Y", " ", "M", "A", Seq(), "CY", false),
      ("N", " ", "M", "A", Seq(), "CY+1", false),
      ("Y", " ", "M", "A", Seq(), "CY+1", false),
      ("N", " ", "V", "M", Seq("CY"), "CY-1", false),
      ("Y", " ", "V", "M", Seq("CY"), "CY-1", false),
      ("N", " ", "V", "M", Seq("CY"), "CY", true),
      ("Y", " ", "V", "M", Seq("CY"), "CY", true),
      ("N", " ", "V", "M", Seq("CY"), "CY+1", false),
      ("Y", " ", "V", "M", Seq("CY"), "CY+1", false),
      ("N", " ", "V", " ", Seq("CY", "CY+1"), "CY-1", false),
      ("Y", " ", "V", " ", Seq("CY", "CY+1"), "CY-1", false),
      ("N", " ", "V", " ", Seq("CY", "CY+1"), "CY", true),
      ("Y", " ", "V", " ", Seq("CY", "CY+1"), "CY", true),
      ("N", " ", "V", " ", Seq("CY", "CY+1"), "CY+1", true),
      ("Y", " ", "V", " ", Seq("CY", "CY+1"), "CY+1", true),
      ("N", " ", "V", "V", Seq("CY", "CY+1"), "CY-1", false),
      ("Y", " ", "V", "V", Seq("CY", "CY+1"), "CY-1", false),
      ("N", " ", "V", "V", Seq("CY", "CY+1"), "CY", true),
      ("Y", " ", "V", "V", Seq("CY", "CY+1"), "CY", true),
      ("N", " ", "V", "V", Seq("CY", "CY+1"), "CY+1", true),
      ("Y", " ", "V", "V", Seq("CY", "CY+1"), "CY+1", true),
      ("N", " ", "V", "A", Seq("CY"), "CY-1", false),
      ("Y", " ", "V", "A", Seq("CY"), "CY-1", false),
      ("N", " ", "V", "A", Seq("CY"), "CY", true),
      ("Y", " ", "V", "A", Seq("CY"), "CY", true),
      ("N", " ", "V", "A", Seq("CY"), "CY+1", false),
      ("Y", " ", "V", "A", Seq("CY"), "CY+1", false),
      ("N", " ", "A", "M", Seq(), "CY-1", false),
      ("Y", " ", "A", "M", Seq(), "CY-1", false),
      ("N", " ", "A", "M", Seq(), "CY", false),
      ("Y", " ", "A", "M", Seq(), "CY", false),
      ("N", " ", "A", "M", Seq(), "CY+1", false),
      ("Y", " ", "A", "M", Seq(), "CY+1", false),
      ("N", " ", "A", " ", Seq(), "CY-1", false),
      ("Y", " ", "A", " ", Seq(), "CY-1", false),
      ("N", " ", "A", " ", Seq(), "CY", false),
      ("Y", " ", "A", " ", Seq(), "CY", false),
      ("N", " ", "A", " ", Seq(), "CY+1", false),
      ("Y", " ", "A", " ", Seq(), "CY+1", false),
      ("N", " ", "A", "V", Seq("CY+1"), "CY-1", false),
      ("Y", " ", "A", "V", Seq("CY+1"), "CY-1", false),
      ("N", " ", "A", "V", Seq("CY+1"), "CY", false),
      ("Y", " ", "A", "V", Seq("CY+1"), "CY", false),
      ("N", " ", "A", "V", Seq("CY+1"), "CY+1", true),
      ("Y", " ", "A", "V", Seq("CY+1"), "CY+1", true),
      ("N", " ", "A", "A", Seq(), "CY-1", false),
      ("Y", " ", "A", "A", Seq(), "CY-1", false),
      ("N", " ", "A", "A", Seq(), "CY", false),
      ("Y", " ", "A", "A", Seq(), "CY", false),
      ("N", " ", "A", "A", Seq(), "CY+1", false),
      ("Y", " ", "A", "A", Seq(), "CY+1", false),
      ("N", " ", " ", "V", Seq("CY+1"), "CY+1", true),
      ("Y", " ", " ", "V", Seq("CY+1"), "CY+1", true),
      ("N", " ", " ", "M", Seq(), "CY+1", false),
      ("Y", " ", " ", "M", Seq(), "CY+1", false),
      ("N", " ", " ", " ", Seq(), "CY", false),
      ("N", " ", " ", " ", Seq(), "CY+1", false),
      ("N", " ", " ", " ", Seq(), "CY-1", false),
      ("N", " ", " ", "A", Seq(), "CY", false),
      ("N", " ", " ", "A", Seq(), "CY+1", false),
      ("N", " ", " ", "A", Seq(), "CY-1", false),
      ("N", " ", " ", "M", Seq(), "CY", false),
      ("N", " ", " ", "M", Seq(), "CY-1", false),
      ("N", " ", " ", "V", Seq("CY+1"), "CY", false),
      ("N", " ", " ", "V", Seq("CY+1"), "CY-1", false),
      ("N", "A", " ", " ", Seq(), "CY", false),
      ("N", "A", " ", " ", Seq(), "CY+1", false),
      ("N", "A", " ", " ", Seq(), "CY-1", false),
      ("N", "A", " ", "A", Seq(), "CY", false),
      ("N", "A", " ", "A", Seq(), "CY+1", false),
      ("N", "A", " ", "A", Seq(), "CY-1", false),
      ("N", "A", " ", "M", Seq(), "CY", false),
      ("N", "A", " ", "M", Seq(), "CY+1", false),
      ("N", "A", " ", "M", Seq(), "CY-1", false),
      ("N", "A", " ", "V", Seq("CY+1"), "CY", false),
      ("N", "A", " ", "V", Seq("CY+1"), "CY+1", false),
      ("N", "A", " ", "V", Seq("CY+1"), "CY-1", false),
      ("N", "M", " ", " ", Seq(), "CY", false),
      ("N", "M", " ", " ", Seq(), "CY+1", false),
      ("N", "M", " ", " ", Seq(), "CY-1", false),
      ("N", "M", " ", "A", Seq(), "CY", false),
      ("N", "M", " ", "A", Seq(), "CY+1", false),
      ("N", "M", " ", "A", Seq(), "CY-1", false),
      ("N", "M", " ", "M", Seq(), "CY", false),
      ("N", "M", " ", "M", Seq(), "CY+1", false),
      ("N", "M", " ", "M", Seq(), "CY-1", false),
      ("N", "M", " ", "V", Seq("CY+1"), "CY", false),
      ("N", "M", " ", "V", Seq("CY+1"), "CY+1", false),
      ("N", "M", " ", "V", Seq("CY+1"), "CY-1", false),
      ("N", "V", " ", " ", Seq("CY-1"), "CY", false),
      ("N", "V", " ", " ", Seq("CY-1"), "CY+1", false),
      ("N", "V", " ", " ", Seq("CY-1"), "CY-1", false),
      ("N", "V", " ", "A", Seq("CY-1"), "CY", false),
      ("N", "V", " ", "A", Seq("CY-1"), "CY+1", false),
      ("N", "V", " ", "A", Seq("CY-1"), "CY-1", false),
      ("N", "V", " ", "M", Seq("CY-1"), "CY", false),
      ("N", "V", " ", "M", Seq("CY-1"), "CY+1", false),
      ("N", "V", " ", "M", Seq("CY-1"), "CY-1", false),
      ("N", "V", " ", "V", Seq("CY-1", "CY+1"), "CY", false),
      ("N", "V", " ", "V", Seq("CY-1", "CY+1"), "CY+1", false),
      ("N", "V", " ", "V", Seq("CY-1", "CY+1"), "CY-1", false),
      ("Y", " ", " ", " ", Seq(), "CY", false),
      ("Y", " ", " ", " ", Seq(), "CY+1", false),
      ("Y", " ", " ", " ", Seq(), "CY-1", false),
      ("Y", " ", " ", "A", Seq(), "CY", false),
      ("Y", " ", " ", "A", Seq(), "CY+1", false),
      ("Y", " ", " ", "A", Seq(), "CY-1", false),
      ("Y", " ", " ", "M", Seq(), "CY", false),
      ("Y", " ", " ", "M", Seq(), "CY-1", false),
      ("Y", " ", " ", "V", Seq("CY+1"), "CY", false),
      ("Y", " ", " ", "V", Seq("CY+1"), "CY-1", false),
      ("Y", "A", " ", " ", Seq(), "CY", false),
      ("Y", "A", " ", " ", Seq(), "CY+1", false),
      ("Y", "A", " ", " ", Seq(), "CY-1", false),
      ("Y", "A", " ", "A", Seq(), "CY", false),
      ("Y", "A", " ", "A", Seq(), "CY+1", false),
      ("Y", "A", " ", "A", Seq(), "CY-1", false),
      ("Y", "A", " ", "M", Seq(), "CY", false),
      ("Y", "A", " ", "M", Seq(), "CY+1", false),
      ("Y", "A", " ", "M", Seq(), "CY-1", false),
      ("Y", "A", " ", "V", Seq("CY+1"), "CY", false),
      ("Y", "A", " ", "V", Seq("CY+1"), "CY+1", false),
      ("Y", "A", " ", "V", Seq("CY+1"), "CY-1", false),
      ("Y", "M", " ", " ", Seq(), "CY", false),
      ("Y", "M", " ", " ", Seq(), "CY+1", false),
      ("Y", "M", " ", " ", Seq(), "CY-1", false),
      ("Y", "M", " ", "A", Seq(), "CY", false),
      ("Y", "M", " ", "A", Seq(), "CY+1", false),
      ("Y", "M", " ", "A", Seq(), "CY-1", false),
      ("Y", "M", " ", "M", Seq(), "CY", false),
      ("Y", "M", " ", "M", Seq(), "CY+1", false),
      ("Y", "M", " ", "M", Seq(), "CY-1", false),
      ("Y", "M", " ", "V", Seq("CY+1"), "CY", false),
      ("Y", "M", " ", "V", Seq("CY+1"), "CY+1", false),
      ("Y", "M", " ", "V", Seq("CY+1"), "CY-1", false),
      ("Y", "V", " ", " ", Seq(), "CY", false),
      ("Y", "V", " ", " ", Seq(), "CY+1", false),
      ("Y", "V", " ", " ", Seq(), "CY-1", false),
      ("Y", "V", " ", "A", Seq(), "CY", false),
      ("Y", "V", " ", "A", Seq(), "CY+1", false),
      ("Y", "V", " ", "A", Seq(), "CY-1", false),
      ("Y", "V", " ", "M", Seq(), "CY", false),
      ("Y", "V", " ", "M", Seq(), "CY+1", false),
      ("Y", "V", " ", "M", Seq(), "CY-1", false),
      ("Y", "V", " ", "V", Seq("CY+1"), "CY", false),
      ("Y", "V", " ", "V", Seq("CY+1"), "CY+1", false),
      ("Y", "V", " ", "V", Seq("CY+1"), "CY-1", false),
    )

  "check all required scenarios" in {
    forAll(scenarios) { (isCrystallised: String,
                       pyStatus: String,
                       cyStatus: String,
                       nyStatus: String,
                       optionsPresented: Seq[String],
                       customerIntent: String,
                       valid: Boolean) =>

      val previousYear = PreviousOptOutTaxYear(toITSAStatus(pyStatus), taxYear = previousTaxYear, isCrystallised == "Y")
      val currentYear = CurrentOptOutTaxYear(toITSAStatus(cyStatus), taxYear = taxYear)
      val nextYear = NextOptOutTaxYear(toITSAStatus(nyStatus), taxYear = nextTaxYear, currentTaxYear = currentYear)

      val optionPresented = optionsPresented.map(option => optionToTaxYear(option))
      val proposition = OptOutProposition(previousYear, currentYear, nextYear).availableTaxYearsForOptOut

      assert(proposition === optionPresented)
    }
  }

  def optionToTaxYear(option: String): TaxYear = {
    option match {
      case "CY-1" => previousTaxYear
      case "CY" => taxYear
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
