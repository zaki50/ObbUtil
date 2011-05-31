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

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

/**
 *
 */
public class ObbInfoV1Test {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testToBytes() {
        final ObbInfoV1 info = create(0, null, null, null);

        final byte[] expected = new byte[] {
                1, 0, 0, 0, //
                1, 0, 0, 0, //
                0, 0, 0, 0, //
                0, 0, 0, 0, 0, 0, 0, 0, //
                16, 0, 0, 0, //
                106, 112, 46, 97, 110, 100, 101, 98, 46, 111, 98, 98, 117, 116, 105, 108, //
                40, 0, 0, 0, //
                -125, -103, 5, 1
        };
        assertTrue(Arrays.equals(expected, info.toBytes().array()));
    }

    private static ObbInfoV1 create(int flags, byte[] salt, String packageName,
            Integer packageVersion) {
        if (packageName == null) {
            packageName = "jp.andeb.obbutil";
        }
        if (packageVersion == null) {
            packageVersion = Integer.valueOf(1);
        }
        final ObbInfoV1 info = new ObbInfoV1(flags, salt, packageName, packageVersion.intValue());
        return info;
    }
}
