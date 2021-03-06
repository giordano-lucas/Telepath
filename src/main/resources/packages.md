# Module telepath

Massive graph-structured data collections are ubiquitous in contemporary data management scenarios such as social networks, linked open data, and chemical compound databases.

The selection and manipulation of paths forms the core of querying graph datasets. Path indexing techniques can speed up this core functionality of querying graph datasets.

We propose a path-index based graph database engine.

# Package com.github.giedomak.telepath.costmodel

Functionality regarding costing physical plans.

# Package com.github.giedomak.telepath.datamodels

Bundle all the data classes we will be using throughout the Telepath module.

This package contains data classes for nodes, edges and paths for example.

# Package com.github.giedomak.telepath.datamodels.integrations

This package holds the wrapper to convert from and to the data classes from PathDB.

# Package com.github.giedomak.telepath.datamodels.stores

Since we are dealing with pathIds instead of lists with edge labels, we've got stores to hold these mappings.

# Package com.github.giedomak.telepath.datamodels.utilities

Some utilities regarding our datamodels. Think about a ParseTreePrinter, or a UnionPuller for example.

# Package com.github.giedomak.telepath.evaluationengine

Evaluate a physical plan in order to retrieve results.

# Package com.github.giedomak.telepath.kpathindex

Wrapper for the path index from the PathDB module.

# Package com.github.giedomak.telepath.kpathindex.utilities

Utilities regarding the path index. A GMark importer for example.

# Package com.github.giedomak.telepath.memorymanager

We should have control over our data and caching mechanism.

# Package com.github.giedomak.telepath.memorymanager.external_merge_sort

Deprecated package holding some external merge sort logic.

# Package com.github.giedomak.telepath.memorymanager.spliterator

Spliterator extension for Java streams.

# Package com.github.giedomak.telepath.physicallibrary

Physcial operators like hash-join, union, nested-loop-join for example.

# Package com.github.giedomak.telepath.physicallibrary.joins

Collection of our join algorithms.

# Package com.github.giedomak.telepath.planner

Enumerate and find the best physical-plan to give to the evaluation engine.

# Package com.github.giedomak.telepath.staticparser

Parse user input into a data-structure we can work with.

# Package com.github.giedomak.telepath.staticparser.rpq

Regular path query extension for the static parser package.
