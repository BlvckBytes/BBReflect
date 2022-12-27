package me.blvckbytes.bbreflection;

public enum Assignability {

  /*
    Definition of assignability:

    // Let there be two types
    TypeA fieldA;
    TypeB fieldB;

    // If fieldB can be >assigned to< fieldA, TypeB is assignable to TypeA
    fieldA = fieldB;

    // If fieldA can be >assigned to< fieldB, TypeA is assignable to TypeB
    fieldB = fieldA;
   */

  // Can the target's (searched) type be assigned to the type's type?
  TARGET_TO_TYPE,

  // Can the type's type be assigned to the target's (searched) type?
  TYPE_TO_TARGET,

  // Assignability is to be ignored
  NONE
  ;

}
