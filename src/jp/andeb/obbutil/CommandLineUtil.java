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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class CommandLineUtil {

    // args for add
    static final Option PACKAGE_NAME;
    static final Option OBB_VERSION;
    static final Option OVERLAY_FLAG;
    static final Option SALT;

    static final Options OPTIONS_FOR_ADD;

    static {
        OPTIONS_FOR_ADD = new Options();

        OptionBuilder.withArgName("OBB package name");
        OptionBuilder.isRequired();
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("パッケージ名");
        OptionBuilder.withLongOpt("name");
        PACKAGE_NAME = OptionBuilder.create('n');
        OPTIONS_FOR_ADD.addOption(PACKAGE_NAME);

        OptionBuilder.withArgName("OBB version");
        OptionBuilder.isRequired();
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("OBB バージョン");
        OptionBuilder.withLongOpt("version");
        OBB_VERSION = OptionBuilder.create('v');
        OPTIONS_FOR_ADD.addOption(OBB_VERSION);

        OptionBuilder.withArgName("overlay flag");
        OptionBuilder.withDescription("オーバーレイフラグ");
        OptionBuilder.withLongOpt("overlay");
        OVERLAY_FLAG = OptionBuilder.create('o');
        OPTIONS_FOR_ADD.addOption(OVERLAY_FLAG);

        OptionBuilder.withArgName("salt");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("8バイトHEXソルト");
        OptionBuilder.withLongOpt("salt");
        SALT = OptionBuilder.create('s');
        OPTIONS_FOR_ADD.addOption(SALT);
    }

    static void printUsage(String progName) {
        System.err.println("Opaque Binary Blob(OBB) Utility");
        System.err.println();
        System.err.println("使い方:");
        System.err.println(" " + progName + " a[dd] [ オプション ] 対象ファイル");
        System.err.println("   OBB 情報をファイルに追加します。");
        System.err.println("   オプション:");
        System.err.println("     -n <package name>      パッケージ名(必須)");
        System.err.println("     -v <package version>   パッケージバージョン(必須)");
        System.err.println("     -o                     OBB オーバーレイフラグをセット");
        System.err.println("     -s <8 byte hex salt>   暗号化で使用しているソルト(例: 00FF3256F9890092)");
        System.err.println();
        System.err.println(" " + progName + " r[emove] 対象ファイル");
        System.err.println("   OBB 情報をファイルから削除します。");
        System.err.println();
        System.err.println(" " + progName + " i[nfo] 対象ファイル");
        System.err.println("   ファイルの OBB 情報を表示します。");
        System.err.println();
    }

    static Integer toInteger(String intStr) {
        try {
            return Integer.valueOf(intStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static byte[] toByteArray(String bytesStr, int byteLength) {
        if (bytesStr == null) {
            return new byte[byteLength];
        }
        if (bytesStr.length() != (byteLength * 2)) {
            return null;
        }
        final byte[] result = new byte[byteLength];
        for (int index = 0; index < byteLength; index++) {
            final int offset = index * 2;
            final int value = Integer.parseInt(bytesStr.substring(offset, offset + 2), 16);
            result[index] = (byte) value;
        }
        return result;
    }

}
