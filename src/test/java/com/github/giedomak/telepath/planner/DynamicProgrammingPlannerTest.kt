/**
 * Copyright (C) 2016-2017 - All rights reserved.
 * This file is part of the telepath project which is released under the GPLv3 license.
 * See file LICENSE.txt or go to http://www.gnu.org/licenses/gpl.txt for full license details.
 * You may use, distribute and modify this code under the terms of the GPLv3 license.
 */

package com.github.giedomak.telepath.planner

import com.github.giedomak.telepath.Telepath
import com.github.giedomak.telepath.cardinalityestimation.KPathIndexCardinalityEstimation
import com.github.giedomak.telepath.costmodel.AdvancedCostModel
import com.github.giedomak.telepath.datamodels.Query
import com.github.giedomak.telepath.datamodels.plans.LogicalPlan
import com.github.giedomak.telepath.datamodels.plans.LogicalPlanTest
import com.github.giedomak.telepath.datamodels.plans.PhysicalPlan
import com.github.giedomak.telepath.datamodels.plans.PhysicalPlanTest
import com.github.giedomak.telepath.datamodels.stores.PathIdentifierStore
import com.github.giedomak.telepath.kpathindex.KPathIndexInMemory
import com.github.giedomak.telepath.physicaloperators.PhysicalOperator
import com.github.giedomak.telepath.planner.enumerator.SimpleEnumerator
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert.assertEquals
import org.junit.Test

class DynamicProgrammingPlannerTest {

    // MOCKS

    private val cardinalityEstimationMock = mock<KPathIndexCardinalityEstimation> {
        on { getCardinality(any<PhysicalPlan>()) }.doReturn(20)
    }

    private val indexMock = mock<KPathIndexInMemory> {
        on { k }.doReturn(3)
    }

    private val telepathMock = mock<Telepath> {
        on { cardinalityEstimation }.doReturn(cardinalityEstimationMock)
        on { planner }.doReturn(DynamicProgrammingPlanner)
        on { costModel }.doReturn(AdvancedCostModel)
        on { pathIdentifierStore }.doReturn(PathIdentifierStore)
        on { enumerator }.doReturn(SimpleEnumerator)
        on { kPathIndex }.doReturn(indexMock)
    }

    private val queryMock = mock<Query> {
        on { telepath }.doReturn(telepathMock)
    }

    @Test
    fun generatesSimplePhysicalPlan() {

        // Generate the LogicalPlan for input
        val input = LogicalPlanTest.generateLogicalPlan(LogicalPlan.CONCATENATION, listOf("a", "b"), queryMock)

        // Generate the physical plan
        val actual = telepathMock.planner.generate(input)

        // Generate the expected physical plan
        val expected = PhysicalPlanTest.generatePhysicalPlan(PhysicalOperator.INDEX_LOOKUP, listOf("a", "b"))

        assertEquals(expected, actual)
    }

    @Test
    fun generatesMultiLevelPhysicalPlan() {

        // Input:
        //       CONCATENATION
        //        /      \
        //       a      UNION
        //              /   \
        //             b     c
        val child = LogicalPlanTest.generateLogicalPlan(LogicalPlan.UNION, listOf("b", "c"), queryMock)
        val input = LogicalPlanTest.generateLogicalPlan(LogicalPlan.CONCATENATION, listOf("a"), queryMock)
        input.setChild(1, child)

        // Parse the input
        val actual = telepathMock.planner.generate(input)

        // Generate the expected physical plan
        //             HASH_JOIN
        //              /     \
        //   INDEX_LOOKUP      UNION
        //        |             /  \
        //        a  INDEX_LOOKUP INDEX_LOOKUP
        //                |            |
        //                b            c
        val child1 = PhysicalPlanTest.generatePhysicalPlan(PhysicalOperator.INDEX_LOOKUP, listOf("a"))
        val child2 = PhysicalPlanTest.generatePhysicalPlan(PhysicalOperator.INDEX_LOOKUP, listOf("b"))
        val child3 = PhysicalPlanTest.generatePhysicalPlan(PhysicalOperator.INDEX_LOOKUP, listOf("c"))
        val child4 = PhysicalPlanTest.generatePhysicalPlanWithChildren(PhysicalOperator.UNION, listOf(child2, child3))
        val expected = PhysicalPlanTest.generatePhysicalPlanWithChildren(PhysicalOperator.HASH_JOIN, listOf(child1, child4))

        assertEquals(expected, actual)
    }
}
