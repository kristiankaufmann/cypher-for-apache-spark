/**
 * Copyright (c) 2016-2017 "Neo4j, Inc." [https://neo4j.com]
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
package org.opencypher.caps.impl.parse.rewriter

import org.neo4j.cypher.internal.frontend.v3_3.inSequence
import org.neo4j.cypher.internal.frontend.v3_3.phases.CompilationPhaseTracer.CompilationPhase.AST_REWRITE
import org.neo4j.cypher.internal.frontend.v3_3.phases.{BaseContext, BaseState, Phase}

case object CAPSPreparatoryRewriting extends Phase[BaseContext, BaseState, BaseState] {

  override def process(from: BaseState, context: BaseContext): BaseState = {

    val rewrittenStatement = from.statement().endoRewrite(inSequence(
      normalizeReturnClauses(context.exceptionCreator)
    ))

    from.withStatement(rewrittenStatement)
  }

  override val phase = AST_REWRITE

  override val description = "rewrite the AST into a shape that semantic analysis can be performed on"

  override def postConditions = Set.empty
}
