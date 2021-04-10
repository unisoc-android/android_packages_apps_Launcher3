package com.sprd.ext.meminfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MeminfoHelperTest {
    private static final String CONFIG_RAM_SIZE = "ro.deviceinfo.ram";
    private static final String RAM_SIZE = "ro.boot.ddrsize";
    private static final String DEFAULT_CONFIG = "unconfig";

    @Test
    public void testGetSystemTotalMemory() {
        MeminfoHelper helper = mock(MeminfoHelper.class);
        doCallRealMethod().when(helper).covertUnitsToSI(anyLong());
        doCallRealMethod().when(helper).getSystemTotalMemory();
        when(helper.getConfig(CONFIG_RAM_SIZE)).thenReturn("2000000000");
        when(helper.getConfig(RAM_SIZE)).thenReturn("2048M");
        assertEquals(2000L * 1000L * 1000L, helper.getSystemTotalMemory());
        when(helper.getConfig(CONFIG_RAM_SIZE)).thenReturn("123456");
        when(helper.getConfig(RAM_SIZE)).thenReturn("2048M");
        assertEquals(123456L, helper.getSystemTotalMemory());
        when(helper.getConfig(CONFIG_RAM_SIZE)).thenReturn(DEFAULT_CONFIG);
        when(helper.getConfig(RAM_SIZE)).thenReturn("1M");
        assertEquals(1000L * 1000L, helper.getSystemTotalMemory());
        when(helper.getConfig(CONFIG_RAM_SIZE)).thenReturn(DEFAULT_CONFIG);
        when(helper.getConfig(RAM_SIZE)).thenReturn("2048M");
        assertEquals(2000L * 1000L * 1000L, helper.getSystemTotalMemory());
    }

    @Test
    public void testGetTotalMemString() {
        MeminfoHelper helper = MeminfoHelper.getInstance(RuntimeEnvironment.application);
        helper.setTotalMemSize(2000L * 1000L * 1000L);
        assertEquals("2.0 GB", helper.getTotalMemString());
        helper.setTotalMemSize(1234567000L);
        assertEquals("1.2 GB", helper.getTotalMemString());
        helper.setTotalMemSize(123456000L);
        assertEquals("123 MB", helper.getTotalMemString());
    }

    @Test
    public void testGetAvailMemString() {
        MeminfoHelper helper = MeminfoHelper.getInstance(RuntimeEnvironment.application);
        helper.setAvailMemSize(2000L * 1000L * 1000L);
        assertEquals("2.0 GB", helper.getAvailMemString());
        helper.setAvailMemSize(1234567000L);
        assertEquals("1.2 GB", helper.getAvailMemString());
        helper.setAvailMemSize(123456000L);
        assertEquals("123 MB", helper.getAvailMemString());
    }
}
