/*
 * Copyright (c) 2016-2018 "Neo4j Sweden, AB" [https://neo4j.com]
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
 *
 * Attribution Notice under the terms of the Apache License 2.0
 *
 * This work was created by the collective efforts of the openCypher community.
 * Without limiting the terms of Section 6, any Derivative Work that is not
 * approved by the public consensus process of the openCypher Implementers Group
 * should not be described as “Cypher” (and Cypher® is a registered trademark of
 * Neo4j Inc.) or as "openCypher". Extensions by implementers or prototypes or
 * proposals for change that have been documented or implemented should only be
 * described as "implementation extensions to Cypher" or as "proposed changes to
 * Cypher that are not yet approved by the openCypher community".
 */
package org.opencypher.spark.api

import java.nio.file.Paths

import org.opencypher.okapi.api.schema.Schema
import org.opencypher.okapi.neo4j.io.Neo4jConfig
import org.opencypher.spark.api.io.fs.{CAPSFileSystem, FSGraphSource}
import org.opencypher.spark.api.io.neo4j.{Neo4jBulkCSVDataSink, Neo4jPropertyGraphDataSource}

import scala.io.Source

object GraphSources {
  def fs(
    rootPath: String,
    customFileSystem: Option[CAPSFileSystem] = None,
    filesPerTable: Option[Int] = Some(1)
  ) = FSGraphSources(rootPath, customFileSystem, filesPerTable)

  def cypher = CypherGraphSources
}

object FSGraphSources {
  def apply(
    rootPath: String,
    customFileSystem: Option[CAPSFileSystem] = None,
    filesPerTable: Option[Int] = Some(1)
  ): FSGraphSourceFactory = FSGraphSourceFactory(rootPath, customFileSystem, filesPerTable)

  case class FSGraphSourceFactory(
    rootPath: String,
    customFileSystem: Option[CAPSFileSystem] = None,
    filesPerTable: Option[Int] = Some(1)
  ) {

    def csv(implicit session: CAPSSession): FSGraphSource =
      new FSGraphSource(rootPath, "csv", customFileSystem, filesPerTable)
  }

  /**
    * Creates a data sink that is capable of writing a property graph into the Neo4j bulk import CSV format
    * (see [[https://neo4j.com/docs/operations-manual/current/tools/import/]]). The data sink generates a shell script
    * within the graph output folder that simplifies the import process.
    *
    * @param rootPath       Directory where the graph is being stored in
    * @param arrayDelimiter Delimiter for array properties
    * @param session        CAPS session
    * @return Neo4j Bulk CSV data sink
    */
  def neo4jBulk(rootPath: String, arrayDelimiter: String = "|")(implicit session: CAPSSession): Neo4jBulkCSVDataSink = {
    new Neo4jBulkCSVDataSink(rootPath, arrayDelimiter)
  }
}

object CypherGraphSources {
  /**
    * Creates a Neo4j Property Graph Data Source
    *
    * @param config             Neo4j connection configuration
    * @param maybeSchema        Optional Neo4j schema to avoid computation on Neo4j server
    * @param omitIncompatibleProperties If set to true, import failures do not throw runtime exceptions but omit the unsupported
    *                           properties instead and log warnings
    * @param session            CAPS session
    * @return Neo4j Property Graph Data Source
    */
  def neo4j(config: Neo4jConfig, maybeSchema: Option[Schema] = None, omitIncompatibleProperties: Boolean = false)
    (implicit session: CAPSSession): Neo4jPropertyGraphDataSource =
    Neo4jPropertyGraphDataSource(config, maybeSchema = maybeSchema, omitIncompatibleProperties = omitIncompatibleProperties)

  // TODO: document
  def neo4j(config: Neo4jConfig, schemaFile: String, omitIncompatibleProperties: Boolean )
    (implicit session: CAPSSession): Neo4jPropertyGraphDataSource = {
    val schemaString = Source.fromFile(Paths.get(schemaFile).toUri).getLines().mkString("\n")

    Neo4jPropertyGraphDataSource(config, maybeSchema = Some(Schema.fromJson(schemaString)), omitIncompatibleProperties = omitIncompatibleProperties)
  }
}