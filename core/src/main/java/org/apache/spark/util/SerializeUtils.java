package org.apache.spark.util;

import com.esotericsoftware.kryo.Kryo;
import com.twitter.chill.IKryoRegistrar;
import com.twitter.chill.KryoInstantiator;
import com.twitter.chill.KryoPool;
import org.apache.spark.logging.AbstractLogStorage;

public class SerializeUtils {
    private static KryoPool serializer;

    public static KryoPool getSerializer() {
        if(serializer == null) {
            KryoInstantiator init = new KryoInstantiator().withRegistrar(new IKryoRegistrar() {
                @Override
                public void apply(Kryo kryo) {
                    kryo.register(AbstractLogStorage.ControlRecord.class);
                    kryo.register(AbstractLogStorage.DPCursor.class);
                    kryo.register(AbstractLogStorage.UpdateStepCursor.class);
                }
            });
            serializer = KryoPool.withByteArrayOutputStream(4* Runtime.getRuntime().availableProcessors(), init);
        }
        return serializer;
    }

}

