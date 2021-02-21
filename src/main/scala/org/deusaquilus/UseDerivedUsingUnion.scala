package org.deusaquilus

import scala.collection.mutable

object UseDerivedUsingUnion {
  import DerivedUsingUnion._
  import WriteToMapOps._

  inline given writeArbitraryToMap[T]: WriteToMap[T] = WriteToMap.derived

  def main(args: Array[String]): Unit = {
    caseClassWithList()
  }

  object CaseClassWithList {
    given writeList[T](using wtm: WriteToMap[T]): WriteToMap[List[T]] with
      def writeToMap(mapOrReturn: mutable.Map[String, Any] | JustReturn)(key: String, values: List[T]): Any =
        val valueKeys = 
          values.map(v =>
            wtm.writeToMap(JustReturn)("k", v)
          )
        mapOrReturn match
          case JustReturn => valueKeys
          case map: mutable.Map[String, Any] => map.put(key, valueKeys); map

    def derivedWithListLeaf = {
      case class Person(firstName: String, lastName: String, nicknames: List[String])
      val p = Person("Yosef", "Bloggs", List("Joseph", "Joe"))
      p.writeToMap
    }

    def derivedWithListLeafManual = {
      case class Person(firstName: String, lastName: String, nicknames: List[String])
      val p = Person("Yosef", "Bloggs", List("Joseph", "Joe"))
      val map = mutable.Map[String, Any]()
      map.put("firstName", "Yosef")
      map.put("lastName", "Bloggs")
      map.put("nicknames", List("Joseph", "Joe"))
      map
    }

    def derivedWithListNode = {
      case class Address(street: String, zip: Int)
      case class Person(firstName: String, lastName: String, nicknames: List[Address])
      val p = 
        Person("Yosef", "Bloggs", List(
          Address("123 Place", 11122), 
          Address("456 Ave", 11122))
        )
      p.writeToMap
    }

    def derivedWithListNodeManual = {
      case class Address(street: String, zip: Int)
      case class Person(firstName: String, lastName: String, nicknames: List[Address])
      val p = 
        Person("Yosef", "Bloggs", List(
          Address("123 Place", 11122), 
          Address("456 Ave", 11122))
        )

      val map = mutable.Map[String, Any]()
      map.put("firstName", "Yosef")
      map.put("lastName", "Bloggs")

      val map1 = mutable.Map[String, Any]()
      map1.put("street", "123 Place")
      map1.put("zip", 11122)

      val map2 = mutable.Map[String, Any]()
      map1.put("street", "456 Ave")
      map1.put("zip", 11122)
      
      map.put("addresses", List(map1, map2))
      map
    }
  }

  def caseClassWithList(): Unit = {
    println(CaseClassWithList.derivedWithListLeaf.toMap)
    println(CaseClassWithList.derivedWithListNode.toMap)
  }
}