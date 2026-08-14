package com.saneb.domain.announcementsource.provider.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.saneb.domain.announcementsource.provider.content.ProviderContentCodes.FailureCode;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderContentUrlValidatorTest {

    @Test
    void selectValidatedRequestAllowsOnlyRegisteredExactHost() {
        ProviderContentUrlValidator validator = validator(address(8, 8, 8, 8));

        ProviderContentUrlValidator.ValidatedRequest request = validator.selectValidatedRequest(
                "https://CITY.EXAMPLE.GO.KR/notices",
                "https://city.example.go.kr/notices/42"
        );

        assertThat(request.allowedHost()).isEqualTo("city.example.go.kr");
        assertThat(request.detailUri()).isEqualTo(URI.create("https://city.example.go.kr/notices/42"));
        assertThatThrownBy(() -> validator.selectValidatedRequest(
                "https://city.example.go.kr/notices",
                "https://sub.city.example.go.kr/notices/42"
        )).isInstanceOfSatisfying(ProviderContentValidationException.class, exception ->
                assertThat(exception.selectFailureCode()).isEqualTo(FailureCode.DETAIL_HOST_NOT_ALLOWED)
        );
    }

    @Test
    void selectRedirectUriAllowsRelativeSameHostOnly() {
        ProviderContentUrlValidator validator = validator(address(8, 8, 8, 8));
        URI current = URI.create("https://city.example.go.kr/notices/42");

        assertThat(validator.selectRedirectUri(current, "../detail/43", "city.example.go.kr"))
                .isEqualTo(URI.create("https://city.example.go.kr/detail/43"));
        assertThatThrownBy(() -> validator.selectRedirectUri(
                current,
                "https://outside.example/detail/43",
                "city.example.go.kr"
        )).isInstanceOfSatisfying(ProviderContentValidationException.class, exception ->
                assertThat(exception.selectFailureCode()).isEqualTo(FailureCode.DETAIL_HOST_NOT_ALLOWED)
        );
    }

    @Test
    void selectRequestTargetReturnsOnlyValidatedPublicAddresses() {
        InetAddress first = address(8, 8, 8, 8);
        InetAddress second = address(1, 1, 1, 1);
        ProviderContentUrlValidator validator = new ProviderContentUrlValidator(
                host -> new InetAddress[]{first, second}
        );

        ProviderContentRequestTarget target = validator.selectRequestTarget(
                URI.create("https://city.example.go.kr/notices/42"),
                "city.example.go.kr"
        );

        assertThat(target.uri()).isEqualTo(URI.create("https://city.example.go.kr/notices/42"));
        assertThat(target.allowedHost()).isEqualTo("city.example.go.kr");
        assertThat(target.pinnedAddresses()).containsExactly(first, second);
    }

    @Test
    void selectValidatedRequestBlocksPrivateLoopbackAndLinkLocalAddresses() {
        List<InetAddress> blocked = List.of(
                address(10, 1, 2, 3),
                address(127, 0, 0, 1),
                address(169, 254, 1, 2),
                address(192, 168, 1, 2)
        );

        for (InetAddress address : blocked) {
            ProviderContentUrlValidator validator = validator(address);
            assertThatThrownBy(() -> validator.selectValidatedRequest(
                    "https://city.example.go.kr/notices",
                    "https://city.example.go.kr/notices/42"
            )).isInstanceOfSatisfying(ProviderContentValidationException.class, exception ->
                    assertThat(exception.selectFailureCode()).isEqualTo(FailureCode.ADDRESS_BLOCKED)
            );
        }
    }

    @Test
    void selectValidatedRequestRejectsUnsafeUriParts() {
        ProviderContentUrlValidator validator = validator(address(8, 8, 8, 8));

        assertThatThrownBy(() -> validator.selectValidatedRequest(
                "https://user:password@city.example.go.kr/notices",
                "https://city.example.go.kr/notices/42"
        )).isInstanceOfSatisfying(ProviderContentValidationException.class, exception ->
                assertThat(exception.selectFailureCode()).isEqualTo(FailureCode.SOURCE_URL_INVALID)
        );
        assertThatThrownBy(() -> validator.selectValidatedRequest(
                "https://city.example.go.kr:8443/notices",
                "https://city.example.go.kr/notices/42"
        )).isInstanceOfSatisfying(ProviderContentValidationException.class, exception ->
                assertThat(exception.selectFailureCode()).isEqualTo(FailureCode.SOURCE_URL_INVALID)
        );
        assertThatThrownBy(() -> validator.selectValidatedRequest(
                "https://city.example.go.kr/notices",
                "file:///etc/passwd"
        )).isInstanceOfSatisfying(ProviderContentValidationException.class, exception ->
                assertThat(exception.selectFailureCode()).isEqualTo(FailureCode.DETAIL_URL_INVALID)
        );
    }

    private ProviderContentUrlValidator validator(InetAddress address) {
        return new ProviderContentUrlValidator(host -> new InetAddress[]{address});
    }

    private static InetAddress address(int first, int second, int third, int fourth) {
        try {
            return InetAddress.getByAddress(new byte[]{
                    (byte) first,
                    (byte) second,
                    (byte) third,
                    (byte) fourth
            });
        } catch (UnknownHostException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
