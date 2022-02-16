/*
 * Copyright 2017 HM Revenue & Customs
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
package testConstants

import models.liabilitycalculation.LiabilityCalculationResponse
import play.api.libs.json._

object NewCalcDataIntegrationTestConstants {

  def getLiabilityCalcResponse(json: String): LiabilityCalculationResponse = {
    Json.fromJson[LiabilityCalculationResponse](Json.parse(json)) match {
      case JsSuccess(value, path) => value
      case _ =>
        println("oculd not parse the json: " + json)
        throw new Exception("invalid json, parse error")
    }
  }

  val liabilityCalculationMinimal = getLiabilityCalcResponse(
  """
      |{
      |  "inputs": {
      |    "personalInformation": {
      |      "taxRegime": "UK"
      |    }
      |  },
      |  "metadata" : {
      |    "calculationTimestamp" : "2019-02-15T09:35:15.094Z",
      |    "crystallised" : true
      |  },
      |  "calculation": {
      |  }
      |}
      |""".stripMargin)

  val liabilityCalculationGiftAid = getLiabilityCalcResponse(
    """
      |{
      |  "inputs": {
      |    "personalInformation": {
      |      "taxRegime": "UK"
      |    }
      |  },
      |  "metadata" : {
      |    "calculationTimestamp" : "2019-02-15T09:35:15.094Z",
      |    "crystallised" : true
      |  },
      |  "calculation": {
      |    "giftAid": {
      |      "grossGiftAidPayments": 12345,
      |      "giftAidTax": 5000.99
      |    }
      |  }
      |}
      |""".stripMargin)

  val liabilityCalculationPensionLumpSums = getLiabilityCalcResponse(
    """
      |{
      |  "inputs": {
      |    "personalInformation": {
      |      "taxRegime": "UK"
      |    }
      |  },
      |  "metadata" : {
      |    "calculationTimestamp" : "2019-02-15T09:35:15.094Z",
      |    "crystallised" : true
      |  },
      |  "calculation": {
      |    "taxCalculation": {
      |      "incomeTax": {
      |        "totalIncomeReceivedFromAllSources": 1234,
      |        "totalAllowancesAndDeductions": 1234,
      |        "totalTaxableIncome": 1234,
      |        "statePensionLumpSumCharges": 5000.99
      |      },
      |      "totalIncomeTaxAndNicsDue": 12345
      |    }
      |  }
      |}
      |""".stripMargin)

  val liabilityCalculationPensionSavings = getLiabilityCalcResponse(
    """
      |{
      |  "inputs": {
      |    "personalInformation": {
      |      "taxRegime": "UK"
      |    }
      |  },
      |  "metadata" : {
      |    "calculationTimestamp" : "2019-02-15T09:35:15.094Z",
      |    "crystallised" : true
      |  },
      |  "calculation": {
      |    "taxCalculation": {
      |      "incomeTax": {
      |        "totalIncomeReceivedFromAllSources": 1234,
      |        "totalAllowancesAndDeductions": 1234,
      |        "totalTaxableIncome": 1234,
      |        "totalPensionSavingsTaxCharges": 5000.99
      |      },
      |      "totalIncomeTaxAndNicsDue": 12345
      |    }
      |  }
      |}
      |""".stripMargin)

  val liabilityCalculationVoluntaryClass2Nic = getLiabilityCalcResponse(
    """
      |{
      |  "inputs": {
      |    "personalInformation": {
      |      "taxRegime": "UK",
      |      "class2VoluntaryContributions": true
      |    }
      |  },
      |  "metadata" : {
      |    "calculationTimestamp" : "2019-02-15T09:35:15.094Z",
      |    "crystallised" : true
      |  },
      |  "calculation": {
      |    "taxCalculation": {
      |      "incomeTax": {
      |        "totalIncomeReceivedFromAllSources": 1234,
      |        "totalAllowancesAndDeductions": 1234,
      |        "totalTaxableIncome": 1234
      |      },
      |      "nics": {
      |        "class2Nics": {
      |          "amount": 5001.11
      |        }
      |      },
      |      "totalIncomeTaxAndNicsDue": 12345
      |    }
      |  }
      |}
      |""".stripMargin)

  val liabilityCalculationNonVoluntaryClass2Nic = getLiabilityCalcResponse(
    """
      |{
      |  "inputs": {
      |    "personalInformation": {
      |      "taxRegime": "UK"
      |    }
      |  },
      |  "metadata" : {
      |    "calculationTimestamp" : "2019-02-15T09:35:15.094Z",
      |    "crystallised" : true
      |  },
      |  "calculation": {
      |    "taxCalculation": {
      |      "incomeTax": {
      |        "totalIncomeReceivedFromAllSources": 1234,
      |        "totalAllowancesAndDeductions": 1234,
      |        "totalTaxableIncome": 1234
      |      },
      |      "nics": {
      |        "class2Nics": {
      |          "amount": 5001.11
      |        }
      |      },
      |      "totalIncomeTaxAndNicsDue": 12345
      |    }
      |  }
      |}
      |""".stripMargin)

}
