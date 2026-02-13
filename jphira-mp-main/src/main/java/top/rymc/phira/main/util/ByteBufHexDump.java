package top.rymc.phira.main.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.util.Locale;

public class ByteBufHexDump {

    public static String copyAndToHex(ByteBuf original) {
        ByteBuf copied = original.copy(original.readerIndex(), original.readableBytes());
        try {
            return ByteBufUtil.hexDump(copied);
        } finally {
            copied.release();
        }
    }

    public static ByteBuf hexToByteBuf(String hex) {
        try {
            byte[] bytes = Hex.decodeHex(hex.toLowerCase(Locale.ROOT));
            return Unpooled.wrappedBuffer(bytes);
        } catch (DecoderException e){
            throw new RuntimeException(e);
        }
    }

}
