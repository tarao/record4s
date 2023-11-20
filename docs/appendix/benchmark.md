Full Benchmark Result
=====================

The source code of the benchmarks is available in the following directories:

- [record4s %](https://github.com/tarao/record4s/tree/master/modules/benchmark_3/src/main/scala/benchmark/record4s)
- [record4s ArrayRecord](https://github.com/tarao/record4s/tree/master/modules/benchmark_3/src/main/scala/benchmark/record4s_arrayrecord)
- [Scala 3 Map](https://github.com/tarao/record4s/tree/master/modules/benchmark_3/src/main/scala/benchmark/map)
- [Scala 3 case class](https://github.com/tarao/record4s/tree/master/modules/benchmark_3/src/main/scala/benchmark/caseclass)
- [shapeless Record](https://github.com/tarao/record4s/tree/master/modules/benchmark_2_13/src/main/scala/benchmark/shapeless)
- [scala-records Rec](https://github.com/tarao/record4s/tree/master/modules/benchmark_2_11/src/main/scala/benchmark/scalarecords)

See [Performance][] for the discussion on the benchmark results.

Runtime metrics
---------------

### Runtime metrics from [[Karlsson '18][]]

![Creation time / Record size](../img/benchmark/Creation.svg)

![Update time / Record size](../img/benchmark/Update.svg)

![Access time / Field index](../img/benchmark/FieldAccess.svg)

![Access time / Record size](../img/benchmark/FieldAccessSize.svg)

![Access time / Degree of polymorphism](../img/benchmark/FieldAccessPoly.svg)

### Other runtime metrics

![Concatenation time / Record size](../img/benchmark/Concatenation.svg)

Compile-time metrics
--------------------

### Compile-time metrics from [[Karlsson '17]]

![Compilation time (record creation) / Record size](../img/benchmark/CompileCreation.svg)

![Compilation time (record creation and all field access) / Record size](../img/benchmark/CompileCreationAndAccess.svg)

### Compile-time metrics from [scala-records-benchmarks]

![Compilation time (record creation and repeated field access) / Record size](../img/benchmark/CompileCreationAndAccessRep.svg)

### Other compile-time metrics

![Compilation time (field update) / Record size](../img/benchmark/CompileUpdate.svg)

![Compilation time (repeated field updates) / Record size](../img/benchmark/CompileUpdateRep.svg)

![Compilation time (field access) / Field index](../img/benchmark/CompileFieldAccess.svg)

![Compilation time (field access) / Record size](../img/benchmark/CompileFieldAccessSize.svg)

![Compilation time (concatenation) / Record size](../img/benchmark/CompileConcatenation.svg)

[Karlsson '17]: https://www.diva-portal.org/smash/get/diva2:1123270/fulltext01.pdf
[Karlsson '18]: https://www.csc.kth.se/~phaller/doc/karlsson-haller18-scala.pdf
[scala-records-benchmarks]: https://github.com/scala-records/scala-records-benchmarks
