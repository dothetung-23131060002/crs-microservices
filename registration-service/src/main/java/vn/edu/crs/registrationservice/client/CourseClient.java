package vn.edu.crs.registrationservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import vn.edu.crs.registrationservice.exception.CourseServiceUnavailableException;

@Component
public class CourseClient {

    private static final String UNAVAILABLE_MESSAGE =
            "Khong the ket noi toi course-service, vui long thu lai sau";

    private final RestTemplate restTemplate;
    private final String courseServiceBaseUrl;

    public CourseClient(
            RestTemplate restTemplate,
            @Value("${course-service.base-url}") String courseServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.courseServiceBaseUrl = stripTrailingSlash(courseServiceBaseUrl);
    }

    public void reserveSeat(Long courseId) {
        String url = courseUrl(courseId, "reserve-seat");
        try {
            restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity.EMPTY, Void.class);
        } catch (HttpClientErrorException.Conflict exception) {
            throw new IllegalStateException("Mon hoc da het cho", exception);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new IllegalStateException("Mon hoc khong ton tai", exception);
        } catch (HttpClientErrorException exception) {
            throw new IllegalStateException("Course-service tu choi yeu cau dat cho", exception);
        } catch (HttpServerErrorException | ResourceAccessException exception) {
            throw new CourseServiceUnavailableException(UNAVAILABLE_MESSAGE, exception);
        } catch (RestClientException exception) {
            throw new CourseServiceUnavailableException(UNAVAILABLE_MESSAGE, exception);
        }
    }

    public void releaseSeat(Long courseId) {
        String url = courseUrl(courseId, "release-seat");
        try {
            restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity.EMPTY, Void.class);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new IllegalStateException("Mon hoc khong ton tai", exception);
        } catch (HttpClientErrorException exception) {
            throw new IllegalStateException("Course-service tu choi yeu cau hoan cho", exception);
        } catch (HttpServerErrorException | ResourceAccessException exception) {
            throw new CourseServiceUnavailableException(UNAVAILABLE_MESSAGE, exception);
        } catch (RestClientException exception) {
            throw new CourseServiceUnavailableException(UNAVAILABLE_MESSAGE, exception);
        }
    }

    private String courseUrl(Long courseId, String operation) {
        return courseServiceBaseUrl + "/internal/courses/" + courseId + "/" + operation;
    }

    private static String stripTrailingSlash(String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }
}
