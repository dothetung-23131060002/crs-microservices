// path: course-service/src/main/java/vn/edu/crs/courseservice/controller/InternalCourseController.java
// purpose: controller rieng cho cac API noi bo, chi danh cho registration-service goi sang
// (file MOI - them vao thu muc controller, khong ghi de file nao)

package vn.edu.crs.courseservice.controller;

import vn.edu.crs.courseservice.dto.CourseDTO;
import vn.edu.crs.courseservice.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/courses")
@RequiredArgsConstructor
public class InternalCourseController {

    private final CourseService courseService;

    @PatchMapping("/{id}/reserve-seat")
    public CourseDTO reserveSeat(@PathVariable Long id) {
        return courseService.reserveSeat(id);
    }

    @PatchMapping("/{id}/release-seat")
    public CourseDTO releaseSeat(@PathVariable Long id) {
        return courseService.releaseSeat(id);
    }
}
