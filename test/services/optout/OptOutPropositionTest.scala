package services.optout

import testUtils.UnitSpec

import scala.io.Source

class OptOutPropositionTest extends UnitSpec {

  //TODO name well
  "Parse opt out scenarios from tsv version" should {
    "return formatted scenarios" when {
      "read" in {
        //TODO comment on how to generate tsv file

        val tsvOptOut = Source.fromFile("OptOutReduced.tsv")

        val lines = tsvOptOut.getLines().drop(2)


        def parseOptionsPresented(option: String): Seq[String] = {
          option match {
            case "Nothing displayed" => Seq()
            case _ => option.replaceAll("\"", "").split("( and |, )").toSeq
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

        // Print the extracted data
        lines.foreach(line => {
          val cells = line.split("\t").take(7)
         //println(cells.mkString("  --  "))
          println(s"(\"${cells(0)}\", \"${cells(1)}\", \"${cells(2)}\", \"${cells(3)}\", ${formatOptionPresented(parseOptionsPresented(cells(4)))}, ${parseCustomerIntent(cells(5))}, ${parseIsValid(cells(6))}),")
        })

        // Close the CSV file
        tsvOptOut.close()
      }
    }
  }


}
