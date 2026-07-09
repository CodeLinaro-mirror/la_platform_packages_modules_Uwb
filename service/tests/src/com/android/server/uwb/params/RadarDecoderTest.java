/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.uwb.params;

import static com.google.common.truth.Truth.assertThat;

import android.platform.test.annotations.Presubmit;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.server.uwb.UwbInjector;
import com.android.server.uwb.util.UwbUtil;

import com.google.uwb.support.fira.FiraParams;
import com.google.uwb.support.fira.FiraProtocolVersion;
import com.google.uwb.support.radar.RadarData;
import com.google.uwb.support.radar.RadarParams;
import com.google.uwb.support.radar.RadarParams.RadarCapabilityFlag;
import com.google.uwb.support.radar.RadarRangingStartedParams;
import com.google.uwb.support.radar.RadarSpecificationParams;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.EnumSet;

/** Unit tests for {@link com.android.server.uwb.params.RadarDecoder}. */
@RunWith(AndroidJUnit4.class)
@SmallTest
@Presubmit
public class RadarDecoderTest {
    private static final FiraProtocolVersion PROTOCOL_VERSION_DUMMY = new FiraProtocolVersion(0, 0);
    public static final String TEST_RADAR_SPECIFICATION_TLV_DATA_STRING = "b00101";
    private static final byte[] TEST_RADAR_SPECIFICATION_TLV_DATA =
            UwbUtil.getByteArray(TEST_RADAR_SPECIFICATION_TLV_DATA_STRING);
    public static final int TEST_RADAR_SPECIFICATION_TLV_NUM_PARAMS = 1;
    private static final EnumSet<RadarCapabilityFlag> RADAR_CAPABILITIES =
            EnumSet.of(RadarCapabilityFlag.HAS_RADAR_SWEEP_SAMPLES_SUPPORT);

    private static final byte[] TEST_RADAR_RANGING_STARTED_TLV_DATA =
            UwbUtil.getByteArray(
                    "000764000000280010"
                            + "010110"
                            + "020105"
                            + "0302ffff"
                            + "040103"
                            + "050109"
                            + "06015a"
                            + "070163"
                            + "080100"
                            + "090101"
                            + "0a02e803"
                            + "0b0100"
                            + "a0020801"
                            + "a1010c"
                            + "a20118"
                            + "a3011e");
    private static final int TEST_RADAR_RANGING_STARTED_TLV_NUM_PARAMS = 16;

    private static final byte[] TEST_RADAR_RANGING_STARTED_NO_OPTIONAL_TLV_DATA =
            UwbUtil.getByteArray(
                    "000764000000280010"
                            + "010110"
                            + "020105"
                            + "0302ffff"
                            + "040103"
                            + "050109"
                            + "06015a"
                            + "070163"
                            + "080100"
                            + "090101"
                            + "0a02e803"
                            + "0b0100");
    private static final int TEST_RADAR_RANGING_STARTED_NO_OPTIONAL_TLV_NUM_PARAMS = 12;

    private final RadarDecoder mRadarDecoder = new RadarDecoder();
    @Mock
    private UwbInjector mUwbInjector;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public static void verifyRadarSpecification(RadarSpecificationParams radarSpecificationParams) {
        assertThat(radarSpecificationParams).isNotNull();
        assertThat(radarSpecificationParams.getRadarCapabilities()).isEqualTo(RADAR_CAPABILITIES);
    }

