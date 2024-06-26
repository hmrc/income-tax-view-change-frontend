package services.optout

import testUtils.UnitSpec

import scala.io.Source

class OptOutPropositionTest extends UnitSpec {


  "OptOut returns correct" should {
    "csv file " when {
      "read" in {
        val csvOpt = Source.fromFile("OptOutReduced.tsv")

        val iter = csvOpt.getLines().drop(2).map(_.split("\t").take(7))

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
        iter.foreach(line => {
          println(line.mkString("  --  "))
          println(s"(\"${line(0)}\", \"${line(1)}\", \"${line(2)}\", \"${line(3)}\", ${formatOptionPresented(parseOptionsPresented(line(4)))}, ${parseCustomerIntent(line(5))}, ${parseIsValid(line(6))}),")
        })

        iter.foreach(a => a.mkString(""))

        // Close the CSV file
        csvOpt.close()
      }
    }
  }


}
