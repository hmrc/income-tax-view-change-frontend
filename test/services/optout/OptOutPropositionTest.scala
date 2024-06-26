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
import testUtils.UnitSpec

import scala.io.Source

class OptOutPropositionTest extends UnitSpec {

  val taxYear: TaxYear = TaxYear.forYearEnd(2021)
  val previousTaxYear: TaxYear = taxYear.previousYear
  val nextTaxYear: TaxYear = taxYear.nextYear

  //TODO name well
  "Parse opt out scenarios from tsv version" should {
    "return formatted scenarios" when {
      //TODO comment on how to generate tsv file

      val tsvOptOut = Source.fromFile("OptOutReduced.tsv")

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
  }
}
