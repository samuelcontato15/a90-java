package com.a90;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class IcoDecoder {

    record Icon(Image image, int hotspotX, int hotspotY) {}

    private IcoDecoder() {}

    static Icon decode(byte[] data) {
        try {
            ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            int type  = b.getShort(2);
            int count = Short.toUnsignedInt(b.getShort(4));
            if (b.getShort(0) != 0 || (type != 1 && type != 2) || count == 0) return null;

            int best = 6;
            long bestArea = -1;
            for (int i = 0; i < count; i++) {
                int e = 6 + i * 16;
                long area = (long) dim(b.get(e)) * dim(b.get(e + 1));
                if (area > bestArea) { bestArea = area; best = e; }
            }

            int hotX = type == 2 ? b.getShort(best + 4) : 0;
            int hotY = type == 2 ? b.getShort(best + 6) : 0;
            int size = b.getInt(best + 8);
            int off  = b.getInt(best + 12);

            boolean png = data[off] == (byte) 0x89 && data[off + 1] == 'P'
                       && data[off + 2] == 'N' && data[off + 3] == 'G';
            Image img = png ? new Image(new ByteArrayInputStream(data, off, size))
                            : decodeDib(b, off);
            return img == null || img.isError() ? null : new Icon(img, hotX, hotY);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int dim(byte v) {
        int d = Byte.toUnsignedInt(v);
        return d == 0 ? 256 : d;
    }

    private static Image decodeDib(ByteBuffer b, int off) {
        int hdrSize = b.getInt(off);
        int w       = b.getInt(off + 4);
        int h       = Math.abs(b.getInt(off + 8)) / 2;
        int bpp     = b.getShort(off + 14);
        int clrUsed = b.getInt(off + 32);

        int paletteSize = bpp <= 8 ? (clrUsed != 0 ? clrUsed : 1 << bpp) : 0;
        int palette     = off + hdrSize;
        int xor         = palette + paletteSize * 4;
        int xorStride   = ((w * bpp + 31) / 32) * 4;
        int and         = xor + xorStride * h;
        int andStride   = ((w + 31) / 32) * 4;
        boolean hasAnd  = and + andStride * h <= b.limit();

        int[] argb = new int[w * h];
        boolean anyAlpha = false;
        for (int y = 0; y < h; y++) {
            int row = xor + (h - 1 - y) * xorStride;
            for (int x = 0; x < w; x++) {
                int px;
                switch (bpp) {
                    case 32 -> px = b.getInt(row + x * 4);
                    case 24 -> px = 0xFF000000 | (Byte.toUnsignedInt(b.get(row + x * 3 + 2)) << 16)
                                               | (Byte.toUnsignedInt(b.get(row + x * 3 + 1)) << 8)
                                               |  Byte.toUnsignedInt(b.get(row + x * 3));
                    case 8, 4, 1 -> {
                        int bit   = x * bpp;
                        int index = (Byte.toUnsignedInt(b.get(row + bit / 8)) >> (8 - bpp - bit % 8))
                                    & ((1 << bpp) - 1);
                        px = 0xFF000000 | (b.getInt(palette + index * 4) & 0xFFFFFF);
                    }
                    default -> { return null; }
                }
                if (bpp == 32 && (px >>> 24) != 0) anyAlpha = true;
                argb[y * w + x] = px;
            }
        }

        if (bpp == 32 && !anyAlpha) {
            for (int i = 0; i < argb.length; i++) argb[i] |= 0xFF000000;
        }

        boolean useMask = hasAnd && !(bpp == 32 && anyAlpha);
        if (useMask) {
            for (int y = 0; y < h; y++) {
                int row = and + (h - 1 - y) * andStride;
                for (int x = 0; x < w; x++) {
                    boolean transparent = ((b.get(row + x / 8) >> (7 - x % 8)) & 1) == 1;
                    int i = y * w + x;
                    argb[i] = transparent ? 0 : (argb[i] | 0xFF000000);
                }
            }
        }

        WritableImage img = new WritableImage(w, h);
        img.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), argb, 0, w);
        return img;
    }
}
