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
// tag::full-example[]
package org.opencypher.spark.examples

import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.opencypher.okapi.api.io.conversion.{NodeMapping, RelationshipMapping}
import org.opencypher.spark.api.CAPSSession
import org.opencypher.spark.api.io.{CAPSNodeTable, CAPSRelationshipTable}
import org.opencypher.spark.util.ConsoleApp

import scala.collection.JavaConverters._

/**
  * Demonstrates basic usage of the CAPS API by loading an example network from existing [[DataFrame]]s including
  * custom entity mappings and running a Cypher query on it.
  */
object CustomDataFrameInputExample extends ConsoleApp {

  // 1) Create CAPS session and retrieve Spark session
  // tag::create-session[]
  val spark: SparkSession = SparkSession
    .builder()
    .master("local[*]")
    .getOrCreate()

  implicit val session: CAPSSession = CAPSSession.create(spark)
  // end::create-session[]

  // 2) Generate some DataFrames that we'd like to interpret as a property graph.
  // tag::prepare-dataframes[]
  val nodesDF: DataFrame = {
    val nodes = List(
      Row(0L, "Alice", 42L),
      Row(1L, "Bob", 23L),
      Row(2L, "Eve", 84L)
    ).asJava
    val nodeSchema = StructType(List(
      StructField("NODE_ID", LongType, false),
      StructField("FIRST_NAME", StringType, false),
      StructField("AGE", LongType, false))
    )
    spark.createDataFrame(nodes, nodeSchema)
  }

  val relsDF: DataFrame = {
    val rels = List(
      Row(0L, 0L, 1L, "23/01/1987"),
      Row(1L, 1L, 2L, "12/12/2009")
    ).asJava
    val relSchema = StructType(List(
      StructField("REL_ID", LongType, false),
      StructField("SOURCE_ID", LongType, false),
      StructField("TARGET_ID", LongType, false),
      StructField("CONNECTED_SINCE", StringType, false))
    )
    spark.createDataFrame(rels, relSchema)
  }
  // end::prepare-dataframes[]

  // 3) Generate node- and relationship tables that wrap the DataFrames and describe their contained data.
  //    Node and relationship mappings are used to explicitly define which DataFrame column stores which specific entity
  //    component (identifiers, properties, optional labels, relationship types).

  // tag::create-node-relationship-tables[]
  val personNodeMapping = NodeMapping
    .withSourceIdKey("NODE_ID")
    .withImpliedLabel("Person")
    .withPropertyKey(propertyKey = "name", sourcePropertyKey = "FIRST_NAME")
    .withPropertyKey(propertyKey = "age", sourcePropertyKey = "AGE")

  val friendOfMapping = RelationshipMapping
    .withSourceIdKey("REL_ID")
    .withSourceStartNodeKey("SOURCE_ID")
    .withSourceEndNodeKey("TARGET_ID")
    .withRelType("FRIEND_OF")
    .withPropertyKey("since", "CONNECTED_SINCE")

  val personTable = CAPSNodeTable.fromMapping(personNodeMapping, nodesDF)
  val friendsTable = CAPSRelationshipTable.fromMapping(friendOfMapping, relsDF)
  // end::create-node-relationship-tables[]

  // 4) Create property graph from graph scans
  // tag::create-graph[]
  val graph = session.readFrom(personTable, friendsTable)
  // end::create-graph[]

  // 5) Execute Cypher query and print results
  // tag::run-query[]
  val result = graph.cypher("MATCH (n:Person) RETURN n.name")
  // end::run-query[]

  // 6) Collect results into string by selecting a specific column.
  //    This operation may be very expensive as it materializes results locally.
  // 6a) type safe version, discards values with wrong type
  // tag::collect-results-typesafe[]
  val safeNames: Set[String] = result.records.collect.flatMap(_ ("n.name").as[String]).toSet
  // end::collect-results-typesafe[]
  // 6b) unsafe version, throws an exception when value cannot be cast
  // tag::collect-results-nontypesafe[]
  val unsafeNames: Set[String] = result.records.collect.map(_ ("n.name").cast[String]).toSet
  // end::collect-results-nontypesafe[]

  println(safeNames)

  def nodes: DataFrame = {
    val nodes = List(
      Row(0L, "Alice", 42L),
      Row(1L, "Bob", 23L),
      Row(2L, "Eve", 84L)
    ).asJava
    val nodeSchema = StructType(List(
      StructField("NODE_ID", LongType, false),
      StructField("FIRST_NAME", StringType, false),
      StructField("AGE", LongType, false))
    )
    spark.createDataFrame(nodes, nodeSchema)
  }

  def rels: DataFrame = {
    val rels = List(
      Row(0L, 0L, 1L, "23/01/1987"),
      Row(1L, 1L, 2L, "12/12/2009")
    ).asJava
    val relSchema = StructType(List(
      StructField("REL_ID", LongType, false),
      StructField("SOURCE_ID", LongType, false),
      StructField("TARGET_ID", LongType, false),
      StructField("CONNECTED_SINCE", StringType, false))
    )
    spark.createDataFrame(rels, relSchema)
  }
}
// end::full-example[]
