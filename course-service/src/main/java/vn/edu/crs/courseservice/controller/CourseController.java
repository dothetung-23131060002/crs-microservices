package vn.edu.crs.courseservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.crs.courseservice.entity.Course;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    /**
     * GET /courses — Trả về danh sách mock 2 môn học.
     * Buổi 1: Chưa kết nối DB, chỉ dùng dữ liệu giả để xác nhận routing hoạt động.
     * Buổi 2+: Sẽ thay bằng Repository/Service thật.
     */
    @GetMapping
    public List<Course> getAllCourses() {
        Course course1 = Course.builder()
                .id(1L)
                .name("Lập trình Java")
                .description("Học lập trình Java cơ bản đến nâng cao")
                .credits(3)
                .soChoConLai(30)
                .totalSlots(40)
                .build();

        Course course2 = Course.builder()
                .id(2L)
                .name("Cơ sở dữ liệu")
                .description("Thiết kế và quản trị cơ sở dữ liệu quan hệ")
                .credits(4)
                .soChoConLai(25)
                .totalSlots(35)
                .build();

        return Arrays.asList(course1, course2);
    }
}
