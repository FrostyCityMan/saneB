import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.stream.Collectors;

/** 고정 공개 호스트의 DNS만 관측한다. HTTP/파일/DB 접근과 설정 변경은 하지 않는다. */
class AttachmentDnsDiagnostic {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.exit(2);
            return;
        }
        String host = switch (args[0]) {
            case "BOEUN" -> "www.boeun.go.kr";
            case "JECHEON" -> "www.jecheon.go.kr";
            default -> null;
        };
        if (host == null) {
            System.exit(2);
            return;
        }
        try {
            String addresses = Arrays.stream(InetAddress.getAllByName(host))
                    .map(InetAddress::getHostAddress).distinct().sorted().collect(Collectors.joining(","));
            System.out.println(addresses.isEmpty() ? "NO_ADDRESS" : "RESOLVED\t" + addresses);
        } catch (UnknownHostException failure) {
            System.out.println("UNKNOWN_HOST");
        } catch (RuntimeException failure) {
            // 예외 메시지·환경·경로·인증정보를 출력하지 않는다.
            System.out.println("RESOLVER_FAILED");
        }
    }
}
