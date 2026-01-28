package uk.gov.hmcts.reform.fact.data.api.services.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostcodeLadderTest {

    @Test
    void fromPartialPostcodeShouldHandleFullPostcodeWithUnit() {
        PostcodeLadder ladder = PostcodeLadder.fromPartialPostcode("sw1a 1aa");

        assertThat(ladder.getMinusUnitNoSpace()).isEqualTo("SW1A1");
        assertThat(ladder.getOutCodeNoSpace()).isEqualTo("SW1A");
        assertThat(ladder.getAreacodeNoSpace()).isEqualTo("SW");
    }

    @Test
    void fromPartialPostcodeShouldHandlePostcodeWithoutUnit() {
        PostcodeLadder ladder = PostcodeLadder.fromPartialPostcode("SW1A 1");

        assertThat(ladder.getMinusUnitNoSpace()).isEqualTo("SW1A1");
        assertThat(ladder.getOutCodeNoSpace()).isEqualTo("SW1A");
        assertThat(ladder.getAreacodeNoSpace()).isEqualTo("SW");
    }

    @Test
    void fromPartialPostcodeShouldHandleNullInput() {
        PostcodeLadder ladder = PostcodeLadder.fromPartialPostcode(null);

        assertThat(ladder.getMinusUnitNoSpace()).isEmpty();
        assertThat(ladder.getOutCodeNoSpace()).isEmpty();
        assertThat(ladder.getAreacodeNoSpace()).isEmpty();
    }
}
