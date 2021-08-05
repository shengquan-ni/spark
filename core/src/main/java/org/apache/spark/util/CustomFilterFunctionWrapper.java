package org.apache.spark.util;

import org.apache.spark.api.java.function.Function;

public class CustomFilterFunctionWrapper<T, U> implements Function<T, U> {

    public Function<T, U> f;

    public CustomFilterFunctionWrapper(Function<T, U> f){
        this.f = f;
    }

    @Override
    public U call(T v1) throws Exception {
        return f.call(v1);
    }
}
