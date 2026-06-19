package com.flowmind.test.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("StudentController & SchoolController — Student/School API")
class StudentSchoolControllerTest extends BaseControllerTest {

    @Nested
    @DisplayName("GET /api/students — List students")
    class ListStudents {
        @Test @DisplayName("should return student list")
        void shouldReturnStudentList() throws Exception {
            mockMvc.perform(get("/api/students")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/students — Create student")
    class CreateStudent {
        @Test @DisplayName("should create new student")
        void shouldCreateStudent() throws Exception {
            String body = """
                    {"name":"测试学员","school":"测试大学","major":"金融学","gpa":"3.8","rank":"5/80","english":"雅思7.0","targetSchool":"985","stage":"材料准备","risk":"低"}
                    """;
            mockMvc.perform(post("/api/students")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("should handle empty create body")
        void shouldHandleEmptyCreateBody() throws Exception {
            mockMvc.perform(post("/api/students")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/students/{id} — Get student")
    class GetStudent {
        @Test @DisplayName("should return student by id")
        void shouldReturnStudentById() throws Exception {
            mockMvc.perform(get("/api/students/1")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should handle non-existent student")
        void shouldHandleNonExistentStudent() throws Exception {
            mockMvc.perform(get("/api/students/99999")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/students/{id} — Update student")
    class UpdateStudent {
        @Test @DisplayName("should update student info")
        void shouldUpdateStudent() throws Exception {
            String body = "{\"name\":\"更新后的名字\",\"gpa\":\"3.9\"}";
            mockMvc.perform(put("/api/students/1")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/students/{id} — Delete student")
    class DeleteStudent {
        @Test @DisplayName("should delete student")
        void shouldDeleteStudent() throws Exception {
            mockMvc.perform(delete("/api/students/1")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/students/{id}/analyze — Analyze student")
    class AnalyzeStudent {
        @Test @DisplayName("should return analysis result")
        void shouldReturnAnalysis() throws Exception {
            mockMvc.perform(post("/api/students/1/analyze")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("GET /api/schools — List schools")
    class ListSchools {
        @Test @DisplayName("should return school list")
        void shouldReturnSchoolList() throws Exception {
            mockMvc.perform(get("/api/schools")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/schools — Add school")
    class CreateSchool {
        @Test @DisplayName("should add new school")
        void shouldAddSchool() throws Exception {
            String body = "{\"name\":\"复旦大学\",\"type\":\"985\",\"location\":\"上海\"}";
            mockMvc.perform(post("/api/schools")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/school-projects — List projects")
    class ListProjects {
        @Test @DisplayName("should return projects list")
        void shouldReturnProjects() throws Exception {
            mockMvc.perform(get("/api/school-projects")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/school-projects — Add project")
    class CreateProject {
        @Test @DisplayName("should add new project")
        void shouldAddProject() throws Exception {
            String body = """
                    {"schoolName":"复旦大学","projectName":"经管学院夏令营","deadline":"2026-08-15","requirements":"排名前30%，六级500+","materials":"成绩单、简历、个人陈述"}
                    """;
            mockMvc.perform(post("/api/school-projects")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/schools/recommend — Get recommendations")
    class Recommend {
        @Test @DisplayName("should return top 5 recommended projects")
        void shouldReturnRecommendations() throws Exception {
            String body = "{\"gpa\":\"3.8\",\"major\":\"金融学\",\"target\":\"985\"}";
            mockMvc.perform(post("/api/schools/recommend")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }
    }
}
