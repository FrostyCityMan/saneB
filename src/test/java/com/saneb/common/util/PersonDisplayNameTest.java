package com.saneb.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class PersonDisplayNameTest {
    @Test void usesNameAndLoginWithoutInternalIdentifierFallback() {
        assertThat(PersonDisplayName.format(" 성함 ", " login ")).isEqualTo("성함 (login)");
        assertThat(PersonDisplayName.format(null, "login")).isEqualTo("login");
        assertThat(PersonDisplayName.format("성함", null)).isEqualTo("성함");
        assertThat(PersonDisplayName.format(" ", null)).isEqualTo("계정 정보 확인 필요");
    }
}
