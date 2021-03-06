package org.deusaquilus

import org.scalameter.api._
import org.scalameter.picklers.Implicits._
import scala.collection.mutable

trait BenchBase {
  lazy val executor = LocalExecutor(
    new Executor.Warmer.Default,
    Aggregator.median[Double],
    measurer
  )
  lazy val measurer = new Measurer.Default
  lazy val reporter = new LoggingReporter[Double]
  lazy val persistor = Persistor.None

  val oneGen = Gen.unit("gen-time")

  /* inputs */

  val sizes = Gen.range("size")(300000, 1500000, 300000)

  val ranges = for {
    size <- sizes
  } yield 0 until size

  val opts = 
    new org.scalameter.japi.ContextBuilder()
      .put("exec.minWarmupRuns", 500)
      .put("exec.maxWarmupRuns", 1000)
      .put("exec.benchRuns", 400000)
      .build()
}

object BenchmarkReflectionNoFields extends Bench[Double] with BenchBase {
  import ex0_intro.Util._

  measure method "Using Reflection No Fields" in {
    //var d: Double = 0
    using(oneGen).config(opts) in { v =>
      val map = mutable.Map[String, Any]()
      for (field <- fieldsPre) {
        map.put(field.getName,  field.invoke(p)) // 0.000123
      }
      // Just to force usage of the variable in case compiler is optimizing it out
      //d = d + map.size.toDouble
    }
  }
}

object BenchmarkReflectAndDeref extends Bench[Double] with BenchBase {
  import ex0_intro.Util._

  measure method "Using Reflection" in {
    using(oneGen).config(opts) in { v =>
      val fields = reflectCaseClassFields(p)
      val map = mutable.Map[String, Any]()
      for (field <- fieldsPre) {
        map.put(field.getName,  field.invoke(p)) // 0.001169 ms
      }
    }
  }
}

object BenchmarkReflectFields extends Bench[Double] with BenchBase {
  import ex0_intro.Util._

  measure method "Using Reflection" in {
    using(oneGen).config(opts) in { v =>
      reflectCaseClassFields(p) // 0.001141 ms
    }
  }
}

object BenchmarkC extends Bench[Double] with BenchBase {
  import ex0_intro.Util._
  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      val map = mutable.Map[String, Any]()
      map.put("firstName",  p.firstName)
      map.put("lastName",  p.lastName)
      map.put("age",  p.age)
      // 0.000096 ms
    }
  }
}


object BenchmarkMacro extends Bench[Double] with BenchBase {
  import ex0_intro.Util._
  import ex0_intro._
  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      val map = mutable.Map[String, Any]()
      PopulateMap.populateMap(p, map) // 0.000096 ms
    }
  }
}

object BenchmarkFunMacro extends Bench[Double] with BenchBase {
  import ex0_intro.Util._
  import ex0_intro._
  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      PopulateMapFun.populateMap(p) // 0.000103 ms
    }
  }
}


object BenchmarkFunctionalJustLoadMap extends Bench[Double] with BenchBase {
  import ex0_intro.Util._
  import ex0_intro._
  val l = List[(String, Any)]("firstName" -> p.firstName, "lastName" -> p.lastName, "age" -> p.age)

  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      l.foldLeft(Map[String, Any]())((map, pair) => map + pair) // 0.000161 ms
    }
  }
}

object BenchmarkFunctionalWithReflect extends Bench[Double] with BenchBase {
  import ex0_intro.Util._
  val fields = reflectCaseClassFields(p)

  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      fields.foldLeft(Map[String, Any]()) { (map, field) => 
        map + ((field.getName, field.invoke(p))) // 0.000181
      }
    }
  }
}

object BenchmarkNestedObjectMapManual extends Bench[Double] with BenchBase {
  case class Name(first: String, last: String)
  case class Person(name: Name, age: Int)
  val p = Person(Name("Joe", "Bloggs"), 123)

  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
    val map = mutable.Map[String, Any]()
    val nameMap = mutable.Map[String, Any]()
    nameMap.put("first", p.name.first)
    nameMap.put("last", p.name.last)
    map.put("name", nameMap)
    map.put("age", p.age)
    } // 0.000156 ms
  }
}


object BenchmarkNestedObjectMap extends Bench[Double] with BenchBase {
  import ex0_intro._
  case class Name(first: String, last: String)
  case class Person(name: Name, age: Int)
  val p = Person(Name("Joe", "Bloggs"), 123)

  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      val map = mutable.Map[String, Any]()
      PopulateSubMaps.populateMap(p, map)
    }
  } // 0.000157
}


object BenchmarkDerived extends Bench[Double] with BenchBase {
  import ex2_bothinone._
  import Derived._
  import WriteToMapOps._
  case class Person(firstName: String, lastName: String, age: Int) derives WriteToMap
  val p = Person("Joe", "Bloggs", 123)

  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      p.writeToMap
    }
  } // 0.000103 ms
}

object BenchmarkDerivedLong extends Bench[Double] with BenchBase {
  import ex2_bothinone._
  import Derived._
  import WriteToMapOps._

  given writeLong: WriteToMap[Long] with
    def writeToMap(map: mutable.Map[String, Any])(key: String, value: Long): Unit = map.put(key, value)

  case class Person(firstName: String, lastName: String, age: Long) derives WriteToMap
  val q = Person("Q", "Q", 4000000000L)

  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      q.writeToMap
    }
  } // 0.000104 ms
}

object BenchmarkMirrorSummon extends Bench[Double] with BenchBase {
  import ex3_mirrorsummon._
  import DerivedMirrorSummon._
  import WriteToMapOps._

