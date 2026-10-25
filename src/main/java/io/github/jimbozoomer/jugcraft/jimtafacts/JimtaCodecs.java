package io.github.jimbozoomer.jugcraft.jimtafacts;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
/** Bounded persisted counters: reject malformed values rather than overflowing storage arithmetic. */
public final class JimtaCodecs {
    private JimtaCodecs(){}
    public static Codec<Long> longRange(long minimum,long maximum){return Codec.LONG.validate(value->value>=minimum&&value<=maximum?DataResult.success(value):DataResult.error(()->"Value outside "+minimum+".."+maximum));}
}
