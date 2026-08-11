// path: course-service/src/main/java/vn/edu/crs/courseservice/controller/CourseController.java
// purpose: giu nguyen POST/PUT/DELETE/GET-by-id cua Buoi 2, THAY THE rieng phuong thuc
//          getAll() cu bang phuong thuc search() moi ben duoi (ho tro keyword+page+size+sort)

package vn.edu.crs.courseservice.controller;

import vn.edu.crs.courseservice.dto.CourseDTO;
import vn.edu.crs.courseservice.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    // Buoi 3: thay the getAll() cu (tra List<CourseDTO>) bang search() moi (tra Page<CourseDTO>)
    // Vi du: GET /courses?keyword=java&page=0&size=5&sort=tenMonHoc,asc
    @GetMapping
    public Page<CourseDTO> search(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return courseService.search(keyword, pageable);
    }

    @GetMapping("/{id}")
    public CourseDTO getById(@PathVariable Long id) {
        return courseService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDTO create(@Valid @RequestBody CourseDTO dto) {
        return courseService.create(dto);
    }

    @PutMapping("/{id}")
    public CourseDTO update(@PathVariable Long id, @Valid @RequestBody CourseDTO dto) {
        return courseService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }
}
