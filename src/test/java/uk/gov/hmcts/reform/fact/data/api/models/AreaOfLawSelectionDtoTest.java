
package uk.gov.hmcts.reform.fact.data.api.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class AreaOfLawSelectionDtoTest {

    static AreaOfLawType adoption = AreaOfLawType.builder().id(UUID.randomUUID()).name("Adoption").build();
    static AreaOfLawType children = AreaOfLawType.builder().id(UUID.randomUUID()).name("Children").build();

    @Test
    void fromShouldMapFieldsAndDefaultSelectedFalse() {

        AreaOfLawSelectionDto dto = AreaOfLawSelectionDto.from(adoption);

        assertEquals(adoption.getId(), dto.getId());
        assertEquals(adoption.getName(), dto.getName());
        assertEquals(adoption.getNameCy(), dto.getNameCy());
        assertEquals(Boolean.FALSE, dto.getSelected(), "Selected should default to FALSE");
    }

    @Test
    void asSelectedShouldMapFieldsAndSetSelectedTrue() {

        AreaOfLawSelectionDto dto = AreaOfLawSelectionDto.asSelected(children);

        assertEquals(children.getId(), dto.getId());
        assertEquals(children.getName(), dto.getName());
        assertEquals(children.getNameCy(), dto.getNameCy());
        assertEquals(Boolean.TRUE, dto.getSelected(), "Selected should be TRUE for asSelected()");
    }

    @Test
    void asUnselectedShouldBehaveLikeFrom() {

        AreaOfLawSelectionDto viaFrom = AreaOfLawSelectionDto.from(adoption);
        AreaOfLawSelectionDto viaUnselected = AreaOfLawSelectionDto.asUnselected(adoption);

        assertEquals(viaFrom, viaUnselected, "asUnselected should be an alias for from()");
        assertEquals(Boolean.FALSE, viaUnselected.getSelected(), "Selected should remain FALSE");
    }

    @Test
    void builderShouldDefaultSelectedToFalse() {
        UUID id = UUID.randomUUID();

        AreaOfLawSelectionDto dto = AreaOfLawSelectionDto.builder()
            .id(id)
            .name("Probate")
            .build();

        assertEquals(Boolean.FALSE, dto.getSelected());
    }

    @Test
    void settersAndGettersShouldWorkAsExpected() {
        UUID id = UUID.randomUUID();
        AreaOfLawSelectionDto dto = new AreaOfLawSelectionDto();

        dto.setId(id);
        dto.setName("Adoption");
        dto.setSelected(Boolean.TRUE);

        assertEquals(id, dto.getId());
        assertEquals("Adoption", dto.getName());
        assertEquals(Boolean.TRUE, dto.getSelected());
    }
}
