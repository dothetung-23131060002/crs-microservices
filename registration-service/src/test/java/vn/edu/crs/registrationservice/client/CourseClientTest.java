package vn.edu.crs.registrationservice.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import vn.edu.crs.registrationservice.exception.CourseServiceUnavailableException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CourseClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private CourseClient courseClient;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        courseClient = new CourseClient(restTemplate, "http://course-service:8082/");
    }

    @Test
    void reserveSeatCallsExpectedInternalPatchEndpoint() {
        server.expect(once(), requestTo("http://course-service:8082/internal/courses/12/reserve-seat"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess());

        assertThatCode(() -> courseClient.reserveSeat(12L)).doesNotThrowAnyException();

        server.verify();
    }

    @Test
    void reserveSeatMapsConflictToBusinessConflict() {
        server.expect(once(), requestTo("http://course-service:8082/internal/courses/12/reserve-seat"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> courseClient.reserveSeat(12L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mon hoc da het cho");

        server.verify();
    }

    @Test
    void reserveSeatMapsNotFoundToBusinessConflict() {
        server.expect(once(), requestTo("http://course-service:8082/internal/courses/9999/reserve-seat"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> courseClient.reserveSeat(9999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mon hoc khong ton tai");

        server.verify();
    }

    @Test
    void releaseSeatCallsExpectedInternalPatchEndpoint() {
        server.expect(once(), requestTo("http://course-service:8082/internal/courses/12/release-seat"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess());

        assertThatCode(() -> courseClient.releaseSeat(12L)).doesNotThrowAnyException();

        server.verify();
    }

    @Test
    void serverErrorProducesExplicitUnavailableException() {
        server.expect(once(), requestTo("http://course-service:8082/internal/courses/12/reserve-seat"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> courseClient.reserveSeat(12L))
                .isInstanceOf(CourseServiceUnavailableException.class)
                .hasMessage("Khong the ket noi toi course-service, vui long thu lai sau");

        server.verify();
    }

    @Test
    void connectionFailureProducesExplicitUnavailableException() {
        RestTemplate unavailableRestTemplate = mock(RestTemplate.class);
        CourseClient unavailableClient = new CourseClient(unavailableRestTemplate, "http://localhost:8082");
        when(unavailableRestTemplate.exchange(
                anyString(), eq(HttpMethod.PATCH), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> unavailableClient.reserveSeat(12L))
                .isInstanceOf(CourseServiceUnavailableException.class)
                .hasMessage("Khong the ket noi toi course-service, vui long thu lai sau");
    }
}
