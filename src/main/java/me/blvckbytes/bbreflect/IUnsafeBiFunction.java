package me.blvckbytes.bbreflect;

@FunctionalInterface
public interface IUnsafeBiFunction<I1, I2, O, E extends Exception> {

  O apply(I1 i1, I2 i2) throws E;

}
