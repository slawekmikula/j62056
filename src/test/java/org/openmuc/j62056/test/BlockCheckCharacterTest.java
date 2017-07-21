package org.openmuc.j62056.test;

import org.junit.Assert;
import org.junit.Test;
import org.openmuc.j62056.internal.HexConverter;

public class BlockCheckCharacterTest {

    @Test
    public void testBccExamplesTest() {
        System.out.println(HexConverter.toShortHexString(0x21 ^ 0x32 ^ 0x03 ^ 0x51));
        Assert.assertEquals(0x41, 0x21 ^ 0x32 ^ 0x03 ^ 0x51);
        Assert.assertEquals(0x05, 0x32 ^ 0x31 ^ 0x33 ^ 0x32 ^ 0x30 ^ 0x33 ^ 0x35 ^ 0x31);
    }

}
