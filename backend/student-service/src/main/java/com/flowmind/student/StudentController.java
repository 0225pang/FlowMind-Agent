package com.flowmind.student;

import com.flowmind.common.core.ApiResponse;
import com.flowmind.common.core.IdGenerator;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final List<Map<String, Object>> students = new CopyOnWriteArrayList<>();

    public StudentController() {
        String[] stages = {"初筛", "材料准备", "夏令营报名", "面试中", "拟录取"};
        String[] risks = {"低", "中", "高"};
        for (int i = 1; i <= 20; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "学员" + String.format("%02d", i));
            row.put("school", "示例大学" + ((i % 5) + 1));
            row.put("major", "金融学");
            row.put("gpa", String.format("%.2f", 3.2 + i % 7 * 0.1));
            row.put("rank", (i % 20 + 1) + "/120");
            row.put("english", i % 2 == 0 ? "六级 560" : "雅思 6.5");
            row.put("targetSchool", "985/211 经管项目");
            row.put("stage", stages[i % stages.length]);
            row.put("risk", risks[i % risks.length]);
            row.put("progress", 40 + i % 6 * 10);
            students.add(row);
        }
    }

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.success(students);
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody Map<String, Object> student) {
        student.put("id", IdGenerator.nextId());
        students.add(student);
        return ApiResponse.success(student);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> one(@PathVariable long id) {
        return ApiResponse.success(students.stream()
                .filter(s -> Objects.equals(((Number) s.get("id")).longValue(), id))
                .findFirst()
                .orElse(Map.of()));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable long id, @RequestBody Map<String, Object> student) {
        student.put("id", id);
        students.removeIf(s -> Objects.equals(((Number) s.get("id")).longValue(), id));
        students.add(student);
        return ApiResponse.success(student);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable long id) {
        students.removeIf(s -> Objects.equals(((Number) s.get("id")).longValue(), id));
        return ApiResponse.success();
    }

    @PostMapping("/{id}/analyze")
    public ApiResponse<?> analyze(@PathVariable long id) {
        return ApiResponse.success(Map.of(
                "studentId", id,
                "risk", "中风险",
                "summary", "排名具备竞争力，科研证明偏弱，建议优先匹配截止较晚且重视实践的项目。"
        ));
    }
}