  inline given writeArbitraryToMap[T]: WriteToMap[T] = WriteToMap.derived
  case class Person(firstName: String, lastName: String, age: Int)
  val p = Person("Joe", "Bloggs", 123)

  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      
      p.writeToMap
    }
  } // 0.000127
}

object BenchmarkDerivedNaive extends Bench[Double] with BenchBase {
  import ex1_genericderive_take1._
  import DerivedNaive._
  import WriteToMapOps._
  case class Person(firstName: String, lastName: String, age: Int) derives WriteToMap
  val p = Person("Joe", "Bloggs", 123)

  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      p.writeToMap
    }
  } // 0.000214
}

object UsingUnionLeafList extends Bench[Double] with BenchBase {
  import ex4_flagcontrol._
  import DerivedFlagControl._
  import WriteToMapOps._
  
  given writeList[T](using wtm: WriteToMap[T]): WriteToMap[List[T]] with
    def writeToMap(map: mutable.Map[String, Any], justReturn: Boolean)(key: String, values: List[T]): Any =
      val valueKeys = 
        values.map(v =>
          wtm.writeToMap(null, true)("k", v)
        )
      if (justReturn) valueKeys
      else map.put(key, valueKeys); map

  case class Person(firstName: String, lastName: String, nicknames: List[String])
  val p = Person("Yosef", "Bloggs", List("Joseph", "Joe"))

  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      p.writeToMap
    }
  } // 0.000144 ms
}

object UsingUnionNodeList extends Bench[Double] with BenchBase {
  import ex4_flagcontrol._
  import DerivedFlagControl._
  import WriteToMapOps._
  
  given writeList[T](using wtm: WriteToMap[T]): WriteToMap[List[T]] with
    def writeToMap(map: mutable.Map[String, Any], justReturn: Boolean)(key: String, values: List[T]): Any =
      val valueKeys = 
        values.map(v =>
          wtm.writeToMap(null, true)("k", v)
        )
      if (justReturn) valueKeys
      else map.put(key, valueKeys); map

  case class Address(street: String, zip: Int)
  case class Person(firstName: String, lastName: String, nicknames: List[Address])
  val p = 
    Person("Yosef", "Bloggs", List(
      Address("123 Place", 11122), 
      Address("456 Ave", 11122))
    )

  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      p.writeToMap
    }
  } // 0.000348 ms
}


object UsingUnionLeafListManual extends Bench[Double] with BenchBase {
  import ex4_flagcontrol._
  import DerivedFlagControl._
  import WriteToMapOps._

  case class Person(firstName: String, lastName: String, nicknames: List[String])
  val p = Person("Yosef", "Bloggs", List("Joseph", "Joe"))

  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      val map = mutable.Map[String, Any]()
      map.put("firstName", "Yosef")
      map.put("lastName", "Bloggs")
      map.put("nicknames", List("Joseph", "Joe"))
      map
    }
  } // 0.000178 ms
}

object UsingFlagControlNodeListManual extends Bench[Double] with BenchBase {
  import ex4_flagcontrol._
  import DerivedFlagControl._
  import WriteToMapOps._
  import UseDerivedFlagControl._
  import UseDerivedFlagControl.CaseClassWithList._

  case class Address(street: String, zip: Int)
  case class Person(firstName: String, lastName: String, nicknames: List[Address])

  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      val map = mutable.Map[String, Any]()
      map.put("firstName", "Yosef"); map.put("lastName", "Bloggs")

      val map1 = mutable.Map[String, Any]()
      map1.put("street", "123 Place"); map1.put("zip", 11122)
      val map2 = mutable.Map[String, Any]()
      map1.put("street", "456 Ave"); map1.put("zip", 11122)
      map.put("addresses", List(map1, map2))
    }
  } // 0.000375ns
}

object UsingImmutable extends Bench[Double] with BenchBase {
  import ex5_immutable._
  import DerivedImmutable._
  import WriteToMapOps._
  import UseDerivedImmutable._
  case class Person(firstName: String, lastName: String, nicknames: List[String])
  val p = Person("Yosef", "Bloggs", List("Joseph", "Joe"))  

  var v = 0
  measure method "Manual" in {
    using(oneGen).config(opts) in { v =>
      p.writeToMap
    }
  } //  0.00089 ms00000
  println(v)
}

object UsingImmutableListLeaf extends Bench[Double] with BenchBase {
  import ex5_immutable._
  import DerivedImmutable._
  import WriteToMapOps._
  import UseDerivedImmutable._

  measure method "Manual" in {
    case class Person(firstName: String, lastName: String, nicknames: List[String])
    val p = Person("Yosef", "Bloggs", List("Joseph", "Joe"))  

    using(oneGen).config(opts) in { vv =>
      p.writeToMap
    }
  } // 0.000090ns
}

object UsingImmutableListNode extends Bench[Double] with BenchBase {
  import ex5_immutable._
  import DerivedImmutable._
  import WriteToMapOps._
  import UseDerivedImmutable._

  case class Address(street: String, zip: Int)
  case class Person(firstName: String, lastName: String, addresses: List[Address])
  val p = 
    Person("Yosef", "Bloggs", List(
      Address("123 Place", 11122), 
      Address("456 Ave", 11122))
    )

  var v = false
  measure method "Manual" in {
    using(oneGen).config(opts) in { vv => //hello
      val map = p.writeToMap
      //v = map.values.find(v => v == "foo").isEmpty // Maybe it's lazy, this should for it to materialize
    }
    println(v)
  } //  0.000087 ns
}


object BenchmarkJustLoadTest {
  def main(args: Array[String]): Unit = {
    val l = List[(String, Any)]("firstName" -> "Joe", "lastName" -> "Bloggs", "age" -> 123)
    println( l.foldLeft(Map[String, Any]())((map, pair) => map + pair) )//helloooooooooooooooo
  }
}
