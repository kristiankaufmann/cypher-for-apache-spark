/*
 * Copyright (c) 2016-2018 "Neo4j, Inc." [https://neo4j.com]
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
package org.opencypher.caps.tck

import java.io.File

import org.opencypher.caps.cosc.impl.{COSCGraph, COSCSession}
import org.opencypher.caps.cosc.test.COSCTestSuite
import org.opencypher.caps.cosc.test.support.creation.cosc.COSCTestGraphFactory
import org.opencypher.caps.tck.Tags.{BlackList, TckCoscTag, WhiteList}
import org.opencypher.caps.test.support.creation.TestGraphFactory
import org.opencypher.tools.tck.api.CypherTCK
import org.scalatest.prop.TableDrivenPropertyChecks._

import scala.util.{Failure, Success, Try}

class TckCoscTest extends COSCTestSuite {

  val factories = Table(
    "factory",
    COSCTestGraphFactory
  )

  val defaultFactory: TestGraphFactory[COSCSession] = COSCTestGraphFactory

  private val scenarios = ScenariosFor("cosc")

  // white list tests are run on all factories
  forAll(factories) { factory =>
    forAll(scenarios.whiteList) { scenario =>
      test(s"[${factory.name}, ${WhiteList.name}] $scenario", WhiteList, TckCoscTag) {
        scenario(TCKGraph(factory, COSCGraph.empty)).execute()
      }
    }
  }

  // black list tests are run on default factory
  forAll(scenarios.blackList) { scenario =>
    test(s"[${defaultFactory.name}, ${BlackList.name}] $scenario", BlackList, TckCoscTag) {
      val tckGraph = TCKGraph(defaultFactory, COSCGraph.empty)

      Try(scenario(tckGraph).execute()) match {
        case Success(_) =>
          throw new RuntimeException(s"A blacklisted scenario actually worked: $scenario")
        case Failure(_) =>
          ()
      }
    }
  }

  ignore("run Custom Scenario") {
    val file = new File(getClass.getResource("CustomTest.feature").toURI)

    CypherTCK
      .parseFilesystemFeature(file)
      .scenarios
      .foreach(scenario => scenario(TCKGraph(defaultFactory, COSCGraph.empty)).execute())
  }

  ignore("run Single Scenario") {
    scenarios.get("A simple pattern with one bound endpoint")
      .foreach(scenario => scenario(TCKGraph(defaultFactory, COSCGraph.empty)).execute())
  }
}