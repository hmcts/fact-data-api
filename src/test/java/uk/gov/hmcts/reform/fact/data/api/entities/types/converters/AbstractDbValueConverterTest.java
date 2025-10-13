package uk.gov.hmcts.reform.fact.data.api.entities.types.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

class AbstractDbValueConverterTest {

    // trivial enum
    @RequiredArgsConstructor
    @Getter
    enum TestEnum implements HasDbValue {
        TEST_1("test-1"),
        TEST_2("test-two"),
        TEST_3("third-test");

        private final String dbValue;
    }

    // trivial converter
    private static final class TestEnumDbValueConverter extends AbstractDbValueConverter<TestEnum> {
        TestEnumDbValueConverter() {
            super(TestEnum.values());
        }
    }

    // broken converter
    private static final class BrokenDbValueConverter extends AbstractDbValueConverter<TestEnum> {
        BrokenDbValueConverter() {
            super(null);
        }
    }

    @Test
    void shouldThrowNPEForNullConstructorValues() {
        assertThrows(NullPointerException.class, BrokenDbValueConverter::new);
    }

    @Test
    void shouldConvertToDbValueForValidEnum() {
        var converter = new TestEnumDbValueConverter();
        for (TestEnum testEnum : TestEnum.values()) {
            assertEquals(testEnum.getDbValue(), converter.convertToDatabaseColumn(testEnum));
        }
    }

    @Test
    void shouldConvertToEnumForValidDbValue() {
        var converter = new TestEnumDbValueConverter();
        for (TestEnum testEnum : TestEnum.values()) {
            assertEquals(testEnum, converter.convertToEntityAttribute(testEnum.getDbValue()));
        }
    }

    @Test
    void shouldReturnNullObjectForNullDbValue() {
        var converter = new TestEnumDbValueConverter();
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void shouldReturnNullDbValueForNullEnum() {
        var converter = new TestEnumDbValueConverter();
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void shouldThrowExceptionForInvalidDbValue() {
        var converter = new TestEnumDbValueConverter();
        assertThrows(
            IllegalArgumentException.class,
            () -> converter.convertToEntityAttribute("invalid")
        );
    }
}
