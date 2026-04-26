package com.github.MadyarovGleb.booking_mvp.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Юнит-тесты для UniversityMapper")
class UniversityMapperTest {

    // --- TO DB CODE ---

    @Test
    @DisplayName("Маппинг в БД код: SPBSTU -> spbptu")
    void toDbCode_ShouldMapSPBSTU_ToSpbptu() {
        String result = UniversityMapper.toDbCode("SPBSTU");
        assertThat(result).isEqualTo("spbptu");
    }

    @Test
    @DisplayName("Маппинг в БД код: SPBU -> spbgu")
    void toDbCode_ShouldMapSPBU_ToSpbgu() {
        String result = UniversityMapper.toDbCode("SPBU");
        assertThat(result).isEqualTo("spbgu");
    }

    @Test
    @DisplayName("Маппинг в БД код: LETI -> leti")
    void toDbCode_ShouldMapLETI_ToLeti() {
        String result = UniversityMapper.toDbCode("LETI");
        assertThat(result).isEqualTo("leti");
    }

    @Test
    @DisplayName("Маппинг в БД код: lowercase input (spbstu -> spbptu)")
    void toDbCode_ShouldHandleLowercaseInput() {
        String result = UniversityMapper.toDbCode("spbstu");
        assertThat(result).isEqualTo("spbptu");
    }

    @Test
    @DisplayName("Маппинг в БД код: mixed case input (SpBsTu -> spbptu)")
    void toDbCode_ShouldHandleMixedCaseInput() {
        String result = UniversityMapper.toDbCode("SpBsTu");
        assertThat(result).isEqualTo("spbptu");
    }

    @Test
    @DisplayName("Маппинг в БД код: null input -> null")
    void toDbCode_ShouldReturnNull_WhenInputIsNull() {
        String result = UniversityMapper.toDbCode(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Маппинг в БД код: unknown code -> null")
    void toDbCode_ShouldReturnNull_WhenCodeIsUnknown() {
        String result = UniversityMapper.toDbCode("UNKNOWN");
        assertThat(result).isNull();
    }

    // --- TO FRONTEND CODE ---

    @Test
    @DisplayName("Маппинг во frontend код: spbptu -> SPBSTU")
    void toFrontendCode_ShouldMapSpbptu_ToSPBSTU() {
        String result = UniversityMapper.toFrontendCode("spbptu");
        assertThat(result).isEqualTo("SPBSTU");
    }

    @Test
    @DisplayName("Маппинг во frontend код: spbgu -> SPBU")
    void toFrontendCode_ShouldMapSpbgu_ToSPBU() {
        String result = UniversityMapper.toFrontendCode("spbgu");
        assertThat(result).isEqualTo("SPBU");
    }

    @Test
    @DisplayName("Маппинг во frontend код: leti -> LETI")
    void toFrontendCode_ShouldMapLeti_ToLETI() {
        String result = UniversityMapper.toFrontendCode("leti");
        assertThat(result).isEqualTo("LETI");
    }

    @Test
    @DisplayName("Маппинг во frontend код: uppercase input (SPBPTU -> SPBSTU)")
    void toFrontendCode_ShouldHandleUppercaseInput() {
        String result = UniversityMapper.toFrontendCode("SPBPTU");
        assertThat(result).isEqualTo("SPBSTU");
    }

    @Test
    @DisplayName("Маппинг во frontend код: mixed case input (SpBgU -> SPBU)")
    void toFrontendCode_ShouldHandleMixedCaseInput() {
        String result = UniversityMapper.toFrontendCode("SpBgU");
        assertThat(result).isEqualTo("SPBU");
    }

    @Test
    @DisplayName("Маппинг во frontend код: null input -> null")
    void toFrontendCode_ShouldReturnNull_WhenInputIsNull() {
        String result = UniversityMapper.toFrontendCode(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Маппинг во frontend код: unknown code -> null")
    void toFrontendCode_ShouldReturnNull_WhenCodeIsUnknown() {
        String result = UniversityMapper.toFrontendCode("unknown");
        assertThat(result).isNull();
    }

    // --- ROUND-TRIP TESTS ---

    @Test
    @DisplayName("Round-trip: SPBSTU -> spbptu -> SPBSTU")
    void roundTrip_ShouldPreserveOriginalValue_ForSPBSTU() {
        String dbCode = UniversityMapper.toDbCode("SPBSTU");
        String frontendCode = UniversityMapper.toFrontendCode(dbCode);
        assertThat(frontendCode).isEqualTo("SPBSTU");
    }

    @Test
    @DisplayName("Round-trip: spbgu -> SPBU -> spbgu")
    void roundTrip_ShouldPreserveOriginalValue_ForSpbgu() {
        String frontendCode = UniversityMapper.toFrontendCode("spbgu");
        String dbCode = UniversityMapper.toDbCode(frontendCode);
        assertThat(dbCode).isEqualTo("spbgu");
    }
}
