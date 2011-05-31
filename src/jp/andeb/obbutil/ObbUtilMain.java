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

import static jp.andeb.obbutil.CommandLineUtil.OBB_VERSION;
import static jp.andeb.obbutil.CommandLineUtil.OPTIONS_FOR_ADD;
import static jp.andeb.obbutil.CommandLineUtil.OVERLAY_FLAG;
import static jp.andeb.obbutil.CommandLineUtil.PACKAGE_NAME;
import static jp.andeb.obbutil.CommandLineUtil.SALT;
import static jp.andeb.obbutil.CommandLineUtil.printUsage;
import static jp.andeb.obbutil.CommandLineUtil.toByteArray;
import static jp.andeb.obbutil.CommandLineUtil.toInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import jp.andeb.obbutil.ObbInfoV1.NotObbException;

public class ObbUtilMain {
    
    private static final String PROGNAME = "ObbUtil";

    static boolean matches(String canonName, String testee) {
        if (testee.isEmpty()) {
            return false;
        }
        final boolean matched = canonName.indexOf(testee) == 0;
        return matched;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage(PROGNAME);
            return;
        }
        final String command = args[0];
        final boolean succeeded;
        if (matches("add", command)) {
            succeeded = doAdd(dropFirst(args));
        } else if (matches("remove", command)) {
            succeeded = doRemove(dropFirst(args));
        } else if (matches("info", command)) {
            succeeded = doInfo(dropFirst(args));
        } else {
            System.err.println("不明なコマンド: " + command);
            printUsage(PROGNAME);
            succeeded = false;
        }

