package uk.gov.hmcts.reform.fact.data.api.services.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostcodeLadderTest {

    @Test
    void fromPartialPostcodeShouldHandleFullPostcodeWithUnit() {
        PostcodeLadder ladder = PostcodeLadder.fromPartialPostcode("sw1a 1aa");

        assertThat(ladder.minusUnitNoSpace()).isEqualTo("SW1A1");
        assertThat(ladder.outcodeNoSpace()).isEqualTo("SW1A");
        assertThat(ladder.areacodeNoSpace()).isEqualTo("SW");
    }

    @Test
    void fromPartialPostcodeShouldHandlePostcodeWithoutUnit() {
        PostcodeLadder ladder = PostcodeLadder.fromPartialPostcode("SW1A 1");

        assertThat(ladder.minusUnitNoSpace()).isEqualTo("SW1A1");
        assertThat(ladder.outcodeNoSpace()).isEqualTo("SW1A");
        assertThat(ladder.areacodeNoSpace()).isEqualTo("SW");
    }

    @Test
    void fromPartialPostcodeShouldHandleNullInput() {
        PostcodeLadder ladder = PostcodeLadder.fromPartialPostcode(null);

        assertThat(ladder.minusUnitNoSpace()).isEmpty();
        assertThat(ladder.outcodeNoSpace()).isEmpty();
        assertThat(ladder.areacodeNoSpace()).isEmpty();
    }
}
