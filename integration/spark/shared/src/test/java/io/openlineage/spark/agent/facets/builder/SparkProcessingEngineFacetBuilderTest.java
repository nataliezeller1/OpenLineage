/*
/* Copyright 2018-2023 contributors to the OpenLineage project
/* SPDX-License-Identifier: Apache-2.0
*/

package io.openlineage.spark.agent.facets.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.openlineage.client.OpenLineage;
import io.openlineage.spark.agent.Versions;
import io.openlineage.spark.api.OpenLineageContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.spark.SparkContext;
import org.apache.spark.scheduler.JobSucceeded$;
import org.apache.spark.scheduler.SparkListenerJobEnd;
import org.apache.spark.scheduler.SparkListenerJobStart;
import org.apache.spark.scheduler.SparkListenerStageCompleted;
import org.apache.spark.scheduler.SparkListenerStageSubmitted;
import org.apache.spark.sql.execution.ui.SparkListenerSQLExecutionEnd;
import org.apache.spark.sql.execution.ui.SparkListenerSQLExecutionStart;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import scala.collection.Seq$;

class SparkProcessingEngineFacetBuilderTest {
  private static SparkContext sparkContext;

  @BeforeAll
  public static void setupSparkContext() {
    sparkContext = Mockito.mock(SparkContext.class);
    Mockito.when(sparkContext.appName()).thenReturn("SparkProcessingEngineFacetBuilderTest");
    Mockito.when(sparkContext.version()).thenReturn("3.3.0");
  }

  @Test
  void testIsDefinedForSparkListenerEvents() {
    SparkProcessingEngineRunFacetBuilder builder =
        new SparkProcessingEngineRunFacetBuilder(
            OpenLineageContext.builder()
                .sparkContext(sparkContext)
                .openLineage(new OpenLineage(Versions.OPEN_LINEAGE_PRODUCER_URI))
                .build());
    assertThat(builder.isDefinedAt(new SparkListenerSQLExecutionEnd(1, 1L))).isTrue();
    assertThat(
            builder.isDefinedAt(
                new SparkListenerSQLExecutionStart(1L, "abc", "abc", "abc", null, 1L)))
        .isTrue();
    assertThat(
            builder.isDefinedAt(
                new SparkListenerJobStart(1, 1L, Seq$.MODULE$.empty(), new Properties())))
        .isTrue();
    assertThat(builder.isDefinedAt(new SparkListenerJobEnd(1, 1L, JobSucceeded$.MODULE$))).isTrue();
    assertThat(builder.isDefinedAt(new SparkListenerStageSubmitted(null, new Properties())))
        .isTrue();
    assertThat(builder.isDefinedAt(new SparkListenerStageCompleted(null))).isTrue();
  }

  @Test
  void testBuild() {
    SparkProcessingEngineRunFacetBuilder builder =
        new SparkProcessingEngineRunFacetBuilder(
            OpenLineageContext.builder()
                .sparkContext(sparkContext)
                .openLineage(new OpenLineage(Versions.OPEN_LINEAGE_PRODUCER_URI))
                .build());

    Map<String, OpenLineage.RunFacet> runFacetMap = new HashMap<>();
    builder.build(new SparkListenerSQLExecutionEnd(1, 1L), runFacetMap::put);
    assertThat(runFacetMap)
        .hasEntrySatisfying(
            "processing_engine",
            facet ->
                assertThat(facet)
                    .isInstanceOf(OpenLineage.ProcessingEngineRunFacet.class)
                    .hasFieldOrPropertyWithValue("name", "spark")
                    .hasFieldOrPropertyWithValue("version", sparkContext.version()));
  }
}
