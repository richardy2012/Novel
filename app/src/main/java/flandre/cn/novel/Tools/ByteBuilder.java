package flandre.cn.novel.Tools;

import java.util.Arrays;

public class ByteBuilder {
    private byte[] bytes;
    private int seek = 0;

    public ByteBuilder(int size) {
        bytes = new byte[size];
    }

    public ByteBuilder(byte[] bytes) {
        this.bytes = bytes;
    }

    public ByteBuilder writeInt(int i) {
        for (int j = 0; j <= 24; j += 8) {
            bytes[seek++] = (byte) ((i >> j) & 255);
        }
        return this;
    }

    public ByteBuilder writeString(String s) {
        return writeBytes(s.getBytes());
    }

    public ByteBuilder writeBytes(byte[] b){
        for (byte b1 : b) {
            bytes[seek++] = b1;
        }
        return this;
    }

    public int readInt(){
        int result = 0;
        for (int j = 0; j <= 24; j += 8) {
            result += bytes[seek++] << j;
        }
        return result;
    }

    public String readString(int i){
        String s = new String(bytes, seek, i);
        seek += i;
        return s;
    }

    public void setSeek(int seek) {
        this.seek = seek;
    }

    public int getSeek() {
        return seek;
    }

    public byte[] getBytes() {
        return Arrays.copyOf(bytes, seek);
    }
}
