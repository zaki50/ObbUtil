/*
 * Copyright 2011 Android DEvelopers' cluB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.andeb.obbutil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

public class ObbInfoV1 {

    private static final int OBB_SIGNATURE = 0x01059983;

    public static int FLAG_OVERLAY = (1 << 0);
    public static int FLAG_SALTED = (1 << 1);

    public static int getObbSignature() {
        return OBB_SIGNATURE;
    }

    private final int packageVersion_;

    private final int flags_;

    private final byte[] salt_;

    private final String packageName_;

    private static final int INFO_VERSION = 1;

    public static final int SALT_LENGTH = 8;

    private static final Charset PACKAGE_NAME_ENCODING = Charset.forName("UTF-8");

    /**
     * 指定された情報で {@link ObbInfoV1} を構築します。
     * 
     * @param flags フラグセット。
     * @param salt ソルト。
     * @param packageName パッケージ名。
     * @param packageVersion パッケージバージョン。
     */
    public ObbInfoV1(int flags, byte[] salt, String packageName, int packageVersion) {
        super();
        if (salt == null) {
            this.salt_ = new byte[SALT_LENGTH];
        } else {
            if (salt.length != SALT_LENGTH) {
                throw new IllegalArgumentException("length of 'salt' must be " + SALT_LENGTH);
            }
            this.salt_ = Arrays.copyOf(salt, SALT_LENGTH);
        }
        this.flags_ = flags;
        if (packageName.isEmpty()) {
            throw new IllegalArgumentException("'packageName' must not be empty.");
        }
        this.packageName_ = packageName;
        this.packageVersion_ = packageVersion;
    }

    public String getPackageName() {
        return packageName_;
    }

    public int getPackageVersion() {
        return packageVersion_;
    }

    public int getFlags() {
        return flags_;
    }

    public boolean isOverlay() {
        return (flags_ & FLAG_OVERLAY) != 0;
    }

    public boolean isSalted() {
        return (flags_ & FLAG_SALTED) != 0;
    }

    public byte[] getSalt() {
        return salt_.clone();
    }

    /**
     * 保持している情報をバイト列に変換し、 {@link ByteBuffer} として返します。
     * 
     * @return 変換されたバイト配列を保持する {@link ByteBuffer}。{@code position} が {@code 0}、
     *         {@code capacity} と {@code limit} がバイト列のサイズと一致した 状態で返されます。この
     *         {@link ByteBuffer} を書き換えても、 {@link ObbInfoV1} が 保持する情報には影響しません。
     */
    public ByteBuffer toBytes() {
        final byte[] packageNameBytes = packageName_.getBytes(PACKAGE_NAME_ENCODING);
        final int totalSize = calcTotalSize(packageNameBytes.length);
        final ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(INFO_VERSION);
        buffer.putInt(packageVersion_);
        buffer.putInt(flags_);
        buffer.put(salt_);
        buffer.putInt(packageNameBytes.length);
        buffer.put(packageNameBytes);
        buffer.putInt(buffer.position());
        buffer.putInt(OBB_SIGNATURE);
        buffer.flip();
        return buffer;
    }

    @Override
    public String toString() {
        if ((flags_ & FLAG_SALTED) == 0) {
            return "ObbInfoV1 [packageName_=" + packageName_ + ", packageVersion_="
                    + packageVersion_
                    + ", flags_=" + flags_ + "]";
        }
        return "ObbInfoV1 [packageName_=" + packageName_ + ", packageVersion_=" + packageVersion_
                + ", flags_=" + (flags_ & ~FLAG_SALTED) + ", salt_=" + Arrays.toString(salt_) + "]";
    }

    public void prettyPrint(PrintStream out) {
        out.println("Package name: " + getPackageName());
        out.println("     Version: " + getPackageVersion());
        final String flagsAsHex = Long.toHexString(getFlags() & 0xffffffffL);
        out.println("       Flags: 0x" + flagsAsHex);
        out.println("     Overlay: "
                + (isOverlay() ? "true" : "false"));
        out.println("      Salted: "
                + (isSalted() ? "true" : "false"));
        out.print("        Salt: ");
        final byte[] salt = getSalt();
        for (int index = 0; index < salt.length; index++) {
            final String hexString = Integer.toHexString(salt[index] & 0xff);
            // hexString.length() returns 1 or 2
            if (hexString.length() == 1) {
                out.print('0');
            }
            out.print(hexString);
        }
        out.println();
    }

    private static final int MAX_BUFFER_SIZE = 32768;

    private static final int MINIMUM_INFO_SIZE = 33;

    private static final int TAG_SIZE = 8;

    public static final class NotObbException extends Exception {
        private static final long serialVersionUID = 1L;

        public NotObbException(String message) {
            super(message);
        }
    }

    public static ObbInfoV1 fromFile(File obbFile) throws FileNotFoundException, IOException,
            NotObbException {
        final RandomAccessFile obb = new RandomAccessFile(obbFile, "r");
        try {
            final ObbInfoV1 info = fromFile(obb);
            return info;
        } finally {
            obb.close();
        }
    }

    public static ObbInfoV1 fromFile(RandomAccessFile obb) throws
            IOException,
            NotObbException {
        final long fileSize = obb.length();
        if (fileSize < TAG_SIZE) {
            throw new NotObbException("too small");
        }

        // seek to head of tag
        obb.seek(fileSize - TAG_SIZE);
        final int footerSize = readIntLe(obb);
        final int signature = readIntLe(obb);
        if (signature != OBB_SIGNATURE) {
            throw new NotObbException("signature not found");
        }
        if (MAX_BUFFER_SIZE < footerSize || footerSize < (MINIMUM_INFO_SIZE - TAG_SIZE)) {
            throw new NotObbException("invalid footer size");
        }

        // seek to head of footer
        obb.seek(fileSize - footerSize - TAG_SIZE);

        final int signatureVersion = readIntLe(obb);
        if (signatureVersion != INFO_VERSION) {
            throw new NotObbException("unsupported version: " + signatureVersion);
        }
        final int packageVersion = readIntLe(obb);
        final int flags = readIntLe(obb);
        final byte[] salt = readBytes(obb, 8);
        final int packageNameSize = readIntLe(obb);
        final byte[] packageNameBytes = readBytes(obb, packageNameSize);
        final String packageName = new String(packageNameBytes, PACKAGE_NAME_ENCODING);

        final ObbInfoV1 info = new ObbInfoV1(flags, salt, packageName, packageVersion);
        return info;
    }

    private static int readIntLe(RandomAccessFile target) throws IOException {
        final int count = (Integer.SIZE / Byte.SIZE);
        int value = 0;
        for (int i = 0; i < count; i++) {
            final int b = target.readUnsignedByte();
            value |= (b << (i * 8));
        }
        return value;
    }

    /**
     * 渡された {@link RandomAccessFile} の現在の位置から、指定されたバイト数を 読み取ります。
     * 
     * @param target 読み取り対象のファイル。
     * @param count 読み込むバイト数。{@code 0} 以上の値を指定すること。必ず指定されてた
     *            バイト数を読み取ります。読み取り可能なバイト数が {@code count} で指定された数より
     *            少ない場合は可能なかぎり読み取った上で {@link IOException} をスローします。
     * @return 読み取ったバイト列を保持するバイト配列。配列の長さは {@code count} と 一致することが保証されます。
     * @throws IOException
     * @Throws IllegalArgumentException
     */
    private static byte[] readBytes(RandomAccessFile target, int count) throws IOException {
        final byte[] bytes = new byte[count];
        int total = 0;
        int len;

        while (0 < (len = target.read(bytes, total, count - total))) {
            total += len;
        }
        if (total < count) {
            throw new IOException();
        }
        return bytes;
    }

    /**
     * ObbInfo の総バイト数を求めます。
     * 
     * @param packageNameSize パッケージ名をバイト列に変換した際のバイト数。正数であること。
     * @return 総バイト数。
     * @throws IllegalArgumentException {@code packageNameSize} が正数でない場合。
     */
    private static int calcTotalSize(int packageNameSize) {
        if (packageNameSize <= 0) {
            throw new IllegalArgumentException("'packageNameSize' must be positive number.");
        }

        final int totalSize = packageNameSize + 32;
        return totalSize;
    }

}