    public static void verifyRadarRangingStarted(
            RadarRangingStartedParams params, boolean hasOptional) {
        assertThat(params).isNotNull();
        assertThat(params.getBurstPeriod()).isEqualTo(100);
        assertThat(params.getSweepPeriod()).isEqualTo(40);
        assertThat(params.getSweepsPerBurst()).isEqualTo(16);
        assertThat(params.getSamplesPerSweep()).isEqualTo(16);
        assertThat(params.getChannelNumber()).isEqualTo(FiraParams.UWB_CHANNEL_5);
        assertThat(params.getSweepOffset()).isEqualTo(-1);
        assertThat(params.getRframeConfig()).isEqualTo(FiraParams.RFRAME_CONFIG_SP3);
        assertThat(params.getPreambleDuration())
                .isEqualTo(RadarParams.PREAMBLE_DURATION_T16384_SYMBOLS);
        assertThat(params.getPreambleCodeIndex()).isEqualTo(90);
        assertThat(params.getSessionPriority()).isEqualTo(99);
        assertThat(params.getBitsPerSample()).isEqualTo(RadarParams.BITS_PER_SAMPLES_32);
        assertThat(params.getPrfMode()).isEqualTo(FiraParams.PRF_MODE_HPRF);
        assertThat(params.getNumberOfBursts()).isEqualTo(1000);
        assertThat(params.getRadarDataType())
                .isEqualTo(RadarParams.RADAR_DATA_TYPE_RADAR_SWEEP_SAMPLES);
        if (hasOptional) {
            assertThat(params.getAntennaBitmap()).isEqualTo(0x0108);
            assertThat(params.getGpioBitmap()).isEqualTo(0x0C);
            assertThat(params.getTxPower()).isEqualTo(24);
            assertThat(params.getRxGain()).isEqualTo(30);
        } else {
            assertThat(params.getAntennaBitmap()).isEqualTo(0);
            assertThat(params.getGpioBitmap()).isEqualTo(0);
            assertThat(params.getTxPower()).isEqualTo(0);
            assertThat(params.getRxGain()).isEqualTo(0);
        }
    }

    @Test
    public void testGetParams_invalidParamType() throws Exception {
        TlvDecoderBuffer tlvDecoderBuffer =
                new TlvDecoderBuffer(
                        TEST_RADAR_SPECIFICATION_TLV_DATA, TEST_RADAR_SPECIFICATION_TLV_NUM_PARAMS);

        assertThat(mRadarDecoder.getParams(tlvDecoderBuffer, RadarData.class,
                                 PROTOCOL_VERSION_DUMMY)).isNull();
    }

    @Test
    public void testGetRadarSpecification() throws Exception {
        TlvDecoderBuffer tlvDecoderBuffer =
                new TlvDecoderBuffer(
                        TEST_RADAR_SPECIFICATION_TLV_DATA, TEST_RADAR_SPECIFICATION_TLV_NUM_PARAMS);
        assertThat(tlvDecoderBuffer.parse()).isTrue();

        RadarSpecificationParams radarSpecificationParams =
                mRadarDecoder.getParams(tlvDecoderBuffer, RadarSpecificationParams.class,
                                 PROTOCOL_VERSION_DUMMY);
        verifyRadarSpecification(radarSpecificationParams);
    }

    @Test
    public void testGetRadarSpecificationViaTlvDecoder() throws Exception {
        TlvDecoderBuffer tlvDecoderBuffer =
                new TlvDecoderBuffer(
                        TEST_RADAR_SPECIFICATION_TLV_DATA, TEST_RADAR_SPECIFICATION_TLV_NUM_PARAMS);
        assertThat(tlvDecoderBuffer.parse()).isTrue();

        RadarSpecificationParams radarSpecificationParams =
                TlvDecoder.getDecoder(RadarParams.PROTOCOL_NAME, mUwbInjector)
                        .getParams(tlvDecoderBuffer, RadarSpecificationParams.class,
                                 PROTOCOL_VERSION_DUMMY);
        verifyRadarSpecification(radarSpecificationParams);
    }

    @Test
    public void testGetRadarRangingStarted() throws Exception {
        TlvDecoderBuffer tlvDecoderBuffer =
                new TlvDecoderBuffer(
                        TEST_RADAR_RANGING_STARTED_TLV_DATA,
                        TEST_RADAR_RANGING_STARTED_TLV_NUM_PARAMS);
        assertThat(tlvDecoderBuffer.parse()).isTrue();

        RadarRangingStartedParams params =
                mRadarDecoder.getParams(tlvDecoderBuffer, RadarRangingStartedParams.class,
                        PROTOCOL_VERSION_DUMMY);
        verifyRadarRangingStarted(params, true);
    }

    @Test
    public void testGetRadarRangingStarted_noOptional() throws Exception {
        TlvDecoderBuffer tlvDecoderBuffer =
                new TlvDecoderBuffer(
                        TEST_RADAR_RANGING_STARTED_NO_OPTIONAL_TLV_DATA,
                        TEST_RADAR_RANGING_STARTED_NO_OPTIONAL_TLV_NUM_PARAMS);
        assertThat(tlvDecoderBuffer.parse()).isTrue();

        RadarRangingStartedParams params =
                mRadarDecoder.getParams(tlvDecoderBuffer, RadarRangingStartedParams.class,
                        PROTOCOL_VERSION_DUMMY);
        verifyRadarRangingStarted(params, false);
    }
}