        System.exit(succeeded ? 0 : 1);
    }

    private static boolean doAdd(String[] args) {
        final CommandLine commandLine;
        try {
            final CommandLineParser parser = new GnuParser();
            commandLine = parser.parse(OPTIONS_FOR_ADD, args);
        } catch (MissingArgumentException e) {
            System.err.println("値が指定されていません: " + e.getOption().getOpt());
            printUsage(PROGNAME);
            return false;
        } catch (MissingOptionException e) {
            System.err.println("必須オプションが指定されていません: " + e.getMissingOptions());
            printUsage(PROGNAME);
            return false;
        } catch (UnrecognizedOptionException e) {
            System.err.println("不明なオプションです: " + e.getOption());
            printUsage(PROGNAME);
            return false;
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            printUsage(PROGNAME);
            return false;
        }

        final String pkgName = commandLine.getOptionValue(PACKAGE_NAME.getOpt());
        final String versionStr = commandLine.getOptionValue(OBB_VERSION.getOpt());
        final Integer version = toInteger(versionStr);
        if (version == null) {
            System.err.println("バージョン番号が不正です: " + versionStr);
            printUsage(PROGNAME);
            return false;
        }
        final boolean isOverlay = commandLine.hasOption(OVERLAY_FLAG.getOpt());
        final String saltStr = commandLine.getOptionValue(SALT.getOpt());
        final byte[] salt;
        if (saltStr == null) {
            salt = null;
        } else {
            salt = toByteArray(saltStr, ObbInfoV1.SALT_LENGTH);
            if (salt == null) {
                System.err.println("ソルト文字列が不正です: " + saltStr);
                printUsage(PROGNAME);
                return false;
            }
        }

        final String[] nonRecognizedArgs = commandLine.getArgs();
        if (nonRecognizedArgs.length == 0) {
            System.err.println("ファイル名が指定されていません。");
            printUsage(PROGNAME);
            return false;
        }
        if (nonRecognizedArgs.length != 1) {
            System.err.println("引き数が多すぎます。");
            printUsage(PROGNAME);
            return false;
        }

        final File targetFile = new File(nonRecognizedArgs[0]);
        final RandomAccessFile targetRaFile;
        try {
            targetRaFile = new RandomAccessFile(targetFile, "rw");
        } catch (FileNotFoundException e) {
            System.err.println("対象ファイルが開けません: " + targetFile.getPath());
            return false;
        }
        try {
            try {
                final ObbInfoV1 info = ObbInfoV1.fromFile(targetRaFile);
                System.err.println("対象ファイルは既に OBB 情報を保持しています: " + info.toString());
                return false;
            } catch (IOException e) {
                System.err.println("対象ファイルの読み取りに失敗しました: " + targetFile.getPath());
                return false;
            } catch (NotObbException e) {
                // 正常系
            }

            final ObbInfoV1 obbInfo = new ObbInfoV1(isOverlay ? ObbInfoV1.FLAG_OVERLAY : 0, salt,
                    pkgName,
                    version.intValue());
            final ByteBuffer obbInfoBytes = obbInfo.toBytes();
            // 書き込み
            targetRaFile.setLength(targetRaFile.length() + obbInfoBytes.remaining());
            targetRaFile.seek(targetRaFile.length() - obbInfoBytes.remaining());
            targetRaFile.write(obbInfoBytes.array(), obbInfoBytes.arrayOffset(),
                    obbInfoBytes.remaining());
        } catch (IOException e) {
            System.err.println("OBB 情報の書き込みに失敗しました: " + targetFile.getPath());
            return false;
        } finally {
            try {
                targetRaFile.close();
            } catch (IOException e) {
                System.err.println("OBB 情報の書き込みに失敗しました: " + targetFile.getPath());
                return false;
            }
        }
        return true;
    }

    private static boolean doRemove(String[] args) {
        if (args.length != 1) {
            printUsage(PROGNAME);
            return false;
        }
        final File targetFile = new File(args[0]);
        final RandomAccessFile targetRaFile;
        try {
            targetRaFile = new RandomAccessFile(targetFile, "rw");
        } catch (FileNotFoundException e) {
            System.err.println("対象ファイルが開けません: " + targetFile.getPath());
            return false;
        }
        try {
            final ObbInfoV1 obbInfo;
            try {
                obbInfo = ObbInfoV1.fromFile(targetRaFile);
            } catch (IOException e) {
                System.err.println("対象ファイルの読み取りに失敗しました: " + targetFile.getPath());
                return false;
            } catch (NotObbException e) {
                System.err.println("対象ファイルは OBB 情報を保持していません: " + targetFile.getPath());
                return false;
            }

            final ByteBuffer obbInfoBytes = obbInfo.toBytes();
            targetRaFile.setLength(targetRaFile.length() - obbInfoBytes.remaining());
        } catch (IOException e) {
            System.err.println("OBB 情報の削除に失敗しました: " + targetFile.getPath());
            return false;
        } finally {
            try {
                targetRaFile.close();
            } catch (IOException e) {
                System.err.println("OBB 情報の削除に失敗しました: " + targetFile.getPath());
                return false;
            }
        }

        System.err.println("OBB 情報の削除が正常に完了しました: " + targetFile.getPath());
        return true;
    }

    private static boolean doInfo(String[] args) {
        if (args.length != 1) {
            printUsage(PROGNAME);
            return false;
        }
        final File targetFile = new File(args[0]);
        try {
            final ObbInfoV1 info = ObbInfoV1.fromFile(targetFile);
            System.out.println("OBB info for " + targetFile.getPath() + ":");
            info.prettyPrint(System.out);
        } catch (FileNotFoundException e) {
            System.err.println("対象ファイルを開けません: " + targetFile.getPath());
            return false;
        } catch (IOException e) {
            System.err.println("対象ファイルの読み取りに失敗しました: " + targetFile.getPath());
            return false;
        } catch (NotObbException e) {
            System.err.println("対象ファイルは OBB 情報を保持していません: " + targetFile.getPath());
            return false;
        }

        return true;
    }

    static String[] dropFirst(String[] source) {
        if (source == null || source.length == 0) {
            return source;
        }
        final String[] result = new String[source.length - 1];
        System.arraycopy(source, 1, result, 0, result.length);
        return result;
    }
}
