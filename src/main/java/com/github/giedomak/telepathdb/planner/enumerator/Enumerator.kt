package com.github.giedomak.telepathdb.planner.enumerator

import com.github.giedomak.telepathdb.datamodels.plans.PhysicalPlan

interface Enumerator {

    fun enumerate(tree1: PhysicalPlan, tree2: PhysicalPlan, logicalOperator: Int): Sequence<PhysicalPlan>

}